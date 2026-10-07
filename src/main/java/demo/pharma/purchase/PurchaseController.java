package demo.pharma.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.audit.AuditService;
import demo.pharma.catalog.MedicineRepository;
import demo.pharma.common.exception.BusinessException;
import demo.pharma.common.exception.NotFoundException;
import demo.pharma.common.web.PageResponse;
import demo.pharma.inventory.InventoryService;
import demo.pharma.inventory.MedicineBatch;
import demo.pharma.inventory.MedicineBatchRepository;
import demo.pharma.supplier.SupplierRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseOrderRepository orders;
    private final SupplierRepository suppliers;
    private final MedicineRepository medicines;
    private final MedicineBatchRepository batches;
    private final InventoryService inventory;
    private final AuditService audit;

    record Line(@NotNull UUID medicineId, @NotNull @Positive BigDecimal quantity,
            @NotNull @PositiveOrZero BigDecimal unitPrice, @PositiveOrZero BigDecimal discount,
            @PositiveOrZero BigDecimal tax) {
    }

    record Create(@NotBlank String purchaseOrderNumber, @NotNull UUID supplierId, @NotNull LocalDate orderDate,
            LocalDate expectedDeliveryDate, String notes, @NotEmpty List<@Valid Line> items) {
    }

    record ReceiveLine(@NotNull UUID medicineId, @NotBlank String batchNumber, LocalDate manufacturingDate,
            @NotNull LocalDate expiryDate, @NotNull @Positive BigDecimal quantity,
            @NotNull @PositiveOrZero BigDecimal purchasePrice, @NotNull @PositiveOrZero BigDecimal mrp,
            @NotNull @PositiveOrZero BigDecimal sellingPrice) {
    }

    record Receive(@NotEmpty List<@Valid ReceiveLine> items) {
    }

    record View(UUID id, String number, UUID supplierId, PurchaseOrderStatus status, BigDecimal total) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    PageResponse<View> list(
            @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC) Pageable p) {
        return PageResponse.of(orders.findAll(p).map(this::view));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_VIEW')")
    View get(@PathVariable UUID id) {
        return view(find(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_CREATE')")
    @Transactional
    View create(@Valid @RequestBody Create r) {
        if (orders.existsByPurchaseOrderNumber(r.purchaseOrderNumber()))
            throw new BusinessException("Purchase order number already exists");
        PurchaseOrder p = new PurchaseOrder();
        p.setPurchaseOrderNumber(r.purchaseOrderNumber());
        p.setSupplier(suppliers.findById(r.supplierId())
                .orElseThrow(() -> new NotFoundException("Supplier", r.supplierId())));
        p.setOrderDate(r.orderDate());
        p.setExpectedDeliveryDate(r.expectedDeliveryDate());
        p.setNotes(r.notes());
        BigDecimal subtotal = BigDecimal.ZERO, tax = BigDecimal.ZERO, discount = BigDecimal.ZERO;
        for (Line l : r.items()) {
            PurchaseOrderItem i = new PurchaseOrderItem();
            i.setPurchaseOrder(p);
            i.setMedicine(medicines.findById(l.medicineId())
                    .orElseThrow(() -> new NotFoundException("Medicine", l.medicineId())));
            i.setQuantity(l.quantity());
            i.setUnitPrice(l.unitPrice());
            i.setDiscount(n(l.discount()));
            i.setTax(n(l.tax()));
            i.setTotal(l.unitPrice().multiply(l.quantity()).subtract(n(l.discount())).add(n(l.tax())));
            p.getItems().add(i);
            subtotal = subtotal.add(l.unitPrice().multiply(l.quantity()));
            tax = tax.add(n(l.tax()));
            discount = discount.add(n(l.discount()));
        }
        p.setSubtotal(subtotal);
        p.setTax(tax);
        p.setDiscount(discount);
        p.setTotalAmount(subtotal.add(tax).subtract(discount));
        orders.save(p);
        audit.logCurrent("PURCHASE_CREATED", "PURCHASE_ORDER", p.getId(), null, p.getPurchaseOrderNumber());
        return view(p);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PURCHASE_APPROVE')")
    View approve(@PathVariable UUID id) {
        PurchaseOrder p = find(id);
        if (p.getStatus() != PurchaseOrderStatus.DRAFT && p.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL)
            throw new BusinessException("Only draft or pending orders can be approved");
        p.setStatus(PurchaseOrderStatus.APPROVED);
        orders.save(p);
        audit.logCurrent("PURCHASE_APPROVED", "PURCHASE_ORDER", id, null, null);
        return view(p);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE_CANCEL')")
    View cancel(@PathVariable UUID id) {
        PurchaseOrder p = find(id);
        if (p.getStatus() == PurchaseOrderStatus.RECEIVED)
            throw new BusinessException("Received purchase cannot be cancelled");
        p.setStatus(PurchaseOrderStatus.CANCELLED);
        return view(orders.save(p));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('PURCHASE_RECEIVE')")
    @Transactional
    View receive(@PathVariable UUID id, @Valid @RequestBody Receive r) {
        PurchaseOrder p = find(id);
        if (p.getStatus() != PurchaseOrderStatus.APPROVED && p.getStatus() != PurchaseOrderStatus.ORDERED
                && p.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED)
            throw new BusinessException("Purchase must be approved before receiving");
        for (ReceiveLine l : r.items()) {
            if (l.manufacturingDate() != null && !l.expiryDate().isAfter(l.manufacturingDate()))
                throw new BusinessException("Expiry date must be after manufacturing date");
            MedicineBatch b = new MedicineBatch();
            b.setMedicine(medicines.findById(l.medicineId())
                    .orElseThrow(() -> new NotFoundException("Medicine", l.medicineId())));
            b.setSupplier(p.getSupplier());
            b.setBatchNumber(l.batchNumber());
            b.setManufacturingDate(l.manufacturingDate());
            b.setExpiryDate(l.expiryDate());
            b.setPurchasePrice(l.purchasePrice());
            b.setMrp(l.mrp());
            b.setSellingPrice(l.sellingPrice());
            b.setQuantity(BigDecimal.ZERO);
            b = batches.save(b);
            inventory.receive(b, l.quantity(), p.getId());
        }
        p.setStatus(PurchaseOrderStatus.RECEIVED);
        orders.save(p);
        audit.logCurrent("PURCHASE_RECEIVED", "PURCHASE_ORDER", id, null, null);
        return view(p);
    }

    private BigDecimal n(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private PurchaseOrder find(UUID id) {
        return orders.findById(id).orElseThrow(() -> new NotFoundException("Purchase order", id));
    }

    private View view(PurchaseOrder p) {
        return new View(p.getId(), p.getPurchaseOrderNumber(), p.getSupplier().getId(), p.getStatus(),
                p.getTotalAmount());
    }
}
