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

    /** Khóa chính; các bảng khác gọi là doctor_id / handled_by / cashier_id... */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** users.id của tài khoản đăng nhập (1-1). */
    private Long userId;

    /** Họ tên đầy đủ. */
    private String fullName;

    /** Số điện thoại. */
    private String phone;

    /** Chuyên khoa của bác sĩ; NULL với lễ tân, dược sĩ, admin. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Specialty specialty;

    /** Học hàm/học vị của bác sĩ (ThS, BSCKI...). */
    private String degree;

    /** Số chứng chỉ hành nghề, duy nhất. */
    private String licenseNo;

    /** Bác sĩ nhận được bệnh nhân chỉ định đích danh (có phụ phí). */
    private boolean acceptsDesignation = false;

    /** false = nghỉ việc, không xếp ca mới. */
    private boolean active = true;
}
