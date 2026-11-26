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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private CarePlan carePlan;

    @Enumerated(EnumType.STRING)
    private Metric metric;

    private BigDecimal value;

    private OffsetDateTime measuredAt;

    @Enumerated(EnumType.STRING)
    private AlertLevel alertLevel;

    private OffsetDateTime alertHandledAt;
}
