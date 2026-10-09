package demo.pharma.purchasereturn;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReturnItemRepository extends JpaRepository<PurchaseReturnItem, UUID> {
}
