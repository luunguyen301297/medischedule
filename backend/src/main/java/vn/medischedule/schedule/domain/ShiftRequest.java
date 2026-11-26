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

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ca bị ảnh hưởng. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Shift shift;

    /** staff.id của bác sĩ gửi yêu cầu. */
    private Long doctorId;

    /** SWAP (xin đổi ca) hoặc UNAVAILABLE (báo bận đột xuất). */
    @Enumerated(EnumType.STRING)
    private Type type;

    /** Lý do bác sĩ nhập. */
    private String reason;

    /** PENDING, APPROVED/REJECTED (admin duyệt), HANDLED (lễ tân đã báo bệnh nhân). */
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    /** staff.id của người duyệt / xử lý. */
    private Long handledBy;

    /** Thời điểm gửi yêu cầu. */
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
