package demo.pharma.catalog;

import demo.pharma.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "manufacturers")
@Getter
@Setter
@NoArgsConstructor
public class Manufacturer extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String gstin;
    private boolean active = true;
}
