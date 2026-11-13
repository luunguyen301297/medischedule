package vn.medischedule.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.auth.domain.RoleCode;
import vn.medischedule.auth.repository.UserRepository;
import vn.medischedule.common.config.AppProperties;

/**
 * Bảo đảm có tài khoản quản trị (ADMIN_USERNAME / ADMIN_PASSWORD, mặc định admin / admin@123).
 * - Đã có user tên "admin": không đụng tới (mật khẩu đổi sau đó được giữ nguyên).
 * - Có admin cũ theo ADMIN_EMAIL nhưng chưa có username: gán username và đặt mật khẩu theo cấu hình.
 * - Chưa có gì: tạo mới.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AppProperties props;
    private final UserRepository users;
    private final AuthService authService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var admin = props.admin();
        if (admin.password() == null || admin.password().isBlank()) {
            log.warn("ADMIN_PASSWORD trống, bỏ qua khởi tạo tài khoản quản trị");
            return;
        }
        String username = admin.username().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) {
            return;
        }
        users.findByEmailIgnoreCase(admin.email()).ifPresentOrElse(u -> {
            u.setUsername(username);
            u.setPasswordHash(authService.encode(admin.password()));
            u.setRole(authService.role(RoleCode.ADMIN));
            log.info("Đã gán tên đăng nhập '{}' cho tài khoản quản trị {}", username, admin.email());
        }, () -> {
            authService.createUser(admin.email(), username, admin.password(), "Quản trị viên", null, RoleCode.ADMIN);
            log.info("Đã tạo tài khoản quản trị '{}'", username);
        });
    }
}
