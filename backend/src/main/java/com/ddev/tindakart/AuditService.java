package com.ddev.tindakart;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate jdbcTemplate;

    public AuditService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public void record(String username, String action, String entityType, String entityId, String details) {
        Long userId = jdbcTemplate.query("SELECT id FROM users WHERE LOWER(username) = LOWER(?)",
                (rs, rowNum) -> rs.getLong("id"), username == null ? "" : username).stream().findFirst().orElse(null);
        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)",
                userId, action, entityType, entityId, details);
    }
}
