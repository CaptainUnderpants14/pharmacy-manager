package demo.pharma.salereturn;

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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sales-returns")
@RequiredArgsConstructor
public class SaleReturnController {
    private final SaleReturnRepository repo;
    private final SaleReturnService service;

    public record ItemView(UUID id, UUID batchId, String batchNumber, String medicineName, BigDecimal quantity,
            BigDecimal unitPrice, BigDecimal refundAmount, boolean restock) {
    }

    public record SummaryView(UUID id, String returnNumber, UUID saleId, String saleNumber, UUID customerId,
            String customerName, Instant returnDate, BigDecimal refundAmount, PaymentMethod refundMethod,
            String reason) {
    }

    public record DetailView(UUID id, String returnNumber, UUID saleId, String saleNumber, UUID customerId,
            String customerName, Instant returnDate, BigDecimal subtotal, BigDecimal tax, BigDecimal refundAmount,
            PaymentMethod refundMethod, String reason, String createdByUsername, List<ItemView> items) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SALE_RETURN_VIEW') or hasAuthority('SALE_VIEW')")
    public PageResponse<SummaryView> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) UUID saleId, @RequestParam(required = false) UUID customerId,
            @PageableDefault(size = 20, sort = "returnDate") Pageable p) {
        return PageResponse.of(repo.findAll((r, q, b) -> {
            var x = b.conjunction();
            if (search != null && !search.isBlank()) {
                String s = "%" + search.toLowerCase() + "%";
                x = b.or(b.like(b.lower(r.get("returnNumber")), s));
            }
            if (saleId != null) {
                x = b.and(x, b.equal(r.get("sale").get("id"), saleId));
            }
            if (customerId != null) {
                x = b.and(x, b.equal(r.get("customer").get("id"), customerId));
            }
            return x;
        }, p).map(this::sv));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SALE_RETURN_VIEW') or hasAuthority('SALE_VIEW')")
    @Transactional(readOnly = true)
    public DetailView get(@PathVariable UUID id) {
        return dv(f(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALE_RETURN_CREATE') or hasAuthority('SALE_CREATE')")
    public DetailView create(@Valid @RequestBody SaleReturnService.CreateReturnRequest r) {
        SaleReturn sr = service.createReturn(r);
        return dv(sr);
    }

    private SaleReturn f(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("SaleReturn", id));
    }

    private SummaryView sv(SaleReturn r) {
        return new SummaryView(r.getId(), r.getReturnNumber(), r.getSale().getId(), r.getSale().getSaleNumber(),
                r.getCustomer() != null ? r.getCustomer().getId() : null,
                r.getCustomer() != null ? r.getCustomer().getName() : null, r.getReturnDate(), r.getRefundAmount(),
                r.getRefundMethod(), r.getReason());
    }

    private DetailView dv(SaleReturn r) {
        List<ItemView> itemViews = r.getItems().stream()
                .map(i -> new ItemView(i.getId(),
                        i.getMedicineBatch() != null ? i.getMedicineBatch().getId() : null,
                        i.getMedicineBatch() != null ? i.getMedicineBatch().getBatchNumber() : null,
                        i.getMedicineBatch() != null && i.getMedicineBatch().getMedicine() != null
                                ? i.getMedicineBatch().getMedicine().getName()
                                : null,
                        i.getQuantity(), i.getUnitPrice(), i.getRefundAmount(), i.isRestock()))
                .toList();

        return new DetailView(r.getId(), r.getReturnNumber(), r.getSale().getId(), r.getSale().getSaleNumber(),
                r.getCustomer() != null ? r.getCustomer().getId() : null,
                r.getCustomer() != null ? r.getCustomer().getName() : null, r.getReturnDate(), r.getSubtotal(),
                r.getTax(), r.getRefundAmount(), r.getRefundMethod(), r.getReason(),
                r.getCreatedBy() != null ? r.getCreatedBy().getUsername() : null, itemViews);
    }
}
