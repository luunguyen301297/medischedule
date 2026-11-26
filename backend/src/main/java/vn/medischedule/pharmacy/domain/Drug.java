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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String activeIngredient;

    private String unit;

    private BigDecimal salePrice;

    private int minStock = 0;

    private boolean active = true;
}
