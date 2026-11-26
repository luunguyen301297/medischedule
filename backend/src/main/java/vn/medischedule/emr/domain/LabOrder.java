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

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** appointment.id của lượt khám chỉ định. */
    private Long appointmentId;

    /** service.id của dịch vụ cận lâm sàng (để tính tiền). */
    private Long serviceId;

    /** ORDERED (đã chỉ định), DONE (có kết quả), CANCELLED. */
    @Enumerated(EnumType.STRING)
    private Status status = Status.ORDERED;

    /** Kết quả dạng chữ / kết luận. */
    private String resultText;

    /** Chỉ số dạng JSON: [{code, value, unit, ref_low, ref_high}]; gửi được sang Data AI. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String resultValues;

    /** Object key MinIO của ảnh/PDF kết quả (không lưu URL). */
    private String resultFileKey;

    /** Thời điểm có kết quả. */
    private OffsetDateTime resultAt;
}
