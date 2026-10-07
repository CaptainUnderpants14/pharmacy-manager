package demo.pharma.inventory;

import java.math.BigDecimal;
import java.util.UUID;

import demo.pharma.common.entity.BaseEntity;
import demo.pharma.security.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_movements", indexes = @Index(name = "idx_movement_batch", columnList = "medicineBatch_id"))
@Getter
@Setter
@NoArgsConstructor
public class StockMovement extends BaseEntity {
    @ManyToOne(optional = false)
    private MedicineBatch medicineBatch;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType movementType;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal previousQuantity;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal newQuantity;
    private String referenceType;
    private UUID referenceId;
    private String reason;
    @ManyToOne
    private AppUser createdBy;
}
