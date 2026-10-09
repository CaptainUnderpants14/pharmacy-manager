package demo.pharma.purchase;

import java.math.BigDecimal;

import demo.pharma.catalog.Medicine;
import demo.pharma.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderItem extends BaseEntity {
    @ManyToOne(optional = false)
    private PurchaseOrder purchaseOrder;
    @ManyToOne(optional = false)
    private Medicine medicine;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;
    @Column(precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;
}
