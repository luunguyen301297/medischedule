package vn.medischedule.system.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tham số hệ thống dạng key/value. */
@Entity
@Table(name = "system_setting")
@Getter
@Setter
@NoArgsConstructor
public class SystemSetting {

    /** Khóa tham số, vd. booking.max_pending_per_phone (giá trị mặc định seed ở V6). */
    @Id
    private String settingKey;

    /** Giá trị dạng chữ; service tự ép kiểu. */
    private String value;

    /** Giải thích tham số cho admin. */
    private String description;
}
