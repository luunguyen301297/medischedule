package vn.medischedule.clinic.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.medischedule.clinic.dto.ClinicDtos.PublicDoctor;
import vn.medischedule.clinic.service.ClinicCatalogService;

import java.util.List;

/** Bác sĩ đang hoạt động cho trang chủ: đếm số bác sĩ và danh sách bác sĩ hàng đầu (public: /api/public/**). */
@RestController
@RequestMapping("/api/public/doctors")
@RequiredArgsConstructor
public class PublicDoctorController {

    private final ClinicCatalogService catalog;

    @GetMapping
    public List<PublicDoctor> list() {
        return catalog.publicDoctors();
    }
}
