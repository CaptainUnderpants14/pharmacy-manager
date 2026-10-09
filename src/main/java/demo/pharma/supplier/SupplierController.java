package demo.pharma.supplier;

import demo.pharma.audit.AuditService;
import demo.pharma.catalog.RecordStatus;
import demo.pharma.common.exception.BusinessException;
import demo.pharma.common.exception.NotFoundException;
import demo.pharma.common.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierRepository suppliers;
    private final AuditService audit;

    record Request(@NotBlank String supplierCode, @NotBlank String companyName, String contactPerson, String phone,
                   @Email String email, String address, String gstin, String paymentTerms, RecordStatus status) {}

    record View(UUID id, String supplierCode, String companyName, String contactPerson, String phone, String email,
                String address, String gstin, String paymentTerms, RecordStatus status) {}

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_VIEW')")
    PageResponse<View> list(@RequestParam(required = false) String search,
                            @PageableDefault(size = 20, sort = "companyName", direction = Sort.Direction.ASC) Pageable page) {
        Page<Supplier> result = suppliers.findAll((root, query, builder) -> {
            if (search == null || search.isBlank()) return builder.conjunction();
            String pattern = "%" + search.toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("companyName")), pattern),
                    builder.like(builder.lower(root.get("supplierCode")), pattern),
                    builder.like(builder.lower(root.get("phone")), pattern));
        }, page);
        return PageResponse.of(result.map(this::view));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_VIEW')")
    View get(@PathVariable UUID id) {
        return view(find(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_CREATE')")
    View create(@Valid @RequestBody Request request) {
        if (suppliers.existsBySupplierCodeIgnoreCase(request.supplierCode())) {
            throw new BusinessException("Supplier code already exists");
        }
        Supplier supplier = new Supplier();
        apply(supplier, request);
        suppliers.save(supplier);
        audit.logCurrent("SUPPLIER_CREATED", "SUPPLIER", supplier.getId(), null, supplier.getSupplierCode());
        return view(supplier);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    View update(@PathVariable UUID id, @Valid @RequestBody Request request) {
        Supplier supplier = find(id);
        if (!supplier.getSupplierCode().equalsIgnoreCase(request.supplierCode())
                && suppliers.existsBySupplierCodeIgnoreCase(request.supplierCode())) {
            throw new BusinessException("Supplier code already exists");
        }
        apply(supplier, request);
        suppliers.save(supplier);
        audit.logCurrent("SUPPLIER_UPDATED", "SUPPLIER", supplier.getId(), null, supplier.getSupplierCode());
        return view(supplier);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    void deactivate(@PathVariable UUID id) {
        Supplier supplier = find(id);
        supplier.setStatus(RecordStatus.INACTIVE);
        suppliers.save(supplier);
        audit.logCurrent("SUPPLIER_DEACTIVATED", "SUPPLIER", supplier.getId(), null, supplier.getSupplierCode());
    }

    private Supplier find(UUID id) {
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("Supplier", id));
    }

    private void apply(Supplier supplier, Request request) {
        supplier.setSupplierCode(request.supplierCode().trim());
        supplier.setCompanyName(request.companyName().trim());
        supplier.setContactPerson(request.contactPerson());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setGstin(request.gstin());
        supplier.setPaymentTerms(request.paymentTerms());
        supplier.setStatus(request.status() == null ? RecordStatus.ACTIVE : request.status());
    }

    private View view(Supplier supplier) {
        return new View(supplier.getId(), supplier.getSupplierCode(), supplier.getCompanyName(),
                supplier.getContactPerson(), supplier.getPhone(), supplier.getEmail(), supplier.getAddress(),
                supplier.getGstin(), supplier.getPaymentTerms(), supplier.getStatus());
    }
}
