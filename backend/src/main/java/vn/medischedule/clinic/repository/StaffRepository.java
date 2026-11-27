package vn.medischedule.clinic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.medischedule.clinic.domain.Staff;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    List<Staff> findBySpecialtyIsNotNull();

    List<Staff> findBySpecialtyId(Long specialtyId);

    Optional<Staff> findByUserId(Long userId);

    List<Staff> findByUserIdIn(Collection<Long> userIds);
}
