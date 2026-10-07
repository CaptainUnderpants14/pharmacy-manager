package demo.pharma.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import demo.pharma.audit.AuditService;
import demo.pharma.common.exception.BusinessException;
import demo.pharma.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final MedicineBatchRepository batches;
    private final StockMovementRepository movements;
    private final AuditService audit;

    @Transactional
    public MedicineBatch adjust(UUID batchId, BigDecimal quantity, MovementType type, String reason) {
        if (quantity == null || quantity.signum() <= 0)
            throw new BusinessException("Adjustment quantity must be greater than zero");
        if (type != MovementType.ADJUSTMENT_IN && type != MovementType.ADJUSTMENT_OUT)
            throw new BusinessException("Adjustment type must be ADJUSTMENT_IN or ADJUSTMENT_OUT");
        MedicineBatch b = batches.findById(batchId).orElseThrow(() -> new NotFoundException("Batch", batchId));
        BigDecimal before = b.getQuantity(),
                after = type == MovementType.ADJUSTMENT_IN ? before.add(quantity) : before.subtract(quantity);
        if (after.signum() < 0)
            throw new BusinessException("Stock cannot become negative");
        b.setQuantity(after);
        batches.save(b);
        movement(b, type, quantity, before, after, "STOCK_ADJUSTMENT", b.getId(), reason);
        audit.logCurrent("STOCK_ADJUSTED", "MEDICINE_BATCH", b.getId(), before.toPlainString(), after.toPlainString());
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
