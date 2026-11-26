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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNo;

    private Long appointmentId;

    private Long patientId;

    private BigDecimal total;

    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String gatewayTxnId;

    @Enumerated(EnumType.STRING)
    private Status status = Status.UNPAID;

    private String einvoiceCode;

    private Long cashierId;

    private OffsetDateTime paidAt;
}
