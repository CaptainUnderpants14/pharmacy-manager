package demo.pharma.branch;

import demo.pharma.catalog.RecordStatus;
import demo.pharma.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "branches", indexes = { @Index(name = "idx_branch_code", columnList = "code") })
@Getter
@Setter
@NoArgsConstructor
public class Branch extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(nullable = false)
    private String name;
    private String address;
    private String phone;
    private String email;
    private boolean isMain = false;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status = RecordStatus.ACTIVE;
}
