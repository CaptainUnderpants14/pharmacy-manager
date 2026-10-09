package demo.pharma.purchasereturn;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, UUID>, JpaSpecificationExecutor<PurchaseReturn> {
    boolean existsByReturnNumberIgnoreCase(String returnNumber);
}
