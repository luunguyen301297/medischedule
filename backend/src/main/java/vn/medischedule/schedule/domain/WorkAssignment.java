package vn.medischedule.schedule.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Một nhân viên làm một ca trong một ngày. */
@Entity
@Table(name = "work_assignment")
@Getter
@Setter
@NoArgsConstructor
public class WorkAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate workDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_shift_id")
    private WorkShift workShift;

    /** users.id của nhân viên (module auth). */
    private Long userId;
}
