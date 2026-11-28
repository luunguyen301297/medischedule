package vn.medischedule.booking.domain;

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

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Yêu cầu đặt đích danh một bác sĩ vào ngày + buổi mong muốn; nhân sự xếp ca sau. Chưa phải lịch hẹn. */
@Entity
@Table(name = "doctor_request")
@Getter
@Setter
@NoArgsConstructor
public class DoctorRequest {

    public enum Status { PENDING, SCHEDULED, REJECTED, CANCELLED }

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** users.id của bác sĩ. */
    private Long doctorUserId;

    /** Ngày mong muốn khám. */
    private LocalDate requestedDate;

    /** work_shift.id: buổi mong muốn (Sáng / Chiều / Tối). */
    private Long workShiftId;

    /** Họ tên người khám. */
    private String fullName;

    /** SĐT liên hệ; giới hạn số yêu cầu chờ theo SĐT này. */
    private String phone;

    /** Mô tả triệu chứng (không bắt buộc). */
    private String symptoms;

    /** users.id người đặt khi đã đăng nhập; NULL = khách. */
    private Long bookerUserId;

    /** PENDING (chờ xếp ca) → SCHEDULED / REJECTED / CANCELLED (đợt sau). */
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    /** Thời điểm gửi. */
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
