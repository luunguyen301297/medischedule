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

    @Id
    @Column(name = "content_key")
    private String key;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String content;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
