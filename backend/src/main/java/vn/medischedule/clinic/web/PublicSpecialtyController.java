package vn.medischedule.clinic.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.medischedule.clinic.dto.ClinicDtos.SpecialtyOption;
import vn.medischedule.clinic.service.ClinicCatalogService;

import java.util.List;

/** Chuyên khoa đang hoạt động cho khách (public theo SecurityConfig: /api/public/**). */
@RestController
@RequestMapping("/api/public/specialties")
@RequiredArgsConstructor
public class PublicSpecialtyController {

    private final ClinicCatalogService catalog;

    @GetMapping
    public List<SpecialtyOption> list() {
        return catalog.activeSpecialtyOptions();
    }
}
