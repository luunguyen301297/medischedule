package vn.medischedule.auth.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.medischedule.auth.dto.AuthDtos.LoginRequest;
import vn.medischedule.auth.dto.AuthDtos.RegisterRequest;
import vn.medischedule.auth.dto.AuthDtos.TokenResponse;
import vn.medischedule.auth.dto.AuthDtos.UserResponse;
import vn.medischedule.auth.repository.UserRepository;
import vn.medischedule.auth.security.AuthUser;
import vn.medischedule.auth.service.AuthService;
import vn.medischedule.common.exception.ApiException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return users.findById(me.id())
                .map(UserResponse::of)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Tài khoản không tồn tại"));
    }
}
