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

    @Id
    private String settingKey;

    private String value;

    private String description;
}
