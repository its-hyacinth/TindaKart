package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class PackageManagementController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;

    public PackageManagementController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/super-admin/packages")
    public List<PackageView> listPackages(Authentication authentication) {
        requireSuperAdmin(authentication);
        return jdbcTemplate.query("SELECT id, name, description, monthly_price, annual_price, active "
                        + "FROM packages ORDER BY monthly_price, name", (rs, rowNum) -> findPackage(rs.getLong("id")));
    }

    @PostMapping("/super-admin/packages")
    @Transactional
    public ResponseEntity<PackageView> createPackage(@Valid @RequestBody PackageRequest request,
                                                      Authentication authentication) {
        requireSuperAdmin(authentication);
        Long id;
        try {
            id = jdbcTemplate.queryForObject("INSERT INTO packages (name, description, monthly_price, annual_price, active) "
                    + "VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, request.name().trim(), blankToNull(request.description()),
                    request.monthlyPrice(), request.annualPrice(), request.active());
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package name already exists");
        }
        saveControls(id, request.features(), request.limits());
        return ResponseEntity.status(HttpStatus.CREATED).body(findPackage(id));
    }

    @PutMapping("/super-admin/packages/{packageId}")
    @Transactional
    public PackageView updatePackage(@PathVariable Long packageId, @Valid @RequestBody PackageRequest request,
                                     Authentication authentication) {
        requireSuperAdmin(authentication);
        try {
            int updated = jdbcTemplate.update("UPDATE packages SET name = ?, description = ?, monthly_price = ?, "
                    + "annual_price = ?, active = ? WHERE id = ?", request.name().trim(), blankToNull(request.description()),
                    request.monthlyPrice(), request.annualPrice(), request.active(), packageId);
            if (updated == 0) throw notFound("Package not found");
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package name already exists");
        }
        saveControls(packageId, request.features(), request.limits());
        return findPackage(packageId);
    }

    @GetMapping("/stores/{storeId}/packages")
    public List<PackageView> availablePackages(@PathVariable Long storeId, Authentication authentication) {
        requireStoreAccess(authentication, storeId);
        return jdbcTemplate.query("SELECT id FROM packages WHERE active = TRUE ORDER BY monthly_price, name",
                (rs, rowNum) -> findPackage(rs.getLong("id")));
    }

    @GetMapping("/stores/{storeId}/subscription")
    public SubscriptionView currentSubscription(@PathVariable Long storeId, Authentication authentication) {
        requireStoreAccess(authentication, storeId);
        return findSubscription(storeId);
    }

    @PutMapping("/stores/{storeId}/subscription")
    @Transactional
    public SubscriptionView selectPackage(@PathVariable Long storeId,
                                          @Valid @RequestBody SelectPackageRequest request,
                                          Authentication authentication) {
        requireStoreAdmin(authentication, storeId);
        Integer active = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM packages WHERE id = ? AND active = TRUE",
                Integer.class, request.packageId());
        if (active == null || active == 0) throw notFound("Active package not found");
        jdbcTemplate.update("UPDATE store_subscriptions SET status = 'CANCELLED', ends_at = CURRENT_TIMESTAMP "
                + "WHERE store_id = ? AND status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED')", storeId);
        Long id = jdbcTemplate.queryForObject("INSERT INTO store_subscriptions (store_id, package_id, status) "
                + "VALUES (?, ?, 'TRIAL') RETURNING id", Long.class, storeId, request.packageId());
        return findSubscriptionById(id);
    }

    private void saveControls(Long packageId, Map<String, Boolean> features, Map<String, Integer> limits) {
        jdbcTemplate.update("DELETE FROM package_features WHERE package_id = ?", packageId);
        if (features != null) features.forEach((key, enabled) -> jdbcTemplate.update(
                "INSERT INTO package_features (package_id, feature_key, enabled) VALUES (?, ?, ?)", packageId, key, enabled));
        jdbcTemplate.update("DELETE FROM package_limits WHERE package_id = ?", packageId);
        if (limits != null) limits.forEach((key, value) -> jdbcTemplate.update(
                "INSERT INTO package_limits (package_id, limit_key, limit_value) VALUES (?, ?, ?)", packageId, key, value));
    }

    private PackageView findPackage(Long id) {
        return jdbcTemplate.query("SELECT id, name, description, monthly_price, annual_price, active FROM packages WHERE id = ?",
                        (rs, rowNum) -> new PackageView(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                                rs.getBigDecimal("monthly_price"), rs.getBigDecimal("annual_price"), rs.getBoolean("active"),
                                controls("package_features", "feature_key", "enabled", id),
                                controls("package_limits", "limit_key", "limit_value", id)), id)
                .stream().findFirst().orElseThrow(() -> notFound("Package not found"));
    }

    private Map<String, Object> controls(String table, String keyColumn, String valueColumn, Long packageId) {
        Map<String, Object> result = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT " + keyColumn + ", " + valueColumn + " FROM " + table + " WHERE package_id = ? ORDER BY " + keyColumn,
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> result.put(rs.getString(1), rs.getObject(2)), packageId);
        return result;
    }

    private SubscriptionView findSubscription(Long storeId) {
        return jdbcTemplate.query("SELECT vs.id, vs.store_id, vs.package_id, p.name AS package_name, vs.status, "
                        + "vs.starts_at, vs.ends_at FROM store_subscriptions vs JOIN packages p ON p.id = vs.package_id "
                        + "WHERE vs.store_id = ? ORDER BY vs.created_at DESC LIMIT 1", (rs, rowNum) -> subscription(rs), storeId)
                .stream().findFirst().orElseThrow(() -> notFound("No subscription found"));
    }

    private SubscriptionView findSubscriptionById(Long id) {
        return jdbcTemplate.query("SELECT vs.id, vs.store_id, vs.package_id, p.name AS package_name, vs.status, "
                        + "vs.starts_at, vs.ends_at FROM store_subscriptions vs JOIN packages p ON p.id = vs.package_id "
                        + "WHERE vs.id = ?", (rs, rowNum) -> subscription(rs), id)
                .stream().findFirst().orElseThrow(() -> notFound("Subscription not found"));
    }

    private SubscriptionView subscription(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SubscriptionView(rs.getLong("id"), rs.getLong("store_id"), rs.getLong("package_id"),
                rs.getString("package_name"), rs.getString("status"), rs.getObject("starts_at", java.time.OffsetDateTime.class),
                rs.getObject("ends_at", java.time.OffsetDateTime.class));
    }

    private void requireSuperAdmin(Authentication authentication) {
        if (!hasRole(authentication, "ROLE_SUPER_ADMIN")) throw new AccessDeniedException("Super Admin permission is required");
    }

    private void requireStoreAdmin(Authentication authentication, Long storeId) {
        if (!tenantAccessService.hasStoreRole(authentication, storeId, "STORE_ADMIN")) {
            throw new AccessDeniedException("Store Admin permission is required");
        }
    }

    private void requireStoreAccess(Authentication authentication, Long storeId) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) return;
        boolean allowed = tenantAccessService.storesFor(authentication).stream().anyMatch(store -> store.id().equals(storeId));
        if (!allowed) throw new AccessDeniedException("You do not have access to this store");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }

    public record PackageView(Long id, String name, String description, BigDecimal monthlyPrice, BigDecimal annualPrice,
                              boolean active, Map<String, Object> features, Map<String, Object> limits) { }
    public record PackageRequest(@NotBlank @Size(max = 120) String name, @Size(max = 500) String description,
                                 @NotNull @DecimalMin("0.00") BigDecimal monthlyPrice,
                                 @NotNull @DecimalMin("0.00") BigDecimal annualPrice,
                                 boolean active, Map<String, Boolean> features, Map<String, Integer> limits) { }
    public record SelectPackageRequest(@NotNull Long packageId) { }
    public record SubscriptionView(Long id, Long storeId, Long packageId, String packageName, String status,
                                   java.time.OffsetDateTime startsAt, java.time.OffsetDateTime endsAt) { }
}
