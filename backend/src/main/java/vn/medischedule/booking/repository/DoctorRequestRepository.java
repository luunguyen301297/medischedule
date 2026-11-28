package vn.medischedule.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.medischedule.booking.domain.DoctorRequest;

import java.time.LocalDate;

public interface DoctorRequestRepository extends JpaRepository<DoctorRequest, Long> {

    long countByPhoneAndStatus(String phone, DoctorRequest.Status status);

    boolean existsByPhoneAndDoctorUserIdAndRequestedDateAndWorkShiftIdAndStatus(
            String phone, Long doctorUserId, LocalDate requestedDate, Long workShiftId, DoctorRequest.Status status);

    /** Khóa theo SĐT tới hết transaction: đếm + ghi không đua giữa hai request cùng SĐT. */
    @Query(value = "SELECT CAST(pg_advisory_xact_lock(hashtext(:phone)) AS text)", nativeQuery = true)
    String lockPhone(String phone);
}
