package demo.pharma.inventory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import demo.pharma.audit.AuditService;
import demo.pharma.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    @Mock MedicineBatchRepository batches;
    @Mock StockMovementRepository movements;
    @Mock AuditService audit;
    @InjectMocks InventoryService service;

    @Test
    void rejectsAdjustmentThatWouldMakeStockNegative() {
        UUID batchId = UUID.randomUUID();
        MedicineBatch batch = new MedicineBatch();
        batch.setQuantity(new BigDecimal("5"));
        when(batches.findById(batchId)).thenReturn(Optional.of(batch));

        assertThrows(BusinessException.class, () -> service.adjust(batchId, new BigDecimal("6"), MovementType.ADJUSTMENT_OUT, "count"));
        verify(batches, never()).save(any());
        verify(movements, never()).save(any());
    }
}
