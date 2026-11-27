package vn.medischedule.schedule.web;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.medischedule.clinic.dto.ClinicDtos.DoctorOption;
import vn.medischedule.schedule.dto.ScheduleDtos.WorkShiftResponse;
import vn.medischedule.schedule.service.WorkScheduleService;

import java.time.LocalDate;
import java.util.List;

/** Dữ liệu cho form đặt lịch của khách (public theo SecurityConfig: /api/public/**). */
@RestController
@RequestMapping("/api/public/booking")
@RequiredArgsConstructor
public class PublicBookingController {

    private final WorkScheduleService service;

    @GetMapping("/shifts")
    public List<WorkShiftResponse> shifts() {
        return service.activeShifts();
    }

    @GetMapping("/doctors")
    public List<DoctorOption> doctors(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam Long shiftId,
                                      @RequestParam(required = false) Long specialtyId) {
        return service.doctorsOnShift(date, shiftId, specialtyId);
    }
}
