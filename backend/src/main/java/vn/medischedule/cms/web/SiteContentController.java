package vn.medischedule.cms.web;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.medischedule.auth.security.AuthUser;
import vn.medischedule.cms.service.SiteContentService;

/** Nội dung trang chủ: ai cũng đọc được, chỉ ADMIN sửa. */
@RestController
@RequiredArgsConstructor
public class SiteContentController {

    private final SiteContentService service;

    @GetMapping("/api/public/site-content/{key}")
    public JsonNode get(@PathVariable String key) {
        return service.get(key);
    }

    @PutMapping("/api/admin/site-content/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public JsonNode save(@PathVariable String key, @RequestBody JsonNode body, @AuthenticationPrincipal AuthUser me) {
        return service.save(key, body, me.id());
    }
}
