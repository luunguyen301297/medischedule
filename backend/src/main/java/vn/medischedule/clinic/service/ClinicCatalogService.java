package vn.medischedule.clinic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.auth.dto.AuthDtos.DoctorAccount;
import vn.medischedule.auth.service.AuthService;
import vn.medischedule.clinic.domain.Room;
import vn.medischedule.clinic.domain.Specialty;
import vn.medischedule.clinic.domain.Staff;
import vn.medischedule.clinic.dto.ClinicDtos.DoctorOption;
import vn.medischedule.clinic.dto.ClinicDtos.RoomRequest;
import vn.medischedule.clinic.dto.ClinicDtos.RoomResponse;
import vn.medischedule.clinic.dto.ClinicDtos.SpecialtyOption;
import vn.medischedule.clinic.dto.ClinicDtos.SpecialtyRequest;
import vn.medischedule.clinic.dto.ClinicDtos.SpecialtyResponse;
import vn.medischedule.clinic.repository.RoomRepository;
import vn.medischedule.clinic.repository.SpecialtyRepository;
import vn.medischedule.clinic.repository.StaffRepository;
import vn.medischedule.common.exception.ApiException;

import java.text.Collator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Danh mục chuyên khoa và phòng khám. Một service cho cả hai vì rule chéo nhau.
 * ponytail: rule "chuyên khoa ngừng thì không còn phòng hoạt động" kiểm không khóa dòng (chỉ admin sửa danh mục);
 * nhiều người sửa đồng thời thì khóa dòng specialty (PESSIMISTIC_WRITE).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClinicCatalogService {

    private final SpecialtyRepository specialties;
    private final RoomRepository rooms;
    private final StaffRepository staff;
    private final AuthService auth;

    // ---------- Chuyên khoa ----------

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> listSpecialties() {
        Summary summary = summary();
        return specialties.findAll().stream()
                .sorted(byVietnameseName())
                .map(summary::of)
                .toList();
    }

    /** Bác sĩ đang hoạt động kèm khoa hiện tại, sắp A→Z. */
    @Transactional(readOnly = true)
    public List<DoctorOption> doctorOptions() {
        Map<Long, Staff> byUser = staff.findAll().stream()
                .collect(Collectors.toMap(Staff::getUserId, Function.identity()));
        Collator vi = Collator.getInstance(Locale.forLanguageTag("vi"));
        return auth.activeDoctors().stream()
                .map(d -> {
                    Staff st = byUser.get(d.userId());
                    Specialty sp = st == null ? null : st.getSpecialty();
                    return new DoctorOption(d.userId(), d.fullName(),
                            sp == null ? null : sp.getId(), sp == null ? null : sp.getName());
                })
                .sorted(Comparator.comparing(DoctorOption::fullName, vi))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialtyOption> activeSpecialtyOptions() {
        return specialties.findByActiveTrue().stream()
                .sorted(byVietnameseName())
                .map(s -> new SpecialtyOption(s.getId(), s.getName()))
                .toList();
    }

    public SpecialtyResponse createSpecialty(SpecialtyRequest req) {
        String name = req.name().trim();
        if (specialties.existsByNameIgnoreCase(name)) {
            throw specialtyExists();
        }
        Specialty s = new Specialty();
        s.setName(name);
        s = saveUniqueName(specialties, s, specialtyExists());
        assignDoctors(s, req.doctorUserIds());
        return toResponse(s);
    }

    public SpecialtyResponse renameSpecialty(Long id, SpecialtyRequest req) {
        Specialty s = specialty(id);
        String name = req.name().trim();
        // Kiểm trước khi sửa entity: sửa trước thì query sẽ auto-flush và nổ lỗi unique ngoài try
        if (specialties.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw specialtyExists();
        }
        s.setName(name);
        s = saveUniqueName(specialties, s, specialtyExists());
        assignDoctors(s, req.doctorUserIds());
        return toResponse(s);
    }

    public SpecialtyResponse setSpecialtyActive(Long id, boolean active) {
        Specialty s = specialty(id);
        if (!active && rooms.existsBySpecialtyIdAndActiveTrue(id)) {
            throw conflict("SPECIALTY_HAS_ACTIVE_ROOMS", "Hãy ngừng hoặc chuyển hết phòng khám của chuyên khoa trước");
        }
        s.setActive(active);
        return toResponse(s);
    }

    public void deleteSpecialty(Long id) {
        deleteUnused(specialties, specialty(id),
                conflict("SPECIALTY_IN_USE", "Chuyên khoa đang được dùng, không thể xóa. Hãy ngừng hoạt động."));
    }

    // ---------- Phòng khám ----------

    @Transactional(readOnly = true)
    public List<RoomResponse> listRooms(Long specialtyId) {
        specialty(specialtyId);
        return rooms.findBySpecialtyIdOrderByName(specialtyId).stream().map(RoomResponse::of).toList();
    }

    public RoomResponse createRoom(RoomRequest req) {
        Specialty target = specialty(req.specialtyId());
        requireActive(target);
        String name = req.name().trim();
        if (rooms.existsByNameIgnoreCase(name)) {
            throw roomExists();
        }
        Room r = new Room();
        r.setName(name);
        r.setSpecialty(target);
        return RoomResponse.of(saveUniqueName(rooms, r, roomExists()));
    }

    public RoomResponse updateRoom(Long id, RoomRequest req) {
        Room r = room(id);
        Specialty target = r.getSpecialty();
        if (!target.getId().equals(req.specialtyId())) {
            target = specialty(req.specialtyId());
            requireActive(target); // chặn "chuyển phòng vào" bất kể phòng đang hoạt động hay không
        }
        String name = req.name().trim();
        if (rooms.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw roomExists();
        }
        r.setName(name);
        r.setSpecialty(target);
        return RoomResponse.of(saveUniqueName(rooms, r, roomExists()));
    }

    public RoomResponse setRoomActive(Long id, boolean active) {
        Room r = room(id);
        if (active) {
            requireActive(r.getSpecialty());
        }
        r.setActive(active);
        return RoomResponse.of(r);
    }

    public void deleteRoom(Long id) {
        deleteUnused(rooms, room(id),
                conflict("ROOM_IN_USE", "Phòng đã có ca trực, không thể xóa. Hãy ngừng hoạt động."));
    }

    // ---------- Dùng chung ----------

    private Specialty specialty(Long id) {
        return specialties.findById(id)
                .orElseThrow(() -> ApiException.notFound("SPECIALTY_NOT_FOUND", "Chuyên khoa không tồn tại"));
    }

    /** Phòng dùng chung (specialty NULL) ngoài phạm vi, coi như không tồn tại. */
    private Room room(Long id) {
        return rooms.findById(id)
                .filter(r -> r.getSpecialty() != null)
                .orElseThrow(() -> ApiException.notFound("ROOM_NOT_FOUND", "Phòng khám không tồn tại"));
    }

    private static void requireActive(Specialty s) {
        if (!s.isActive()) {
            throw conflict("SPECIALTY_INACTIVE", "Hãy mở lại chuyên khoa trước");
        }
    }

    /** Hai request cùng lúc lọt qua exists… thì index unique chặn ở đây; lỗi ràng buộc khác giữ nguyên. */
    private static <T> T saveUniqueName(JpaRepository<T, Long> repo, T entity, ApiException duplicate) {
        try {
            return repo.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            // uq_*_name_ci và *_name_key
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("_name_")) {
                throw duplicate;
            }
            throw e;
        }
    }

    /** "Đang được dùng" chạm bảng của module khác nên dựa vào FK; flush ngay để lỗi nổ trong try. */
    private static <T> void deleteUnused(JpaRepository<T, Long> repo, T entity, ApiException inUse) {
        try {
            repo.delete(entity);
            repo.flush();
        } catch (DataIntegrityViolationException e) {
            throw inUse;
        }
    }

    /**
     * Bác sĩ của khoa = đúng danh sách ids: bỏ chọn thì về "chưa có khoa", chọn bác sĩ khoa khác thì chuyển sang,
     * chưa có hồ sơ staff thì tạo từ tài khoản. Chỉ nhận bác sĩ đang hoạt động.
     */
    private void assignDoctors(Specialty s, List<Long> doctorUserIds) {
        Set<Long> ids = doctorUserIds == null ? new HashSet<>() : new HashSet<>(doctorUserIds);
        if (ids.contains(null)) {
            throw doctorInvalid();
        }
        Map<Long, String> active = activeDoctorNames();
        if (!active.keySet().containsAll(ids)) {
            throw doctorInvalid();
        }
        for (Staff st : staff.findBySpecialtyId(s.getId())) {
            // Bác sĩ đã khóa / đổi vai trò không hiện trên form nên giữ nguyên khoa, không tự gỡ
            if (active.containsKey(st.getUserId()) && !ids.contains(st.getUserId())) {
                st.setSpecialty(null);
            }
        }
        Map<Long, Staff> existing = staff.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(Staff::getUserId, Function.identity()));
        for (Long userId : ids) {
            Staff st = existing.get(userId);
            if (st == null) {
                st = new Staff();
                st.setUserId(userId);
                st.setFullName(active.get(userId));
                staff.save(st);
            }
            st.setSpecialty(s);
        }
        staff.flush();
    }

    private Map<Long, String> activeDoctorNames() {
        return auth.activeDoctors().stream()
                .collect(Collectors.toMap(DoctorAccount::userId, DoctorAccount::fullName));
    }

    private static ApiException doctorInvalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, "DOCTOR_INVALID", "Bác sĩ không hợp lệ, hãy tải lại danh sách");
    }

    private SpecialtyResponse toResponse(Specialty s) {
        return summary().of(s);
    }

    /**
     * Phòng và bác sĩ (đang hoạt động) của mọi khoa, gom trong bộ nhớ.
     * ponytail: đọc toàn bộ room/staff mỗi lần, ổn ở quy mô vài chục; lớn thì query theo specialtyId.
     */
    private Summary summary() {
        Map<Long, String> active = activeDoctorNames();
        Map<Long, List<Room>> roomsBy = rooms.findBySpecialtyIsNotNull().stream()
                .collect(Collectors.groupingBy(r -> r.getSpecialty().getId()));
        Map<Long, List<Staff>> doctorsBy = staff.findBySpecialtyIsNotNull().stream()
                .filter(st -> active.containsKey(st.getUserId()))
                .collect(Collectors.groupingBy(st -> st.getSpecialty().getId()));
        return new Summary(roomsBy, doctorsBy, active);
    }

    private record Summary(Map<Long, List<Room>> roomsBy, Map<Long, List<Staff>> doctorsBy, Map<Long, String> names) {
        SpecialtyResponse of(Specialty s) {
            List<Room> rs = roomsBy.getOrDefault(s.getId(), List.of());
            List<Staff> ds = doctorsBy.getOrDefault(s.getId(), List.of());
            return new SpecialtyResponse(s.getId(), s.getName(), s.isActive(), rs.size(),
                    rs.size() == 1 ? rs.get(0).getName() : null,
                    ds.stream().map(Staff::getUserId).toList(),
                    ds.size() == 1 ? names.get(ds.get(0).getUserId()) : null);
        }
    }

    /** Sắp A→Z theo tiếng Việt, không phụ thuộc collation của DB. */
    private static Comparator<Specialty> byVietnameseName() {
        Collator vi = Collator.getInstance(Locale.forLanguageTag("vi"));
        return (a, b) -> vi.compare(a.getName(), b.getName());
    }

    private static ApiException specialtyExists() {
        return conflict("SPECIALTY_EXISTS", "Chuyên khoa đã tồn tại");
    }

    private static ApiException roomExists() {
        return conflict("ROOM_EXISTS", "Tên phòng đã tồn tại");
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
