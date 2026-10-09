package demo.pharma.expense;

import demo.pharma.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expense_categories")
@Getter
@Setter
@NoArgsConstructor
public class ExpenseCategory extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    private String description;
    private boolean active = true;
}
