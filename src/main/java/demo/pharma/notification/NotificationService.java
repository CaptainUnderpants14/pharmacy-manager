package demo.pharma.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import demo.pharma.common.exception.NotFoundException;
import demo.pharma.security.AppUser;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;

    @Transactional
    public Notification send(String title, String message, NotificationType type, AppUser recipient) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type == null ? NotificationType.SYSTEM : type);
        n.setRecipientUser(recipient);
        n.setReadStatus(false);
        return repo.save(n);
    }

    @Transactional
    public Notification broadcast(String title, String message, NotificationType type) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type == null ? NotificationType.SYSTEM : type);
        n.setRecipientUser(null);
        n.setReadStatus(false);
        return repo.save(n);
    }

    @Transactional
    public void markAsRead(UUID id) {
        Notification n = repo.findById(id).orElseThrow(() -> new NotFoundException("Notification", id));
        n.setReadStatus(true);
        repo.save(n);
    }

    @Transactional
    public void markAllAsReadForUser(AppUser user) {
        List<Notification> unread = repo.findUnreadForUser(user);
        for (Notification n : unread) {
            n.setReadStatus(true);
        }
        repo.saveAll(unread);
    }
}
