package vn.medischedule.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.medischedule.clinic.domain.Room;

import java.util.List;

/** Request/response của module clinic. */
public final class ClinicDtos {

    private ClinicDtos() {}

    public record SpecialtyRequest(
            @NotBlank(message = "Nhập tên chuyên khoa")
            @Size(max = 100, message = "Tên chuyên khoa tối đa 100 ký tự") String name,
            /** users.id của các bác sĩ thuộc khoa; null coi như rỗng. */
            List<Long> doctorUserIds) {}

    /** singleRoomName / singleDoctorName chỉ có khi đúng 1 phòng / 1 bác sĩ; chỉ tính bác sĩ đang hoạt động. */
    public record SpecialtyResponse(Long id, String name, boolean active, long roomCount, String singleRoomName,
                                    List<Long> doctorUserIds, String singleDoctorName) {}

    /** Bác sĩ đang hoạt động cho ô chọn; specialtyId null = chưa có khoa. */
    public record DoctorOption(Long userId, String fullName, Long specialtyId, String specialtyName) {}

    public record RoomRequest(
            @NotBlank(message = "Nhập tên phòng")
            @Size(max = 50, message = "Tên phòng tối đa 50 ký tự") String name,
            @NotNull(message = "Chọn chuyên khoa") Long specialtyId) {}

    /** Chỉ dùng cho phòng thuộc chuyên khoa; phòng dùng chung không đi qua API này. */
    public record RoomResponse(Long id, String name, Long specialtyId, boolean active) {
        public static RoomResponse of(Room r) {
            return new RoomResponse(r.getId(), r.getName(), r.getSpecialty().getId(), r.isActive());
        }
    }

    /** Hồ sơ nhân viên cho admin xem; cột chuyên môn chỉ có với bác sĩ. */
    public record StaffProfile(Long userId, String fullName, String phone, String specialtyName, Long specialtyId, String degree,
                               String licenseNo, boolean acceptsDesignation, boolean active) {}

    /** Cột chuyên môn (specialtyId, degree, licenseNo, acceptsDesignation) chỉ lưu với bác sĩ. */
    public record StaffProfileRequest(
            Long specialtyId,
            @Size(max = 50, message = "Học hàm/học vị tối đa 50 ký tự") String degree,
            @Size(max = 30, message = "Số chứng chỉ tối đa 30 ký tự") String licenseNo,
            boolean acceptsDesignation,
            boolean active) {}

    /** Bác sĩ đang hoạt động cho trang chủ (public); specialtyName null = chưa có khoa. */
    public record PublicDoctor(Long userId, String fullName, String degree, String specialtyName, String avatarUrl) {}

    /** Dữ liệu public cho form đặt lịch. */
    public record SpecialtyOption(Long id, String name) {}
}
