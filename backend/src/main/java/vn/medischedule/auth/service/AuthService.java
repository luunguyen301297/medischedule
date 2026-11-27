package vn.medischedule.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.auth.domain.Role;
import vn.medischedule.auth.domain.RoleCode;
import vn.medischedule.auth.domain.User;
import vn.medischedule.auth.dto.AuthDtos.LoginRequest;
import vn.medischedule.auth.dto.AuthDtos.RegisterRequest;
import vn.medischedule.auth.dto.AuthDtos.TokenResponse;
import vn.medischedule.auth.dto.AuthDtos.UserResponse;
import vn.medischedule.auth.repository.RoleRepository;
import vn.medischedule.auth.repository.UserRepository;
import vn.medischedule.auth.security.JwtService;
import vn.medischedule.common.exception.ApiException;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

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
