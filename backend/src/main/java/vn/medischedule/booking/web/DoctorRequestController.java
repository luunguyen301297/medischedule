package vn.medischedule.booking.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.medischedule.auth.security.AuthUser;
import vn.medischedule.booking.dto.DoctorRequestDtos.DoctorRequestResponse;
import vn.medischedule.booking.dto.DoctorRequestDtos.GuestDoctorRequest;
import vn.medischedule.booking.dto.DoctorRequestDtos.PatientDoctorRequest;
import vn.medischedule.booking.service.DoctorRequestService;

/** Gửi yêu cầu đặt bác sĩ: khách qua /api/public (SecurityConfig permitAll), bệnh nhân qua endpoint có xác thực. */
@RestController
@RequiredArgsConstructor
public class DoctorRequestController {

    private final DoctorRequestService service;

    @PostMapping("/api/public/doctor-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorRequestResponse guest(@Valid @RequestBody GuestDoctorRequest req) {
        return service.createForGuest(req);
    }

    @PostMapping("/api/doctor-requests")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorRequestResponse patient(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PatientDoctorRequest req) {
        return service.createForPatient(me.id(), req);
    }
}
