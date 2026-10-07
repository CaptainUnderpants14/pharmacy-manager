package demo.pharma.employee;

import demo.pharma.common.exception.*;
import demo.pharma.common.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeRepository repo;

    record Request(@NotBlank String employeeCode, @NotBlank String firstName, @NotBlank String lastName, String phone,
            @Email String email, String address, String designation, LocalDate joiningDate,
            @PositiveOrZero BigDecimal salary, EmployeeStatus status, String branch) {
    }

    record View(UUID id, String employeeCode, String firstName, String lastName, String phone, String designation,
            EmployeeStatus status) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    PageResponse<View> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) EmployeeStatus status,
            @PageableDefault(size = 20, sort = "firstName") Pageable p) {
        return PageResponse.of(repo.findAll((r, q, b) -> {
            var x = b.conjunction();
            if (search != null)
                x = b.or(b.like(b.lower(r.get("firstName")), "%" + search.toLowerCase() + "%"),
                        b.like(b.lower(r.get("lastName")), "%" + search.toLowerCase() + "%"),
                        b.like(b.lower(r.get("employeeCode")), "%" + search.toLowerCase() + "%"));
            return status == null ? x : b.and(x, b.equal(r.get("status"), status));
        }, p).map(this::v));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    View create(@Valid @RequestBody Request r) {
        if (repo.existsByEmployeeCodeIgnoreCase(r.employeeCode()))
            throw new BusinessException("Employee code already exists");
        Employee e = new Employee();
        a(e, r);
        return v(repo.save(e));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    View get(@PathVariable UUID id) {
        return v(f(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    View update(@PathVariable UUID id, @Valid @RequestBody Request r) {
        Employee e = f(id);
        a(e, r);
        return v(repo.save(e));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    View status(@PathVariable UUID id, @RequestParam EmployeeStatus status) {
        Employee e = f(id);
        e.setStatus(status);
        return v(repo.save(e));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_DELETE')")
    void delete(@PathVariable UUID id) {
        Employee e = f(id);
        e.setStatus(EmployeeStatus.INACTIVE);
        repo.save(e);
    }

    private Employee f(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Employee", id));
    }

    private void a(Employee e, Request r) {
        e.setEmployeeCode(r.employeeCode());
        e.setFirstName(r.firstName());
        e.setLastName(r.lastName());
        e.setPhone(r.phone());
        e.setEmail(r.email());
        e.setAddress(r.address());
        e.setDesignation(r.designation());
        e.setJoiningDate(r.joiningDate());
        e.setSalary(r.salary());
        e.setStatus(r.status() == null ? EmployeeStatus.ACTIVE : r.status());
        e.setBranch(r.branch());
    }

    private View v(Employee e) {
        return new View(e.getId(), e.getEmployeeCode(), e.getFirstName(), e.getLastName(), e.getPhone(),
                e.getDesignation(), e.getStatus());
    }
}
