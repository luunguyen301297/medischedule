package vn.medischedule.auth.security;

/**
 * Người dùng đang đăng nhập, lấy từ JWT (không truy vấn DB mỗi request).
 * Lấy trong controller: @AuthenticationPrincipal AuthUser me
 */
public record AuthUser(Long id, String email, String role) {}
