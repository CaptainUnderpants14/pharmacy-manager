package demo.pharma.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.common.web.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
public class AuditController {
    private final AuditLogRepository logs;

    @GetMapping
    public PageResponse<AuditLog> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable page) {
        return PageResponse.of(logs.findAll(page));
    }
}
