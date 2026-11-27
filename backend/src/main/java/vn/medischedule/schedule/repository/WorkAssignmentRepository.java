package vn.medischedule.schedule.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.medischedule.schedule.domain.WorkAssignment;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignment, Long> {

    @EntityGraph(attributePaths = "workShift")
    List<WorkAssignment> findByWorkDateBetween(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "workShift")
    List<WorkAssignment> findByWorkDateAndUserIdIn(LocalDate date, Collection<Long> userIds);
}
