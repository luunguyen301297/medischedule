package vn.medischedule.schedule.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

import java.time.LocalTime;

/** Ca làm việc chuẩn (Sáng, Chiều...) dùng khi phân lịch nhân viên. */
@Entity
@Table(name = "work_shift")
@Getter
@Setter
@NoArgsConstructor
public class WorkShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên ca, duy nhất không phân biệt hoa thường. */
    private String name;

    /**
     * LocalTimeJdbcType: đọc/ghi LocalTime thẳng qua driver. Mặc định Hibernate đổi qua java.sql.Time theo
     * hibernate.jdbc.time_zone tại ngày 1970-01-01 (VN khi đó +8) nên lệch 1 giờ.
     */
    @JdbcType(LocalTimeJdbcType.class)
    private LocalTime startTime;

    /** Luôn sau startTime (không có ca qua đêm). */
    @JdbcType(LocalTimeJdbcType.class)
    private LocalTime endTime;

    /** Màu hiển thị dạng #rrggbb. */
    private String color;

    /** false = không chọn được khi phân lịch mới, lịch cũ vẫn hiện. */
    private boolean active = true;

    /** Hai ca chồng giờ nhau. */
    public boolean overlaps(WorkShift o) {
        return startTime.isBefore(o.endTime) && o.startTime.isBefore(endTime);
    }
}
