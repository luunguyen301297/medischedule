package vn.medischedule.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Cấu hình riêng của ứng dụng (khóa "app" trong application.yml). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, List<String> corsOrigins, Admin admin, Minio minio) {

    public record Jwt(String secret, long ttlMinutes) {}

    public record Admin(String username, String email, String password) {}

    public record Minio(String endpoint, String accessKey, String secretKey, String bucket) {}
}
