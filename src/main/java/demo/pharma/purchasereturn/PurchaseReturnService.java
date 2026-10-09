package demo.pharma.purchasereturn;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import demo.pharma.audit.AuditService;
import demo.pharma.common.exception.BusinessException;
import demo.pharma.common.exception.NotFoundException;
import demo.pharma.inventory.MedicineBatch;
import demo.pharma.inventory.MedicineBatchRepository;
import demo.pharma.inventory.MovementType;
import demo.pharma.inventory.StockMovement;
import demo.pharma.inventory.StockMovementRepository;
import demo.pharma.purchase.PurchaseOrder;
import demo.pharma.purchase.PurchaseOrderRepository;
import demo.pharma.security.AppUser;
import demo.pharma.security.UserRepository;
import demo.pharma.supplier.Supplier;
import demo.pharma.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseReturnService {
    private final PurchaseReturnRepository returnRepo;
    private final SupplierRepository supplierRepo;
    private final PurchaseOrderRepository orderRepo;
    private final MedicineBatchRepository batchRepo;
    private final StockMovementRepository movementRepo;
    private final UserRepository userRepo;
    private final AuditService audit;

    public record ItemReturnRequest(UUID batchId, BigDecimal quantity, BigDecimal unitPrice) {
    }

    public record CreateReturnRequest(String returnNumber, UUID supplierId, UUID purchaseOrderId, LocalDate returnDate,
            PurchaseReturnStatus refundStatus, String reason, List<ItemReturnRequest> items) {
    }

    @Transactional
    public PurchaseReturn createReturn(CreateReturnRequest req) {
        if (req.supplierId() == null) {
            throw new BusinessException("Supplier ID is required");
        }
        Supplier supplier = supplierRepo.findById(req.supplierId())
                .orElseThrow(() -> new NotFoundException("Supplier", req.supplierId()));

        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessException("Purchase return must contain at least one item");
        }

        String retNum = req.returnNumber();
        if (retNum == null || retNum.isBlank()) {
            retNum = "PR-" + System.currentTimeMillis();
        } else if (returnRepo.existsByReturnNumberIgnoreCase(retNum)) {
            throw new BusinessException("Return number already exists: " + retNum);
        }

        PurchaseReturn pr = new PurchaseReturn();
        pr.setReturnNumber(retNum);
        pr.setSupplier(supplier);
        if (req.purchaseOrderId() != null) {
            PurchaseOrder po = orderRepo.findById(req.purchaseOrderId())
                    .orElseThrow(() -> new NotFoundException("PurchaseOrder", req.purchaseOrderId()));
            pr.setPurchaseOrder(po);
        }
        pr.setReturnDate(req.returnDate() != null ? req.returnDate() : LocalDate.now());
        pr.setRefundStatus(req.refundStatus() != null ? req.refundStatus() : PurchaseReturnStatus.PENDING);
        pr.setReason(req.reason());
        pr.setCreatedBy(getCurrentUser());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseReturnItem> returnItems = new ArrayList<>();

        for (ItemReturnRequest itemReq : req.items()) {
            if (itemReq.batchId() == null) {
                throw new BusinessException("Batch ID is required for purchase return items");
            }
            if (itemReq.quantity() == null || itemReq.quantity().signum() <= 0) {
                throw new BusinessException("Returned quantity must be greater than zero");
            }

            MedicineBatch batch = batchRepo.findById(itemReq.batchId())
                    .orElseThrow(() -> new NotFoundException("MedicineBatch", itemReq.batchId()));

            BigDecimal prevQty = batch.getQuantity();
            if (prevQty.compareTo(itemReq.quantity()) < 0) {
                throw new BusinessException("Cannot return more stock than available in batch " + batch.getBatchNumber());
            }

            BigDecimal unitPrice = itemReq.unitPrice() != null ? itemReq.unitPrice() : batch.getPurchasePrice();
            BigDecimal itemTotal = unitPrice.multiply(itemReq.quantity());

            // Deduct stock from batch
            BigDecimal newQty = prevQty.subtract(itemReq.quantity());
            batch.setQuantity(newQty);
            batchRepo.save(batch);

            // Record movement
            StockMovement m = new StockMovement();
            m.setMedicineBatch(batch);
            m.setMovementType(MovementType.PURCHASE_RETURN);
            m.setQuantity(itemReq.quantity());
            m.setPreviousQuantity(prevQty);
            m.setNewQuantity(newQty);
            m.setReferenceType("PURCHASE_RETURN");
            m.setReferenceId(pr.getId());
            m.setReason(req.reason() != null ? req.reason() : "Return to Supplier");
            movementRepo.save(m);

            PurchaseReturnItem pri = new PurchaseReturnItem();
            pri.setPurchaseReturn(pr);
            pri.setMedicineBatch(batch);
            pri.setQuantity(itemReq.quantity());
            pri.setUnitPrice(unitPrice);
            pri.setTotalAmount(itemTotal);

            returnItems.add(pri);

            totalAmount = totalAmount.add(itemTotal);
        }

        pr.setTotalAmount(totalAmount);
        pr.setItems(returnItems);

        returnRepo.save(pr);
        audit.logCurrent("PURCHASE_RETURN_CREATED", "PURCHASE_RETURN", pr.getId(), null, pr.getReturnNumber());
        return pr;
    }

    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        return userRepo.findByUsernameIgnoreCaseOrEmailIgnoreCase(name, name).orElse(null);
    }
}
