package vn.medischedule.clinic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import vn.medischedule.auth.dto.AuthDtos.DoctorAccount;
import vn.medischedule.auth.service.AuthService;
import vn.medischedule.clinic.domain.Room;
import vn.medischedule.clinic.domain.Specialty;
import vn.medischedule.clinic.domain.Staff;
import vn.medischedule.clinic.dto.ClinicDtos.RoomRequest;
import vn.medischedule.clinic.dto.ClinicDtos.SpecialtyOption;
import vn.medischedule.clinic.dto.ClinicDtos.SpecialtyRequest;
import vn.medischedule.clinic.repository.RoomRepository;
import vn.medischedule.clinic.repository.SpecialtyRepository;
import vn.medischedule.clinic.repository.StaffRepository;
import vn.medischedule.common.exception.ApiException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Unit test (không Spring, không DB): rule danh mục chuyên khoa và phòng khám. */
class ClinicCatalogServiceTest {

    SpecialtyRepository specialties = mock(SpecialtyRepository.class);
    RoomRepository rooms = mock(RoomRepository.class);
    StaffRepository staff = mock(StaffRepository.class);
    AuthService auth = mock(AuthService.class);
    ClinicCatalogService service = new ClinicCatalogService(specialties, rooms, staff, auth);

    Specialty open = specialty(1L, "Nhi", true);
    Specialty closed = specialty(2L, "Mắt", false);
    Room room = room(10L, "P101", open);
    Room shared = room(11L, "XN1", null);

    @BeforeEach
    void setUp() {
        when(specialties.findById(1L)).thenReturn(Optional.of(open));
        when(specialties.findById(2L)).thenReturn(Optional.of(closed));
        when(rooms.findById(10L)).thenReturn(Optional.of(room));
        when(rooms.findById(11L)).thenReturn(Optional.of(shared));
        when(specialties.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(rooms.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void duplicateSpecialtyNameIsRejectedAfterTrim() {
        when(specialties.existsByNameIgnoreCase("nhi")).thenReturn(true);
        assertCode(() -> service.createSpecialty(new SpecialtyRequest(" nhi ", null)), "SPECIALTY_EXISTS");
        verify(specialties, never()).saveAndFlush(any());
    }

    @Test
    void renameToAnotherSpecialtyNameIsRejectedAndEntityUntouched() {
        when(specialties.existsByNameIgnoreCaseAndIdNot("Mắt", 1L)).thenReturn(true);
        assertCode(() -> service.renameSpecialty(1L, new SpecialtyRequest("Mắt", null)), "SPECIALTY_EXISTS");
        assertThat(open.getName()).isEqualTo("Nhi");
    }

    @Test
    void createdSpecialtyIsActiveWithTrimmedName() {
        var res = service.createSpecialty(new SpecialtyRequest("  Da liễu  ", null));
        assertThat(res.name()).isEqualTo("Da liễu");
        assertThat(res.active()).isTrue();
        assertThat(res.roomCount()).isZero();
    }

    @Test
    void concurrentDuplicateNameFromIndexBecomesExists() {
        when(specialties.saveAndFlush(any())).thenThrow(violation("uq_specialty_name_ci"));
        assertCode(() -> service.createSpecialty(new SpecialtyRequest("Nhi", null)), "SPECIALTY_EXISTS");
        when(rooms.saveAndFlush(any())).thenThrow(violation("room_name_key"));
        assertCode(() -> service.createRoom(new RoomRequest("P102", 1L)), "ROOM_EXISTS");
    }

    @Test
    void otherConstraintViolationOnSaveIsNotReportedAsDuplicate() {
        when(rooms.saveAndFlush(any())).thenThrow(violation("room_specialty_id_fkey"));
        assertThatThrownBy(() -> service.createRoom(new RoomRequest("P102", 1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cannotDeactivateSpecialtyWithActiveRooms() {
        when(rooms.existsBySpecialtyIdAndActiveTrue(1L)).thenReturn(true);
        assertCode(() -> service.setSpecialtyActive(1L, false), "SPECIALTY_HAS_ACTIVE_ROOMS");
        assertThat(open.isActive()).isTrue();
    }

    @Test
    void deactivateAndReactivateSpecialty() {
        assertThat(service.setSpecialtyActive(1L, false).active()).isFalse();
        assertThat(service.setSpecialtyActive(1L, true).active()).isTrue();
    }

    @Test
    void deleteBlockedByForeignKeyReportsInUse() {
        doThrow(violation("service_specialty_id_fkey")).when(specialties).flush();
        assertCode(() -> service.deleteSpecialty(1L), "SPECIALTY_IN_USE");
        doThrow(violation("shift_room_id_fkey")).when(rooms).flush();
        assertCode(() -> service.deleteRoom(10L), "ROOM_IN_USE");
    }

    @Test
    void unusedRoomIsDeletedAndFlushed() {
        service.deleteRoom(10L);
        verify(rooms).delete(room);
        verify(rooms).flush();
    }

    @Test
    void duplicateRoomNameIsRejected() {
        when(rooms.existsByNameIgnoreCase("p101")).thenReturn(true);
        assertCode(() -> service.createRoom(new RoomRequest("p101", 1L)), "ROOM_EXISTS");
        when(rooms.existsByNameIgnoreCaseAndIdNot("XN1", 10L)).thenReturn(true);
        assertCode(() -> service.updateRoom(10L, new RoomRequest("XN1", 1L)), "ROOM_EXISTS");
        assertThat(room.getName()).isEqualTo("P101");
    }

    @Test
    void inactiveSpecialtyRejectsNewMovedAndReactivatedRooms() {
        assertCode(() -> service.createRoom(new RoomRequest("P102", 2L)), "SPECIALTY_INACTIVE");
        room.setActive(false); // chuyển vào bị chặn kể cả khi phòng đang ngừng
        assertCode(() -> service.updateRoom(10L, new RoomRequest("P101", 2L)), "SPECIALTY_INACTIVE");
        assertThat(room.getSpecialty()).isSameAs(open);
        room.setSpecialty(closed);
        assertCode(() -> service.setRoomActive(10L, true), "SPECIALTY_INACTIVE");
        assertThat(room.isActive()).isFalse();
    }

    @Test
    void roomInInactiveSpecialtyCanStillBeRenamed() {
        room.setSpecialty(closed);
        assertThat(service.updateRoom(10L, new RoomRequest("P201", 2L)).name()).isEqualTo("P201");
    }

    @Test
    void unknownIdsAreNotFound() {
        assertCode(() -> service.setSpecialtyActive(99L, true), "SPECIALTY_NOT_FOUND");
        assertCode(() -> service.listRooms(99L), "SPECIALTY_NOT_FOUND");
        assertCode(() -> service.createRoom(new RoomRequest("P102", 99L)), "SPECIALTY_NOT_FOUND");
        assertCode(() -> service.deleteRoom(99L), "ROOM_NOT_FOUND");
    }

    @Test
    void sharedRoomIsOutOfScope() {
        assertCode(() -> service.updateRoom(11L, new RoomRequest("XN2", 1L)), "ROOM_NOT_FOUND");
        assertCode(() -> service.setRoomActive(11L, false), "ROOM_NOT_FOUND");
        assertCode(() -> service.deleteRoom(11L), "ROOM_NOT_FOUND");
        verify(rooms, never()).delete(any());
    }

    @Test
    void publicOptionsAreSortedByVietnameseAlphabet() {
        when(specialties.findByActiveTrue()).thenReturn(List.of(
                specialty(3L, "Tim mạch", true), specialty(4L, "Đông y", true), specialty(5L, "Da liễu", true)));
        assertThat(service.activeSpecialtyOptions()).extracting(SpecialtyOption::name)
                .containsExactly("Da liễu", "Đông y", "Tim mạch");
    }

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOf(ApiException.class).extracting("code").isEqualTo(code);
    }

    private static DataIntegrityViolationException violation(String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("violates constraint \"" + constraint + "\""));
    }

    private static Specialty specialty(Long id, String name, boolean active) {
        Specialty s = new Specialty();
        s.setId(id);
        s.setName(name);
        s.setActive(active);
        return s;
    }

    private static Room room(Long id, String name, Specialty specialty) {
        Room r = new Room();
        r.setId(id);
        r.setName(name);
        r.setSpecialty(specialty);
        return r;
    }

    // ---------- Gán bác sĩ ----------

    @Test
    void assignCreatesStaffMovesFromOtherSpecialtyAndUnassignsRemoved() {
        when(auth.activeDoctors()).thenReturn(List.of(
                new DoctorAccount(100L, "BS Mới"), new DoctorAccount(101L, "BS Khoa Mắt"), new DoctorAccount(102L, "BS Bị Bỏ")));
        Staff fromEye = staffOf(101L, closed);
        Staff removed = staffOf(102L, open);
        when(staff.findBySpecialtyId(1L)).thenReturn(List.of(removed));
        when(staff.findByUserIdIn(any())).thenReturn(List.of(fromEye));
        when(staff.save(any())).thenAnswer(i -> i.getArgument(0));

        service.renameSpecialty(1L, new SpecialtyRequest("Nhi", List.of(100L, 101L, 101L)));

        assertThat(fromEye.getSpecialty()).isSameAs(open);
        assertThat(removed.getSpecialty()).isNull();
        verify(staff).save(argThat(st -> st.getUserId() == 100L && st.getFullName().equals("BS Mới")));
    }

    @Test
    void inactiveOrUnknownDoctorRejectedBeforeAnyChange() {
        when(auth.activeDoctors()).thenReturn(List.of(new DoctorAccount(100L, "BS A")));
        Staff current = staffOf(100L, open);
        when(staff.findBySpecialtyId(1L)).thenReturn(List.of(current));

        assertCode(() -> service.renameSpecialty(1L, new SpecialtyRequest("Nhi", List.of(999L))), "DOCTOR_INVALID");
        assertThat(current.getSpecialty()).isSameAs(open);
        verify(staff, never()).save(any());
    }

    @Test
    void lockedDoctorKeepsSpecialtyAndIsNotCounted() {
        when(auth.activeDoctors()).thenReturn(List.of(new DoctorAccount(100L, "BS A")));
        Staff locked = staffOf(200L, open);
        Staff activeDoc = staffOf(100L, open);
        when(staff.findBySpecialtyId(1L)).thenReturn(List.of(locked, activeDoc));
        when(staff.findByUserIdIn(any())).thenReturn(List.of(activeDoc));
        when(staff.findBySpecialtyIsNotNull()).thenReturn(List.of(locked, activeDoc));
        when(rooms.findBySpecialtyIsNotNull()).thenReturn(List.of(room));

        var res = service.renameSpecialty(1L, new SpecialtyRequest("Nhi", List.of(100L)));

        assertThat(locked.getSpecialty()).isSameAs(open);
        assertThat(res.doctorUserIds()).containsExactly(100L);
        assertThat(res.singleDoctorName()).isEqualTo("BS A");
        assertThat(res.singleRoomName()).isEqualTo("P101");
    }

    private static Staff staffOf(Long userId, Specialty sp) {
        Staff st = new Staff();
        st.setUserId(userId);
        st.setSpecialty(sp);
        return st;
    }
}
