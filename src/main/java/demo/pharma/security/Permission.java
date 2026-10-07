package demo.pharma.security;

import demo.pharma.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String code;
    @Column(nullable = false)
    private String description;

    public Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
