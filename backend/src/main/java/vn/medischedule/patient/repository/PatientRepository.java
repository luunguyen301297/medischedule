package vn.medischedule.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.medischedule.patient.domain.Patient;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    /** Hồ sơ còn hiệu lực (chưa bị gộp) của một tài khoản. */
    List<Patient> findByOwnerUserIdAndMergedIntoIsNullOrderByIdAsc(Long ownerUserId);

    /** Hồ sơ còn hiệu lực có đúng SĐT này (tìm khi lễ tân đặt hộ). */
    List<Patient> findTop20ByPhoneAndMergedIntoIsNullOrderByIdAsc(String phone);
}
