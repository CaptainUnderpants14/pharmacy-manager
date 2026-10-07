package demo.pharma.audit;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import demo.pharma.security.AppUser;
import demo.pharma.security.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository logs;
    private final UserRepository users;

    public void log(AppUser actor, String action, String type, UUID id, String oldValue, String newValue, String ip) {
        AuditLog l = new AuditLog();
        l.setUser(actor);
        l.setAction(action);
        l.setEntityType(type);
        l.setEntityId(id);
        l.setOldValue(oldValue);
        l.setNewValue(newValue);
        l.setIpAddress(ip);
        logs.save(l);
    }

    public void logCurrent(String action, String type, UUID id, String oldValue, String newValue) {
        String n = SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser u = n == null ? null : users.findByUsernameIgnoreCaseOrEmailIgnoreCase(n, n).orElse(null);
        log(u, action, type, id, oldValue, newValue, null);
    }
}
