package vn.medischedule.schedule.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import vn.medischedule.auth.dto.AuthDtos.StaffAccount;
import vn.medischedule.auth.service.AuthService;
import vn.medischedule.clinic.dto.ClinicDtos.DoctorOption;
import vn.medischedule.clinic.service.ClinicCatalogService;
import vn.medischedule.common.exception.ApiException;
import vn.medischedule.schedule.domain.WorkAssignment;
import vn.medischedule.schedule.domain.WorkShift;
import vn.medischedule.schedule.dto.ScheduleDtos.AssignRequest;
import vn.medischedule.schedule.dto.ScheduleDtos.WorkShiftRequest;
import vn.medischedule.schedule.repository.ShiftRepository;
import vn.medischedule.schedule.repository.WorkAssignmentRepository;
import vn.medischedule.schedule.repository.WorkShiftRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Unit test (không Spring, không DB): rule ca làm việc và phân ca. */
class WorkScheduleServiceTest {

    WorkShiftRepository shifts = mock(WorkShiftRepository.class);
    WorkAssignmentRepository assignments = mock(WorkAssignmentRepository.class);
    AuthService auth = mock(AuthService.class);
    ClinicCatalogService catalog = mock(ClinicCatalogService.class);
    ShiftRepository clinicShifts = mock(ShiftRepository.class);
    WorkScheduleService service = new WorkScheduleService(shifts, clinicShifts, assignments, auth, catalog);

    LocalDate tomorrow = LocalDate.now(WorkScheduleService.CLINIC_ZONE).plusDays(1);
    WorkShift morning = shift(1L, "Sáng", "07:00", "11:30", true);
    WorkShift lateMorning = shift(2L, "Giữa", "11:00", "14:00", true);
    WorkShift afternoon = shift(3L, "Chiều", "13:00", "17:00", true);
    WorkShift off = shift(4L, "Tối", "17:30", "21:00", false);

    @BeforeEach
    void setUp() {
        for (WorkShift s : List.of(morning, lateMorning, afternoon, off)) {
            when(shifts.findById(s.getId())).thenReturn(Optional.of(s));
        }
        when(shifts.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(assignments.saveAllAndFlush(anyList())).thenAnswer(i -> i.getArgument(0));
        when(auth.staffAccounts()).thenReturn(List.of(
                new StaffAccount(10L, "An", "DOCTOR", true),
                new StaffAccount(11L, "Bình", "RECEPTIONIST", true),
                new StaffAccount(12L, "Cường", "PHARMACIST", false),
                new StaffAccount(13L, "Admin", "ADMIN", true)));
    }

    @Test
    void doctorsOnShiftKeepsAssignedDoctorsAndFiltersSpecialty() {
        when(assignments.findByWorkDateAndWorkShiftId(tomorrow, 1L)).thenReturn(List.of(
                assignment(1L, tomorrow, morning, 10L), assignment(2L, tomorrow, morning, 11L),
                assignment(3L, tomorrow, morning, 14L)));
        when(catalog.doctorOptions()).thenReturn(List.of(
                new DoctorOption(10L, "An", 5L, "Nội"),
                new DoctorOption(14L, "Dũng", 6L, "Nhi"),
                new DoctorOption(15L, "Em", 5L, "Nội")));   // không có ca
        assertThat(service.doctorsOnShift(tomorrow, 1L, null)).extracting(DoctorOption::userId).containsExactly(10L, 14L);
        assertThat(service.doctorsOnShift(tomorrow, 1L, 6L)).extracting(DoctorOption::userId).containsExactly(14L);
        assertThat(service.doctorsOnShift(tomorrow, 3L, null)).isEmpty();
    }

    @Test
    void endNotAfterStartIsRejected() {
        assertCode(() -> service.createShift(req("Ca", "11:00", "11:00")), "INVALID_TIME_RANGE");
        verify(shifts, never()).saveAndFlush(any());
    }

    @Test
    void duplicateNameIsRejectedAfterTrim() {
        when(shifts.existsByNameIgnoreCase("sáng")).thenReturn(true);
        assertCode(() -> service.createShift(req(" sáng ", "07:00", "11:00")), "WORK_SHIFT_EXISTS");
        when(shifts.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("x",
                new RuntimeException("duplicate key uq_work_shift_name_ci")));
        assertCode(() -> service.createShift(req("Khuya", "01:00", "05:00")), "WORK_SHIFT_EXISTS");
    }

    @Test
    void deletingUsedShiftBecomesInUse() {
        doThrow(new DataIntegrityViolationException("fk")).when(shifts).flush();
        assertCode(() -> service.deleteShift(1L), "WORK_SHIFT_IN_USE");
    }

    @Test
    void pastDateCannotBeAssignedOrUnassigned() {
        LocalDate yesterday = tomorrow.minusDays(2);
        assertCode(() -> service.assign(new AssignRequest(yesterday, 1L, List.of(10L))), "PAST_DATE");
        WorkAssignment old = assignment(99L, yesterday, morning, 10L);
        when(assignments.findById(99L)).thenReturn(Optional.of(old));
        assertCode(() -> service.unassign(99L), "PAST_DATE");
        verify(assignments, never()).delete(any());
    }

    @Test
    void inactiveShiftAndDisabledStaffAreRejected() {
        assertCode(() -> service.assign(new AssignRequest(tomorrow, 4L, List.of(10L))), "WORK_SHIFT_INACTIVE");
        assertCode(() -> service.assign(new AssignRequest(tomorrow, 1L, List.of(12L))), "INVALID_STAFF");
        assertCode(() -> service.assign(new AssignRequest(tomorrow, 1L, List.of(999L))), "INVALID_STAFF");
        assertCode(() -> service.assign(new AssignRequest(tomorrow, 1L, List.of(13L))), "INVALID_STAFF");
    }

    @Test
    void staffOptionsExcludeAdminAndDisabled() {
        assertThat(service.staffOptions()).extracting(o -> o.userId()).containsExactly(10L, 11L);
    }

    @Test
    void alreadyAssignedIsSkippedAndNonOverlappingShiftAllowed() {
        when(assignments.findByWorkDateAndUserIdIn(eq(tomorrow), any())).thenReturn(List.of(
                assignment(1L, tomorrow, afternoon, 10L),   // cùng ca → bỏ qua
                assignment(2L, tomorrow, morning, 11L)));   // ca Sáng không chồng ca Chiều → được
        var created = service.assign(new AssignRequest(tomorrow, 3L, List.of(10L, 11L, 11L)));
        assertThat(created).extracting(a -> a.userId()).containsExactly(11L);
        assertThat(created.get(0).fullName()).isEqualTo("Bình");
    }

    @Test
    void overlappingShiftSameDayIsRejected() {
        when(assignments.findByWorkDateAndUserIdIn(eq(tomorrow), any()))
                .thenReturn(List.of(assignment(1L, tomorrow, morning, 10L)));
        assertCode(() -> service.assign(new AssignRequest(tomorrow, 2L, List.of(10L))), "SHIFT_OVERLAP");
        verify(assignments, never()).saveAllAndFlush(anyList());
    }

    @Test
    void scheduleRangeIsLimited() {
        assertCode(() -> service.schedule(tomorrow, tomorrow.minusDays(1)), "INVALID_RANGE");
        assertCode(() -> service.schedule(tomorrow, tomorrow.plusDays(42)), "INVALID_RANGE");
        assertThat(service.schedule(tomorrow, tomorrow.plusDays(41))).isEmpty();
    }

    @Test
    void scheduleKeepsNamesOfDisabledStaff() {
        when(assignments.findByWorkDateBetween(tomorrow, tomorrow))
                .thenReturn(List.of(assignment(5L, tomorrow, morning, 12L)));
        assertThat(service.schedule(tomorrow, tomorrow).get(0).fullName()).isEqualTo("Cường");
    }

    private static WorkShiftRequest req(String name, String start, String end) {
        return new WorkShiftRequest(name, LocalTime.parse(start), LocalTime.parse(end), "#1677FF");
    }

    private static WorkShift shift(Long id, String name, String start, String end, boolean active) {
        WorkShift s = new WorkShift();
        s.setId(id);
        s.setName(name);
        s.setStartTime(LocalTime.parse(start));
        s.setEndTime(LocalTime.parse(end));
        s.setColor("#1677ff");
        s.setActive(active);
        return s;
    }

    private static WorkAssignment assignment(Long id, LocalDate date, WorkShift s, Long userId) {
        WorkAssignment a = new WorkAssignment();
        a.setId(id);
        a.setWorkDate(date);
        a.setWorkShift(s);
        a.setUserId(userId);
        return a;
    }

    private static void assertCode(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOf(ApiException.class).extracting("code").isEqualTo(code);
    }
}
