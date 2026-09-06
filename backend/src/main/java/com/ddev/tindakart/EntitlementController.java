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
@RequestMapping("/api/stores/{storeId}/entitlements")
public class EntitlementController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;

    public EntitlementController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public Entitlements get(@PathVariable Long storeId, Authentication authentication) {
        if (!tenantAccessService.hasStoreAccess(authentication, storeId)) {
            throw new AccessDeniedException("Store access is required");
        }
        Map<String, Boolean> features = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT pf.feature_key, pf.enabled FROM store_subscriptions ss JOIN package_features pf ON pf.package_id = ss.package_id WHERE ss.store_id = ? AND ss.status IN ('TRIAL', 'ACTIVE')",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> features.put(rs.getString("feature_key"), rs.getBoolean("enabled")), storeId);
        jdbcTemplate.query("SELECT feature_key FROM store_feature_addons WHERE store_id = ? AND active = TRUE",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> features.put(rs.getString("feature_key"), true), storeId);
        Map<String, Integer> limits = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT pl.limit_key, pl.limit_value FROM store_subscriptions ss JOIN package_limits pl ON pl.package_id = ss.package_id WHERE ss.store_id = ? AND ss.status IN ('TRIAL', 'ACTIVE')",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> limits.put(rs.getString("limit_key"), rs.getInt("limit_value")), storeId);
        Integer seats = jdbcTemplate.query("SELECT seat_count FROM store_staff_seats WHERE store_id = ?",
                (rs, rowNum) -> rs.getInt("seat_count"), storeId).stream().findFirst().orElse(0);
        limits.computeIfPresent("MAX_STAFF", (key, value) -> value + seats);
        return new Entitlements(features, limits);
    }

    public record Entitlements(Map<String, Boolean> features, Map<String, Integer> limits) { }
}
