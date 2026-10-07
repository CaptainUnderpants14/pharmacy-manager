package demo.pharma.catalog;

import demo.pharma.audit.AuditService;
import demo.pharma.common.exception.*;
import demo.pharma.common.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.*;
import java.util.*;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {
    private final MedicineRepository medicines;
    private final CategoryRepository categories;
    private final ManufacturerRepository manufacturers;
    private final AuditService audit;

    record Upsert(@NotBlank @Size(max = 60) String medicineCode, @NotBlank String name, String genericName,
            String brandName, UUID manufacturerId, UUID categoryId, DosageForm dosageForm, String strength,
            String packSize, String unit, boolean prescriptionRequired, @DecimalMin("0.0") BigDecimal taxPercentage,
            @DecimalMin("0.0") BigDecimal reorderLevel, String description, RecordStatus status) {
    }

    record View(UUID id, String medicineCode, String name, String genericName, String brandName, String category,
            String manufacturer, RecordStatus status, BigDecimal reorderLevel) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MEDICINE_VIEW')")
    public PageResponse<View> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) RecordStatus status, @RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 20, sort = "name") Pageable page) {
        Specification<Medicine> s = (r, q, b) -> b.conjunction();
        if (search != null && !search.isBlank())
            s = s.and((r, q, b) -> b.or(b.like(b.lower(r.get("name")), "%" + search.toLowerCase() + "%"),
                    b.like(b.lower(r.get("genericName")), "%" + search.toLowerCase() + "%"),
                    b.like(b.lower(r.get("brandName")), "%" + search.toLowerCase() + "%"),
                    b.like(b.lower(r.get("medicineCode")), "%" + search.toLowerCase() + "%")));
        if (status != null)
            s = s.and((r, q, b) -> b.equal(r.get("status"), status));
        if (categoryId != null)
            s = s.and((r, q, b) -> b.equal(r.get("category").get("id"), categoryId));
        return PageResponse.of(medicines.findAll(s, page).map(this::view));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MEDICINE_VIEW')")
    public View get(@PathVariable UUID id) {
        return view(find(id));
    }

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MEDICINE_CREATE')")
    @Transactional
    public View create(@Valid @RequestBody Upsert r) {
        if (medicines.existsByMedicineCodeIgnoreCase(r.medicineCode()))
            throw new BusinessException("Medicine code already exists");
        Medicine m = new Medicine();
        apply(m, r);
        medicines.save(m);
        audit.logCurrent("MEDICINE_CREATED", "MEDICINE", m.getId(), null, m.getMedicineCode());
        return view(m);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MEDICINE_UPDATE')")
    @Transactional
    public View update(@PathVariable UUID id, @Valid @RequestBody Upsert r) {
        Medicine m = find(id);
        if (!m.getMedicineCode().equalsIgnoreCase(r.medicineCode())
                && medicines.existsByMedicineCodeIgnoreCase(r.medicineCode()))
            throw new BusinessException("Medicine code already exists");
        String before = m.getMedicineCode();
        apply(m, r);
        audit.logCurrent("MEDICINE_UPDATED", "MEDICINE", m.getId(), before, m.getMedicineCode());
        return view(m);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MEDICINE_DELETE')")
    public void deactivate(@PathVariable UUID id) {
        Medicine m = find(id);
        m.setStatus(RecordStatus.INACTIVE);
        medicines.save(m);
        audit.logCurrent("MEDICINE_DEACTIVATED", "MEDICINE", id, null, null);
    }

    private Medicine find(UUID id) {
        return medicines.findById(id).orElseThrow(() -> new NotFoundException("Medicine", id));
    }

    private void apply(Medicine m, Upsert r) {
        m.setMedicineCode(r.medicineCode().trim());
        m.setName(r.name().trim());
        m.setGenericName(r.genericName());
        m.setBrandName(r.brandName());
        m.setCategory(r.categoryId() == null ? null
                : categories.findById(r.categoryId())
                        .orElseThrow(() -> new NotFoundException("Category", r.categoryId())));
        m.setManufacturer(r.manufacturerId() == null ? null
                : manufacturers.findById(r.manufacturerId())
                        .orElseThrow(() -> new NotFoundException("Manufacturer", r.manufacturerId())));
        m.setDosageForm(r.dosageForm());
        m.setStrength(r.strength());
        m.setPackSize(r.packSize());
        m.setUnit(r.unit());
        m.setPrescriptionRequired(r.prescriptionRequired());
        m.setTaxPercentage(r.taxPercentage() == null ? BigDecimal.ZERO : r.taxPercentage());
        m.setReorderLevel(r.reorderLevel() == null ? BigDecimal.ZERO : r.reorderLevel());
        m.setDescription(r.description());
        m.setStatus(r.status() == null ? RecordStatus.ACTIVE : r.status());
    }

    private View view(Medicine m) {
        return new View(m.getId(), m.getMedicineCode(), m.getName(), m.getGenericName(), m.getBrandName(),
                m.getCategory() == null ? null : m.getCategory().getName(),
                m.getManufacturer() == null ? null : m.getManufacturer().getName(), m.getStatus(), m.getReorderLevel());
    }
}
