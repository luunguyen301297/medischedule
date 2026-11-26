package vn.medischedule.pharmacy.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Danh mục thuốc; tồn kho = tổng quantity các lô. */
@Entity
@Table(name = "drug")
@Getter
@Setter
@NoArgsConstructor
public class Drug {

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên thuốc (biệt dược). */
    private String name;

    /** Hoạt chất; đối chiếu với dị ứng của bệnh nhân khi kê đơn. */
    private String activeIngredient;

    /** Đơn vị bán (viên, chai...). */
    private String unit;

    /** Giá bán mỗi đơn vị (VND). */
    private BigDecimal salePrice;

    /** Ngưỡng cảnh báo sắp hết; so với tổng quantity các lô. */
    private int minStock = 0;

    /** false = ngừng kinh doanh. */
    private boolean active = true;
}
