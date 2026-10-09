package demo.pharma.purchasereturn;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.common.exception.NotFoundException;
import demo.pharma.common.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/purchase-returns")
@RequiredArgsConstructor
public class PurchaseReturnController {
    private final PurchaseReturnRepository repo;
    private final PurchaseReturnService service;

    public record ItemView(UUID id, UUID batchId, String batchNumber, String medicineName, BigDecimal quantity,
            BigDecimal unitPrice, BigDecimal totalAmount) {
    }

    public record SummaryView(UUID id, String returnNumber, UUID supplierId, String supplierName, UUID purchaseOrderId,
            String purchaseOrderNumber, LocalDate returnDate, BigDecimal totalAmount, PurchaseReturnStatus refundStatus,
            String reason) {
    }

    public record DetailView(UUID id, String returnNumber, UUID supplierId, String supplierName, UUID purchaseOrderId,
            String purchaseOrderNumber, LocalDate returnDate, BigDecimal totalAmount, PurchaseReturnStatus refundStatus,
            String reason, String createdByUsername, List<ItemView> items) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_RETURN_VIEW') or hasAuthority('PURCHASE_VIEW')")
    public PageResponse<SummaryView> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) UUID supplierId, @RequestParam(required = false) UUID purchaseOrderId,
            @PageableDefault(size = 20, sort = "returnDate") Pageable p) {
        return PageResponse.of(repo.findAll((r, q, b) -> {
            var x = b.conjunction();
            if (search != null && !search.isBlank()) {
                String s = "%" + search.toLowerCase() + "%";
                x = b.or(b.like(b.lower(r.get("returnNumber")), s));
            }
            if (supplierId != null) {
                x = b.and(x, b.equal(r.get("supplier").get("id"), supplierId));
            }
            if (purchaseOrderId != null) {
                x = b.and(x, b.equal(r.get("purchaseOrder").get("id"), purchaseOrderId));
            }
            return x;
        }, p).map(this::sv));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_RETURN_VIEW') or hasAuthority('PURCHASE_VIEW')")
    @Transactional(readOnly = true)
    public DetailView get(@PathVariable UUID id) {
        return dv(f(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_RETURN_CREATE') or hasAuthority('PURCHASE_CREATE')")
    public DetailView create(@Valid @RequestBody PurchaseReturnService.CreateReturnRequest r) {
        PurchaseReturn pr = service.createReturn(r);
        return dv(pr);
    }

    private PurchaseReturn f(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("PurchaseReturn", id));
    }

    private SummaryView sv(PurchaseReturn r) {
        return new SummaryView(r.getId(), r.getReturnNumber(), r.getSupplier().getId(),
                r.getSupplier().getCompanyName(),
                r.getPurchaseOrder() != null ? r.getPurchaseOrder().getId() : null,
                r.getPurchaseOrder() != null ? r.getPurchaseOrder().getPurchaseOrderNumber() : null, r.getReturnDate(),
                r.getTotalAmount(), r.getRefundStatus(), r.getReason());
    }

    private DetailView dv(PurchaseReturn r) {
        List<ItemView> itemViews = r.getItems().stream()
                .map(i -> new ItemView(i.getId(), i.getMedicineBatch().getId(), i.getMedicineBatch().getBatchNumber(),
                        i.getMedicineBatch().getMedicine() != null ? i.getMedicineBatch().getMedicine().getName() : null,
                        i.getQuantity(), i.getUnitPrice(), i.getTotalAmount()))
                .toList();

        return new DetailView(r.getId(), r.getReturnNumber(), r.getSupplier().getId(),
                r.getSupplier().getCompanyName(),
                r.getPurchaseOrder() != null ? r.getPurchaseOrder().getId() : null,
                r.getPurchaseOrder() != null ? r.getPurchaseOrder().getPurchaseOrderNumber() : null, r.getReturnDate(),
                r.getTotalAmount(), r.getRefundStatus(), r.getReason(),
                r.getCreatedBy() != null ? r.getCreatedBy().getUsername() : null, itemViews);
    }
}
