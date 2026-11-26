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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate effectiveFrom;

    private BigDecimal perVisitAmount;

    private BigDecimal consultPercent;

    private BigDecimal designationBonus;
}
