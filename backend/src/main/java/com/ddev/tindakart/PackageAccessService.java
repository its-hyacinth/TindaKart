package com.ddev.tindakart;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PackageAccessService {
    private final JdbcTemplate jdbcTemplate;

    public PackageAccessService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public void requireFeature(Authentication authentication, Long storeId, String featureKey) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) return;
        Integer enabled = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM store_subscriptions vs "
                + "JOIN package_features pf ON pf.package_id = vs.package_id "
                + "WHERE vs.store_id = ? AND vs.status IN ('TRIAL', 'ACTIVE') AND pf.feature_key = ? AND pf.enabled = TRUE",
                Integer.class, storeId, featureKey);
        Integer addon = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM store_feature_addons "
                + "WHERE store_id = ? AND feature_key = ? AND active = TRUE", Integer.class, storeId, featureKey);
        if ((enabled == null || enabled == 0) && (addon == null || addon == 0)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Your package does not include the " + featureKey + " feature");
    }

    public boolean withinLimit(Long storeId, String limitKey, long currentValue, long requestedIncrease) {
        Integer limit = jdbcTemplate.query("SELECT pl.limit_value FROM store_subscriptions vs "
                        + "JOIN package_limits pl ON pl.package_id = vs.package_id WHERE vs.store_id = ? "
                        + "AND vs.status IN ('TRIAL', 'ACTIVE') AND pl.limit_key = ?",
                (rs, rowNum) -> rs.getInt("limit_value"), storeId, limitKey).stream().findFirst().orElse(null);
        if (limit == null) return true;
        if ("MAX_STAFF".equals(limitKey)) {
            Integer purchasedSeats = jdbcTemplate.query("SELECT seat_count FROM store_staff_seats WHERE store_id = ?",
                    (rs, rowNum) -> rs.getInt("seat_count"), storeId).stream().findFirst().orElse(0);
            limit += purchasedSeats;
        }
        return currentValue + requestedIncrease <= limit;
    }
}
