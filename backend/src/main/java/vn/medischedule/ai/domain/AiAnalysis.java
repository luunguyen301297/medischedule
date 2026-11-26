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

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** patient.id của hồ sơ được phân tích. */
    private Long patientId;

    /** appointment.id khi đặt lịch từ kết quả AI hoặc đưa vào bệnh án; NULL nếu chưa gắn. */
    private Long appointmentId;

    /** IMAGE (ảnh da liễu), LAB (chỉ số xét nghiệm), COMBINED (cả hai). */
    @Enumerated(EnumType.STRING)
    private Type type;

    /** Triệu chứng bệnh nhân mô tả. */
    private String symptoms;

    /** Mảng JSON object key MinIO của ảnh gốc (không lưu URL). */
    @JdbcTypeCode(SqlTypes.JSON)
    private String imageKeys;

    /** Chỉ số bệnh nhân nhập, JSON: [{code, value, unit, ref_low, ref_high}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String labValues;

    /** Nhãn phân loại của Image AI. */
    private String imageLabel;

    /** Độ tin cậy của nhãn (0–1). */
    private BigDecimal imageConfidence;

    /** Cảnh báo của Data AI, JSON: [{code, level, message}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String labFlags;

    /** Lời khuyên do LLM sinh ra, trả về bệnh nhân; chỉ để tham khảo, không phải chẩn đoán. */
    private String advice;

    /** Phiên bản model đã chạy, để truy vết khi đổi model. */
    private String modelVersion;

    /** PENDING (đang xử lý), DONE, FAILED. */
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    /** Lỗi từ AI service khi FAILED. */
    private String errorMessage;

    /** Thời điểm bệnh nhân đồng ý xử lý dữ liệu sức khỏe (NĐ 13/2023). */
    private OffsetDateTime consentAt;

    /** staff.id của bác sĩ xem lại kết quả. */
    private Long reviewedBy;

    /** Thời điểm bác sĩ xem lại. */
    private OffsetDateTime reviewedAt;

    /** Nhận xét của bác sĩ về kết quả AI. */
    private String doctorNote;

    /** Thời điểm gửi phân tích. */
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
