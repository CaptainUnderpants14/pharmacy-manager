package demo.pharma.salereturn;

import java.math.BigDecimal;

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
@Table(name = "sale_return_items")
@Getter
@Setter
@NoArgsConstructor
public class SaleReturnItem extends BaseEntity {
    @ManyToOne(optional = false)
    private SaleReturn saleReturn;
    @ManyToOne
    private MedicineBatch medicineBatch;
    @Column(precision = 14, scale = 3, nullable = false)
    private BigDecimal quantity;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal unitPrice;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal refundAmount;
    private boolean restock = true;
}
