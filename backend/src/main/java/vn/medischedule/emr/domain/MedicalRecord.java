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

    @Id
    private Long appointmentId;

    private String symptoms;

    @JdbcTypeCode(SqlTypes.JSON)
    private String vitalSigns;

    @Column(name = "icd10_code")
    private String icd10Code;

    private String diagnosis;

    private String advice;

    private LocalDate followUpDate;

    private OffsetDateTime signedAt;
}
