package demo.pharma.purchase;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, UUID>, JpaSpecificationExecutor<PurchaseOrder> {
    boolean existsByPurchaseOrderNumber(String number);

    long countByStatus(PurchaseOrderStatus status);
}
