package vn.medischedule.system.domain;

import jakarta.persistence.Entity;
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

/** Nhật ký thao tác (giá trị trước/sau trong detail), thay cho bảng lịch sử riêng. */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** users.id người thao tác; NULL nếu do hệ thống. */
    private Long userId;

    /** Hành động, vd. APPOINTMENT_RESCHEDULE, PRICE_CHANGE, PATIENT_MERGE, RECORD_VIEW. */
    private String action;

    /** Tên bảng bị tác động. */
    private String entity;

    /** Khóa của dòng bị tác động. */
    private Long entityId;

    /** JSON giá trị trước / sau. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String detail;

    /** Thời điểm thao tác. */
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
