package vn.medischedule.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.medischedule.auth.domain.User;

import java.time.OffsetDateTime;

/** Request/response của module auth. */
public final class AuthDtos {

    private AuthDtos() {}

    /** login: email, số điện thoại hoặc tên đăng nhập. */
    public record LoginRequest(
            @NotBlank String login,
            @NotBlank String password) {}

    /** Bệnh nhân tự đăng ký; luôn nhận vai trò PATIENT. SĐT bắt buộc vì dùng để đăng nhập. */
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 số, bắt đầu bằng 0") String phone) {}

    /** Admin tạo tài khoản nhân viên với vai trò chỉ định. */
    public record CreateUserRequest(
            @NotBlank @Email String email,
            @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "Tên đăng nhập 3–50 ký tự: chữ, số, dấu chấm, gạch") String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 15) String phone,
            @NotBlank String role) {}

    /** Admin sửa thông tin tài khoản; username bị bỏ qua với tài khoản bệnh nhân. */
    public record UpdateUserRequest(
            @NotBlank @Email String email,
            @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "Tên đăng nhập 3–50 ký tự: chữ, số, dấu chấm, gạch") String username,
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 số, bắt đầu bằng 0") String phone) {}

    /** Phát khi admin sửa tài khoản, để module khác đồng bộ bản sao họ tên / SĐT (vd. clinic.staff). */
    public record UserUpdatedEvent(Long userId, String fullName, String phone) {}

    public record ChangeRoleRequest(@NotBlank String role) {}

    public record UserResponse(Long id, String email, String username, String fullName, String phone, String role,
                               boolean enabled, OffsetDateTime lastLoginAt, OffsetDateTime createdAt) {
        public static UserResponse of(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getUsername(), u.getFullName(), u.getPhone(),
                    u.getRole().getCode(), u.isEnabled(), u.getLastLoginAt(), u.getCreatedAt());
        }
    }

    /** Tài khoản bác sĩ đang hoạt động, cho module khác (clinic) đọc qua AuthService. */
    public record DoctorAccount(Long userId, String fullName) {}

    /** Tài khoản nhân viên (mọi vai trò trừ PATIENT), kể cả đã khóa. */
    public record StaffAccount(Long userId, String fullName, String role, boolean enabled) {}

    public record TokenResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {}
}
