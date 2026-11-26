package vn.medischedule.billing.domain;

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

import java.math.BigDecimal;

/** Dòng hóa đơn: đúng một trong service hoặc drugId (CHECK trong DB). */
@Entity
@Table(name = "invoice_item")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceItem {

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Hóa đơn chứa dòng này. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Invoice invoice;

    /** Dịch vụ tính tiền; đúng một trong service hoặc drugId có giá trị. */
    @ManyToOne(fetch = FetchType.LAZY)
    private MedicalService service;

    /** drug.id khi dòng là thuốc. */
    private Long drugId;

    /** Số lượng. */
    private int quantity;

    /** Đơn giá chốt lúc lập hóa đơn (VND). */
    private BigDecimal unitPrice;

    /** Thành tiền = quantity * unitPrice (VND). */
    private BigDecimal amount;
}
