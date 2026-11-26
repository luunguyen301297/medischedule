package vn.medischedule.pharmacy.domain;

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

/** Dòng thuốc; dược sĩ đề xuất thuốc thay thế, bác sĩ duyệt. */
@Entity
@Table(name = "prescription_item")
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionItem {

    public enum SubstituteStatus { PENDING, APPROVED, REJECTED }

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Đơn thuốc chứa dòng này. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Prescription prescription;

    /** Thuốc bác sĩ kê. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Drug drug;

    /** Tổng số lượng cấp. */
    private int quantity;

    /** Liều dùng (vd. Sáng 1, tối 1). */
    private String dosage;

    /** Số ngày dùng. */
    private short days;

    /** Thuốc thay thế dược sĩ đề xuất khi hết hàng. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Drug substituteDrug;

    /** Trạng thái đề xuất thay thế: PENDING, APPROVED/REJECTED (bác sĩ duyệt). */
    @Enumerated(EnumType.STRING)
    private SubstituteStatus substituteStatus;
}
