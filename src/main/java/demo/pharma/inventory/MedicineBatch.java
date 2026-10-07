package demo.pharma.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

import demo.pharma.catalog.Medicine;
import demo.pharma.common.entity.BaseEntity;
import demo.pharma.supplier.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicine_batches", uniqueConstraints = @UniqueConstraint(name = "uk_batch_medicine", columnNames = {
        "medicine_id", "batchNumber" }), indexes = @Index(name = "idx_batch_expiry", columnList = "expiryDate"))
@Getter
@Setter
@NoArgsConstructor
public class MedicineBatch extends BaseEntity {
    @ManyToOne(optional = false)
    private Medicine medicine;
    @Column(nullable = false)
    private String batchNumber;
    private LocalDate manufacturingDate;
    @Column(nullable = false)
    private LocalDate expiryDate;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal purchasePrice;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal mrp;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal sellingPrice;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;
    @ManyToOne
    private Supplier supplier;
}
