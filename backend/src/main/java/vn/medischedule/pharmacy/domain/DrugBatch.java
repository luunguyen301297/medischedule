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

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Thuốc của lô. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Drug drug;

    /** Số lô của nhà sản xuất; duy nhất trong cùng một thuốc. */
    private String batchNo;

    /** Hạn dùng; cấp phát lô hết hạn gần nhất trước. */
    private LocalDate expiryDate;

    /** Số lượng còn trong lô (không âm). */
    private int quantity;

    /** Giá nhập mỗi đơn vị (VND). */
    private BigDecimal importPrice;

    /** Nhà cung cấp (dạng chữ). */
    private String supplier;

    /** Thời điểm nhập kho. */
    @CreationTimestamp
    private OffsetDateTime receivedAt;
}
