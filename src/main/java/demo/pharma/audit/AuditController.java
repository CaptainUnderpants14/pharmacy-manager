package demo.pharma.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
public class AuditController {
    private final AuditLogRepository logs;

    record View(UUID id, String username, String action, String entityType, UUID entityId, String oldValue, String newValue, String ipAddress, Instant createdAt) {}

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<View> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable page) {
        return PageResponse.of(logs.findAll(page).map(log -> new View(log.getId(), log.getUser() == null ? null : log.getUser().getUsername(), log.getAction(), log.getEntityType(), log.getEntityId(), log.getOldValue(), log.getNewValue(), log.getIpAddress(), log.getCreatedAt())));
    }
}
