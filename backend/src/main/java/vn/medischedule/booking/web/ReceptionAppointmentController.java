package vn.medischedule.booking.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.medischedule.auth.security.AuthUser;
import vn.medischedule.booking.dto.AppointmentDtos.AppointmentRow;
import vn.medischedule.booking.dto.AppointmentDtos.CancelRequest;
import vn.medischedule.booking.dto.AppointmentDtos.SlotGrid;
import vn.medischedule.booking.dto.AppointmentDtos.StaffBookingRequest;
import vn.medischedule.booking.service.AppointmentService;
import vn.medischedule.patient.dto.PatientDtos.PatientLite;
import vn.medischedule.patient.service.PatientService;

import java.time.LocalDate;
import java.util.List;

/**
 * Menu Lịch hẹn cho ADMIN và RECEPTIONIST. Không đặt dưới /api/admin vì SecurityConfig khóa prefix đó cho ADMIN.
 */
@RestController
@RequestMapping("/api/reception")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
@RequiredArgsConstructor
public class ReceptionAppointmentController {

    private final AppointmentService service;
    private final PatientService patients;

    @GetMapping("/appointments")
    public Page<AppointmentRow> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(required = false) Long doctorUserId,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return service.list(from, to, doctorUserId, status, q, page, size);
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentRow create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody StaffBookingRequest req) {
        return service.createByStaff(me.id(), req);
    }

    @PostMapping("/appointments/{id}/cancel")
    public AppointmentRow cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest req) {
        return service.cancel(id, req);
    }

    @GetMapping("/slots")
    public SlotGrid slots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @RequestParam Long workShiftId, @RequestParam Long doctorUserId) {
        return service.slots(date, workShiftId, doctorUserId);
    }

    @GetMapping("/patients")
    public List<PatientLite> patients(@RequestParam String phone) {
        return patients.findByPhone(phone);
    }
}
