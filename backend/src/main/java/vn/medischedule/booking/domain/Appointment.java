package vn.medischedule.booking.domain;

import jakarta.persistence.Column;
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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Lịch hẹn và cũng là lượt khám; walk-in là channel WALK_IN. doctorId phải bằng doctor của ca (FK ghép trong DB). */
@Entity
@Table(name = "appointment")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    public enum Channel { APP, GUEST, AI, WALK_IN }

    public enum Status { BOOKED, CHECKED_IN, IN_PROGRESS, DONE, CANCELLED, NO_SHOW }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bookingCode;

    private Long patientId;

    private String bookerPhone;

    private Long shiftId;

    private OffsetDateTime startAt;

    private Long doctorId;

    @Column(name = "is_designated")
    private boolean designated = false;

    private BigDecimal designationFee = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    private Voucher voucher;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    private Status status = Status.BOOKED;

    private Short queueNumber;

    private OffsetDateTime checkedInAt;

    private OffsetDateTime startedAt;

    private OffsetDateTime finishedAt;

    private short rescheduleCount = 0;

    private String cancelReason;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
