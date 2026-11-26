package vn.medischedule.clinic.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chuyên khoa. */
@Entity
@Table(name = "specialty")
@Getter
@Setter
@NoArgsConstructor
public class Specialty {

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên chuyên khoa, duy nhất (vd. Da liễu, Nhi). */
    private String name;

    /** false = ẩn khỏi danh sách đặt lịch, giữ lại cho dữ liệu cũ. */
    private boolean active = true;
}
