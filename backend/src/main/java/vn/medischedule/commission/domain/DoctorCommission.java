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

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** staff.id của bác sĩ. */
    private Long doctorId;

    /** Ngày đầu tháng tính thưởng; mỗi bác sĩ một dòng mỗi tháng. */
    private LocalDate month;

    /** Chính sách áp dụng cho tháng này. */
    @ManyToOne(fetch = FetchType.LAZY)
    private CommissionPolicy policy;

    /** Số lượt khám đã thanh toán trong tháng. */
    private int visitCount = 0;

    /** Số lượt được chỉ định đích danh. */
    private int designatedCount = 0;

    /** Tổng thưởng (VND). */
    private BigDecimal amount = BigDecimal.ZERO;

    /** DRAFT (còn tính lại được) hoặc LOCKED (đã chốt). */
    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    /** Nội dung bác sĩ yêu cầu đối soát. */
    private String disputeNote;

    /** OPEN / RESOLVED; NULL = không có khiếu nại. */
    @Enumerated(EnumType.STRING)
    private DisputeStatus disputeStatus;
}
