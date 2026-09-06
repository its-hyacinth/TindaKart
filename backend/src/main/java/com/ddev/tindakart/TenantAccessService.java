package com.ddev.tindakart;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class TenantAccessService {
    private final JdbcTemplate jdbcTemplate;

    public TenantAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<StoreAccess> storesFor(Authentication authentication) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) {
            return jdbcTemplate.query("SELECT id, name, code FROM stores ORDER BY name", (rs, rowNum) -> store(rs));
        }
        return jdbcTemplate.query("SELECT DISTINCT s.id, s.name, s.code FROM stores s "
                        + "JOIN store_user_roles sur ON sur.store_id = s.id "
                        + "JOIN users u ON u.id = sur.user_id WHERE LOWER(u.username) = LOWER(?) ORDER BY s.name",
                (rs, rowNum) -> store(rs), authentication.getName());
    }

    public boolean hasStoreRole(Authentication authentication, Long storeId, String roleName) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) return true;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM store_user_roles sur "
                        + "JOIN users u ON u.id = sur.user_id JOIN roles r ON r.id = sur.role_id "
                        + "WHERE sur.store_id = ? AND LOWER(u.username) = LOWER(?) AND r.name = ?",
                Integer.class, storeId, authentication.getName(), roleName);
        return count != null && count > 0;
    }

    public boolean hasStoreAccess(Authentication authentication, Long storeId) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) return true;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM store_user_roles sur "
                        + "JOIN users u ON u.id = sur.user_id WHERE sur.store_id = ? AND LOWER(u.username) = LOWER(?)",
                Integer.class, storeId, authentication.getName());
        return count != null && count > 0;
    }

    private StoreAccess store(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new StoreAccess(rs.getLong("id"), rs.getString("name"), rs.getString("code"));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    public record StoreAccess(Long id, String name, String code) { }
}
