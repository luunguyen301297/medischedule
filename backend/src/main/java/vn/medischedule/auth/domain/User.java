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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Tên đăng nhập, không bắt buộc (vd. "admin"). Đăng nhập được bằng email hoặc username. */
    @Column(unique = true)
    private String username;

    @Column(name = "password_hash")   // NULL khi tài khoản chỉ đăng nhập Google (V5)
    private String passwordHash;

    /** ID tài khoản Google đã liên kết (V5). */
    @Column(name = "google_sub", unique = true)
    private String googleSub;

    /** Hash của token trong link quên mật khẩu (không lưu token gốc). */
    @Column(name = "reset_token_hash")
    private String resetTokenHash;

    @Column(name = "reset_expires_at")
    private OffsetDateTime resetExpiresAt;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

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
