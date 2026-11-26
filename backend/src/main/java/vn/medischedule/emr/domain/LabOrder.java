package vn.medischedule.emr.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** Chỉ định cận lâm sàng và kết quả. serviceId trỏ bảng giá (module billing). */
@Entity
@Table(name = "lab_order")
@Getter
@Setter
@NoArgsConstructor
public class LabOrder {

    public enum Status { ORDERED, DONE, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long appointmentId;

    private Long serviceId;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ORDERED;

    private String resultText;

    @JdbcTypeCode(SqlTypes.JSON)
    private String resultValues;

    private String resultFileKey;

    private OffsetDateTime resultAt;
}
