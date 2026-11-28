package vn.medischedule.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.booking.domain.Appointment;
import vn.medischedule.booking.dto.AppointmentDtos.AppointmentRow;
import vn.medischedule.booking.dto.AppointmentDtos.CancelRequest;
import vn.medischedule.booking.dto.AppointmentDtos.NewPatient;
import vn.medischedule.booking.dto.AppointmentDtos.Slot;
import vn.medischedule.booking.dto.AppointmentDtos.SlotGrid;
import vn.medischedule.booking.dto.AppointmentDtos.StaffBookingRequest;
import vn.medischedule.booking.repository.AppointmentRepository;
import vn.medischedule.booking.repository.AppointmentRepository.AppointmentRowView;
import vn.medischedule.clinic.dto.ClinicDtos.BookingDoctorInfo;
import vn.medischedule.clinic.service.ClinicCatalogService;
import vn.medischedule.common.exception.ApiException;
import vn.medischedule.patient.dto.PatientDtos.PatientLite;
import vn.medischedule.patient.service.PatientService;
import vn.medischedule.schedule.dto.ScheduleDtos.ClinicShift;
import vn.medischedule.schedule.dto.ScheduleDtos.WorkShiftResponse;
import vn.medischedule.schedule.service.WorkScheduleService;
import vn.medischedule.system.service.SystemSettingService;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lịch hẹn phía nhân viên (ADMIN, RECEPTIONIST): danh sách, lưới giờ, đặt hộ, hủy.
 * Ca khám (bảng shift) sinh lười từ phân ca làm việc khi có lịch đầu tiên. Thiết kế: docs/design/admin-booking-tech-design.md
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Đặt được hôm nay → hôm nay + 6, giống form đặt lịch của khách. */
    static final int DAYS_AHEAD = 6;
    static final int MAX_LIST_DAYS = 31;
    static final int MAX_PAGE_SIZE = 100;
    static final String MAX_PENDING_KEY = "booking.max_pending_per_phone";
    static final int MAX_PENDING_DEFAULT = 2;
    /** Bỏ 0/O, 1/I cho khách đọc mã qua điện thoại (GĐ-08). */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppointmentRepository appointments;
    private final WorkScheduleService schedule;
    private final ClinicCatalogService catalog;
    private final PatientService patients;
    private final SystemSettingService settings;

    /** Đổi được trong test. */
    Clock clock = Clock.system(CLINIC_ZONE);

    // ---------- Danh sách ----------

    @Transactional(readOnly = true)
    public Page<AppointmentRow> list(LocalDate from, LocalDate to, Long doctorUserId, String status, String q,
                                     int page, int size) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from == null ? today : from;
        LocalDate end = to == null ? start : to;
        if (end.isBefore(start)) {
            throw ApiException.badRequest("RANGE_INVALID", "Ngày kết thúc phải sau ngày bắt đầu");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_LIST_DAYS) {
            throw ApiException.badRequest("RANGE_TOO_LONG", "Chỉ xem tối đa %d ngày một lần".formatted(MAX_LIST_DAYS));
        }
        String st = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!st.isEmpty() && !isStatus(st)) {
            throw ApiException.badRequest("STATUS_INVALID", "Trạng thái không hợp lệ");
        }
        String term = q == null ? "" : q.trim();
        String like = "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String prefix = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String code = term.toUpperCase(Locale.ROOT);
        long doctor = doctorUserId == null ? 0 : doctorUserId;
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageNo = Math.max(page, 0);
        List<AppointmentRow> rows = appointments.searchRows(start, end, doctor, st, term, like, prefix, code,
                pageSize, (long) pageNo * pageSize).stream().map(AppointmentService::toRow).toList();
        long total = appointments.countRows(start, end, doctor, st, term, like, prefix, code);
        return new PageImpl<>(rows, PageRequest.of(pageNo, pageSize), total);
    }

    // ---------- Lưới giờ ----------

    @Transactional(readOnly = true)
    public SlotGrid slots(LocalDate date, Long workShiftId, Long doctorUserId) {
        Context c = context(date, workShiftId, doctorUserId);
        ClinicShift shift = schedule.findClinicShift(c.doctor.staffId(), date, workShiftId)
                .orElseGet(() -> WorkScheduleService.plannedClinicShift(c.workShift, c.doctor.roomId(),
                        c.doctor.slotMinutes()));
        String roomName = c.doctor.roomName();
        if (!shift.roomId().equals(c.doctor.roomId())) {
            roomName = null; // ca khám đã lưu ở phòng khác phòng hiện tại của khoa; web ẩn tên phòng
        }
        Set<LocalTime> taken = takenTimes(c.doctor.staffId(), date);
        LocalTime now = LocalTime.now(clock);
        boolean today = date.equals(LocalDate.now(clock));
        List<Slot> slots = new ArrayList<>();
        for (LocalTime t : grid(shift)) {
            if (today && !t.isAfter(now)) {
                continue; // giờ đã qua không hiện (AC-14)
            }
            slots.add(new Slot(t, !taken.contains(t)));
        }
        return new SlotGrid(c.doctor.fullName(), c.doctor.specialtyName(), roomName, shift.slotMinutes(), slots);
    }

    // ---------- Đặt hộ ----------

    /**
     * Có bác sĩ = lịch chỉ định: bắt buộc giờ trống trong ca khám (sinh lười). Không bác sĩ = chỉ ngày + buổi (+ chuyên
     * khoa), phòng khám xếp bác sĩ và giờ khi check-in (AC-24, AC-25).
     */
    public AppointmentRow createByStaff(Long staffUserId, StaffBookingRequest req) {
        String phone = req.phone().trim();
        NewPatient np = req.newPatient();
        if (req.patientId() == null && np == null) {
            throw ApiException.badRequest("PATIENT_REQUIRED", "Chọn hồ sơ bệnh nhân hoặc nhập thông tin người khám");
        }
        Appointment a = new Appointment();
        a.setVisitDate(req.date());
        a.setWorkShiftId(req.workShiftId());

        if (req.doctorUserId() != null) {
            if (req.startTime() == null) {
                throw ApiException.badRequest("SLOT_REQUIRED", "Chọn giờ khám với bác sĩ đã chỉ định");
            }
            Context c = context(req.date(), req.workShiftId(), req.doctorUserId());
            ClinicShift shift = schedule.lockOrCreateClinicShift(c.doctor.staffId(), req.date(), c.workShift,
                    c.doctor.roomId(), c.doctor.slotMinutes());
            LocalTime time = req.startTime().truncatedTo(ChronoUnit.MINUTES);
            boolean today = req.date().equals(LocalDate.now(clock));
            if (!grid(shift).contains(time) || (today && !time.isAfter(LocalTime.now(clock)))) {
                throw ApiException.badRequest("SLOT_INVALID", "Giờ khám không hợp lệ, vui lòng chọn lại");
            }
            if (appointments.countByShiftIdAndStatusNot(shift.id(), Appointment.Status.CANCELLED) >= shift.maxPatients()) {
                throw new ApiException(HttpStatus.CONFLICT, "SHIFT_FULL", "Buổi khám này đã kín lịch");
            }
            a.setShiftId(shift.id());
            a.setDoctorId(c.doctor.staffId());
            a.setSpecialtyId(c.doctor.specialtyId());
            a.setStartAt(req.date().atTime(time).atZone(CLINIC_ZONE).toOffsetDateTime());
            a.setDesignated(true);
        } else {
            WorkShiftResponse ws = checkDateAndShift(req.date(), req.workShiftId());
            if (req.date().equals(LocalDate.now(clock)) && !ws.endTime().isAfter(LocalTime.now(clock))) {
                throw ApiException.badRequest("SHIFT_ENDED", "Buổi khám này của hôm nay đã kết thúc");
            }
            if (req.specialtyId() != null && catalog.activeSpecialtyOptions().stream()
                    .noneMatch(o -> o.id().equals(req.specialtyId()))) {
                throw ApiException.badRequest("SPECIALTY_INVALID", "Chuyên khoa không còn hoạt động");
            }
            a.setSpecialtyId(req.specialtyId());
        }

        appointments.lockPhone(phone);
        int maxPending = settings.intValue(MAX_PENDING_KEY, MAX_PENDING_DEFAULT);
        if (appointments.countByBookerPhoneAndStatus(phone, Appointment.Status.BOOKED) >= maxPending) {
            throw new ApiException(HttpStatus.CONFLICT, "TOO_MANY_PENDING",
                    "Số điện thoại này đã có %d lịch đang chờ khám".formatted(maxPending));
        }

        PatientLite patient;
        if (req.patientId() != null) {
            patient = patients.requireActive(req.patientId());
            if (!phone.equals(patient.phone())) {
                throw ApiException.badRequest("PATIENT_PHONE_MISMATCH", "Hồ sơ đã chọn không thuộc số điện thoại này");
            }
        } else {
            // Đã giữ khóa theo SĐT (lockPhone) nên hai request cùng khách không tạo hai hồ sơ
            patient = patients.findOrCreateAtReception(np.fullName(), phone, np.dateOfBirth(), np.gender());
        }

        a.setBookingCode(newBookingCode());
        a.setPatientId(patient.id());
        a.setBookerPhone(phone);
        a.setChannel(Appointment.Channel.STAFF);
        a.setCreatedByUserId(staffUserId);
        try {
            a = appointments.saveAndFlush(a);
        } catch (DataIntegrityViolationException e) {
            // Transaction đã hỏng: ném ngay, không truy vấn thêm (TL action item 3)
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("uq_appointment_doctor_slot")) {
                throw new ApiException(HttpStatus.CONFLICT, "SLOT_TAKEN", "Giờ này vừa có người đặt, vui lòng chọn giờ khác");
            }
            throw e;
        }
        return row(a.getId());
    }

    // ---------- Hủy ----------

    public AppointmentRow cancel(Long id, CancelRequest req) {
        Appointment a = appointments.lockById(id)
                .orElseThrow(() -> ApiException.notFound("APPOINTMENT_NOT_FOUND", "Lịch hẹn không tồn tại"));
        if (a.getStatus() != Appointment.Status.BOOKED) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_CANCELLABLE", "Chỉ hủy được lịch đang ở trạng thái Đã đặt");
        }
        a.setStatus(Appointment.Status.CANCELLED);
        a.setCancelReason(req.reason().trim());
        appointments.flush();
        return row(id);
    }

    // ---------- Nội bộ ----------

    private record Context(WorkShiftResponse workShift, BookingDoctorInfo doctor) {}

    /** Kiểm ngày, buổi, phân ca và thông tin bác sĩ dùng chung cho lưới giờ và đặt hộ. */
    private Context context(LocalDate date, Long workShiftId, Long doctorUserId) {
        WorkShiftResponse ws = checkDateAndShift(date, workShiftId);
        BookingDoctorInfo doctor = catalog.bookingDoctor(doctorUserId);
        if (!schedule.isAssigned(date, workShiftId, doctorUserId)) {
            throw ApiException.badRequest("NOT_ON_SHIFT", "Bác sĩ không trực buổi này");
        }
        return new Context(ws, doctor);
    }

    /** Ngày trong [hôm nay, hôm nay + 6] và buổi đang bật. */
    private WorkShiftResponse checkDateAndShift(LocalDate date, Long workShiftId) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || date.isAfter(today.plusDays(DAYS_AHEAD))) {
            throw ApiException.badRequest("DATE_OUT_OF_RANGE", "Chỉ đặt được trong 7 ngày tới");
        }
        return schedule.activeShift(workShiftId);
    }

    /** Giờ bắt đầu các slot: start + k*slot, slot phải kết thúc trong ca. */
    private static List<LocalTime> grid(ClinicShift shift) {
        List<LocalTime> times = new ArrayList<>();
        int slot = shift.slotMinutes();
        for (LocalTime t = shift.startTime();
             !t.plusMinutes(slot).isAfter(shift.endTime()) && times.size() < shift.maxPatients();
             t = t.plusMinutes(slot)) {
            times.add(t);
        }
        return times;
    }

    private Set<LocalTime> takenTimes(Long doctorStaffId, LocalDate date) {
        OffsetDateTime from = date.atStartOfDay(CLINIC_ZONE).toOffsetDateTime();
        OffsetDateTime to = date.plusDays(1).atStartOfDay(CLINIC_ZONE).toOffsetDateTime();
        Set<LocalTime> taken = new HashSet<>();
        for (OffsetDateTime at : appointments.findTakenStartAt(doctorStaffId, from, to)) {
            taken.add(at.atZoneSameInstant(CLINIC_ZONE).toLocalTime().truncatedTo(ChronoUnit.MINUTES));
        }
        return taken;
    }

    private String newBookingCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (!appointments.existsByBookingCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không sinh được mã lịch duy nhất");
    }

    private AppointmentRow row(Long id) {
        return appointments.findRow(id).map(AppointmentService::toRow)
                .orElseThrow(() -> ApiException.notFound("APPOINTMENT_NOT_FOUND", "Lịch hẹn không tồn tại"));
    }

    private static AppointmentRow toRow(AppointmentRowView v) {
        return new AppointmentRow(v.getId(), v.getBookingCode(), LocalDate.parse(v.getVisitDate()), v.getWorkShiftName(),
                v.getWorkShiftStart(), v.getWorkShiftEnd(), clinicTime(v.getStartAt()), clinicTime(v.getEndAt()),
                v.getStatus(), v.getChannel(), Boolean.TRUE.equals(v.getDesignated()), v.getPatientId(), v.getPatientCode(), v.getPatientName(), v.getPhone(),
                v.getDoctorUserId(), v.getDoctorName(), v.getSpecialtyName(), v.getRoomName(), v.getCreatedByName(),
                v.getCancelReason(), clinicTime(v.getCreatedAt()));
    }

    /** Giờ phòng khám (+07:00) cho JSON, không phụ thuộc múi giờ của server. */
    private static OffsetDateTime clinicTime(Instant at) {
        return at == null ? null : at.atZone(CLINIC_ZONE).toOffsetDateTime();
    }

    private static boolean isStatus(String s) {
        for (Appointment.Status st : Appointment.Status.values()) {
            if (st.name().equals(s)) {
                return true;
            }
        }
        return false;
    }
}
