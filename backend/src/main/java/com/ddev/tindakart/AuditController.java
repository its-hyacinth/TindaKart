package com.ddev.tindakart;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/super-admin/audit")
public class AuditController {
    private final JdbcTemplate jdbcTemplate;

    public AuditController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<AuditLogView> list(@RequestParam(defaultValue = "100") int limit, Authentication authentication) {
        requireSuperAdmin(authentication);
        if (limit < 1 || limit > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 500");
        }
        return jdbcTemplate.query("SELECT a.id, a.action, a.entity_type, a.entity_id, a.details, a.created_at, u.username "
                        + "FROM audit_logs a LEFT JOIN users u ON u.id = a.user_id ORDER BY a.created_at DESC, a.id DESC LIMIT ?",
                (rs, rowNum) -> new AuditLogView(rs.getLong("id"), rs.getString("action"), rs.getString("entity_type"),
                        rs.getString("entity_id"), rs.getString("details"), rs.getObject("created_at", OffsetDateTime.class),
                        rs.getString("username")), limit);
    }

    private void requireSuperAdmin(Authentication authentication) {
        if (authentication.getAuthorities().stream().noneMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Super Admin permission is required");
        }
    }

    public record AuditLogView(Long id, String action, String entityType, String entityId, String details,
                               OffsetDateTime createdAt, String username) { }
}
