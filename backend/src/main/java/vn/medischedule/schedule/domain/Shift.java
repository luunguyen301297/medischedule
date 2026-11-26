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

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** staff.id của bác sĩ trực. */
    private Long doctorId;

    /** room.id nơi khám. */
    private Long roomId;

    /** Ngày trực. */
    private LocalDate workDate;

    /** Giờ bắt đầu ca. */
    private LocalTime startTime;

    /** Giờ kết thúc ca (sau startTime). */
    private LocalTime endTime;

    /** Độ dài mỗi slot (phút); slot = startTime + k * slotMinutes. */
    private Short slotMinutes;

    /** Số lịch hẹn tối đa của ca; kiểm khi đặt trong transaction có khóa dòng ca. */
    private Short maxPatients;

    /** DRAFT (đang soạn), PUBLISHED (bệnh nhân thấy và đặt được), CANCELLED. */
    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;
}
