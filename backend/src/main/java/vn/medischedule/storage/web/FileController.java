package vn.medischedule.storage.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.medischedule.storage.service.StorageService;

import java.time.Duration;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FileController {

    private static final String PUBLIC_PREFIX = "/api/public/files/";

    private final StorageService storage;

    /** Admin upload ảnh cho trang chủ. Trả về đường dẫn công khai để lưu vào nội dung. */
    @PostMapping(value = "/api/admin/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        String key = storage.uploadPublicImage(file);
        return Map.of("url", PUBLIC_PREFIX + key);
    }

    /** Xem ảnh công khai (chỉ thư mục site/), cache 7 ngày vì tên file là UUID không đổi. */
    @GetMapping(PUBLIC_PREFIX + "site/**")
    public ResponseEntity<byte[]> get(HttpServletRequest req) {
        String key = req.getRequestURI().substring(PUBLIC_PREFIX.length());
        var obj = storage.read(key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(obj.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(obj.bytes());
    }
}
