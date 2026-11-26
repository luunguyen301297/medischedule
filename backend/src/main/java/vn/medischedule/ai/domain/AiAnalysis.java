package vn.medischedule.ai.domain;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Một lần bệnh nhân gửi ảnh da liễu và/hoặc chỉ số xét nghiệm cho AI service. Ảnh ở MinIO, imageKeys là mảng object key. */
@Entity
@Table(name = "ai_analysis")
@Getter
@Setter
@NoArgsConstructor
public class AiAnalysis {

    public enum Type { IMAGE, LAB, COMBINED }

    public enum Status { PENDING, DONE, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;

    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String symptoms;

    @JdbcTypeCode(SqlTypes.JSON)
    private String imageKeys;

    @JdbcTypeCode(SqlTypes.JSON)
    private String labValues;

    private String imageLabel;

    private BigDecimal imageConfidence;

    @JdbcTypeCode(SqlTypes.JSON)
    private String labFlags;

    private String advice;

    private String modelVersion;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private String errorMessage;

    private OffsetDateTime consentAt;

    private Long reviewedBy;

    private OffsetDateTime reviewedAt;

    private String doctorNote;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
