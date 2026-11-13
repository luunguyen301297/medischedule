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

    @Id
    private Short id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;
}
