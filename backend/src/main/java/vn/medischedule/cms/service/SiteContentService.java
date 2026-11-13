package vn.medischedule.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.cms.domain.SiteContent;
import vn.medischedule.cms.repository.SiteContentRepository;
import vn.medischedule.common.exception.ApiException;

import java.time.OffsetDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SiteContentService {

    /** Chỉ các khối đã biết mới được đọc / ghi. */
    public static final Set<String> KEYS = Set.of("homepage");
    private static final int MAX_BYTES = 100_000;

    private final SiteContentRepository repo;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public JsonNode get(String key) {
        checkKey(key);
        return repo.findById(key)
                .map(c -> read(c.getContent()))
                .orElseGet(mapper::createObjectNode);
    }

    @Transactional
    public JsonNode save(String key, JsonNode body, Long userId) {
        checkKey(key);
        if (body == null || !body.isObject()) {
            throw ApiException.badRequest("INVALID_CONTENT", "Nội dung phải là một đối tượng JSON");
        }
        String json = body.toString();
        if (json.length() > MAX_BYTES) {
            throw ApiException.badRequest("CONTENT_TOO_LARGE", "Nội dung quá lớn (tối đa 100 KB)");
        }
        SiteContent c = repo.findById(key).orElseGet(() -> {
            SiteContent n = new SiteContent();
            n.setKey(key);
            return n;
        });
        c.setContent(json);
        c.setUpdatedBy(userId);
        c.setUpdatedAt(OffsetDateTime.now());
        repo.save(c);
        return body;
    }

    private void checkKey(String key) {
        if (!KEYS.contains(key)) {
            throw ApiException.notFound("CONTENT_NOT_FOUND", "Không có khối nội dung: " + key);
        }
    }

    private JsonNode read(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("site_content hỏng JSON", e);
        }
    }
}
