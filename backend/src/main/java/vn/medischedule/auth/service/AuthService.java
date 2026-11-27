package vn.medischedule.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.auth.domain.Role;
import vn.medischedule.auth.domain.RoleCode;
import vn.medischedule.auth.domain.User;
import vn.medischedule.auth.dto.AuthDtos.DoctorAccount;
import vn.medischedule.auth.dto.AuthDtos.LoginRequest;
import vn.medischedule.auth.dto.AuthDtos.RegisterRequest;
import vn.medischedule.auth.dto.AuthDtos.TokenResponse;
import vn.medischedule.auth.dto.AuthDtos.UpdateUserRequest;
import vn.medischedule.auth.dto.AuthDtos.UserUpdatedEvent;
import vn.medischedule.auth.dto.AuthDtos.UserResponse;
import vn.medischedule.auth.repository.RoleRepository;
import vn.medischedule.auth.repository.UserRepository;
import vn.medischedule.auth.security.JwtService;
import vn.medischedule.common.exception.ApiException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final ApplicationEventPublisher events;

    /** Đăng nhập bằng email, số điện thoại hoặc tên đăng nhập. */
    @Transactional
    public TokenResponse login(LoginRequest req) {
        String login = req.login().trim();
        Optional<User> found;
        if (login.contains("@")) {
            found = users.findByEmailIgnoreCase(login);
        } else if (login.matches("\\+?\\d+")) {
            // Tên đăng nhập cũng có thể toàn số: không khớp SĐT thì thử tiếp tên đăng nhập
            found = users.findByPhone(login).or(() -> users.findByUsernameIgnoreCase(login));
        } else {
            found = users.findByUsernameIgnoreCase(login);
        }
        User user = found
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("BAD_CREDENTIALS", "Tài khoản hoặc mật khẩu không đúng"));
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Tài khoản đã bị khóa");
        }
        user.setLastLoginAt(OffsetDateTime.now());
        return token(user);
    }

    /** Tài khoản vai trò DOCTOR chưa bị khóa. */
    @Transactional(readOnly = true)
    public List<DoctorAccount> activeDoctors() {
        return users.findByRole_CodeAndEnabledTrue(RoleCode.DOCTOR).stream()
                .map(u -> new DoctorAccount(u.getId(), u.getFullName()))
                .toList();
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        User user = createUser(req.email(), null, req.password(), req.fullName(), req.phone(), RoleCode.PATIENT);
        return token(user);
    }

    /** Dùng chung cho đăng ký bệnh nhân, admin tạo nhân viên và khởi tạo admin. */
    @Transactional
    public User createUser(String email, String username, String rawPassword, String fullName, String phone, String roleCode) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email đã được sử dụng");
        }
        String uname = username == null || username.isBlank() ? null : username.trim().toLowerCase();
        if (uname != null && users.existsByUsernameIgnoreCase(uname)) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_EXISTS", "Tên đăng nhập đã được sử dụng");
        }
        String ph = phone == null || phone.isBlank() ? null : phone.trim();
        if (ph != null && users.existsByPhone(ph)) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_EXISTS", "Số điện thoại đã được sử dụng");
        }
        User u = new User();
        u.setEmail(normalized);
        u.setUsername(uname);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setFullName(fullName.trim());
        u.setPhone(ph);
        u.setRole(role(roleCode));
        return users.save(u);
    }

    /** Tài khoản theo id, cho module khác đọc. */
    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.of(find(id));
    }

    /** Admin sửa họ tên, email, tên đăng nhập (chỉ nhân viên), SĐT (bắt buộc với bệnh nhân vì dùng đăng nhập). */
    @Transactional
    public User updateUser(Long id, UpdateUserRequest req) {
        User u = find(id);
        boolean patient = RoleCode.PATIENT.equals(u.getRole().getCode());
        String email = req.email().trim().toLowerCase();
        if (!email.equalsIgnoreCase(u.getEmail()) && users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email đã được sử dụng");
        }
        String uname = patient ? u.getUsername()
                : req.username() == null || req.username().isBlank() ? null : req.username().trim().toLowerCase();
        if (uname != null && !uname.equalsIgnoreCase(u.getUsername()) && users.existsByUsernameIgnoreCase(uname)) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_EXISTS", "Tên đăng nhập đã được sử dụng");
        }
        String ph = req.phone() == null || req.phone().isBlank() ? null : req.phone().trim();
        if (patient && ph == null) {
            throw ApiException.badRequest("PHONE_REQUIRED", "Bệnh nhân cần số điện thoại để đăng nhập");
        }
        if (ph != null && !ph.equals(u.getPhone()) && users.existsByPhone(ph)) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_EXISTS", "Số điện thoại đã được sử dụng");
        }
        u.setEmail(email);
        u.setUsername(uname);
        u.setFullName(req.fullName().trim());
        u.setPhone(ph);
        events.publishEvent(new UserUpdatedEvent(u.getId(), u.getFullName(), ph));
        return u;
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Tài khoản không tồn tại"));
    }

    public Role role(String code) {
        return roles.findByCode(code)
                .orElseThrow(() -> ApiException.badRequest("ROLE_NOT_FOUND", "Vai trò không tồn tại: " + code));
    }

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    private TokenResponse token(User u) {
        return new TokenResponse(jwt.issue(u), "Bearer", jwt.ttlSeconds(), UserResponse.of(u));
    }
}
