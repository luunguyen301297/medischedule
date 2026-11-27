package vn.medischedule.clinic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.medischedule.clinic.domain.Specialty;

import java.util.List;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Specialty> findByActiveTrue();
}
