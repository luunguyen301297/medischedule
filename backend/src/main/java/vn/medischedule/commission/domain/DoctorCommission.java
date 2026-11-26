package vn.medischedule.commission.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Thưởng tháng của bác sĩ; month là ngày đầu tháng. */
@Entity
@Table(name = "doctor_commission")
@Getter
@Setter
@NoArgsConstructor
public class DoctorCommission {

    public enum Status { DRAFT, LOCKED }

    public enum DisputeStatus { OPEN, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long doctorId;

    private LocalDate month;

    @ManyToOne(fetch = FetchType.LAZY)
    private CommissionPolicy policy;

    private int visitCount = 0;

    private int designatedCount = 0;

    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    private String disputeNote;

    @Enumerated(EnumType.STRING)
    private DisputeStatus disputeStatus;
}
