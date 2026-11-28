package vn.medischedule.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.medischedule.system.domain.SystemSetting;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
