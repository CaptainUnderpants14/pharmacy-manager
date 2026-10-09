package demo.pharma.customer;

import java.math.BigDecimal;
import java.time.LocalDate;

import demo.pharma.catalog.RecordStatus;
import demo.pharma.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_code", columnList = "customerCode"),
        @Index(name = "idx_customer_phone", columnList = "phone")
})
@Getter
@Setter
@NoArgsConstructor
public class Customer extends BaseEntity {
    @Column(nullable = false, unique = true, length = 60)
    private String customerCode;
    @Column(nullable = false)
    private String name;
    private String phone;
    private String email;
    private String address;
    private String gender;
    private LocalDate dateOfBirth;
    @Column(columnDefinition = "text")
    private String medicalHistory;
    @Column(precision = 14, scale = 2)
    private BigDecimal creditBalance = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status = RecordStatus.ACTIVE;
}
