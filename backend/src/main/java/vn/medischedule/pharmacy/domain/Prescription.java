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

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** appointment.id; mỗi lượt khám tối đa một đơn. */
    private Long appointmentId;

    /** Mã đơn thuốc liên thông quốc gia, duy nhất. */
    private String nationalRxCode;

    /** DRAFT (bác sĩ đang kê), SIGNED (đã ký, chờ cấp), DISPENSED (dược sĩ đã cấp). */
    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    /** Thời điểm bác sĩ ký đơn. */
    private OffsetDateTime signedAt;

    /** staff.id của dược sĩ cấp thuốc. */
    private Long dispensedBy;

    /** Thời điểm cấp thuốc. */
    private OffsetDateTime dispensedAt;
}
