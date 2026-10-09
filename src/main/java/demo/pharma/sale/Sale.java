package demo.pharma.sale;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import demo.pharma.branch.Branch;
import demo.pharma.common.entity.BaseEntity;
import demo.pharma.customer.Customer;
import demo.pharma.payment.PaymentMethod;
import demo.pharma.payment.PaymentStatus;
import demo.pharma.prescription.Prescription;
import demo.pharma.security.AppUser;
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
@Table(name = "sales", indexes = {
        @Index(name = "idx_sales_number", columnList = "saleNumber"),
        @Index(name = "idx_sales_customer", columnList = "customer_id"),
        @Index(name = "idx_sales_date", columnList = "saleDate")
})
@Getter
@Setter
@NoArgsConstructor
public class Sale extends BaseEntity {
    @Column(nullable = false, unique = true, length = 60)
    private String saleNumber;
    @ManyToOne
    private Customer customer;
    @ManyToOne
    private Branch branch;
    @ManyToOne
    private Prescription prescription;
    @Column(nullable = false)
    private Instant saleDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status = SaleStatus.COMPLETED;
    @Column(precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    @Column(length = 500)
    private String notes;
    @ManyToOne
    private AppUser createdBy;
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();
}
