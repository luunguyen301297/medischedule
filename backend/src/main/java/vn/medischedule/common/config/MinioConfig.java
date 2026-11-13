package vn.medischedule.common.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Client MinIO dùng chung; module storage sẽ dùng để tạo presigned URL. */
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(AppProperties props) {
        var m = props.minio();
        return MinioClient.builder()
                .endpoint(m.endpoint())
                .credentials(m.accessKey(), m.secretKey())
                .build();
    }
}
