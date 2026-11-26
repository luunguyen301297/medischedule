package vn.medischedule.pharmacy.domain;

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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Lô thuốc; cấp phát trừ lô hết hạn gần nhất trước. */
@Entity
@Table(name = "drug_batch")
@Getter
@Setter
@NoArgsConstructor
public class DrugBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Drug drug;

    private String batchNo;

    private LocalDate expiryDate;

    private int quantity;

    private BigDecimal importPrice;

    private String supplier;

    @CreationTimestamp
    private OffsetDateTime receivedAt;
}
