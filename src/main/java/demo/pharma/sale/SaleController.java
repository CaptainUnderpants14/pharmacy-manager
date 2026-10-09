package demo.pharma.sale;

import java.math.BigDecimal;
import java.time.Instant;
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
import demo.pharma.payment.PaymentMethod;
import demo.pharma.payment.PaymentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {
    private final SaleRepository repo;
    private final SaleService service;

    public record ItemView(UUID id, UUID medicineId, String medicineName, UUID batchId, String batchNumber,
            BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, BigDecimal tax, BigDecimal total) {
    }

    public record SummaryView(UUID id, String saleNumber, UUID customerId, String customerName, UUID branchId,
            String branchName, Instant saleDate, SaleStatus status, BigDecimal totalAmount, PaymentStatus paymentStatus,
            PaymentMethod paymentMethod) {
    }

    public record DetailView(UUID id, String saleNumber, UUID customerId, String customerName, UUID branchId,
            String branchName, UUID prescriptionId, String prescriptionNumber, Instant saleDate, SaleStatus status,
            BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal totalAmount, BigDecimal paidAmount,
            BigDecimal changeAmount, PaymentStatus paymentStatus, PaymentMethod paymentMethod, String notes,
            String createdByUsername, List<ItemView> items) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SALE_VIEW') or hasAuthority('DASHBOARD_VIEW')")
    public PageResponse<SummaryView> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) UUID customerId, @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) SaleStatus status,
            @PageableDefault(size = 20, sort = "saleDate") Pageable p) {
        return PageResponse.of(repo.findAll((r, q, b) -> {
            var x = b.conjunction();
            if (search != null && !search.isBlank()) {
                String s = "%" + search.toLowerCase() + "%";
                x = b.or(b.like(b.lower(r.get("saleNumber")), s));
            }
            if (customerId != null) {
                x = b.and(x, b.equal(r.get("customer").get("id"), customerId));
            }
            if (branchId != null) {
                x = b.and(x, b.equal(r.get("branch").get("id"), branchId));
            }
            return status == null ? x : b.and(x, b.equal(r.get("status"), status));
        }, p).map(this::sv));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SALE_VIEW')")
    public SummaryView getSummary(@PathVariable UUID id) {
        return sv(f(id));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAuthority('SALE_VIEW')")
    @Transactional(readOnly = true)
    public DetailView getDetail(@PathVariable UUID id) {
        return dv(f(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALE_CREATE')")
    public DetailView create(@Valid @RequestBody SaleService.CreateSaleRequest r) {
        Sale sale = service.createSale(r);
        return dv(sale);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SALE_CANCEL')")
    public DetailView cancel(@PathVariable UUID id) {
        Sale sale = service.cancelSale(id);
        return dv(sale);
    }

    private Sale f(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Sale", id));
    }

    private SummaryView sv(Sale s) {
        return new SummaryView(s.getId(), s.getSaleNumber(),
                s.getCustomer() != null ? s.getCustomer().getId() : null,
                s.getCustomer() != null ? s.getCustomer().getName() : null,
                s.getBranch() != null ? s.getBranch().getId() : null,
                s.getBranch() != null ? s.getBranch().getName() : null, s.getSaleDate(), s.getStatus(),
                s.getTotalAmount(), s.getPaymentStatus(), s.getPaymentMethod());
    }

    private DetailView dv(Sale s) {
        List<ItemView> itemViews = s.getItems().stream()
                .map(i -> new ItemView(i.getId(), i.getMedicine().getId(), i.getMedicine().getName(),
                        i.getMedicineBatch() != null ? i.getMedicineBatch().getId() : null, i.getBatchNumber(),
                        i.getQuantity(), i.getUnitPrice(), i.getDiscount(), i.getTax(), i.getTotal()))
                .toList();

        return new DetailView(s.getId(), s.getSaleNumber(),
                s.getCustomer() != null ? s.getCustomer().getId() : null,
                s.getCustomer() != null ? s.getCustomer().getName() : null,
                s.getBranch() != null ? s.getBranch().getId() : null,
                s.getBranch() != null ? s.getBranch().getName() : null,
                s.getPrescription() != null ? s.getPrescription().getId() : null,
                s.getPrescription() != null ? s.getPrescription().getPrescriptionNumber() : null, s.getSaleDate(),
                s.getStatus(), s.getSubtotal(), s.getDiscount(), s.getTax(), s.getTotalAmount(), s.getPaidAmount(),
                s.getChangeAmount(), s.getPaymentStatus(), s.getPaymentMethod(), s.getNotes(),
                s.getCreatedBy() != null ? s.getCreatedBy().getUsername() : null, itemViews);
    }
}
