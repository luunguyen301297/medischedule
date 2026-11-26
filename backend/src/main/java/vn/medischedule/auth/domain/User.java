package vn.medischedule.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email đăng nhập, duy nhất. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Tên đăng nhập, không bắt buộc (vd. "admin"). Đăng nhập được bằng email hoặc username. */
    @Column(unique = true)
    private String username;

    /** Mật khẩu đã băm bcrypt. */
    @Column(name = "password_hash")   // NULL khi tài khoản chỉ đăng nhập Google (V5)
    private String passwordHash;

    /** ID tài khoản Google đã liên kết (V5). */
    @Column(name = "google_sub", unique = true)
    private String googleSub;

    /** Hash của token trong link quên mật khẩu (không lưu token gốc). */
    @Column(name = "reset_token_hash")
    private String resetTokenHash;

    /** Hạn của link quên mật khẩu. */
    @Column(name = "reset_expires_at")
    private OffsetDateTime resetExpiresAt;

    /** Token Firebase Cloud Messaging của app để gửi push. */
    @Column(name = "fcm_token")
    private String fcmToken;

    /** Tên hiển thị của tài khoản. */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Số điện thoại liên hệ. */
    private String phone;

    /** Vai trò duy nhất của tài khoản; quyết định hasRole(...). */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** false = tài khoản bị khóa, không đăng nhập được. */
    @Column(nullable = false)
    private boolean enabled = true;

    /** Lần đăng nhập thành công gần nhất. */
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /** Thời điểm tạo tài khoản. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Thời điểm sửa gần nhất. */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
