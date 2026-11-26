package vn.medischedule.clinic.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phòng khám, phòng xét nghiệm. */
@Entity
@Table(name = "room")
@Getter
@Setter
@NoArgsConstructor
public class Room {

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên/số phòng, duy nhất (vd. P101). */
    private String name;

    /** Chuyên khoa dùng phòng; NULL với phòng dùng chung (xét nghiệm). */
    @ManyToOne(fetch = FetchType.LAZY)
    private Specialty specialty;

    /** false = phòng ngừng sử dụng. */
    private boolean active = true;
}
