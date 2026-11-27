package vn.medischedule.auth.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.medischedule.auth.domain.User;
import vn.medischedule.auth.dto.AuthDtos.ChangeRoleRequest;
import vn.medischedule.auth.dto.AuthDtos.CreateUserRequest;
import vn.medischedule.auth.dto.AuthDtos.UpdateUserRequest;
import vn.medischedule.auth.dto.AuthDtos.UserResponse;
import vn.medischedule.auth.repository.UserRepository;
import vn.medischedule.auth.service.AuthService;
import vn.medischedule.common.exception.ApiException;

/** Quản lý tài khoản, chỉ ADMIN (chặn cả ở SecurityConfig: /api/admin/**). */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private static final String PATIENT = "PATIENT";

    private final UserRepository users;
    private final AuthService authService;

    /** group: "staff" = mọi vai trò trừ PATIENT, "patient" = chỉ PATIENT; bỏ trống = tất cả. */
    @GetMapping
    public Page<UserResponse> list(@RequestParam(required = false) String role,
                                   @RequestParam(required = false) String group,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id").descending());
        String keyword = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        String roleCode = role == null || role.isBlank() ? null : role;
        String notRole = null;
        if ("patient".equals(group)) roleCode = PATIENT;
        else if ("staff".equals(group)) notRole = PATIENT;
        return users.search(roleCode, notRole, keyword, pageable).map(UserResponse::of);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
        return UserResponse.of(authService.createUser(req.email(), req.username(), req.password(), req.fullName(), req.phone(), req.role()));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return UserResponse.of(authService.updateUser(id, req));
    }

    @PatchMapping("/{id}/role")
    @Transactional
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest req) {
        User u = find(id);
        var newRole = authService.role(req.role());
        // Nhân viên và bệnh nhân là hai loại tài khoản tách biệt, không chuyển qua lại
        if (PATIENT.equals(u.getRole().getCode()) != PATIENT.equals(newRole.getCode())) {
            throw ApiException.badRequest("ROLE_GROUP_CHANGE", "Không thể chuyển tài khoản giữa nhân viên và bệnh nhân");
        }
        u.setRole(newRole);
        return UserResponse.of(u);
    }

    @PatchMapping("/{id}/enabled")
    @Transactional
    public UserResponse setEnabled(@PathVariable Long id, @RequestParam boolean value) {
        User u = find(id);
        u.setEnabled(value);
        return UserResponse.of(u);
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Tài khoản không tồn tại"));
    }
}
