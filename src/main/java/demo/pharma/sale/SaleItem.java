package demo.pharma.sale;

import java.math.BigDecimal;

import demo.pharma.catalog.Medicine;
import demo.pharma.common.entity.BaseEntity;
import demo.pharma.inventory.MedicineBatch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sale_items")
@Getter
@Setter
@NoArgsConstructor
public class SaleItem extends BaseEntity {
    @ManyToOne(optional = false)
    private Sale sale;
    @ManyToOne(optional = false)
    private Medicine medicine;
    @ManyToOne
    private MedicineBatch medicineBatch;
    private String batchNumber;
    @Column(precision = 14, scale = 3, nullable = false)
    private BigDecimal quantity;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal unitPrice;
    @Column(precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal total;
}
