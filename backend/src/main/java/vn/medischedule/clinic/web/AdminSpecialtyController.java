package vn.medischedule.clinic.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.medischedule.clinic.dto.ClinicDtos.DoctorOption;
import vn.medischedule.clinic.dto.ClinicDtos.RoomRequest;
import vn.medischedule.clinic.dto.ClinicDtos.RoomResponse;
import vn.medischedule.clinic.dto.ClinicDtos.SpecialtyRequest;
import vn.medischedule.clinic.dto.ClinicDtos.SpecialtyResponse;
import vn.medischedule.clinic.service.ClinicCatalogService;

import java.util.List;

/** Danh mục chuyên khoa và phòng khám, chỉ ADMIN (chặn cả ở SecurityConfig: /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSpecialtyController {

    private final ClinicCatalogService catalog;

    @GetMapping("/specialties")
    public List<SpecialtyResponse> list() {
        return catalog.listSpecialties();
    }

    @GetMapping("/doctors")
    public List<DoctorOption> doctors() {
        return catalog.doctorOptions();
    }

    @PostMapping("/specialties")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialtyResponse create(@Valid @RequestBody SpecialtyRequest req) {
        return catalog.createSpecialty(req);
    }

    @PutMapping("/specialties/{id}")
    public SpecialtyResponse rename(@PathVariable Long id, @Valid @RequestBody SpecialtyRequest req) {
        return catalog.renameSpecialty(id, req);
    }

    @PatchMapping("/specialties/{id}/active")
    public SpecialtyResponse setActive(@PathVariable Long id, @RequestParam boolean value) {
        return catalog.setSpecialtyActive(id, value);
    }

    @DeleteMapping("/specialties/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        catalog.deleteSpecialty(id);
    }

    @GetMapping("/specialties/{id}/rooms")
    public List<RoomResponse> rooms(@PathVariable Long id) {
        return catalog.listRooms(id);
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@Valid @RequestBody RoomRequest req) {
        return catalog.createRoom(req);
    }

    @PutMapping("/rooms/{id}")
    public RoomResponse updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest req) {
        return catalog.updateRoom(id, req);
    }

    @PatchMapping("/rooms/{id}/active")
    public RoomResponse setRoomActive(@PathVariable Long id, @RequestParam boolean value) {
        return catalog.setRoomActive(id, value);
    }

    @DeleteMapping("/rooms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable Long id) {
        catalog.deleteRoom(id);
    }
}
