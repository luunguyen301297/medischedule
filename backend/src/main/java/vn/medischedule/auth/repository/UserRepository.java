package vn.medischedule.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.medischedule.auth.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    Page<User> findByRole_Code(String roleCode, Pageable pageable);

    List<User> findByRole_CodeAndEnabledTrue(String roleCode);

    @EntityGraph(attributePaths = "role")
    List<User> findByRole_CodeNot(String roleCode);

    /**
     * Lọc theo vai trò (tùy chọn), loại trừ một vai trò (tùy chọn) và từ khóa trong họ tên / email / tên đăng nhập / SĐT.
     * q đã ở dạng '%từ khóa%' chữ thường.
     */
    @Query("""
            select u from User u
            where (:role is null or u.role.code = :role)
              and (:notRole is null or u.role.code <> :notRole)
              and (:q is null or lower(u.fullName) like :q or lower(u.email) like :q or lower(coalesce(u.username, '')) like :q
                   or coalesce(u.phone, '') like :q)
            """)
    Page<User> search(@Param("role") String role, @Param("notRole") String notRole, @Param("q") String q, Pageable pageable);
}
