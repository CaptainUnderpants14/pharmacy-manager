package demo.pharma.prescription;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID>, JpaSpecificationExecutor<Prescription> {
    boolean existsByPrescriptionNumberIgnoreCase(String prescriptionNumber);
}
