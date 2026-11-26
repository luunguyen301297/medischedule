package vn.medischedule.clinic.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Hồ sơ nhân viên; các cột chuyên môn chỉ dùng cho bác sĩ. userId trỏ users (module auth). */
@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String fullName;

    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    private Specialty specialty;

    private String degree;

    private String licenseNo;

    private boolean acceptsDesignation = false;

    private boolean active = true;
}
