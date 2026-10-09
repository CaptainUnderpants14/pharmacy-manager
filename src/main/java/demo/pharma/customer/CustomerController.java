package demo.pharma.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.audit.AuditService;
import demo.pharma.catalog.RecordStatus;
import demo.pharma.common.exception.BusinessException;
import demo.pharma.common.exception.NotFoundException;
import demo.pharma.common.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerRepository repo;
    private final AuditService audit;

    public record Request(@NotBlank String customerCode, @NotBlank String name, String phone, String email,
            String address, String gender, LocalDate dateOfBirth, String medicalHistory, BigDecimal creditBalance,
            RecordStatus status) {
    }

    public record View(UUID id, String customerCode, String name, String phone, String email, String address,
            String gender, LocalDate dateOfBirth, String medicalHistory, BigDecimal creditBalance,
            RecordStatus status) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public PageResponse<View> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) RecordStatus status,
            @PageableDefault(size = 20, sort = "name") Pageable p) {
        return PageResponse.of(repo.findAll((r, q, b) -> {
            var x = b.conjunction();
            if (search != null && !search.isBlank()) {
                String s = "%" + search.toLowerCase() + "%";
                x = b.or(b.like(b.lower(r.get("name")), s), b.like(b.lower(r.get("customerCode")), s),
                        b.like(b.lower(r.get("phone")), s));
            }
            return status == null ? x : b.and(x, b.equal(r.get("status"), status));
        }, p).map(this::v));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public View get(@PathVariable UUID id) {
        return v(f(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public View create(@Valid @RequestBody Request r) {
        if (repo.existsByCustomerCodeIgnoreCase(r.customerCode())) {
            throw new BusinessException("Customer code already exists");
        }
        Customer c = new Customer();
        a(c, r);
        repo.save(c);
        audit.logCurrent("CUSTOMER_CREATED", "CUSTOMER", c.getId(), null, c.getCustomerCode());
        return v(c);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    public View update(@PathVariable UUID id, @Valid @RequestBody Request r) {
        Customer c = f(id);
        a(c, r);
        repo.save(c);
        audit.logCurrent("CUSTOMER_UPDATED", "CUSTOMER", id, null, c.getCustomerCode());
        return v(c);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_DELETE')")
    public void delete(@PathVariable UUID id) {
        Customer c = f(id);
        c.setStatus(RecordStatus.INACTIVE);
        repo.save(c);
        audit.logCurrent("CUSTOMER_DEACTIVATED", "CUSTOMER", id, null, null);
    }

    private Customer f(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Customer", id));
    }

    private void a(Customer c, Request r) {
        c.setCustomerCode(r.customerCode());
        c.setName(r.name());
        c.setPhone(r.phone());
        c.setEmail(r.email());
        c.setAddress(r.address());
        c.setGender(r.gender());
        c.setDateOfBirth(r.dateOfBirth());
        c.setMedicalHistory(r.medicalHistory());
        c.setCreditBalance(r.creditBalance() == null ? BigDecimal.ZERO : r.creditBalance());
        c.setStatus(r.status() == null ? RecordStatus.ACTIVE : r.status());
    }

    private View v(Customer c) {
        return new View(c.getId(), c.getCustomerCode(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress(),
                c.getGender(), c.getDateOfBirth(), c.getMedicalHistory(), c.getCreditBalance(), c.getStatus());
    }
}
