package demo.pharma.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import demo.pharma.common.entity.BaseEntity;
import demo.pharma.security.AppUser;
import demo.pharma.supplier.Supplier;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_orders", indexes = { @Index(name = "idx_purchase_number", columnList = "purchaseOrderNumber"),
        @Index(name = "idx_purchase_status", columnList = "status") })
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String purchaseOrderNumber;
    @ManyToOne(optional = false)
    private Supplier supplier;
    @Column(nullable = false)
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;
    @Column(precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String notes;
    @ManyToOne
    private AppUser createdBy;
    @ManyToOne
    private AppUser approvedBy;
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();
}
