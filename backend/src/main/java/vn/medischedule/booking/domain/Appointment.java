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
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Lịch hẹn và cũng là lượt khám; walk-in là channel WALK_IN. doctorId phải bằng doctor của ca (FK ghép trong DB). */
@Entity
@Table(name = "appointment")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    public enum Channel { APP, GUEST, AI, WALK_IN, STAFF }

    public enum Status { BOOKED, CHECKED_IN, IN_PROGRESS, DONE, CANCELLED, NO_SHOW }

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã lịch gửi cho khách, duy nhất; khách tra cứu bằng mã + SĐT. */
    private String bookingCode;

    /** patient.id của người đi khám. */
    private Long patientId;

    /** SĐT người đặt; giới hạn tối đa 2 lịch chờ và đếm no-show theo SĐT này. */
    private String bookerPhone;

    /** Ngày khám; có cả khi chưa xếp bác sĩ. */
    private LocalDate visitDate;

    /** work_shift.id: buổi khách chọn (Sáng / Chiều / Tối). */
    private Long workShiftId;

    /** specialty.id khách chọn khi không chỉ định bác sĩ; NULL = chưa chọn. */
    private Long specialtyId;

    /** shift.id của ca khám; NULL = chưa xếp bác sĩ (xếp khi check-in). */
    private Long shiftId;

    /** Giờ hẹn (đầu slot); NULL khi chưa xếp. Unique theo bác sĩ, trừ lịch đã hủy và walk-in. */
    private OffsetDateTime startAt;

    /** staff.id của bác sĩ; NULL khi chưa xếp. DB ép bằng bác sĩ của ca (FK ghép shift_id, doctor_id). */
    private Long doctorId;

    /** true = bệnh nhân chỉ định đích danh bác sĩ. */
    @Column(name = "is_designated")
    private boolean designated = false;

    /** Phụ phí chỉ định, chốt giá lúc đặt (VND). */
    private BigDecimal designationFee = BigDecimal.ZERO;

    /** Mã ưu đãi áp dụng, nếu có. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Voucher voucher;

    /** Kênh đặt: APP, GUEST (web không đăng nhập), AI (đặt từ kết quả AI), WALK_IN (vãng lai tại quầy), STAFF (nhân viên đặt hộ). */
    @Enumerated(EnumType.STRING)
    private Channel channel;

    /** BOOKED → CHECKED_IN → IN_PROGRESS → DONE; hoặc CANCELLED / NO_SHOW. */
    @Enumerated(EnumType.STRING)
    private Status status = Status.BOOKED;

    /** Số thứ tự trong ca, cấp khi check-in. */
    private Short queueNumber;

    /** Thời điểm check-in tại quầy. */
    private OffsetDateTime checkedInAt;

    /** Thời điểm bác sĩ bắt đầu khám. */
    private OffsetDateTime startedAt;

    /** Thời điểm khám xong; thời gian khám = finishedAt - startedAt. */
    private OffsetDateTime finishedAt;

    /** Số lần đã đổi lịch, tối đa 2. */
    private short rescheduleCount = 0;

    /** Lý do hủy. */
    private String cancelReason;

    /** users.id nhân viên đặt hộ; NULL = khách tự đặt. */
    private Long createdByUserId;

    /** Thời điểm đặt lịch. */
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
