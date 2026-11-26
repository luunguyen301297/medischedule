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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;

    private Long doctorId;

    @Column(name = "icd10_code")
    private String icd10Code;

    @JdbcTypeCode(SqlTypes.JSON)
    private String thresholds;

    @JdbcTypeCode(SqlTypes.JSON)
    private String reminderTimes;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
