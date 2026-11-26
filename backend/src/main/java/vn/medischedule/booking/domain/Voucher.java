package vn.medischedule.booking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Mã ưu đãi. */
@Entity
@Table(name = "voucher")
@Getter
@Setter
@NoArgsConstructor
public class Voucher {

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã ưu đãi bệnh nhân nhập, duy nhất. */
    private String code;

    /** Phần trăm giảm (1–100). */
    private Short discountPercent;

    /** Ngày hết hạn (tính cả ngày này). */
    private LocalDate validTo;

    /** Số lượt dùng tối đa; NULL = không giới hạn. */
    private Integer usageLimit;

    /** Số lượt đã dùng. */
    private int usedCount = 0;
}
