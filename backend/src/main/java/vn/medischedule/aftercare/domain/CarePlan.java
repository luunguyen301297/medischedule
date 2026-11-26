package vn.medischedule.aftercare.domain;

import jakarta.persistence.Column;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** Kế hoạch theo dõi bệnh mạn tính; ngưỡng và giờ nhắc lưu JSON. */
@Entity
@Table(name = "care_plan")
@Getter
@Setter
@NoArgsConstructor
public class CarePlan {

    public enum Status { ACTIVE, ENDED }

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** patient.id của bệnh nhân mạn tính. */
    private Long patientId;

    /** staff.id của bác sĩ lập kế hoạch. */
    private Long doctorId;

    /** Mã ICD-10 của bệnh mạn tính. */
    @Column(name = "icd10_code")
    private String icd10Code;

    /** Ngưỡng cảnh báo dạng JSON: [{metric, warn, critical}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String thresholds;

    /** Giờ nhắc đo / uống thuốc dạng JSON: ["07:00", "19:00"]. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String reminderTimes;

    /** ACTIVE hoặc ENDED. */
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    /** Thời điểm lập kế hoạch. */
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
