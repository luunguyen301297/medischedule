package vn.medischedule.aftercare.domain;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Chỉ số bệnh nhân nhập; vượt ngưỡng thì ghi alertLevel. */
@Entity
@Table(name = "health_measurement")
@Getter
@Setter
@NoArgsConstructor
public class HealthMeasurement {

    public enum Metric { BP_SYS, BP_DIA, GLUCOSE, WEIGHT }

    public enum AlertLevel { WARNING, CRITICAL }

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kế hoạch theo dõi của chỉ số này. */
    @ManyToOne(fetch = FetchType.LAZY)
    private CarePlan carePlan;

    /** BP_SYS / BP_DIA (huyết áp tâm thu / tâm trương), GLUCOSE, WEIGHT. */
    @Enumerated(EnumType.STRING)
    private Metric metric;

    /** Giá trị đo, một chữ số thập phân. */
    private BigDecimal value;

    /** Thời điểm đo (bệnh nhân nhập). */
    private OffsetDateTime measuredAt;

    /** WARNING / CRITICAL khi vượt ngưỡng; NULL = bình thường. */
    @Enumerated(EnumType.STRING)
    private AlertLevel alertLevel;

    /** Thời điểm bác sĩ đánh dấu đã xử lý cảnh báo. */
    private OffsetDateTime alertHandledAt;
}
