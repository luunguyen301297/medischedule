package vn.medischedule.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.medischedule.booking.domain.Appointment;
import vn.medischedule.booking.dto.AppointmentDtos.CancelRequest;
import vn.medischedule.booking.dto.AppointmentDtos.NewPatient;
import vn.medischedule.booking.dto.AppointmentDtos.Slot;
import vn.medischedule.booking.dto.AppointmentDtos.StaffBookingRequest;
import vn.medischedule.booking.repository.AppointmentRepository;
import vn.medischedule.booking.repository.AppointmentRepository.AppointmentRowView;
import vn.medischedule.clinic.dto.ClinicDtos.BookingDoctorInfo;
import vn.medischedule.clinic.dto.ClinicDtos.SpecialtyOption;
import vn.medischedule.clinic.service.ClinicCatalogService;
import vn.medischedule.common.exception.ApiException;
import vn.medischedule.patient.dto.PatientDtos.PatientLite;
import vn.medischedule.patient.service.PatientService;
import vn.medischedule.schedule.dto.ScheduleDtos.ClinicShift;
import vn.medischedule.schedule.dto.ScheduleDtos.WorkShiftResponse;
import vn.medischedule.schedule.service.WorkScheduleService;
import vn.medischedule.system.service.SystemSettingService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Unit test (không Spring, không DB): lưới giờ, đặt hộ, hủy lịch. Hôm nay = 28/11/2026 09:10 giờ VN. */
class AppointmentServiceTest {

    AppointmentRepository appointments = mock(AppointmentRepository.class);
    WorkScheduleService schedule = mock(WorkScheduleService.class);
    ClinicCatalogService catalog = mock(ClinicCatalogService.class);
    PatientService patients = mock(PatientService.class);
    SystemSettingService settings = mock(SystemSettingService.class);
    AppointmentService service = new AppointmentService(appointments, schedule, catalog, patients, settings);

    static final LocalDate TODAY = LocalDate.of(2026, 11, 28);
    static final String PHONE = "0912000111";
    WorkShiftResponse morning = new WorkShiftResponse(1L, "Sáng", LocalTime.of(7, 0), LocalTime.of(11, 30), "#000000", true);
    BookingDoctorInfo doctor = new BookingDoctorInfo(50L, 100L, "BS A", 3L, "Nhi", 15, 13L, "P101");
    ClinicShift saved = new ClinicShift(900L, 13L, LocalTime.of(7, 0), LocalTime.of(11, 30), 15, 18);

    @BeforeEach
    void setUp() {
        service.clock = Clock.fixed(ZonedDateTime.of(TODAY, LocalTime.of(9, 10), AppointmentService.CLINIC_ZONE).toInstant(),
                AppointmentService.CLINIC_ZONE);
        when(schedule.activeShift(1L)).thenReturn(morning);
        when(catalog.bookingDoctor(100L)).thenReturn(doctor);
        when(schedule.isAssigned(any(), eq(1L), eq(100L))).thenReturn(true);
        when(schedule.findClinicShift(anyLong(), any(), anyLong())).thenReturn(Optional.empty());
        when(schedule.lockOrCreateClinicShift(anyLong(), any(), any(), anyLong(), anyInt())).thenReturn(saved);
        when(appointments.findTakenStartAt(anyLong(), any(), any())).thenReturn(List.of());
        when(settings.intValue(eq(AppointmentService.MAX_PENDING_KEY), anyInt())).thenReturn(2);
        when(appointments.saveAndFlush(any())).thenAnswer(i -> {
            Appointment a = i.getArgument(0);
            a.setId(1L);
            return a;
        });
        AppointmentRowView view = mock(AppointmentRowView.class);
        when(view.getVisitDate()).thenReturn(TODAY.plusDays(1).toString());
        when(appointments.findRow(1L)).thenReturn(Optional.of(view));
    }

    @Test
    void slotGridForPlannedShiftHidesPastAndMarksTaken() {
        when(appointments.findTakenStartAt(eq(50L), any(), any()))
                .thenReturn(List.of(TODAY.atTime(9, 30).atZone(AppointmentService.CLINIC_ZONE).toOffsetDateTime()));

        var grid = service.slots(TODAY, 1L, 100L);

        assertThat(grid.slotMinutes()).isEqualTo(15);
        assertThat(grid.roomName()).isEqualTo("P101");
        // 07:00 … 11:15 là 18 slot; đã qua 09:10 nên bắt đầu từ 09:15
        assertThat(grid.slots()).first().extracting(Slot::time).isEqualTo(LocalTime.of(9, 15));
        assertThat(grid.slots()).last().extracting(Slot::time).isEqualTo(LocalTime.of(11, 15));
        assertThat(grid.slots()).filteredOn(s -> !s.available()).extracting(Slot::time).containsExactly(LocalTime.of(9, 30));
    }

    @Test
    void savedShiftKeepsItsOwnSlotLength() {
        when(schedule.findClinicShift(50L, TODAY.plusDays(1), 1L))
                .thenReturn(Optional.of(new ClinicShift(900L, 13L, LocalTime.of(7, 0), LocalTime.of(11, 30), 30, 9)));

        var grid = service.slots(TODAY.plusDays(1), 1L, 100L);

        assertThat(grid.slotMinutes()).isEqualTo(30);
        assertThat(grid.slots()).hasSize(9).first().extracting(Slot::time).isEqualTo(LocalTime.of(7, 0));
    }

    @Test
    void doctorNotOnShiftIsRejected() {
        when(schedule.isAssigned(any(), eq(1L), eq(100L))).thenReturn(false);
        assertCode(() -> service.slots(TODAY, 1L, 100L), "NOT_ON_SHIFT");
    }

    @Test
    void dateOutsideSevenDaysIsRejected() {
        assertCode(() -> service.slots(TODAY.plusDays(7), 1L, 100L), "DATE_OUT_OF_RANGE");
        assertCode(() -> service.slots(TODAY.minusDays(1), 1L, 100L), "DATE_OUT_OF_RANGE");
    }

    @Test
    void createWithNewPatientSavesStaffBooking() {
        when(patients.findOrCreateAtReception("Nguyễn A", PHONE, null, "Nữ"))
                .thenReturn(new PatientLite(77L, "BN000077", "Nguyễn A", null, "Nữ", PHONE));

        service.createByStaff(5L, request(TODAY.plusDays(1), LocalTime.of(7, 15), null,
                new NewPatient("Nguyễn A", null, "Nữ")));

        verify(appointments).lockPhone(PHONE);
        verify(appointments).saveAndFlush(argThat(a -> a.getPatientId() == 77L && a.getShiftId() == 900L
                && a.getDoctorId() == 50L && a.getChannel() == Appointment.Channel.STAFF
                && a.getStatus() == Appointment.Status.BOOKED && a.getCreatedByUserId() == 5L
                && a.getBookerPhone().equals(PHONE) && a.getBookingCode().matches("^[A-HJ-NP-Z2-9]{8}$")
                && a.getStartAt().toLocalTime().equals(LocalTime.of(7, 15))));
    }

    @Test
    void createWithExistingPatientChecksPhone() {
        when(patients.requireActive(77L)).thenReturn(new PatientLite(77L, "BN000077", "Nguyễn A", null, null, "0988000000"));
        assertCode(() -> service.createByStaff(5L, request(TODAY.plusDays(1), LocalTime.of(7, 15), 77L, null)),
                "PATIENT_PHONE_MISMATCH");
        verify(appointments, never()).saveAndFlush(any());
    }

    @Test
    void timeOffGridOrPastIsRejected() {
        assertCode(() -> service.createByStaff(5L, request(TODAY.plusDays(1), LocalTime.of(7, 10), 77L, null)),
                "SLOT_INVALID");
        assertCode(() -> service.createByStaff(5L, request(TODAY, LocalTime.of(9, 0), 77L, null)), "SLOT_INVALID");
    }

    @Test
    void tooManyPendingForPhoneIsRejected() {
        when(appointments.countByBookerPhoneAndStatus(PHONE, Appointment.Status.BOOKED)).thenReturn(2L);
        assertCode(() -> service.createByStaff(5L, request(TODAY.plusDays(1), LocalTime.of(7, 15), 77L, null)),
                "TOO_MANY_PENDING");
        verify(patients, never()).findOrCreateAtReception(anyString(), anyString(), any(), any());
    }

    @Test
    void unassignedBookingHasNoDoctorShiftOrTime() {
        when(patients.requireActive(77L)).thenReturn(new PatientLite(77L, "BN000077", "Nguyễn A", null, null, PHONE));
        when(catalog.activeSpecialtyOptions()).thenReturn(List.of(new SpecialtyOption(3L, "Nhi")));

        service.createByStaff(5L, unassigned(TODAY.plusDays(1), 3L));

        verify(schedule, never()).lockOrCreateClinicShift(anyLong(), any(), any(), anyLong(), anyInt());
        verify(catalog, never()).bookingDoctor(any());
        verify(appointments).saveAndFlush(argThat(a -> a.getDoctorId() == null && a.getShiftId() == null
                && a.getStartAt() == null && !a.isDesignated() && a.getSpecialtyId() == 3L
                && a.getVisitDate().equals(TODAY.plusDays(1)) && a.getWorkShiftId() == 1L));
    }

    @Test
    void unassignedBookingChecksShiftEndAndSpecialty() {
        WorkShiftResponse early = new WorkShiftResponse(1L, "Sớm", LocalTime.of(6, 0), LocalTime.of(9, 0), "#000000", true);
        when(schedule.activeShift(1L)).thenReturn(early);
        assertCode(() -> service.createByStaff(5L, unassigned(TODAY, null)), "SHIFT_ENDED");

        when(catalog.activeSpecialtyOptions()).thenReturn(List.of());
        assertCode(() -> service.createByStaff(5L, unassigned(TODAY.plusDays(1), 9L)), "SPECIALTY_INVALID");
    }

    @Test
    void designatedDoctorRequiresTimeAndIsMarked() {
        assertCode(() -> service.createByStaff(5L, request(TODAY.plusDays(1), null, 77L, null)), "SLOT_REQUIRED");

        when(patients.requireActive(77L)).thenReturn(new PatientLite(77L, "BN000077", "Nguyễn A", null, null, PHONE));
        service.createByStaff(5L, request(TODAY.plusDays(1), LocalTime.of(7, 30), 77L, null));
        verify(appointments).saveAndFlush(argThat(a -> a.isDesignated() && a.getSpecialtyId() == 3L));
    }

    @Test
    void patientIsRequired() {
        assertCode(() -> service.createByStaff(5L, request(TODAY.plusDays(1), LocalTime.of(7, 15), null, null)),
                "PATIENT_REQUIRED");
    }

    @Test
    void cancelOnlyBookedAppointments() {
        Appointment booked = new Appointment();
        when(appointments.lockById(1L)).thenReturn(Optional.of(booked));

        service.cancel(1L, new CancelRequest("  Khách bận  "));

        assertThat(booked.getStatus()).isEqualTo(Appointment.Status.CANCELLED);
        assertThat(booked.getCancelReason()).isEqualTo("Khách bận");

        assertCode(() -> service.cancel(1L, new CancelRequest("lần hai")), "NOT_CANCELLABLE");
    }

    @Test
    void listRejectsTooLongRange() {
        assertCode(() -> service.list(TODAY, TODAY.plusDays(31), null, null, null, 0, 20), "RANGE_TOO_LONG");
    }

    @Test
    void listEscapesSearchAndUsesSentinels() {
        service.list(null, null, null, "booked", "50%_", 0, 500);

        verify(appointments).searchRows(any(), any(), eq(0L), eq("BOOKED"), eq("50%_"), eq("%50\\%\\_%"),
                eq("50\\%\\_%"), eq("50%_"), eq(AppointmentService.MAX_PAGE_SIZE), eq(0L));
    }

    private static StaffBookingRequest request(LocalDate date, LocalTime time, Long patientId, NewPatient np) {
        return new StaffBookingRequest(date, 1L, null, 100L, time, PHONE, patientId, np);
    }

    private static StaffBookingRequest unassigned(LocalDate date, Long specialtyId) {
        return new StaffBookingRequest(date, 1L, specialtyId, null, null, PHONE, 77L, null);
    }

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOf(ApiException.class).extracting("code").isEqualTo(code);
    }
}
