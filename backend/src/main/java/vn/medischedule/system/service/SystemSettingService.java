package vn.medischedule.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.system.repository.SystemSettingRepository;

/** Đọc tham số hệ thống (bảng system_setting). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemSettingService {

    private final SystemSettingRepository settings;

    /** Giá trị số nguyên của khóa; thiếu khóa hoặc giá trị không phải số thì trả mặc định. */
    public int intValue(String key, int defaultValue) {
        return settings.findById(key)
                .map(s -> {
                    try {
                        return Integer.parseInt(s.getValue().trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
}
