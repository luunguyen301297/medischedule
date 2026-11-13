package vn.medischedule.storage.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.medischedule.common.config.AppProperties;
import vn.medischedule.common.exception.ApiException;

import java.util.Map;
import java.util.UUID;

/** Lưu / đọc file trên MinIO (bucket MINIO_BUCKET). */
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private final MinioClient minio;
    private final AppProperties props;

    public record StoredObject(byte[] bytes, String contentType) {}

    /** Ảnh công khai cho trang chủ: lưu ở site/{uuid}.{ext}. */
    public String uploadPublicImage(MultipartFile file) {
        String type = file.getContentType();
        if (type == null || !IMAGE_TYPES.containsKey(type)) {
            throw ApiException.badRequest("INVALID_IMAGE", "Chỉ nhận ảnh JPG, PNG hoặc WEBP");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw ApiException.badRequest("IMAGE_TOO_LARGE", "Ảnh tối đa 5 MB");
        }
        String key = "site/" + UUID.randomUUID() + "." + IMAGE_TYPES.get(type);
        try (var in = file.getInputStream()) {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(bucket()).object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(type)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Không lưu được file lên MinIO", e);
        }
        return key;
    }

    public StoredObject read(String key) {
        if (key.contains("..")) {
            throw ApiException.notFound("FILE_NOT_FOUND", "Không tìm thấy file");
        }
        try {
            var stat = minio.statObject(StatObjectArgs.builder().bucket(bucket()).object(key).build());
            try (var in = minio.getObject(GetObjectArgs.builder().bucket(bucket()).object(key).build())) {
                return new StoredObject(in.readAllBytes(), stat.contentType());
            }
        } catch (ErrorResponseException e) {
            throw ApiException.notFound("FILE_NOT_FOUND", "Không tìm thấy file");
        } catch (Exception e) {
            throw new IllegalStateException("Không đọc được file từ MinIO", e);
        }
    }

    private String bucket() {
        return props.minio().bucket();
    }
}
