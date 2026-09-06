package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vendors/{vendorId}/staff")
public class StaffManagementController {
    private static final List<String> ASSIGNABLE_ROLES = List.of(
            "VENDOR_ADMIN", "STAFF", "CASHIER", "INVENTORY_STAFF", "DEBT_STAFF", "DELIVERY_STAFF");

    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;
    private final PasswordEncoder passwordEncoder;
    private final PackageAccessService packageAccessService;
    private final AuditService auditService;

    public StaffManagementController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService,
                                     PasswordEncoder passwordEncoder, PackageAccessService packageAccessService,
                                     AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
        this.passwordEncoder = passwordEncoder;
        this.packageAccessService = packageAccessService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<StaffView> list(@PathVariable Long vendorId, Authentication authentication) {
        requireVendorAdmin(authentication, vendorId);
        return jdbcTemplate.query("SELECT DISTINCT u.id, u.username, u.display_name, u.enabled "
                        + "FROM users u WHERE EXISTS (SELECT 1 FROM vendor_user_roles vur "
                        + "WHERE vur.user_id = u.id AND vur.vendor_id = ?) OR EXISTS "
                        + "(SELECT 1 FROM store_user_roles sur JOIN stores s ON s.id = sur.store_id "
                        + "WHERE sur.user_id = u.id AND s.vendor_id = ?) ORDER BY u.display_name",
                (rs, rowNum) -> staff(rs, vendorId), vendorId, vendorId);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<StaffView> create(@PathVariable Long vendorId,
                                             @Valid @RequestBody CreateStaffRequest request,
                                             Authentication authentication) {
        requireVendorAdmin(authentication, vendorId);
        long currentStaff = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT u.id) FROM users u WHERE u.id <> COALESCE((SELECT owner_user_id FROM vendors WHERE id = ?), -1) AND (EXISTS "
                + "(SELECT 1 FROM vendor_user_roles vur WHERE vur.user_id = u.id AND vur.vendor_id = ?) OR EXISTS "
                + "(SELECT 1 FROM store_user_roles sur JOIN stores s ON s.id = sur.store_id WHERE sur.user_id = u.id AND s.vendor_id = ?))", Long.class, vendorId, vendorId, vendorId);
        if (!packageAccessService.withinLimit(vendorId, "MAX_STAFF", currentStaff, 1)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your package staff limit has been reached");
        }
        validateRole(request.role());
        if (!"VENDOR_ADMIN".equals(request.role()) && request.storeIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff must be assigned to at least one store");
        }
        validateStores(vendorId, request.storeIds());
        Long userId;
        try {
            userId = jdbcTemplate.queryForObject("INSERT INTO users (username, password_hash, display_name) "
                    + "VALUES (?, ?, ?) RETURNING id", Long.class, request.username().trim(),
                    passwordEncoder.encode(request.password()), request.displayName().trim());
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        assignGlobalRole(userId, request.role());
        if ("VENDOR_ADMIN".equals(request.role())) {
            jdbcTemplate.update("INSERT INTO vendor_user_roles (user_id, vendor_id, role_id) "
                    + "SELECT ?, ?, id FROM roles WHERE name = ?", userId, vendorId, request.role());
        } else {
            assignStores(userId, vendorId, request.storeIds(), request.role());
        }
        auditService.record(authentication.getName(), "STAFF_CREATED", "USER", userId.toString(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(findStaff(vendorId, userId));
    }

    @PatchMapping("/{userId}/status")
    @Transactional
    public StaffView updateStatus(@PathVariable Long vendorId, @PathVariable Long userId,
                                  @Valid @RequestBody StatusRequest request,
                                  Authentication authentication) {
        requireVendorAdmin(authentication, vendorId);
        if (!List.of("ACTIVE", "INACTIVE").contains(request.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff status must be ACTIVE or INACTIVE");
        }
        requireStaffBelongsToVendor(vendorId, userId);
        jdbcTemplate.update("UPDATE users SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                "ACTIVE".equals(request.status()), userId);
        auditService.record(authentication.getName(), "STAFF_STATUS_CHANGED", "USER", userId.toString(), request.status());
        return findStaff(vendorId, userId);
    }

    @PutMapping("/{userId}/stores")
    @Transactional
    public StaffView assignStores(@PathVariable Long vendorId, @PathVariable Long userId,
                                  @Valid @RequestBody StoreAssignmentRequest request,
                                  Authentication authentication) {
        requireVendorAdmin(authentication, vendorId);
        requireStaffBelongsToVendor(vendorId, userId);
        if (request.storeIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one store is required");
        }
        validateStores(vendorId, request.storeIds());
        Long roleId = jdbcTemplate.queryForObject("SELECT role_id FROM store_user_roles WHERE user_id = ? "
                + "AND store_id IN (SELECT id FROM stores WHERE vendor_id = ?) LIMIT 1", Long.class, userId, vendorId);
        if (roleId == null) {
            roleId = jdbcTemplate.queryForObject("SELECT role_id FROM vendor_user_roles WHERE user_id = ? "
                    + "AND vendor_id = ? LIMIT 1", Long.class, userId, vendorId);
        }
        jdbcTemplate.update("DELETE FROM store_user_roles WHERE user_id = ? AND store_id IN "
                + "(SELECT id FROM stores WHERE vendor_id = ?)", userId, vendorId);
        if (roleId != null) {
            jdbcTemplate.update("INSERT INTO store_user_roles (user_id, store_id, role_id) "
                    + "SELECT ?, id, ? FROM stores WHERE vendor_id = ? AND id = ANY (?::bigint[]) "
                    + "ON CONFLICT DO NOTHING", userId, roleId, vendorId, request.storeIds().toArray(Long[]::new));
        }
        auditService.record(authentication.getName(), "STAFF_STORES_ASSIGNED", "USER", userId.toString(), request.storeIds().toString());
        return findStaff(vendorId, userId);
    }

    private StaffView staff(java.sql.ResultSet rs, Long vendorId) throws java.sql.SQLException {
        return findStaff(vendorId, rs.getLong("id"));
    }

    private StaffView findStaff(Long vendorId, Long userId) {
        StaffView result = jdbcTemplate.query("SELECT id, username, display_name, enabled FROM users "
                        + "WHERE id = ?", (rs, rowNum) -> new StaffView(rs.getLong("id"), rs.getString("username"),
                        rs.getString("display_name"), rs.getBoolean("enabled"), roles(vendorId, userId), stores(userId)), userId)
                .stream().findFirst().orElseThrow(() -> notFound("Staff member not found"));
        return result;
    }

    private List<String> roles(Long vendorId, Long userId) {
        return jdbcTemplate.query("SELECT DISTINCT r.name FROM roles r WHERE EXISTS "
                        + "(SELECT 1 FROM vendor_user_roles vur WHERE vur.role_id = r.id AND vur.user_id = ? AND vur.vendor_id = ?) "
                        + "OR EXISTS (SELECT 1 FROM store_user_roles sur JOIN stores s ON s.id = sur.store_id "
                        + "WHERE sur.role_id = r.id AND sur.user_id = ? AND s.vendor_id = ?) ORDER BY r.name",
                (rs, rowNum) -> rs.getString("name"), userId, vendorId, userId, vendorId);
    }

    private List<StoreSummary> stores(Long userId) {
        return jdbcTemplate.query("SELECT s.id, s.name, s.code FROM stores s JOIN store_user_roles sur "
                        + "ON sur.store_id = s.id WHERE sur.user_id = ? ORDER BY s.name",
                (rs, rowNum) -> new StoreSummary(rs.getLong("id"), rs.getString("name"), rs.getString("code")), userId);
    }

    private void assignGlobalRole(Long userId, String role) {
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE name = ? "
                + "ON CONFLICT DO NOTHING", userId, role);
    }

    private void assignStores(Long userId, Long vendorId, List<Long> storeIds, String role) {
        jdbcTemplate.update("INSERT INTO store_user_roles (user_id, store_id, role_id) "
                + "SELECT ?, id, (SELECT id FROM roles WHERE name = ?) FROM stores "
                + "WHERE vendor_id = ? AND id = ANY (?::bigint[]) ON CONFLICT DO NOTHING",
                userId, role, vendorId, storeIds.toArray(Long[]::new));
    }

    private void validateStores(Long vendorId, List<Long> storeIds) {
        if (storeIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid store ID");
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores WHERE vendor_id = ? "
                + "AND id = ANY (?::bigint[])", Integer.class, vendorId, storeIds.toArray(Long[]::new));
        if (count == null || count != storeIds.stream().distinct().count()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every store must belong to this vendor");
        }
    }

    private void requireVendorAdmin(Authentication authentication, Long vendorId) {
        if (!tenantAccessService.hasVendorRole(authentication, vendorId, "VENDOR_ADMIN")) {
            throw new AccessDeniedException("Vendor Admin permission is required");
        }
    }

    private void requireStaffBelongsToVendor(Long vendorId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users u WHERE u.id = ? AND "
                + "(EXISTS (SELECT 1 FROM vendor_user_roles vur WHERE vur.user_id = u.id AND vur.vendor_id = ?) "
                + "OR EXISTS (SELECT 1 FROM store_user_roles sur JOIN stores s ON s.id = sur.store_id "
                + "WHERE sur.user_id = u.id AND s.vendor_id = ?))", Integer.class, userId, vendorId, vendorId);
        if (count == null || count == 0) throw notFound("Staff member not found for this vendor");
    }

    private void validateRole(String role) {
        if (!ASSIGNABLE_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid staff role");
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    public record StaffView(Long id, String username, String displayName, boolean enabled,
                            List<String> roles, List<StoreSummary> stores) { }
    public record StoreSummary(Long id, String name, String code) { }
    public record CreateStaffRequest(@NotBlank @Size(max = 120) String username,
                                     @NotBlank @Size(min = 8, max = 255) String password,
                                     @NotBlank @Size(max = 255) String displayName,
                                     @NotBlank String role,
                                     List<Long> storeIds) {
        public CreateStaffRequest {
            storeIds = storeIds == null ? List.of() : storeIds;
        }
    }
    public record StoreAssignmentRequest(List<Long> storeIds) {
        public StoreAssignmentRequest {
            storeIds = storeIds == null ? List.of() : storeIds;
        }
    }
    public record StatusRequest(@NotBlank String status) { }
}
