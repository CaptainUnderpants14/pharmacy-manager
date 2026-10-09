package demo.pharma.notification;

import demo.pharma.common.entity.BaseEntity;
import demo.pharma.security.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "recipient_user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "text", nullable = false)
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type = NotificationType.SYSTEM;
    private boolean readStatus = false;
    @ManyToOne
    private AppUser recipientUser;
}
