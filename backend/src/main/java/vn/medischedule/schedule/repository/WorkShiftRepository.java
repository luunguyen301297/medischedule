package vn.medischedule.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.medischedule.schedule.domain.WorkShift;

import java.util.List;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<WorkShift> findAllByOrderByStartTimeAscNameAsc();
}
