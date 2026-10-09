package demo.pharma.customerpayment;

import java.math.BigDecimal;
import java.time.LocalDate;

import demo.pharma.common.entity.BaseEntity;
import demo.pharma.customer.Customer;
import demo.pharma.payment.PaymentMethod;
import demo.pharma.sale.Sale;
import demo.pharma.security.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_payments")
@Getter
@Setter
@NoArgsConstructor
public class CustomerPayment extends BaseEntity {
    @Column(nullable = false, unique = true, length = 60)
    private String paymentNumber;
    @ManyToOne
    private Customer customer;
    @ManyToOne
    private Sale sale;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    private String referenceNumber;
    @Column(nullable = false)
    private LocalDate paymentDate;
    @Column(length = 500)
    private String notes;
    @ManyToOne
    private AppUser createdBy;
}
