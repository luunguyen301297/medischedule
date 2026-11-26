package vn.medischedule.commission.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Chính sách thưởng theo ngày hiệu lực. */
@Entity
@Table(name = "commission_policy")
@Getter
@Setter
@NoArgsConstructor
public class CommissionPolicy {

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ngày bắt đầu hiệu lực, duy nhất; tháng dùng chính sách mới nhất có hiệu lực. */
    private LocalDate effectiveFrom;

    /** Thưởng mỗi lượt khám (VND). */
    private BigDecimal perVisitAmount;

    /** Phần trăm phí khám được hưởng (0–100). */
    private BigDecimal consultPercent;

    /** Thưởng mỗi lượt được chỉ định đích danh (VND). */
    private BigDecimal designationBonus;
}
