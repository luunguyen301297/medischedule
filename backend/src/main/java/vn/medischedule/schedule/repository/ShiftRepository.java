package vn.medischedule.schedule.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.medischedule.schedule.domain.Shift;

import java.time.LocalDate;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Optional<Shift> findByDoctorIdAndWorkDateAndWorkShiftId(Long doctorId, LocalDate workDate, Long workShiftId);

    /** Khóa dòng ca khám tới hết transaction: đếm chỗ + ghi lịch không đua nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shift s where s.doctorId = :doctorId and s.workDate = :workDate and s.workShiftId = :workShiftId")
    Optional<Shift> lockByDoctorAndDateAndWorkShift(@Param("doctorId") Long doctorId, @Param("workDate") LocalDate workDate,
                                                    @Param("workShiftId") Long workShiftId);

    /**
     * Tạo ca khám nếu chưa có; hai request đồng thời thì một bên không chèn gì. Giờ truyền dạng chuỗi rồi CAST để
     * tránh lệch múi giờ khi bind LocalTime (xem WorkShift.startTime).
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO shift (doctor_id, room_id, work_date, start_time, end_time, slot_minutes, max_patients, status,
                               work_shift_id)
            VALUES (:doctorId, :roomId, :workDate, CAST(:startTime AS time), CAST(:endTime AS time), :slotMinutes,
                    :maxPatients, 'PUBLISHED', :workShiftId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("doctorId") Long doctorId, @Param("roomId") Long roomId, @Param("workDate") LocalDate workDate,
                       @Param("startTime") String startTime, @Param("endTime") String endTime,
                       @Param("slotMinutes") short slotMinutes, @Param("maxPatients") short maxPatients,
                       @Param("workShiftId") Long workShiftId);
}
