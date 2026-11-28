package vn.medischedule.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/** Request/response yêu cầu đặt bác sĩ. */
public final class DoctorRequestDtos {

    private DoctorRequestDtos() {}

    /** Khách chưa đăng nhập: kèm họ tên + SĐT. */
    public record GuestDoctorRequest(
            @NotNull(message = "Chọn bác sĩ") Long doctorUserId,
            @NotNull(message = "Chọn ngày khám") LocalDate date,
            @NotNull(message = "Chọn buổi khám") Long workShiftId,
            @NotBlank(message = "Nhập họ và tên") @Size(max = 120, message = "Họ tên tối đa 120 ký tự") String fullName,
            @NotBlank(message = "Nhập số điện thoại")
            @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 số, bắt đầu bằng 0") String phone,
            @Size(max = 500, message = "Mô tả triệu chứng tối đa 500 ký tự") String symptoms) {}

    /** Bệnh nhân đã đăng nhập: họ tên + SĐT lấy từ tài khoản. */
    public record PatientDoctorRequest(
            @NotNull(message = "Chọn bác sĩ") Long doctorUserId,
            @NotNull(message = "Chọn ngày khám") LocalDate date,
            @NotNull(message = "Chọn buổi khám") Long workShiftId,
            @Size(max = 500, message = "Mô tả triệu chứng tối đa 500 ký tự") String symptoms) {}

    public record DoctorRequestResponse(Long id, Long doctorUserId, String doctorName, LocalDate date, String workShiftName,
                                        LocalTime startTime, LocalTime endTime, String fullName, String phone,
                                        String symptoms, String status, OffsetDateTime createdAt) {}
}
