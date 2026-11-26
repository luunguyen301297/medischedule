package vn.medischedule.pharmacy.domain;

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

import java.time.OffsetDateTime;

/** Đơn thuốc của một lượt khám. */
@Entity
@Table(name = "prescription")
@Getter
@Setter
@NoArgsConstructor
public class Prescription {

    public enum Status { DRAFT, SIGNED, DISPENSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long appointmentId;

    private String nationalRxCode;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    private OffsetDateTime signedAt;

    private Long dispensedBy;

    private OffsetDateTime dispensedAt;
}
