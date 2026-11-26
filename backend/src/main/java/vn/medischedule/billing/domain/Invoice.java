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
import java.time.OffsetDateTime;

/** Hóa đơn kèm thanh toán; bán thuốc lẻ thì appointmentId NULL. */
@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
public class Invoice {

    public enum PaymentMethod { CASH, QR, CARD }

    public enum Status { UNPAID, PAID, REFUNDED }

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Số hóa đơn, duy nhất. */
    private String invoiceNo;

    /** appointment.id; NULL khi bán thuốc lẻ. */
    private Long appointmentId;

    /** patient.id người thanh toán. */
    private Long patientId;

    /** Tổng tiền trước BHYT và giảm giá (VND). */
    private BigDecimal total;

    /** Phần BHYT chi trả (VND). */
    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    /** Phần giảm giá voucher (VND). */
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** CASH, QR, CARD; NULL khi chưa thanh toán. */
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    /** Mã giao dịch cổng thanh toán (QR/thẻ), duy nhất; chống ghi nhận trùng. */
    private String gatewayTxnId;

    /** UNPAID, PAID, REFUNDED. */
    @Enumerated(EnumType.STRING)
    private Status status = Status.UNPAID;

    /** Mã hóa đơn điện tử. */
    private String einvoiceCode;

    /** staff.id của thu ngân / lễ tân. */
    private Long cashierId;

    /** Thời điểm thanh toán. */
    private OffsetDateTime paidAt;
}
