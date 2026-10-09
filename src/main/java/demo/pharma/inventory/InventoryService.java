package demo.pharma.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import demo.pharma.audit.AuditService;
import demo.pharma.catalog.Medicine;
import demo.pharma.common.exception.BusinessException;
import demo.pharma.common.exception.NotFoundException;
import demo.pharma.notification.NotificationService;
import demo.pharma.notification.NotificationType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private static final BigDecimal LOW_STOCK_THRESHOLD = BigDecimal.valueOf(5);

    private final MedicineBatchRepository batches;
    private final StockMovementRepository movements;
    private final AuditService audit;
    private final NotificationService notifications;

    @Transactional
    public MedicineBatch adjust(UUID batchId, BigDecimal quantity, MovementType type, String reason) {
        if (quantity == null || quantity.signum() <= 0)
            throw new BusinessException("Adjustment quantity must be greater than zero");
        if (type != MovementType.ADJUSTMENT_IN && type != MovementType.ADJUSTMENT_OUT && type != MovementType.DAMAGE)
            throw new BusinessException("Adjustment type must be ADJUSTMENT_IN, ADJUSTMENT_OUT or DAMAGE");
        MedicineBatch b = batches.findById(batchId).orElseThrow(() -> new NotFoundException("Batch", batchId));
        BigDecimal before = b.getQuantity(),
                after = type == MovementType.ADJUSTMENT_IN ? before.add(quantity) : before.subtract(quantity);
        if (after.signum() < 0)
            throw new BusinessException("Stock cannot become negative");
        b.setQuantity(after);
        batches.save(b);
        movement(b, type, quantity, before, after, "STOCK_ADJUSTMENT", b.getId(), reason);
        audit.logCurrent("STOCK_ADJUSTED", "MEDICINE_BATCH", b.getId(), before.toPlainString(), after.toPlainString());
        checkLowStock(b, before, after);
        return b;
    }

    @Transactional
    public void receive(MedicineBatch b, BigDecimal amount, UUID referenceId) {
        if (amount.signum() <= 0)
            throw new BusinessException("Received quantity must be greater than zero");
        BigDecimal before = b.getQuantity(), after = before.add(amount);
        b.setQuantity(after);
        batches.save(b);
        movement(b, MovementType.PURCHASE, amount, before, after, "PURCHASE_ORDER", referenceId, "Goods received");
    }

    public List<MedicineBatch> selectFefo(UUID medicineId) {
        return batches.findAvailableFefo(medicineId, LocalDate.now());
    }

    /**
     * Sends a LOW_STOCK broadcast notification when a medicine's total available
     * quantity falls below the threshold (5 units). Fires only on the downward
     * crossing so repeated sales or adjustments below the threshold do not spam.
     */
    public void checkLowStock(MedicineBatch batch, BigDecimal before, BigDecimal after) {
        BigDecimal delta = after.subtract(before);
        if (delta.signum() >= 0)
            return;
        Medicine med = batch.getMedicine();
        BigDecimal availableAfter = batches.findAvailableFefo(med.getId(), LocalDate.now())
                .stream()
                .map(b -> b.getQuantity().subtract(b.getReservedQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableBefore = availableAfter.subtract(delta);
        if (availableBefore.compareTo(LOW_STOCK_THRESHOLD) >= 0
                && availableAfter.compareTo(LOW_STOCK_THRESHOLD) < 0) {
            notifications.broadcast("Low Stock Alert",
                    med.getName() + " (" + med.getMedicineCode() + ") is low on stock: only "
                            + availableAfter.stripTrailingZeros().toPlainString()
                            + " units available across active batches. Reorder soon.",
                    NotificationType.LOW_STOCK);
        }
    }

    private void movement(MedicineBatch b, MovementType t, BigDecimal q, BigDecimal before, BigDecimal after, String rt,
            UUID rid, String reason) {
        StockMovement m = new StockMovement();
        m.setMedicineBatch(b);
        m.setMovementType(t);
        m.setQuantity(q);
        m.setPreviousQuantity(before);
        m.setNewQuantity(after);
        m.setReferenceType(rt);
        m.setReferenceId(rid);
        m.setReason(reason);
        movements.save(m);
    }
}
