package vn.medischedule.schedule.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import vn.medischedule.schedule.dto.ScheduleDtos.AssignRequest;
import vn.medischedule.schedule.dto.ScheduleDtos.AssignmentResponse;
import vn.medischedule.schedule.dto.ScheduleDtos.StaffOption;
import vn.medischedule.schedule.dto.ScheduleDtos.WorkShiftRequest;
import vn.medischedule.schedule.dto.ScheduleDtos.WorkShiftResponse;
import vn.medischedule.schedule.service.WorkScheduleService;

import java.time.LocalDate;
import java.util.List;

/** Ca làm việc và lịch làm việc nhân viên, chỉ ADMIN (chặn cả ở SecurityConfig: /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminWorkScheduleController {

    private final WorkScheduleService service;

    @GetMapping("/work-shifts")
    public List<WorkShiftResponse> shifts() {
        return service.listShifts();
    }

    @PostMapping("/work-shifts")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkShiftResponse createShift(@Valid @RequestBody WorkShiftRequest req) {
        return service.createShift(req);
    }

    @PutMapping("/work-shifts/{id}")
    public WorkShiftResponse updateShift(@PathVariable Long id, @Valid @RequestBody WorkShiftRequest req) {
        return service.updateShift(id, req);
    }

    @PatchMapping("/work-shifts/{id}/active")
    public WorkShiftResponse setShiftActive(@PathVariable Long id, @RequestParam boolean value) {
        return service.setShiftActive(id, value);
    }

    @DeleteMapping("/work-shifts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShift(@PathVariable Long id) {
        service.deleteShift(id);
    }

    @GetMapping("/work-schedule")
    public List<AssignmentResponse> schedule(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.schedule(from, to);
    }

    @GetMapping("/work-schedule/staff")
    public List<StaffOption> staff() {
        return service.staffOptions();
    }

    @PostMapping("/work-schedule")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AssignmentResponse> assign(@Valid @RequestBody AssignRequest req) {
        return service.assign(req);
    }

    @DeleteMapping("/work-schedule/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable Long id) {
        service.unassign(id);
    }
}
