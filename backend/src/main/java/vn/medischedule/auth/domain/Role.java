package vn.medischedule.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Vai trò. Danh sách cố định, seed trong V1__auth_roles_users.sql. */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
public class Role {

    /** Khóa cố định: 1 ADMIN, 2 DOCTOR, 3 RECEPTIONIST, 4 PHARMACIST, 5 PATIENT (seed ở V1). */
    @Id
    private Short id;

    /** Mã vai trò dùng trong hasRole('...'), xem RoleCode. */
    @Column(nullable = false, unique = true)
    private String code;

    /** Tên hiển thị tiếng Việt. */
    @Column(nullable = false)
    private String name;
}
