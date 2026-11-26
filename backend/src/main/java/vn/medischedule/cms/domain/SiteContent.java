package vn.medischedule.cms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** Một khối nội dung hiển thị công khai (vd. "homepage"), lưu dạng JSON. */
@Entity
@Table(name = "site_content")
@Getter
@Setter
@NoArgsConstructor
public class SiteContent {

    /** Khóa khối nội dung, vd. "homepage". */
    @Id
    @Column(name = "content_key")
    private String key;

    /** Nội dung dạng JSON, cấu trúc do frontend định nghĩa. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String content;

    /** users.id của admin sửa gần nhất. */
    @Column(name = "updated_by")
    private Long updatedBy;

    /** Thời điểm sửa gần nhất. */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
