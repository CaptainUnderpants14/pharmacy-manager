package demo.pharma.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

import demo.pharma.common.entity.BaseEntity;
import demo.pharma.security.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employees", indexes = { @Index(name = "idx_employee_code", columnList = "employeeCode"),
        @Index(name = "idx_employee_email", columnList = "email") })
@Getter
@Setter
@NoArgsConstructor
public class Employee extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String employeeCode;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    private String phone;
    private String email;
    private String address;
    private String designation;
    private LocalDate joiningDate;
    private BigDecimal salary;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;
    private String branch;
    @OneToOne
    private AppUser user;
}
