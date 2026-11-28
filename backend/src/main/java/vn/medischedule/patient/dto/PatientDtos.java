package vn.medischedule.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.medischedule.patient.domain.Patient;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Request/response của module patient. */
public final class PatientDtos {

    private PatientDtos() {}

    /** Ảnh hồ sơ: rỗng, https://… hoặc ảnh đã tải lên (/api/public/files/…); chặn javascript:, data:. */
    public static final String AVATAR_URL = "^((https?://|/api/public/files/)\\S+)?$";

    public record PatientProfile(Long id, String patientCode, String relation, String fullName, LocalDate dateOfBirth,
                                 String gender, String nationalId, String phone, String address, String bhytNumber,
                                 LocalDate bhytValidTo, Short bhytRate, String allergyNote, String source,
                                 OffsetDateTime createdAt, String avatarUrl) {
        public static PatientProfile of(Patient p) {
            return new PatientProfile(p.getId(), p.getPatientCode(), p.getRelation().name(), p.getFullName(),
                    p.getDateOfBirth(), p.getGender(), p.getNationalId(), p.getPhone(), p.getAddress(),
                    p.getBhytNumber(), p.getBhytValidTo(), p.getBhytRate(), p.getAllergyNote(),
                    p.getSource() == null ? null : p.getSource().name(), p.getCreatedAt(), p.getAvatarUrl());
        }
    }

    /** Admin tạo/sửa hồ sơ đi khám. ownerUserId chỉ dùng khi tạo. bhytRate: 80, 95, 100 hoặc null. */
    public record PatientRequest(
            Long ownerUserId,
            @NotNull(message = "Chọn quan hệ") Patient.Relation relation,
            @NotBlank(message = "Nhập họ tên") @Size(max = 120, message = "Họ tên tối đa 120 ký tự") String fullName,
            LocalDate dateOfBirth,
            @Size(max = 6) String gender,
            @Pattern(regexp = "^(\\d{12})?$", message = "CCCD gồm 12 số") String nationalId,
            @NotBlank(message = "Nhập số điện thoại")
            @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 số, bắt đầu bằng 0") String phone,
            @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự") String address,
            @Size(max = 15, message = "Số thẻ BHYT tối đa 15 ký tự") String bhytNumber,
            LocalDate bhytValidTo,
            Short bhytRate,
            String allergyNote,
            @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
            @Pattern(regexp = AVATAR_URL, message = "Ảnh phải là đường dẫn http(s):// hoặc ảnh đã tải lên") String avatarUrl) {}
}
