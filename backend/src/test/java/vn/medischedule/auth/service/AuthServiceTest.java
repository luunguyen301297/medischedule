package vn.medischedule.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import vn.medischedule.auth.domain.Role;
import vn.medischedule.auth.domain.User;
import vn.medischedule.auth.dto.AuthDtos.LoginRequest;
import vn.medischedule.auth.repository.RoleRepository;
import vn.medischedule.auth.repository.UserRepository;
import vn.medischedule.auth.security.JwtService;
import vn.medischedule.common.exception.ApiException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Unit test (không Spring, không DB): tra tài khoản khi đăng nhập và chặn trùng SĐT. */
class AuthServiceTest {

    UserRepository users = mock(UserRepository.class);
    JwtService jwt = mock(JwtService.class);
    AuthService service = new AuthService(users, mock(RoleRepository.class), NoOpPasswordEncoder.getInstance(), jwt);
    User user = new User();

    @BeforeEach
    void setUp() {
        Role role = mock(Role.class);
        when(role.getCode()).thenReturn("PATIENT");
        user.setRole(role);
        user.setPasswordHash("secret123");
        user.setEnabled(true);
        when(jwt.issue(any())).thenReturn("token");
        when(users.findByPhone(any())).thenReturn(Optional.empty());
        when(users.findByUsernameIgnoreCase(any())).thenReturn(Optional.empty());
    }

    @Test
    void loginByPhone() {
        when(users.findByPhone("0912345678")).thenReturn(Optional.of(user));
        assertThat(service.login(new LoginRequest(" 0912345678 ", "secret123")).accessToken()).isEqualTo("token");
    }

    @Test
    void numericUsernameFallsBackWhenNoPhoneMatches() {
        when(users.findByUsernameIgnoreCase("12345")).thenReturn(Optional.of(user));
        assertThat(service.login(new LoginRequest("12345", "secret123")).accessToken()).isEqualTo("token");
        verify(users).findByPhone("12345");
    }

    @Test
    void wrongPasswordByPhoneIsRejected() {
        when(users.findByPhone("0912345678")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.login(new LoginRequest("0912345678", "wrong")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void duplicatePhoneIsRejected() {
        when(users.existsByPhone("0912345678")).thenReturn(true);
        assertThatThrownBy(() -> service.createUser("a@b.vn", null, "secret123", "A", "0912345678", "PATIENT"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Số điện thoại");
        verify(users, never()).save(any());
    }
}
