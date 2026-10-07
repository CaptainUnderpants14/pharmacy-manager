package demo.pharma.audit;

import java.util.UUID;

import demo.pharma.common.entity.BaseEntity;
import demo.pharma.security.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs", indexes = { @Index(name = "idx_audit_entity", columnList = "entityType,entityId") })
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser user;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(nullable = false, length = 80)
    private String entityType;
    @Column(nullable = false)
    private UUID entityId;
    @Column(columnDefinition = "text")
    private String oldValue;
    @Column(columnDefinition = "text")
    private String newValue;
    private String ipAddress;
}
