package demo.pharma.salereturn;

import java.math.BigDecimal;
import java.time.Instant;
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
import demo.pharma.payment.PaymentMethod;
import demo.pharma.sale.Sale;
import demo.pharma.sale.SaleRepository;
import demo.pharma.security.AppUser;
import demo.pharma.security.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SaleReturnService {
    private final SaleReturnRepository returnRepo;
    private final SaleRepository saleRepo;
    private final MedicineBatchRepository batchRepo;
    private final StockMovementRepository movementRepo;
    private final UserRepository userRepo;
    private final AuditService audit;

    public record ItemReturnRequest(UUID batchId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal refundAmount,
            Boolean restock) {
    }

    public record CreateReturnRequest(String returnNumber, UUID saleId, PaymentMethod refundMethod, String reason,
            List<ItemReturnRequest> items) {
    }

    @Transactional
    public SaleReturn createReturn(CreateReturnRequest req) {
        if (req.saleId() == null) {
            throw new BusinessException("Sale ID is required");
        }
        Sale sale = saleRepo.findById(req.saleId()).orElseThrow(() -> new NotFoundException("Sale", req.saleId()));

        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessException("Sale return must have at least one item");
        }

        String retNum = req.returnNumber();
        if (retNum == null || retNum.isBlank()) {
            retNum = "SR-" + System.currentTimeMillis();
        } else if (returnRepo.existsByReturnNumberIgnoreCase(retNum)) {
            throw new BusinessException("Return number already exists: " + retNum);
        }

        SaleReturn sr = new SaleReturn();
        sr.setReturnNumber(retNum);
        sr.setSale(sale);
        sr.setCustomer(sale.getCustomer());
        sr.setReturnDate(Instant.now());
        sr.setRefundMethod(req.refundMethod() == null ? PaymentMethod.CASH : req.refundMethod());
        sr.setReason(req.reason());
        sr.setCreatedBy(getCurrentUser());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        List<SaleReturnItem> returnItems = new ArrayList<>();

        for (ItemReturnRequest itemReq : req.items()) {
            if (itemReq.quantity() == null || itemReq.quantity().signum() <= 0) {
                throw new BusinessException("Returned quantity must be greater than zero");
            }

            MedicineBatch batch = null;
            if (itemReq.batchId() != null) {
                batch = batchRepo.findById(itemReq.batchId())
                        .orElseThrow(() -> new NotFoundException("MedicineBatch", itemReq.batchId()));
            }

            BigDecimal unitPrice = itemReq.unitPrice() != null ? itemReq.unitPrice()
                    : (batch != null ? batch.getSellingPrice() : BigDecimal.ZERO);
            BigDecimal refAmt = itemReq.refundAmount() != null ? itemReq.refundAmount()
                    : unitPrice.multiply(itemReq.quantity());
            boolean restock = itemReq.restock() == null || itemReq.restock();

            if (restock && batch != null) {
                BigDecimal prevQty = batch.getQuantity();
                BigDecimal newQty = prevQty.add(itemReq.quantity());
                batch.setQuantity(newQty);
                batchRepo.save(batch);

                StockMovement m = new StockMovement();
                m.setMedicineBatch(batch);
                m.setMovementType(MovementType.SALE_RETURN);
                m.setQuantity(itemReq.quantity());
                m.setPreviousQuantity(prevQty);
                m.setNewQuantity(newQty);
                m.setReferenceType("SALE_RETURN");
                m.setReferenceId(sr.getId());
                m.setReason(req.reason() != null ? req.reason() : "Customer Return");
                movementRepo.save(m);
            }

            SaleReturnItem sri = new SaleReturnItem();
            sri.setSaleReturn(sr);
            sri.setMedicineBatch(batch);
            sri.setQuantity(itemReq.quantity());
            sri.setUnitPrice(unitPrice);
            sri.setRefundAmount(refAmt);
            sri.setRestock(restock);

            returnItems.add(sri);

            subtotal = subtotal.add(unitPrice.multiply(itemReq.quantity()));
            totalRefund = totalRefund.add(refAmt);
        }

        sr.setSubtotal(subtotal);
        sr.setRefundAmount(totalRefund);
        sr.setItems(returnItems);

        returnRepo.save(sr);
        audit.logCurrent("SALE_RETURN_CREATED", "SALE_RETURN", sr.getId(), null, sr.getReturnNumber());
        return sr;
    }

    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        return userRepo.findByUsernameIgnoreCaseOrEmailIgnoreCase(name, name).orElse(null);
    }
}
