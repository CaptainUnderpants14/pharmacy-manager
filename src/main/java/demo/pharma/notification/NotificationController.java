package demo.pharma.notification;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.common.exception.NotFoundException;
import demo.pharma.common.web.PageResponse;
import demo.pharma.security.AppUser;
import demo.pharma.security.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repo;
    private final NotificationService service;
    private final UserRepository userRepo;

    public record View(UUID id, String title, String message, NotificationType type, boolean readStatus,
            Instant createdAt) {
    }

    public record CreateRequest(@NotBlank String title, @NotBlank String message, NotificationType type,
            UUID recipientUserId) {
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<View> listForUser(@PageableDefault(size = 20) Pageable p) {
        AppUser u = getCurrentUser();
        return PageResponse.of(repo.findForUser(u, p).map(this::v));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW') or hasAuthority('SETTINGS_MANAGE')")
    public View create(@Valid @RequestBody CreateRequest r) {
        AppUser recipient = r.recipientUserId() == null ? getCurrentUser()
                : userRepo.findById(r.recipientUserId())
                        .orElseThrow(() -> new NotFoundException("User", r.recipientUserId()));
        Notification n = service.send(r.title(), r.message(), r.type(), recipient);
        return v(n);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public void markRead(@PathVariable UUID id) {
        service.markAsRead(id);
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public void markAllRead() {
        AppUser u = getCurrentUser();
        if (u != null) {
            service.markAllAsReadForUser(u);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public void delete(@PathVariable UUID id) {
        Notification n = repo.findById(id).orElseThrow(() -> new NotFoundException("Notification", id));
        repo.delete(n);
    }

    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        return userRepo.findByUsernameIgnoreCaseOrEmailIgnoreCase(name, name).orElse(null);
    }

    private View v(Notification n) {
        return new View(n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.isReadStatus(), n.getCreatedAt());
    }
}
