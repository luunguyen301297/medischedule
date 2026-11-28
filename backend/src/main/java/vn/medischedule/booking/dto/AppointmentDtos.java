package vn.medischedule.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

/** Request/response lịch hẹn phía nhân viên (menu Lịch hẹn). */
public final class AppointmentDtos {

    private AppointmentDtos() {}

    /**
     * Một dòng danh sách lịch hẹn; cũng là response khi đặt hộ. Lịch chưa xếp bác sĩ: startAt, endAt, doctor*, roomName
     * null, giờ lấy theo buổi (workShiftStart/End "HH:mm").
     */
    public record AppointmentRow(Long id, String bookingCode, LocalDate visitDate, String workShiftName,
                                 String workShiftStart, String workShiftEnd, OffsetDateTime startAt, OffsetDateTime endAt,
                                 String status, String channel, boolean designated, Long patientId, String patientCode,
                                 String patientName, String phone, Long doctorUserId, String doctorName,
                                 String specialtyName, String roomName, String createdByName, String cancelReason,
                                 OffsetDateTime createdAt) {}

    /**
     * Nhân viên đặt hộ: chọn hồ sơ có sẵn (patientId) hoặc tạo mới (newPatient). doctorUserId null = khách không chỉ định,
     * phòng khám xếp bác sĩ khi check-in; có bác sĩ thì bắt buộc startTime.
     */
    public record StaffBookingRequest(
            @NotNull(message = "Chọn ngày khám") LocalDate date,
            @NotNull(message = "Chọn buổi khám") Long workShiftId,
            Long specialtyId,
            Long doctorUserId,
            LocalTime startTime,
            @NotBlank(message = "Nhập số điện thoại")
            @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 số, bắt đầu bằng 0") String phone,
            Long patientId,
            @Valid NewPatient newPatient) {}

    public record NewPatient(
            @NotBlank(message = "Nhập họ và tên") @Size(max = 120, message = "Họ tên tối đa 120 ký tự") String fullName,
            LocalDate dateOfBirth,
            @Pattern(regexp = "^(Nam|Nữ|Khác)?$", message = "Giới tính: Nam, Nữ hoặc Khác") String gender) {}

    public record CancelRequest(
            @NotBlank(message = "Nhập lý do hủy") @Size(max = 255, message = "Lý do tối đa 255 ký tự") String reason) {}

    /** Lưới giờ của bác sĩ trong một buổi. */
    public record SlotGrid(String doctorName, String specialtyName, String roomName, int slotMinutes, List<Slot> slots) {}

    public record Slot(LocalTime time, boolean available) {}
}
