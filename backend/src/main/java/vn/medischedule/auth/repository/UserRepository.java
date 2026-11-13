package vn.medischedule.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.medischedule.auth.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    Page<User> findByRole_Code(String roleCode, Pageable pageable);

    /** Lọc theo vai trò (tùy chọn) và từ khóa trong họ tên / email / tên đăng nhập. q đã ở dạng '%từ khóa%' chữ thường. */
    @Query("""
            select u from User u
            where (:role is null or u.role.code = :role)
              and (:q is null or lower(u.fullName) like :q or lower(u.email) like :q or lower(coalesce(u.username, '')) like :q)
            """)
    Page<User> search(@Param("role") String role, @Param("q") String q, Pageable pageable);
}
