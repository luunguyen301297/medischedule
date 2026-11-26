package vn.medischedule.billing.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Bảng giá: phí khám, cận lâm sàng, phụ phí chỉ định bác sĩ. Đặt tên MedicalService để khỏi trùng @Service. */
@Entity
@Table(name = "service")
@Getter
@Setter
@NoArgsConstructor
public class MedicalService {

    public enum Type { CONSULT, LAB, DESIGNATION_FEE }

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên dịch vụ. */
    private String name;

    /** CONSULT (phí khám), LAB (cận lâm sàng), DESIGNATION_FEE (phụ phí chỉ định bác sĩ). */
    @Enumerated(EnumType.STRING)
    private Type type;

    /** specialty.id nếu dịch vụ thuộc một chuyên khoa. */
    private Long specialtyId;

    /** Giá hiện hành (VND); lịch sử đổi giá ghi audit_log. */
    private BigDecimal price;

    /** false = ngừng cung cấp. */
    private boolean active = true;
}
