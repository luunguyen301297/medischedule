package vn.medischedule.patient.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.medischedule.patient.dto.PatientDtos.PatientProfile;
import vn.medischedule.patient.dto.PatientDtos.PatientRequest;
import vn.medischedule.patient.service.PatientService;

import java.util.List;

/** Admin xem / thêm / sửa hồ sơ bệnh nhân (chặn cả ở SecurityConfig: /api/admin/**). */
@RestController
@RequestMapping("/api/admin/patients")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPatientController {

    private final PatientService patients;

    /** Hồ sơ bản thân + người thân của một tài khoản bệnh nhân. */
    @GetMapping
    public List<PatientProfile> byOwner(@RequestParam Long ownerUserId) {
        return patients.byOwner(ownerUserId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientProfile create(@Valid @RequestBody PatientRequest req) {
        return patients.create(req);
    }

    @PutMapping("/{id}")
    public PatientProfile update(@PathVariable Long id, @Valid @RequestBody PatientRequest req) {
        return patients.update(id, req);
    }
}
