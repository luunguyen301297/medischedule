package vn.medischedule.aftercare.domain;

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
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** Khảo sát sau khám; điểm thấp thì chính dòng này thành phiếu xử lý (ticketStatus). */
@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
public class Feedback {

    public enum TicketStatus { OPEN, RESOLVED }

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** appointment.id; mỗi lượt khám một phiếu. */
    private Long appointmentId;

    /** Điểm sẵn sàng giới thiệu (0–10). */
    private short nps;

    /** Điểm tiếp đón (1–5). */
    private Short scoreReception;

    /** Điểm thời gian chờ (1–5). */
    private Short scoreWaiting;

    /** Điểm bác sĩ (1–5). */
    private Short scoreDoctor;

    /** Điểm cơ sở vật chất (1–5). */
    private Short scoreFacility;

    /** Góp ý tự do. */
    private String comment;

    /** OPEN / RESOLVED khi điểm thấp cần xử lý; NULL = không cần xử lý. */
    @Enumerated(EnumType.STRING)
    private TicketStatus ticketStatus;

    /** staff.id người xử lý phiếu. */
    private Long handledBy;

    /** Cách đã xử lý. */
    private String resolution;

    /** Thời điểm gửi khảo sát. */
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
