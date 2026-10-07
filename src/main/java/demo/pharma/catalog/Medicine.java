package demo.pharma.catalog;

import java.math.BigDecimal;

import demo.pharma.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicines", indexes = { @Index(name = "idx_medicine_code", columnList = "medicineCode"),
        @Index(name = "idx_medicine_name", columnList = "name"),
        @Index(name = "idx_medicine_generic", columnList = "genericName") })
@Getter
@Setter
@NoArgsConstructor
public class Medicine extends BaseEntity {
    @Column(nullable = false, unique = true, length = 60)
    private String medicineCode;
    @Column(nullable = false)
    private String name;
    private String genericName;
    private String brandName;
    @ManyToOne(fetch = FetchType.LAZY)
    private Manufacturer manufacturer;
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;
    private String composition;
    @Enumerated(EnumType.STRING)
    private DosageForm dosageForm;
    private String strength;
    private String packSize;
    private String unit;
    private boolean prescriptionRequired;
    @Column(precision = 5, scale = 2)
    private BigDecimal taxPercentage = BigDecimal.ZERO;
    @Column(precision = 14, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;
    @Column(columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status = RecordStatus.ACTIVE;
}
