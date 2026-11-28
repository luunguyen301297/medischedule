package vn.medischedule.booking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.medischedule.booking.domain.Appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByBookingCode(String bookingCode);

    long countByBookerPhoneAndStatus(String bookerPhone, Appointment.Status status);

    long countByShiftIdAndStatusNot(Long shiftId, Appointment.Status status);

    /** Giờ đã có lịch (chưa hủy, không tính walk-in) của bác sĩ trong khoảng. */
    @Query("""
            select a.startAt from Appointment a
            where a.doctorId = :doctorId and a.startAt >= :from and a.startAt < :to
              and a.status <> vn.medischedule.booking.domain.Appointment.Status.CANCELLED
              and a.channel <> vn.medischedule.booking.domain.Appointment.Channel.WALK_IN
            """)
    List<OffsetDateTime> findTakenStartAt(@Param("doctorId") Long doctorId, @Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> lockById(@Param("id") Long id);

    /** Khóa theo SĐT người đặt tới hết transaction: đếm lịch chờ + ghi không đua. Khóa riêng với doctor_request. */
    @Query(value = "SELECT CAST(pg_advisory_xact_lock(hashtext('appointment:' || :phone)) AS text)", nativeQuery = true)
    String lockPhone(@Param("phone") String phone);

    // ---------- Read-model danh sách ----------
    // Lịch chưa xếp bác sĩ không có ca khám / bác sĩ / phòng nên các bảng đó LEFT JOIN.
    // Ngoại lệ có chủ đích với quy tắc "không gọi chéo repository": chỉ ĐỌC, join bảng của module patient / clinic /
    // schedule / auth để lọc, tìm và phân trang trong một query. Mọi thao tác ghi vẫn đi qua service module sở hữu.
    // Tham số rỗng dùng giá trị canh (0, '') thay vì NULL để Postgres suy được kiểu tham số.

    String ROW_SELECT = """
            SELECT a.id AS "id", a.booking_code AS "bookingCode", a.start_at AS "startAt",
                   a.start_at + make_interval(mins => sh.slot_minutes) AS "endAt", a.status AS "status",
                   a.channel AS "channel", p.id AS "patientId", p.patient_code AS "patientCode",
                   p.full_name AS "patientName", a.booker_phone AS "phone", st.user_id AS "doctorUserId",
                   st.full_name AS "doctorName", COALESCE(sp.name, asp.name) AS "specialtyName", r.name AS "roomName",
                   cu.full_name AS "createdByName", a.cancel_reason AS "cancelReason", a.created_at AS "createdAt",
                   to_char(a.visit_date, 'YYYY-MM-DD') AS "visitDate", ws.name AS "workShiftName",
                   to_char(ws.start_time, 'HH24:MI') AS "workShiftStart", to_char(ws.end_time, 'HH24:MI') AS "workShiftEnd",
                   a.is_designated AS "designated"
            """;

    String ROW_FROM = """
            FROM appointment a
            JOIN patient p ON p.id = a.patient_id
            LEFT JOIN shift sh ON sh.id = a.shift_id
            LEFT JOIN staff st ON st.id = a.doctor_id
            LEFT JOIN room r ON r.id = sh.room_id
            LEFT JOIN specialty sp ON sp.id = r.specialty_id
            LEFT JOIN specialty asp ON asp.id = a.specialty_id
            LEFT JOIN work_shift ws ON ws.id = a.work_shift_id
            LEFT JOIN users cu ON cu.id = a.created_by_user_id
            """;

    String ROW_FILTER = """
            WHERE a.visit_date BETWEEN :from AND :to
              AND (:doctorUserId = 0 OR st.user_id = :doctorUserId)
              AND (:status = '' OR a.status = :status)
              AND (:q = '' OR p.full_name ILIKE :qLike ESCAPE '\\' OR a.booker_phone LIKE :qPrefix
                   OR p.phone LIKE :qPrefix OR a.booking_code = :qCode)
            """;

    @Query(value = ROW_SELECT + ROW_FROM + ROW_FILTER + " ORDER BY a.visit_date, ws.start_time NULLS LAST, a.start_at NULLS LAST, a.id"
            + " LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<AppointmentRowView> searchRows(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                        @Param("doctorUserId") long doctorUserId, @Param("status") String status,
                                        @Param("q") String q, @Param("qLike") String qLike,
                                        @Param("qPrefix") String qPrefix, @Param("qCode") String qCode,
                                        @Param("limit") int limit, @Param("offset") long offset);

    @Query(value = "SELECT count(*) " + ROW_FROM + ROW_FILTER, nativeQuery = true)
    long countRows(@Param("from") LocalDate from, @Param("to") LocalDate to,
                   @Param("doctorUserId") long doctorUserId, @Param("status") String status, @Param("q") String q,
                   @Param("qLike") String qLike, @Param("qPrefix") String qPrefix, @Param("qCode") String qCode);

    @Query(value = ROW_SELECT + ROW_FROM + " WHERE a.id = :id", nativeQuery = true)
    Optional<AppointmentRowView> findRow(@Param("id") Long id);

    /** Projection của read-model; tên getter khớp alias trong ROW_SELECT. timestamptz của native query về Instant. */
    interface AppointmentRowView {
        Long getId();
        String getBookingCode();
        Instant getStartAt();
        Instant getEndAt();
        String getStatus();
        String getChannel();
        Long getPatientId();
        String getPatientCode();
        String getPatientName();
        String getPhone();
        Long getDoctorUserId();
        String getDoctorName();
        String getSpecialtyName();
        String getRoomName();
        String getCreatedByName();
        String getCancelReason();
        Instant getCreatedAt();
        String getVisitDate();
        String getWorkShiftName();
        String getWorkShiftStart();
        String getWorkShiftEnd();
        Boolean getDesignated();
    }
}
