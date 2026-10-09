package demo.pharma.purchasereturn;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import demo.pharma.common.entity.BaseEntity;
import demo.pharma.purchase.PurchaseOrder;
import demo.pharma.security.AppUser;
import demo.pharma.supplier.Supplier;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_returns")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseReturn extends BaseEntity {
    @Column(nullable = false, unique = true, length = 60)
    private String returnNumber;
    @ManyToOne(optional = false)
    private Supplier supplier;
    @ManyToOne
    private PurchaseOrder purchaseOrder;
    @Column(nullable = false)
    private LocalDate returnDate;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseReturnStatus refundStatus = PurchaseReturnStatus.PENDING;
    private String reason;
    @ManyToOne
    private AppUser createdBy;
    @OneToMany(mappedBy = "purchaseReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseReturnItem> items = new ArrayList<>();
}
