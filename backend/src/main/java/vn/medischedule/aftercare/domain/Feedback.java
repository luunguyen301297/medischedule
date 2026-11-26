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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long appointmentId;

    private short nps;

    private Short scoreReception;

    private Short scoreWaiting;

    private Short scoreDoctor;

    private Short scoreFacility;

    private String comment;

    @Enumerated(EnumType.STRING)
    private TicketStatus ticketStatus;

    private Long handledBy;

    private String resolution;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
