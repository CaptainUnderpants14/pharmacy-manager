package demo.pharma.prescription;

import java.time.LocalDate;

import demo.pharma.common.entity.BaseEntity;
import demo.pharma.customer.Customer;
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
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
public class Prescription extends BaseEntity {
    @Column(nullable = false, unique = true, length = 60)
    private String prescriptionNumber;
    @ManyToOne
    private Customer customer;
    @Column(nullable = false)
    private String doctorName;
    private String doctorLicenseNumber;
    private String hospitalName;
    @Column(nullable = false)
    private LocalDate prescriptionDate;
    @Column(columnDefinition = "text")
    private String notes;
    private String fileUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrescriptionStatus status = PrescriptionStatus.ACTIVE;
}
