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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    private Drug drug;

    private int quantity;

    private String dosage;

    private short days;

    @ManyToOne(fetch = FetchType.LAZY)
    private Drug substituteDrug;

    @Enumerated(EnumType.STRING)
    private SubstituteStatus substituteStatus;
}
