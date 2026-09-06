package com.ddev.tindakart;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendors/{vendorId}/entitlements")
public class EntitlementController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;

    public EntitlementController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public Entitlements get(@PathVariable Long vendorId, Authentication authentication) {
        if (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                && tenantAccessService.vendorsFor(authentication).stream().noneMatch(v -> v.id().equals(vendorId))) {
            throw new AccessDeniedException("Vendor access is required");
        }
        Map<String, Boolean> features = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT pf.feature_key, pf.enabled FROM vendor_subscriptions vs JOIN package_features pf ON pf.package_id = vs.package_id WHERE vs.vendor_id = ? AND vs.status IN ('TRIAL', 'ACTIVE')",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> features.put(rs.getString("feature_key"), rs.getBoolean("enabled")), vendorId);
        jdbcTemplate.query("SELECT feature_key FROM vendor_feature_addons WHERE vendor_id = ? AND active = TRUE",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> features.put(rs.getString("feature_key"), true), vendorId);
        Map<String, Integer> limits = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT pl.limit_key, pl.limit_value FROM vendor_subscriptions vs JOIN package_limits pl ON pl.package_id = vs.package_id WHERE vs.vendor_id = ? AND vs.status IN ('TRIAL', 'ACTIVE')",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> limits.put(rs.getString("limit_key"), rs.getInt("limit_value")), vendorId);
        Integer seats = jdbcTemplate.query("SELECT seat_count FROM vendor_staff_seats WHERE vendor_id = ?",
                (rs, rowNum) -> rs.getInt("seat_count"), vendorId).stream().findFirst().orElse(0);
        limits.computeIfPresent("MAX_STAFF", (key, value) -> value + seats);
        return new Entitlements(features, limits);
    }

    public record Entitlements(Map<String, Boolean> features, Map<String, Integer> limits) { }
}
