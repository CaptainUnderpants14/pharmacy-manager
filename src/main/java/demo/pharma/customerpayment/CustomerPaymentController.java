package demo.pharma.customerpayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.audit.AuditService;
import demo.pharma.common.exception.BusinessException;
import demo.pharma.common.exception.NotFoundException;
import demo.pharma.common.web.PageResponse;
import demo.pharma.customer.Customer;
import demo.pharma.customer.CustomerRepository;
import demo.pharma.payment.PaymentMethod;
import demo.pharma.payment.PaymentStatus;
import demo.pharma.sale.Sale;
import demo.pharma.sale.SaleRepository;
import demo.pharma.security.AppUser;
import demo.pharma.security.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customer-payments")
@RequiredArgsConstructor
public class CustomerPaymentController {
    private final CustomerPaymentRepository repo;
    private final CustomerRepository customerRepo;
    private final SaleRepository saleRepo;
    private final UserRepository userRepo;
    private final AuditService audit;

    public record Request(String paymentNumber, UUID customerId, UUID saleId, @NotNull @Positive BigDecimal amount,
            PaymentMethod paymentMethod, String referenceNumber, LocalDate paymentDate, String notes) {
    }

    public record View(UUID id, String paymentNumber, UUID customerId, String customerName, UUID saleId,
            String saleNumber, BigDecimal amount, PaymentMethod paymentMethod, String referenceNumber,
            LocalDate paymentDate, String notes, String createdByUsername) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public PageResponse<View> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) UUID customerId, @RequestParam(required = false) UUID saleId,
            @PageableDefault(size = 20, sort = "paymentDate") Pageable p) {
        return PageResponse.of(repo.findAll((r, q, b) -> {
            var x = b.conjunction();
            if (search != null && !search.isBlank()) {
                String s = "%" + search.toLowerCase() + "%";
                x = b.or(b.like(b.lower(r.get("paymentNumber")), s), b.like(b.lower(r.get("referenceNumber")), s));
            }
            if (customerId != null) {
                x = b.and(x, b.equal(r.get("customer").get("id"), customerId));
            }
            if (saleId != null) {
                x = b.and(x, b.equal(r.get("sale").get("id"), saleId));
            }
            return x;
        }, p).map(this::v));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public View get(@PathVariable UUID id) {
        return v(f(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    public View create(@Valid @RequestBody Request r) {
        String num = r.paymentNumber();
        if (num == null || num.isBlank()) {
            num = "PAY-CUST-" + System.currentTimeMillis();
        } else if (repo.existsByPaymentNumberIgnoreCase(num)) {
            throw new BusinessException("Payment number already exists: " + num);
        }

        CustomerPayment p = new CustomerPayment();
        p.setPaymentNumber(num);
        p.setAmount(r.amount());
        p.setPaymentMethod(r.paymentMethod() == null ? PaymentMethod.CASH : r.paymentMethod());
        p.setReferenceNumber(r.referenceNumber());
        p.setPaymentDate(r.paymentDate() == null ? LocalDate.now() : r.paymentDate());
        p.setNotes(r.notes());
        p.setCreatedBy(getCurrentUser());

        if (r.customerId() != null) {
            Customer c = customerRepo.findById(r.customerId())
                    .orElseThrow(() -> new NotFoundException("Customer", r.customerId()));
            p.setCustomer(c);
        }

        if (r.saleId() != null) {
            Sale s = saleRepo.findById(r.saleId()).orElseThrow(() -> new NotFoundException("Sale", r.saleId()));
            p.setSale(s);
            if (p.getCustomer() == null && s.getCustomer() != null) {
                p.setCustomer(s.getCustomer());
            }

            // Update sale paid amount and payment status
            BigDecimal newPaid = s.getPaidAmount().add(r.amount());
            s.setPaidAmount(newPaid);
            if (newPaid.compareTo(s.getTotalAmount()) >= 0) {
                s.setPaymentStatus(PaymentStatus.PAID);
            } else if (newPaid.signum() > 0) {
                s.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
            }
            saleRepo.save(s);
        }

        repo.save(p);
        audit.logCurrent("CUSTOMER_PAYMENT_CREATED", "CUSTOMER_PAYMENT", p.getId(), null, p.getPaymentNumber());
        return v(p);
    }

    private CustomerPayment f(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("CustomerPayment", id));
    }

    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        return userRepo.findByUsernameIgnoreCaseOrEmailIgnoreCase(name, name).orElse(null);
    }

    private View v(CustomerPayment p) {
        return new View(p.getId(), p.getPaymentNumber(), p.getCustomer() != null ? p.getCustomer().getId() : null,
                p.getCustomer() != null ? p.getCustomer().getName() : null,
                p.getSale() != null ? p.getSale().getId() : null,
                p.getSale() != null ? p.getSale().getSaleNumber() : null, p.getAmount(), p.getPaymentMethod(),
                p.getReferenceNumber(), p.getPaymentDate(), p.getNotes(),
                p.getCreatedBy() != null ? p.getCreatedBy().getUsername() : null);
    }
}
