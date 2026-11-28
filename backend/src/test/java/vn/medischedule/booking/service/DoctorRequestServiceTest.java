package vn.medischedule.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.medischedule.auth.dto.AuthDtos.UserResponse;
import vn.medischedule.auth.service.AuthService;
import vn.medischedule.booking.domain.DoctorRequest;
import vn.medischedule.booking.dto.DoctorRequestDtos.GuestDoctorRequest;
import vn.medischedule.booking.dto.DoctorRequestDtos.PatientDoctorRequest;
import vn.medischedule.booking.repository.DoctorRequestRepository;
import vn.medischedule.clinic.dto.ClinicDtos.PublicDoctor;
import vn.medischedule.clinic.service.ClinicCatalogService;
import vn.medischedule.common.exception.ApiException;
import vn.medischedule.schedule.dto.ScheduleDtos.WorkShiftResponse;
import vn.medischedule.schedule.service.WorkScheduleService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Unit test (không Spring, không DB): rule gửi yêu cầu đặt bác sĩ. Hôm nay = 28/11/2026 14:00 giờ VN. */
class DoctorRequestServiceTest {

    DoctorRequestRepository requests = mock(DoctorRequestRepository.class);
    ClinicCatalogService catalog = mock(ClinicCatalogService.class);
    WorkScheduleService schedule = mock(WorkScheduleService.class);
    AuthService auth = mock(AuthService.class);
    DoctorRequestService service = new DoctorRequestService(requests, catalog, schedule, auth);

    static final LocalDate TODAY = LocalDate.of(2026, 11, 28);
    WorkShiftResponse morning = new WorkShiftResponse(1L, "Sáng", LocalTime.of(7, 0), LocalTime.of(11, 30), "#000000", true);
    WorkShiftResponse evening = new WorkShiftResponse(3L, "Tối", LocalTime.of(17, 30), LocalTime.of(21, 0), "#000000", true);

    @BeforeEach
    void setUp() {
        service.clock = Clock.fixed(ZonedDateTime.of(TODAY, LocalTime.of(14, 0), DoctorRequestService.CLINIC_ZONE).toInstant(),
                DoctorRequestService.CLINIC_ZONE);
        when(catalog.publicDoctors()).thenReturn(List.of(new PublicDoctor(100L, "BS A", null, "Nhi", null, null, false)));
        when(schedule.activeShifts()).thenReturn(List.of(morning, evening));
        when(requests.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void guestRequestIsSavedPending() {
        var res = service.createForGuest(guest(100L, TODAY.plusDays(1), 1L));

        assertThat(res.doctorName()).isEqualTo("BS A");
        assertThat(res.workShiftName()).isEqualTo("Sáng");
        assertThat(res.status()).isEqualTo("PENDING");
        verify(requests).lockPhone("0912000111");
        verify(requests).saveAndFlush(argThat(r -> r.getBookerUserId() == null && r.getFullName().equals("Nguyễn A")
                && r.getStatus() == DoctorRequest.Status.PENDING));
    }

    @Test
    void patientNameAndPhoneComeFromAccount() {
        when(auth.get(7L)).thenReturn(user(7L, "0988000222"));

        var res = service.createForPatient(7L, new PatientDoctorRequest(100L, TODAY.plusDays(2), 3L));

        assertThat(res.fullName()).isEqualTo("BN Bảy");
        assertThat(res.phone()).isEqualTo("0988000222");
        verify(requests).saveAndFlush(argThat(r -> r.getBookerUserId() == 7L));
    }

    @Test
    void patientWithoutPhoneIsRejected() {
        when(auth.get(7L)).thenReturn(user(7L, null));
        assertCode(() -> service.createForPatient(7L, new PatientDoctorRequest(100L, TODAY, 3L)), "PHONE_REQUIRED");
    }

    @Test
    void unknownOrInactiveDoctorIsRejected() {
        assertCode(() -> service.createForGuest(guest(999L, TODAY.plusDays(1), 1L)), "DOCTOR_UNAVAILABLE");
        verifyNoSave();
    }

    @Test
    void dateOutsideSevenDaysIsRejected() {
        assertCode(() -> service.createForGuest(guest(100L, TODAY.minusDays(1), 1L)), "DATE_OUT_OF_RANGE");
        assertCode(() -> service.createForGuest(guest(100L, TODAY.plusDays(7), 1L)), "DATE_OUT_OF_RANGE");
        service.createForGuest(guest(100L, TODAY.plusDays(6), 1L));
    }

    @Test
    void inactiveShiftAndEndedShiftTodayAreRejected() {
        assertCode(() -> service.createForGuest(guest(100L, TODAY.plusDays(1), 2L)), "SHIFT_INVALID");
        assertCode(() -> service.createForGuest(guest(100L, TODAY, 1L)), "SHIFT_ENDED");
        service.createForGuest(guest(100L, TODAY, 3L)); // buổi tối hôm nay còn mở
    }

    @Test
    void duplicatePendingRequestIsRejected() {
        when(requests.existsByPhoneAndDoctorUserIdAndRequestedDateAndWorkShiftIdAndStatus(
                eq("0912000111"), eq(100L), any(), eq(1L), eq(DoctorRequest.Status.PENDING))).thenReturn(true);
        assertCode(() -> service.createForGuest(guest(100L, TODAY.plusDays(1), 1L)), "REQUEST_EXISTS");
        verifyNoSave();
    }

    @Test
    void tooManyPendingRequestsAreRejected() {
        when(requests.countByPhoneAndStatus("0912000111", DoctorRequest.Status.PENDING)).thenReturn(3L);
        assertCode(() -> service.createForGuest(guest(100L, TODAY.plusDays(1), 1L)), "TOO_MANY_PENDING");
        verifyNoSave();
    }

    private void verifyNoSave() {
        verify(requests, never()).saveAndFlush(any());
    }

    private static GuestDoctorRequest guest(Long doctor, LocalDate date, Long shift) {
        return new GuestDoctorRequest(doctor, date, shift, " Nguyễn A ", "0912000111");
    }

    private static UserResponse user(Long id, String phone) {
        return new UserResponse(id, "bn@x.test", null, "BN Bảy", phone, "PATIENT", true, null, null);
    }

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOf(ApiException.class).extracting("code").isEqualTo(code);
    }
}
