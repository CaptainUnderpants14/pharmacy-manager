package demo.pharma.catalog;

import demo.pharma.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;
    private String description;
    private boolean active = true;
}
