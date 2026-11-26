package vn.medischedule.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Bệnh án của một lượt khám (1-1 với appointment, khóa là appointmentId). Ký xong thì khóa sửa. */
@Entity
@Table(name = "medical_record")
@Getter
@Setter
@NoArgsConstructor
public class MedicalRecord {

    /** appointment.id; vừa là khóa chính vừa là khóa ngoại (1 lượt khám 1 bệnh án). */
    @Id
    private Long appointmentId;

    /** Triệu chứng, lý do khám. */
    private String symptoms;

    /** Sinh hiệu dạng JSON: {bp, pulse, temp, spo2, weight}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String vitalSigns;

    /** Mã ICD-10 của chẩn đoán chính; danh mục nạp ở frontend. */
    @Column(name = "icd10_code")
    private String icd10Code;

    /** Chẩn đoán. */
    private String diagnosis;

    /** Lời dặn của bác sĩ. */
    private String advice;

    /** Ngày hẹn tái khám. */
    private LocalDate followUpDate;

    /** Thời điểm bác sĩ ký; khác NULL thì bệnh án bị khóa sửa. */
    private OffsetDateTime signedAt;
}
