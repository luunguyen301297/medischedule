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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Type type;

    private Long specialtyId;

    private BigDecimal price;

    private boolean active = true;
}
