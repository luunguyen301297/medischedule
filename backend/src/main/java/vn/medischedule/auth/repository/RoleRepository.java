package vn.medischedule.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.medischedule.auth.domain.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByCode(String code);
}
