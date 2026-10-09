package demo.pharma.prescription;

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
import demo.pharma.common.exception.BusinessException;
import demo.pharma.common.exception.NotFoundException;
import demo.pharma.common.web.PageResponse;
import demo.pharma.customer.CustomerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {
    private final PrescriptionRepository repo;
    private final CustomerRepository customers;
    private final AuditService audit;

    public record Request(@NotBlank String prescriptionNumber, UUID customerId, @NotBlank String doctorName,
            String doctorLicenseNumber, String hospitalName, @NotNull LocalDate prescriptionDate, String notes,
            String fileUrl, PrescriptionStatus status) {
    }

    public record View(UUID id, String prescriptionNumber, UUID customerId, String customerName, String doctorName,
            String doctorLicenseNumber, String hospitalName, LocalDate prescriptionDate, String notes, String fileUrl,
            PrescriptionStatus status) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRESCRIPTION_VIEW')")
    public PageResponse<View> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) PrescriptionStatus status,
            @PageableDefault(size = 20, sort = "prescriptionDate") Pageable p) {
        return PageResponse.of(repo.findAll((r, q, b) -> {
            var x = b.conjunction();
            if (search != null && !search.isBlank()) {
                String s = "%" + search.toLowerCase() + "%";
                x = b.or(b.like(b.lower(r.get("prescriptionNumber")), s), b.like(b.lower(r.get("doctorName")), s),
                        b.like(b.lower(r.get("hospitalName")), s));
            }
            if (customerId != null) {
                x = b.and(x, b.equal(r.get("customer").get("id"), customerId));
            }
            return status == null ? x : b.and(x, b.equal(r.get("status"), status));
        }, p).map(this::v));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRESCRIPTION_VIEW')")
    public View get(@PathVariable UUID id) {
        return v(f(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRESCRIPTION_CREATE')")
    public View create(@Valid @RequestBody Request r) {
        if (repo.existsByPrescriptionNumberIgnoreCase(r.prescriptionNumber())) {
            throw new BusinessException("Prescription number already exists");
        }
        Prescription p = new Prescription();
        a(p, r);
        repo.save(p);
        audit.logCurrent("PRESCRIPTION_CREATED", "PRESCRIPTION", p.getId(), null, p.getPrescriptionNumber());
        return v(p);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRESCRIPTION_UPDATE')")
    public View update(@PathVariable UUID id, @Valid @RequestBody Request r) {
        Prescription p = f(id);
        a(p, r);
        repo.save(p);
        audit.logCurrent("PRESCRIPTION_UPDATED", "PRESCRIPTION", id, null, p.getPrescriptionNumber());
        return v(p);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRESCRIPTION_DELETE')")
    public void delete(@PathVariable UUID id) {
        Prescription p = f(id);
        p.setStatus(PrescriptionStatus.CANCELLED);
        repo.save(p);
        audit.logCurrent("PRESCRIPTION_CANCELLED", "PRESCRIPTION", id, null, null);
    }

    private Prescription f(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Prescription", id));
    }

    private void a(Prescription p, Request r) {
        p.setPrescriptionNumber(r.prescriptionNumber());
        if (r.customerId() != null) {
            p.setCustomer(customers.findById(r.customerId())
                    .orElseThrow(() -> new NotFoundException("Customer", r.customerId())));
        } else {
            p.setCustomer(null);
        }
        p.setDoctorName(r.doctorName());
        p.setDoctorLicenseNumber(r.doctorLicenseNumber());
        p.setHospitalName(r.hospitalName());
        p.setPrescriptionDate(r.prescriptionDate());
        p.setNotes(r.notes());
        p.setFileUrl(r.fileUrl());
        p.setStatus(r.status() == null ? PrescriptionStatus.ACTIVE : r.status());
    }

    private View v(Prescription p) {
        return new View(p.getId(), p.getPrescriptionNumber(), p.getCustomer() != null ? p.getCustomer().getId() : null,
                p.getCustomer() != null ? p.getCustomer().getName() : null, p.getDoctorName(),
                p.getDoctorLicenseNumber(), p.getHospitalName(), p.getPrescriptionDate(), p.getNotes(), p.getFileUrl(),
                p.getStatus());
    }
}
