package demo.pharma.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

import demo.pharma.branch.Branch;
import demo.pharma.common.entity.BaseEntity;
import demo.pharma.payment.PaymentMethod;
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
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expense_category", columnList = "category_id"),
        @Index(name = "idx_expense_date", columnList = "expenseDate")
})
@Getter
@Setter
@NoArgsConstructor
public class Expense extends BaseEntity {
    @Column(nullable = false, unique = true, length = 60)
    private String expenseNumber;
    @ManyToOne(optional = false)
    private ExpenseCategory category;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private LocalDate expenseDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    private String referenceNumber;
    private String payee;
    @Column(length = 500)
    private String description;
    @ManyToOne
    private Branch branch;
    @ManyToOne
    private AppUser createdBy;
}
