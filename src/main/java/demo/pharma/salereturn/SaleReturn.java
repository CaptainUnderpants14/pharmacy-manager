package demo.pharma.salereturn;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import demo.pharma.common.entity.BaseEntity;
import demo.pharma.customer.Customer;
import demo.pharma.payment.PaymentMethod;
import demo.pharma.sale.Sale;
import demo.pharma.security.AppUser;
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
@Table(name = "sale_returns")
@Getter
@Setter
@NoArgsConstructor
public class SaleReturn extends BaseEntity {
    @Column(nullable = false, unique = true, length = 60)
    private String returnNumber;
    @ManyToOne(optional = false)
    private Sale sale;
    @ManyToOne
    private Customer customer;
    @Column(nullable = false)
    private Instant returnDate;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal refundAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod refundMethod = PaymentMethod.CASH;
    private String reason;
    @ManyToOne
    private AppUser createdBy;
    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleReturnItem> items = new ArrayList<>();
}
