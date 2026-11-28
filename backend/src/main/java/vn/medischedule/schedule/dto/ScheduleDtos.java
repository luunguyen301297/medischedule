package vn.medischedule.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.medischedule.schedule.domain.WorkShift;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Request/response của ca làm việc và lịch làm việc. */
public final class ScheduleDtos {

    private ScheduleDtos() {}

    public record WorkShiftRequest(
            @NotBlank(message = "Nhập tên ca")
            @Size(max = 50, message = "Tên ca tối đa 50 ký tự") String name,
            @NotNull(message = "Chọn giờ bắt đầu") LocalTime startTime,
            @NotNull(message = "Chọn giờ kết thúc") LocalTime endTime,
            @NotNull(message = "Chọn màu")
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Màu dạng #rrggbb") String color) {}

    public record WorkShiftResponse(Long id, String name, LocalTime startTime, LocalTime endTime, String color,
                                    boolean active) {
        public static WorkShiftResponse of(WorkShift s) {
            return new WorkShiftResponse(s.getId(), s.getName(), s.getStartTime(), s.getEndTime(), s.getColor(),
                    s.isActive());
        }
    }

    public record AssignRequest(
            @NotNull(message = "Chọn ngày") LocalDate date,
            @NotNull(message = "Chọn ca") Long shiftId,
            @NotEmpty(message = "Chọn ít nhất một nhân viên") List<@NotNull Long> userIds) {}

    public record AssignmentResponse(Long id, LocalDate date, Long shiftId, Long userId, String fullName, String role) {}

    /** Nhân viên đang hoạt động cho ô chọn khi phân ca. */
    public record StaffOption(Long userId, String fullName, String role) {}

    /** Ca khám của bác sĩ (bảng shift) cho đặt lịch; id null = ca dự kiến, chưa lưu. */
    public record ClinicShift(Long id, Long roomId, LocalTime startTime, LocalTime endTime, int slotMinutes,
                              int maxPatients) {}
}
