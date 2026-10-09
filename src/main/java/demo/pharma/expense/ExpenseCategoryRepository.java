package demo.pharma.expense;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
