package vn.medischedule.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.auth.domain.RoleCode;
import vn.medischedule.auth.dto.AuthDtos.StaffAccount;
import vn.medischedule.auth.service.AuthService;
import vn.medischedule.clinic.dto.ClinicDtos.DoctorOption;
import vn.medischedule.clinic.service.ClinicCatalogService;
import vn.medischedule.common.exception.ApiException;
import vn.medischedule.schedule.domain.Shift;
import vn.medischedule.schedule.domain.WorkAssignment;
import vn.medischedule.schedule.domain.WorkShift;
import vn.medischedule.schedule.dto.ScheduleDtos.AssignRequest;
import vn.medischedule.schedule.dto.ScheduleDtos.AssignmentResponse;
import vn.medischedule.schedule.dto.ScheduleDtos.ClinicShift;
import vn.medischedule.schedule.dto.ScheduleDtos.StaffOption;
import vn.medischedule.schedule.dto.ScheduleDtos.WorkShiftRequest;
import vn.medischedule.schedule.dto.ScheduleDtos.WorkShiftResponse;
import vn.medischedule.schedule.repository.ShiftRepository;
import vn.medischedule.schedule.repository.WorkAssignmentRepository;
import vn.medischedule.schedule.repository.WorkShiftRepository;

import java.text.Collator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ca làm việc chuẩn và phân ca nhân viên theo ngày.
 * ponytail: kiểm chồng giờ không khóa dòng (chỉ admin phân ca); cần chặn tuyệt đối thì exclusion constraint.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkScheduleService {

    static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Lưới tháng 6 tuần. */
    static final int MAX_RANGE_DAYS = 42;

    private final WorkShiftRepository shifts;
    private final ShiftRepository clinicShifts;
    private final WorkAssignmentRepository assignments;
    private final AuthService auth;
    private final ClinicCatalogService catalog;

    // ---------- Ca khám (bảng shift) cho đặt lịch ----------

    /** Ca làm việc đang bật; không có hoặc đã tắt → SHIFT_INVALID. */
    @Transactional(readOnly = true)
    public WorkShiftResponse activeShift(Long workShiftId) {
        return shifts.findById(workShiftId).filter(WorkShift::isActive).map(WorkShiftResponse::of)
                .orElseThrow(() -> ApiException.badRequest("SHIFT_INVALID", "Buổi khám không còn mở"));
    }

    @Transactional(readOnly = true)
    public boolean isAssigned(LocalDate date, Long workShiftId, Long userId) {
        return assignments.existsByWorkDateAndWorkShiftIdAndUserId(date, workShiftId, userId);
    }

    /** Ca khám đã lưu của bác sĩ (staff.id) trong ngày + ca làm việc, nếu có. */
    @Transactional(readOnly = true)
    public Optional<ClinicShift> findClinicShift(Long doctorStaffId, LocalDate date, Long workShiftId) {
        return clinicShifts.findByDoctorIdAndWorkDateAndWorkShiftId(doctorStaffId, date, workShiftId)
                .map(WorkScheduleService::toClinicShift);
    }

    /** Ca dự kiến (chưa lưu) từ ca làm việc + thời gian khám của khoa; số khách tối đa = phút ca / slot. */
    public static ClinicShift plannedClinicShift(WorkShiftResponse ws, Long roomId, int slotMinutes) {
        long minutes = ChronoUnit.MINUTES.between(ws.startTime(), ws.endTime());
        return new ClinicShift(null, roomId, ws.startTime(), ws.endTime(), slotMinutes, (int) (minutes / slotMinutes));
    }

    /**
     * Lấy hoặc tạo ca khám rồi khóa dòng tới hết transaction (sinh lười lúc đặt lịch, GĐ-05). Ca đã có giữ nguyên phòng
     * và độ dài slot cũ (AC-18).
     */
    public ClinicShift lockOrCreateClinicShift(Long doctorStaffId, LocalDate date, WorkShiftResponse ws, Long roomId,
                                               int slotMinutes) {
        ClinicShift planned = plannedClinicShift(ws, roomId, slotMinutes);
        if (planned.maxPatients() < 1) {
            throw ApiException.badRequest("SHIFT_TOO_SHORT", "Ca làm việc ngắn hơn thời gian khám một lượt");
        }
        clinicShifts.insertIfAbsent(doctorStaffId, roomId, date, ws.startTime().toString(), ws.endTime().toString(),
                (short) slotMinutes, (short) planned.maxPatients(), ws.id());
        return clinicShifts.lockByDoctorAndDateAndWorkShift(doctorStaffId, date, ws.id())
                .map(WorkScheduleService::toClinicShift)
                // Không chèn được vì trùng (bác sĩ, ngày, giờ bắt đầu) với một ca khám khác
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "SHIFT_CONFLICT",
                        "Bác sĩ đã có ca khám khác trùng giờ bắt đầu trong ngày"));
    }

    private static ClinicShift toClinicShift(Shift s) {
        return new ClinicShift(s.getId(), s.getRoomId(), s.getStartTime(), s.getEndTime(), s.getSlotMinutes(),
                s.getMaxPatients());
    }

    // ---------- Ca làm việc ----------

    @Transactional(readOnly = true)
    public List<WorkShiftResponse> listShifts() {
        return shifts.findAllByOrderByStartTimeAscNameAsc().stream().map(WorkShiftResponse::of).toList();
    }

    /** Ca đang bật cho form đặt lịch public. */
    @Transactional(readOnly = true)
    public List<WorkShiftResponse> activeShifts() {
        return shifts.findAllByOrderByStartTimeAscNameAsc().stream()
                .filter(WorkShift::isActive).map(WorkShiftResponse::of).toList();
    }

    /** Bác sĩ đang hoạt động được phân vào ca của ngày; specialtyId null = mọi khoa. A→Z. */
    @Transactional(readOnly = true)
    public List<DoctorOption> doctorsOnShift(LocalDate date, Long shiftId, Long specialtyId) {
        Set<Long> onShift = assignments.findByWorkDateAndWorkShiftId(date, shiftId).stream()
                .map(WorkAssignment::getUserId).collect(Collectors.toSet());
        if (onShift.isEmpty()) {
            return List.of();
        }
        return catalog.doctorOptions().stream()
                .filter(d -> onShift.contains(d.userId()))
                .filter(d -> specialtyId == null || specialtyId.equals(d.specialtyId()))
                .toList();
    }

    public WorkShiftResponse createShift(WorkShiftRequest req) {
        String name = req.name().trim();
        if (shifts.existsByNameIgnoreCase(name)) {
            throw shiftExists();
        }
        WorkShift s = new WorkShift();
        apply(s, name, req);
        return WorkShiftResponse.of(save(s));
    }

    public WorkShiftResponse updateShift(Long id, WorkShiftRequest req) {
        WorkShift s = shift(id);
        String name = req.name().trim();
        // Kiểm trước khi sửa entity: sửa trước thì query auto-flush và nổ lỗi unique ngoài try
        if (shifts.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw shiftExists();
        }
        apply(s, name, req);
        return WorkShiftResponse.of(save(s));
    }

    public WorkShiftResponse setShiftActive(Long id, boolean active) {
        WorkShift s = shift(id);
        s.setActive(active);
        return WorkShiftResponse.of(s);
    }

    /** Ca đã được phân thì FK chặn; flush ngay để lỗi nổ trong try. */
    public void deleteShift(Long id) {
        try {
            shifts.delete(shift(id));
            shifts.flush();
        } catch (DataIntegrityViolationException e) {
            throw conflict("WORK_SHIFT_IN_USE", "Ca đã được phân lịch, không thể xóa. Hãy tắt ca.");
        }
    }

    private static void apply(WorkShift s, String name, WorkShiftRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw ApiException.badRequest("INVALID_TIME_RANGE", "Giờ kết thúc phải sau giờ bắt đầu");
        }
        s.setName(name);
        s.setStartTime(req.startTime());
        s.setEndTime(req.endTime());
        s.setColor(req.color().toLowerCase(Locale.ROOT));
    }

    private WorkShift save(WorkShift s) {
        try {
            return shifts.saveAndFlush(s);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("uq_work_shift_name_ci")) {
                throw shiftExists();
            }
            throw e;
        }
    }

    // ---------- Lịch làm việc ----------

    @Transactional(readOnly = true)
    public List<AssignmentResponse> schedule(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw ApiException.badRequest("INVALID_RANGE", "Khoảng ngày không hợp lệ (tối đa " + MAX_RANGE_DAYS + " ngày)");
        }
        Map<Long, StaffAccount> staff = staffById();
        return assignments.findByWorkDateBetween(from, to).stream()
                .map(a -> toResponse(a, staff.get(a.getUserId())))
                .sorted(Comparator.comparing(AssignmentResponse::date)
                        .thenComparing(AssignmentResponse::fullName, Collator.getInstance(Locale.forLanguageTag("vi"))))
                .toList();
    }

    /** Nhân viên phân ca được (đang hoạt động, không phải quản trị viên), A→Z. */
    @Transactional(readOnly = true)
    public List<StaffOption> staffOptions() {
        Collator vi = Collator.getInstance(Locale.forLanguageTag("vi"));
        return auth.staffAccounts().stream()
                .filter(WorkScheduleService::schedulable)
                .map(a -> new StaffOption(a.userId(), a.fullName(), a.role()))
                .sorted(Comparator.comparing(StaffOption::fullName, vi))
                .toList();
    }

    /** Gán nhân viên vào ca của ngày; người đã có trong ca được bỏ qua. Trả các phân ca mới tạo. */
    public List<AssignmentResponse> assign(AssignRequest req) {
        requireNotPast(req.date());
        WorkShift target = shift(req.shiftId());
        if (!target.isActive()) {
            throw conflict("WORK_SHIFT_INACTIVE", "Ca đã tắt, không phân lịch mới được");
        }
        Map<Long, StaffAccount> staff = staffById();
        Set<Long> ids = new LinkedHashSet<>(req.userIds());
        for (Long id : ids) {
            StaffAccount a = staff.get(id);
            if (a == null || !schedulable(a)) {
                throw ApiException.badRequest("INVALID_STAFF", "Nhân viên không tồn tại, đã bị khóa hoặc là quản trị viên");
            }
        }
        for (WorkAssignment existing : assignments.findByWorkDateAndUserIdIn(req.date(), ids)) {
            WorkShift other = existing.getWorkShift();
            if (other.getId().equals(target.getId())) {
                ids.remove(existing.getUserId());
            } else if (other.overlaps(target)) {
                throw conflict("SHIFT_OVERLAP", staff.get(existing.getUserId()).fullName()
                        + " đã làm ca " + other.getName() + " trùng giờ trong ngày này");
            }
        }
        List<WorkAssignment> created = ids.stream().map(id -> {
            WorkAssignment a = new WorkAssignment();
            a.setWorkDate(req.date());
            a.setWorkShift(target);
            a.setUserId(id);
            return a;
        }).toList();
        try {
            return assignments.saveAllAndFlush(created).stream()
                    .map(a -> toResponse(a, staff.get(a.getUserId())))
                    .toList();
        } catch (DataIntegrityViolationException e) {
            // Hai request cùng lúc lọt qua bước bỏ trùng
            throw conflict("ALREADY_ASSIGNED", "Nhân viên đã có trong ca này, hãy tải lại lịch");
        }
    }

    public void unassign(Long id) {
        WorkAssignment a = assignments.findById(id)
                .orElseThrow(() -> ApiException.notFound("ASSIGNMENT_NOT_FOUND", "Phân ca không tồn tại"));
        requireNotPast(a.getWorkDate());
        assignments.delete(a);
    }

    // ---------- Dùng chung ----------

    /** Quản trị viên không đi ca. */
    private static boolean schedulable(StaffAccount a) {
        return a.enabled() && !RoleCode.ADMIN.equals(a.role());
    }

    private static void requireNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now(CLINIC_ZONE))) {
            throw ApiException.badRequest("PAST_DATE", "Không sửa được lịch của ngày đã qua");
        }
    }

    private Map<Long, StaffAccount> staffById() {
        return auth.staffAccounts().stream().collect(Collectors.toMap(StaffAccount::userId, Function.identity()));
    }

    private static AssignmentResponse toResponse(WorkAssignment a, StaffAccount who) {
        return new AssignmentResponse(a.getId(), a.getWorkDate(), a.getWorkShift().getId(), a.getUserId(),
                who == null ? "(không rõ)" : who.fullName(), who == null ? null : who.role());
    }

    private WorkShift shift(Long id) {
        return shifts.findById(id)
                .orElseThrow(() -> ApiException.notFound("WORK_SHIFT_NOT_FOUND", "Ca làm việc không tồn tại"));
    }

    private static ApiException shiftExists() {
        return conflict("WORK_SHIFT_EXISTS", "Tên ca đã tồn tại");
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
