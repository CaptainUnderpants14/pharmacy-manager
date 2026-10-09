package demo.pharma.sale;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import demo.pharma.audit.AuditService;
import demo.pharma.branch.Branch;
import demo.pharma.branch.BranchRepository;
import demo.pharma.catalog.Medicine;
import demo.pharma.catalog.MedicineRepository;
import demo.pharma.common.exception.BusinessException;
import demo.pharma.common.exception.NotFoundException;
import demo.pharma.customer.Customer;
import demo.pharma.customer.CustomerRepository;
import demo.pharma.inventory.InventoryService;
import demo.pharma.inventory.MedicineBatch;
import demo.pharma.inventory.MedicineBatchRepository;
import demo.pharma.inventory.MovementType;
import demo.pharma.inventory.StockMovement;
import demo.pharma.inventory.StockMovementRepository;
import demo.pharma.payment.PaymentMethod;
import demo.pharma.payment.PaymentStatus;
import demo.pharma.prescription.Prescription;
import demo.pharma.prescription.PrescriptionRepository;
import demo.pharma.security.AppUser;
import demo.pharma.security.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SaleService {
    private final SaleRepository saleRepo;
    private final MedicineRepository medicineRepo;
    private final MedicineBatchRepository batchRepo;
    private final StockMovementRepository movementRepo;
    private final CustomerRepository customerRepo;
    private final BranchRepository branchRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final UserRepository userRepo;
    private final InventoryService inventoryService;
    private final AuditService audit;

    public record ItemRequest(UUID medicineId, UUID batchId, BigDecimal quantity, BigDecimal unitPrice,
            BigDecimal discount, BigDecimal tax) {
    }

    public record CreateSaleRequest(String saleNumber, UUID customerId, UUID branchId, UUID prescriptionId,
            PaymentMethod paymentMethod, BigDecimal paidAmount, BigDecimal discount, BigDecimal tax, String notes,
            List<ItemRequest> items) {
    }

    @Transactional
    public Sale createSale(CreateSaleRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessException("Sale must contain at least one item");
        }

        String saleNum = req.saleNumber();
        if (saleNum == null || saleNum.isBlank()) {
            saleNum = "SALE-" + System.currentTimeMillis();
        } else if (saleRepo.existsBySaleNumberIgnoreCase(saleNum)) {
            throw new BusinessException("Sale number already exists: " + saleNum);
        }

        Sale sale = new Sale();
        sale.setSaleNumber(saleNum);
        sale.setSaleDate(Instant.now());
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setNotes(req.notes());

        if (req.customerId() != null) {
            Customer c = customerRepo.findById(req.customerId())
                    .orElseThrow(() -> new NotFoundException("Customer", req.customerId()));
            sale.setCustomer(c);
        }

        if (req.branchId() != null) {
            Branch b = branchRepo.findById(req.branchId())
                    .orElseThrow(() -> new NotFoundException("Branch", req.branchId()));
            sale.setBranch(b);
        }

        if (req.prescriptionId() != null) {
            Prescription p = prescriptionRepo.findById(req.prescriptionId())
                    .orElseThrow(() -> new NotFoundException("Prescription", req.prescriptionId()));
            sale.setPrescription(p);
        }

        AppUser currentUser = getCurrentUser();
        sale.setCreatedBy(currentUser);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal itemTaxSum = BigDecimal.ZERO;
        BigDecimal itemDiscountSum = BigDecimal.ZERO;

        List<SaleItem> saleItems = new ArrayList<>();

        for (ItemRequest itemReq : req.items()) {
            if (itemReq.medicineId() == null) {
                throw new BusinessException("Medicine ID is required for each sale item");
            }
            if (itemReq.quantity() == null || itemReq.quantity().signum() <= 0) {
                throw new BusinessException("Quantity must be greater than zero");
            }

            Medicine med = medicineRepo.findById(itemReq.medicineId())
                    .orElseThrow(() -> new NotFoundException("Medicine", itemReq.medicineId()));

            BigDecimal requestedQty = itemReq.quantity();
            BigDecimal unitPrice = itemReq.unitPrice();

            if (itemReq.batchId() != null) {
                MedicineBatch batch = batchRepo.findById(itemReq.batchId())
                        .orElseThrow(() -> new NotFoundException("MedicineBatch", itemReq.batchId()));

                BigDecimal avail = batch.getQuantity().subtract(batch.getReservedQuantity());
                if (avail.compareTo(requestedQty) < 0) {
                    throw new BusinessException("Insufficient stock in batch " + batch.getBatchNumber() + " for " + med.getName());
                }

                if (unitPrice == null || unitPrice.signum() <= 0) {
                    unitPrice = batch.getSellingPrice();
                }

                BigDecimal disc = itemReq.discount() == null ? BigDecimal.ZERO : itemReq.discount();
                BigDecimal tx = itemReq.tax() == null ? BigDecimal.ZERO : itemReq.tax();
                BigDecimal total = unitPrice.multiply(requestedQty).subtract(disc).add(tx);

                // Deduct stock
                BigDecimal prevQty = batch.getQuantity();
                BigDecimal newQty = prevQty.subtract(requestedQty);
                batch.setQuantity(newQty);
                batchRepo.save(batch);

                // Record movement
                createMovement(batch, MovementType.SALE, requestedQty, prevQty, newQty, "SALE_ORDER", sale.getId(), "POS Sale");
                inventoryService.checkLowStock(batch, prevQty, newQty);

                SaleItem si = new SaleItem();
                si.setSale(sale);
                si.setMedicine(med);
                si.setMedicineBatch(batch);
                si.setBatchNumber(batch.getBatchNumber());
                si.setQuantity(requestedQty);
                si.setUnitPrice(unitPrice);
                si.setDiscount(disc);
                si.setTax(tx);
                si.setTotal(total);

                saleItems.add(si);

                subtotal = subtotal.add(unitPrice.multiply(requestedQty));
                itemDiscountSum = itemDiscountSum.add(disc);
                itemTaxSum = itemTaxSum.add(tx);
            } else {
                // Auto FEFO selection
                List<MedicineBatch> fefoBatches = batchRepo.findAvailableFefo(med.getId(), LocalDate.now());
                BigDecimal remainingToDeduct = requestedQty;
                BigDecimal totalAvailable = fefoBatches.stream()
                        .map(b -> b.getQuantity().subtract(b.getReservedQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalAvailable.compareTo(requestedQty) < 0) {
                    throw new BusinessException("Insufficient total FEFO stock for medicine: " + med.getName());
                }

                for (MedicineBatch batch : fefoBatches) {
                    if (remainingToDeduct.signum() <= 0) break;

                    BigDecimal avail = batch.getQuantity().subtract(batch.getReservedQuantity());
                    if (avail.signum() <= 0) continue;

                    BigDecimal deductFromThisBatch = avail.min(remainingToDeduct);
                    BigDecimal batchUnitPrice = unitPrice != null ? unitPrice : batch.getSellingPrice();

                    BigDecimal disc = (itemReq.discount() != null) 
                            ? itemReq.discount().multiply(deductFromThisBatch).divide(requestedQty, 2, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal tx = (itemReq.tax() != null)
                            ? itemReq.tax().multiply(deductFromThisBatch).divide(requestedQty, 2, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal total = batchUnitPrice.multiply(deductFromThisBatch).subtract(disc).add(tx);

                    BigDecimal prevQty = batch.getQuantity();
                    BigDecimal newQty = prevQty.subtract(deductFromThisBatch);
                    batch.setQuantity(newQty);
                    batchRepo.save(batch);

                    createMovement(batch, MovementType.SALE, deductFromThisBatch, prevQty, newQty, "SALE_ORDER", sale.getId(), "POS Sale FEFO");
                    inventoryService.checkLowStock(batch, prevQty, newQty);

                    SaleItem si = new SaleItem();
                    si.setSale(sale);
                    si.setMedicine(med);
                    si.setMedicineBatch(batch);
                    si.setBatchNumber(batch.getBatchNumber());
                    si.setQuantity(deductFromThisBatch);
                    si.setUnitPrice(batchUnitPrice);
                    si.setDiscount(disc);
                    si.setTax(tx);
                    si.setTotal(total);

                    saleItems.add(si);

                    subtotal = subtotal.add(batchUnitPrice.multiply(deductFromThisBatch));
                    itemDiscountSum = itemDiscountSum.add(disc);
                    itemTaxSum = itemTaxSum.add(tx);

                    remainingToDeduct = remainingToDeduct.subtract(deductFromThisBatch);
                }
            }
        }

        BigDecimal overallDiscount = req.discount() != null ? req.discount() : itemDiscountSum;
        BigDecimal overallTax = req.tax() != null ? req.tax() : itemTaxSum;
        BigDecimal totalAmount = subtotal.subtract(overallDiscount).add(overallTax);

        PaymentMethod pm = req.paymentMethod() == null ? PaymentMethod.CASH : req.paymentMethod();
        BigDecimal paid = req.paidAmount() == null ? totalAmount : req.paidAmount();
        BigDecimal change = paid.subtract(totalAmount).max(BigDecimal.ZERO);

        PaymentStatus pStatus = paid.compareTo(totalAmount) >= 0 ? PaymentStatus.PAID
                : (paid.signum() > 0 ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.UNPAID);

        sale.setSubtotal(subtotal);
        sale.setDiscount(overallDiscount);
        sale.setTax(overallTax);
        sale.setTotalAmount(totalAmount);
        sale.setPaidAmount(paid);
        sale.setChangeAmount(change);
        sale.setPaymentMethod(pm);
        sale.setPaymentStatus(pStatus);
        sale.setItems(saleItems);

        saleRepo.save(sale);
        audit.logCurrent("SALE_COMPLETED", "SALE", sale.getId(), null, sale.getSaleNumber());

        return sale;
    }

    @Transactional
    public Sale cancelSale(UUID saleId) {
        Sale sale = saleRepo.findById(saleId).orElseThrow(() -> new NotFoundException("Sale", saleId));
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new BusinessException("Sale is already cancelled");
        }

        // Restore batch quantities
        for (SaleItem item : sale.getItems()) {
            if (item.getMedicineBatch() != null) {
                MedicineBatch batch = item.getMedicineBatch();
                BigDecimal prevQty = batch.getQuantity();
                BigDecimal newQty = prevQty.add(item.getQuantity());
                batch.setQuantity(newQty);
                batchRepo.save(batch);

                createMovement(batch, MovementType.SALE_RETURN, item.getQuantity(), prevQty, newQty, "SALE_CANCEL", sale.getId(), "Sale Cancellation Restock");
            }
        }

        sale.setStatus(SaleStatus.CANCELLED);
        saleRepo.save(sale);
        audit.logCurrent("SALE_CANCELLED", "SALE", sale.getId(), null, sale.getSaleNumber());
        return sale;
    }

    private void createMovement(MedicineBatch b, MovementType t, BigDecimal q, BigDecimal before, BigDecimal after, String rt, UUID rid, String reason) {
        StockMovement m = new StockMovement();
        m.setMedicineBatch(b);
        m.setMovementType(t);
        m.setQuantity(q);
        m.setPreviousQuantity(before);
        m.setNewQuantity(after);
        m.setReferenceType(rt);
        m.setReferenceId(rid);
        m.setReason(reason);
        movementRepo.save(m);
    }

    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        return userRepo.findByUsernameIgnoreCaseOrEmailIgnoreCase(name, name).orElse(null);
    }
}
