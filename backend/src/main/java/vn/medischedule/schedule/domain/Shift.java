package vn.medischedule.schedule.domain;

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

import java.time.LocalDate;
import java.time.LocalTime;

/** Ca trực; slot tính từ giờ ca và slotMinutes. doctorId, roomId trỏ module clinic. */
@Entity
@Table(name = "shift")
@Getter
@Setter
@NoArgsConstructor
public class Shift {

    public enum Status { DRAFT, PUBLISHED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long doctorId;

    private Long roomId;

    private LocalDate workDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Short slotMinutes;

    private Short maxPatients;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;
}
