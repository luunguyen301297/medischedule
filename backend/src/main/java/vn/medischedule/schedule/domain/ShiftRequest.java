package vn.medischedule.schedule.domain;

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

import java.time.OffsetDateTime;

/** Bác sĩ xin đổi ca hoặc báo bận. */
@Entity
@Table(name = "shift_request")
@Getter
@Setter
@NoArgsConstructor
public class ShiftRequest {

    public enum Type { SWAP, UNAVAILABLE }

    public enum Status { PENDING, APPROVED, REJECTED, HANDLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Shift shift;

    private Long doctorId;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String reason;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private Long handledBy;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
