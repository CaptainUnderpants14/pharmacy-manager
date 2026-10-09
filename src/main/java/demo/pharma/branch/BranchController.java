package demo.pharma.branch;

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
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {
    private final BranchRepository repo;
    private final AuditService audit;

    public record Request(@NotBlank String code, @NotBlank String name, String address, String phone, String email,
            Boolean isMain, RecordStatus status) {
    }

    public record View(UUID id, String code, String name, String address, String phone, String email, boolean isMain,
            RecordStatus status) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BRANCH_VIEW') or hasAuthority('SETTINGS_MANAGE')")
    public PageResponse<View> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) RecordStatus status,
            @PageableDefault(size = 20, sort = "name") Pageable p) {
        return PageResponse.of(repo.findAll((r, q, b) -> {
            var x = b.conjunction();
            if (search != null && !search.isBlank()) {
                String s = "%" + search.toLowerCase() + "%";
                x = b.or(b.like(b.lower(r.get("name")), s), b.like(b.lower(r.get("code")), s));
            }
            return status == null ? x : b.and(x, b.equal(r.get("status"), status));
        }, p).map(this::v));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BRANCH_VIEW') or hasAuthority('SETTINGS_MANAGE')")
    public View get(@PathVariable UUID id) {
        return v(f(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BRANCH_MANAGE') or hasAuthority('SETTINGS_MANAGE')")
    public View create(@Valid @RequestBody Request r) {
        if (repo.existsByCodeIgnoreCase(r.code())) {
            throw new BusinessException("Branch code already exists");
        }
        Branch b = new Branch();
        a(b, r);
        repo.save(b);
        audit.logCurrent("BRANCH_CREATED", "BRANCH", b.getId(), null, b.getCode());
        return v(b);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BRANCH_MANAGE') or hasAuthority('SETTINGS_MANAGE')")
    public View update(@PathVariable UUID id, @Valid @RequestBody Request r) {
        Branch b = f(id);
        a(b, r);
        repo.save(b);
        audit.logCurrent("BRANCH_UPDATED", "BRANCH", id, null, b.getCode());
        return v(b);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BRANCH_MANAGE') or hasAuthority('SETTINGS_MANAGE')")
    public void delete(@PathVariable UUID id) {
        Branch b = f(id);
        b.setStatus(RecordStatus.INACTIVE);
        repo.save(b);
        audit.logCurrent("BRANCH_DEACTIVATED", "BRANCH", id, null, null);
    }

    private Branch f(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Branch", id));
    }

    private void a(Branch b, Request r) {
        b.setCode(r.code());
        b.setName(r.name());
        b.setAddress(r.address());
        b.setPhone(r.phone());
        b.setEmail(r.email());
        if (r.isMain() != null) {
            b.setMain(r.isMain());
        }
        b.setStatus(r.status() == null ? RecordStatus.ACTIVE : r.status());
    }

    private View v(Branch b) {
        return new View(b.getId(), b.getCode(), b.getName(), b.getAddress(), b.getPhone(), b.getEmail(), b.isMain(),
                b.getStatus());
    }
}
