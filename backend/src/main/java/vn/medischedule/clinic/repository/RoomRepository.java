package vn.medischedule.clinic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.medischedule.clinic.domain.Room;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Room> findBySpecialtyIdOrderByName(Long specialtyId);

    boolean existsBySpecialtyIdAndActiveTrue(Long specialtyId);

    /** Mọi phòng thuộc chuyên khoa (cả đang ngừng), để đếm và lấy tên khi chỉ có một phòng. */
    List<Room> findBySpecialtyIsNotNull();
}
