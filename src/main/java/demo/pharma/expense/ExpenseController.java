package demo.pharma.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
import demo.pharma.branch.BranchRepository;
import demo.pharma.common.exception.BusinessException;
import demo.pharma.common.exception.NotFoundException;
import demo.pharma.common.web.PageResponse;
import demo.pharma.payment.PaymentMethod;
import demo.pharma.security.AppUser;
import demo.pharma.security.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseCategoryRepository categoryRepo;
    private final ExpenseRepository expenseRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;
    private final AuditService audit;

    // Expense Category DTOs
    public record CategoryRequest(@NotBlank String name, String description, Boolean active) {
    }

    public record CategoryView(UUID id, String name, String description, boolean active) {
    }

    // Expense DTOs
    public record ExpenseRequest(String expenseNumber, @NotNull UUID categoryId, @NotNull @Positive BigDecimal amount,
            @NotNull LocalDate expenseDate, PaymentMethod paymentMethod, String referenceNumber, String payee,
            String description, UUID branchId) {
    }

    public record ExpenseView(UUID id, String expenseNumber, UUID categoryId, String categoryName, BigDecimal amount,
            LocalDate expenseDate, PaymentMethod paymentMethod, String referenceNumber, String payee,
            String description, UUID branchId, String branchName, String createdByUsername) {
    }

    // Category Endpoints
    @GetMapping("/expense-categories")
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    public List<CategoryView> listCategories() {
        return categoryRepo.findAll().stream()
                .map(c -> new CategoryView(c.getId(), c.getName(), c.getDescription(), c.isActive()))
                .toList();
    }

    @PostMapping("/expense-categories")
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public CategoryView createCategory(@Valid @RequestBody CategoryRequest r) {
        if (categoryRepo.existsByNameIgnoreCase(r.name())) {
            throw new BusinessException("Expense category already exists: " + r.name());
        }
        ExpenseCategory c = new ExpenseCategory();
        c.setName(r.name());
        c.setDescription(r.description());
        c.setActive(r.active() == null || r.active());
        categoryRepo.save(c);
        audit.logCurrent("EXPENSE_CATEGORY_CREATED", "EXPENSE_CATEGORY", c.getId(), null, c.getName());
        return new CategoryView(c.getId(), c.getName(), c.getDescription(), c.isActive());
    }

    @PutMapping("/expense-categories/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_UPDATE')")
    public CategoryView updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest r) {
        ExpenseCategory c = categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("ExpenseCategory", id));
        c.setName(r.name());
        c.setDescription(r.description());
        if (r.active() != null) {
            c.setActive(r.active());
        }
        categoryRepo.save(c);
        audit.logCurrent("EXPENSE_CATEGORY_UPDATED", "EXPENSE_CATEGORY", id, null, c.getName());
        return new CategoryView(c.getId(), c.getName(), c.getDescription(), c.isActive());
    }

    // Expense Endpoints
    @GetMapping("/expenses")
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    public PageResponse<ExpenseView> listExpenses(@RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) UUID branchId,
            @PageableDefault(size = 20, sort = "expenseDate") Pageable p) {
        return PageResponse.of(expenseRepo.findAll((r, q, b) -> {
            var x = b.conjunction();
            if (search != null && !search.isBlank()) {
                String s = "%" + search.toLowerCase() + "%";
                x = b.or(b.like(b.lower(r.get("expenseNumber")), s), b.like(b.lower(r.get("payee")), s),
                        b.like(b.lower(r.get("description")), s));
            }
            if (categoryId != null) {
                x = b.and(x, b.equal(r.get("category").get("id"), categoryId));
            }
            if (branchId != null) {
                x = b.and(x, b.equal(r.get("branch").get("id"), branchId));
            }
            return x;
        }, p).map(this::ev));
    }

    @GetMapping("/expenses/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    public ExpenseView getExpense(@PathVariable UUID id) {
        return ev(fe(id));
    }

    @PostMapping("/expenses")
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public ExpenseView createExpense(@Valid @RequestBody ExpenseRequest r) {
        String num = r.expenseNumber();
        if (num == null || num.isBlank()) {
            num = "EXP-" + System.currentTimeMillis();
        } else if (expenseRepo.existsByExpenseNumberIgnoreCase(num)) {
            throw new BusinessException("Expense number already exists: " + num);
        }

        Expense e = new Expense();
        e.setExpenseNumber(num);
        ae(e, r);
        e.setCreatedBy(getCurrentUser());
        expenseRepo.save(e);
        audit.logCurrent("EXPENSE_CREATED", "EXPENSE", e.getId(), null, e.getExpenseNumber());
        return ev(e);
    }

    @PutMapping("/expenses/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_UPDATE')")
    public ExpenseView updateExpense(@PathVariable UUID id, @Valid @RequestBody ExpenseRequest r) {
        Expense e = fe(id);
        ae(e, r);
        expenseRepo.save(e);
        audit.logCurrent("EXPENSE_UPDATED", "EXPENSE", id, null, e.getExpenseNumber());
        return ev(e);
    }

    @DeleteMapping("/expenses/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_DELETE')")
    public void deleteExpense(@PathVariable UUID id) {
        Expense e = fe(id);
        expenseRepo.delete(e);
        audit.logCurrent("EXPENSE_DELETED", "EXPENSE", id, null, null);
    }

    private Expense fe(UUID id) {
        return expenseRepo.findById(id).orElseThrow(() -> new NotFoundException("Expense", id));
    }

    private void ae(Expense e, ExpenseRequest r) {
        ExpenseCategory cat = categoryRepo.findById(r.categoryId())
                .orElseThrow(() -> new NotFoundException("ExpenseCategory", r.categoryId()));
        e.setCategory(cat);
        e.setAmount(r.amount());
        e.setExpenseDate(r.expenseDate());
        e.setPaymentMethod(r.paymentMethod() == null ? PaymentMethod.CASH : r.paymentMethod());
        e.setReferenceNumber(r.referenceNumber());
        e.setPayee(r.payee());
        e.setDescription(r.description());
        if (r.branchId() != null) {
            e.setBranch(branchRepo.findById(r.branchId())
                    .orElseThrow(() -> new NotFoundException("Branch", r.branchId())));
        } else {
            e.setBranch(null);
        }
    }

    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        return userRepo.findByUsernameIgnoreCaseOrEmailIgnoreCase(name, name).orElse(null);
    }

    private ExpenseView ev(Expense e) {
        return new ExpenseView(e.getId(), e.getExpenseNumber(), e.getCategory().getId(), e.getCategory().getName(),
                e.getAmount(), e.getExpenseDate(), e.getPaymentMethod(), e.getReferenceNumber(), e.getPayee(),
                e.getDescription(), e.getBranch() != null ? e.getBranch().getId() : null,
                e.getBranch() != null ? e.getBranch().getName() : null,
                e.getCreatedBy() != null ? e.getCreatedBy().getUsername() : null);
    }
}
