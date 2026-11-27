package vn.medischedule.clinic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.medischedule.clinic.domain.Room;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Room> findBySpecialtyIdOrderByName(Long specialtyId);

    boolean existsBySpecialtyIdAndActiveTrue(Long specialtyId);

    /** Số phòng (cả đang ngừng) của từng chuyên khoa; chuyên khoa chưa có phòng thì không có dòng. */
    @Query("select r.specialty.id as specialtyId, count(r) as roomCount from Room r "
            + "where r.specialty is not null group by r.specialty.id")
    List<RoomCount> countBySpecialty();

    interface RoomCount {
        Long getSpecialtyId();

        long getRoomCount();
    }
}
