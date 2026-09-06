package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class StoreHierarchyController {
    private final JdbcTemplate jdbc;
    private final TenantAccessService access;
    private final AuditService audit;
    private final PasswordEncoder passwordEncoder;

    public StoreHierarchyController(JdbcTemplate jdbc, TenantAccessService access, AuditService audit,
                                    PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.access = access;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/super-admin/stores")
    public List<StoreView> stores(Authentication authentication) {
        requireSuperAdmin(authentication);
        return jdbc.query("SELECT id, name, code, address, status FROM stores ORDER BY name",
                (rs, row) -> store(rs));
    }

    @GetMapping("/super-admin/stores/{storeId}")
    public StoreView store(@PathVariable Long storeId, Authentication authentication) {
        requireSuperAdmin(authentication);
        return findStore(storeId);
    }

    @PostMapping("/super-admin/stores")
    @Transactional
    public ResponseEntity<StoreView> createStore(@Valid @RequestBody StoreRequest request,
                                                  Authentication authentication) {
        requireSuperAdmin(authentication);
        Long id = jdbc.queryForObject("INSERT INTO stores (name, code, address) "
                + "VALUES (?, ?, ?) RETURNING id", Long.class, request.name().trim(),
                request.code().trim().toUpperCase(), blank(request.address()));
        audit.record(authentication.getName(), "STORE_CREATED", "STORE", id.toString(), request.name().trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(findStore(id));
    }

    @PatchMapping("/super-admin/stores/{storeId}/status")
    public StoreView status(@PathVariable Long storeId, @Valid @RequestBody StatusRequest request,
                            Authentication authentication) {
        requireSuperAdmin(authentication);
        if (!List.of("ACTIVE", "INACTIVE").contains(request.status())) {
            throw bad("Store status must be ACTIVE or INACTIVE");
        }
        if (jdbc.update("UPDATE stores SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.status(), storeId) == 0) throw notFound("Store not found");
        return findStore(storeId);
    }

    @GetMapping("/stores/{storeId}/staff")
    public List<StaffView> staff(@PathVariable Long storeId, Authentication authentication) {
        requireStoreAdmin(authentication, storeId);
        return jdbc.query("SELECT u.id, u.username, u.display_name, u.enabled FROM users u "
                + "JOIN store_user_roles sur ON sur.user_id = u.id WHERE sur.store_id = ? "
                + "ORDER BY u.display_name", (rs, row) -> findStaff(storeId, rs.getLong("id")), storeId);
    }

    @PostMapping("/stores/{storeId}/staff")
    @Transactional
    public ResponseEntity<StaffView> createStaff(@PathVariable Long storeId,
                                                 @Valid @RequestBody CreateStaffRequest request,
                                                 Authentication authentication) {
        requireStoreAdmin(authentication, storeId);
        if (!List.of("STAFF", "CASHIER", "INVENTORY_STAFF", "DEBT_STAFF", "DELIVERY_STAFF").contains(request.role())) {
            throw bad("Invalid store staff role");
        }
        Long userId;
        try {
            userId = jdbc.queryForObject("INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?) RETURNING id",
                    Long.class, request.username().trim(), passwordEncoder.encode(request.password()), request.displayName().trim());
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE name = ? ON CONFLICT DO NOTHING",
                userId, request.role());
        jdbc.update("INSERT INTO store_user_roles (user_id, store_id, role_id) SELECT ?, ?, id FROM roles WHERE name = ?",
                userId, storeId, request.role());
        audit.record(authentication.getName(), "STAFF_CREATED", "USER", userId.toString(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(findStaff(storeId, userId));
    }

    @PatchMapping("/stores/{storeId}/staff/{userId}/status")
    public StaffView staffStatus(@PathVariable Long storeId, @PathVariable Long userId,
                                 @Valid @RequestBody StatusRequest request, Authentication authentication) {
        requireStoreAdmin(authentication, storeId);
        if (!List.of("ACTIVE", "INACTIVE").contains(request.status())) throw bad("Invalid staff status");
        if (jdbc.update("UPDATE users SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? "
                + "AND EXISTS (SELECT 1 FROM store_user_roles WHERE user_id = ? AND store_id = ?)",
                "ACTIVE".equals(request.status()), userId, userId, storeId) == 0) throw notFound("Staff not found");
        return findStaff(storeId, userId);
    }

    private StoreView findStore(Long id) {
        return jdbc.query("SELECT id, name, code, address, status FROM stores WHERE id = ?",
                (rs, row) -> store(rs), id).stream().findFirst().orElseThrow(() -> notFound("Store not found"));
    }

    private StaffView findStaff(Long storeId, Long userId) {
        return jdbc.query("SELECT id, username, display_name, enabled FROM users u WHERE id = ? "
                        + "AND EXISTS (SELECT 1 FROM store_user_roles WHERE user_id = u.id AND store_id = ?)",
                (rs, row) -> new StaffView(rs.getLong("id"), rs.getString("username"), rs.getString("display_name"), rs.getBoolean("enabled")),
                userId, storeId).stream().findFirst().orElseThrow(() -> notFound("Staff not found"));
    }

    private StoreView store(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new StoreView(rs.getLong("id"), rs.getString("name"), rs.getString("code"),
                rs.getString("address"), rs.getString("status"));
    }

    private void requireStoreAdmin(Authentication authentication, Long storeId) {
        if (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                && !access.hasStoreRole(authentication, storeId, "STORE_ADMIN")) {
            throw new AccessDeniedException("Store Admin permission is required");
        }
    }

    private void requireSuperAdmin(Authentication authentication) {
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            throw new AccessDeniedException("Super Admin permission is required");
        }
    }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }

    public record StoreView(Long id, String name, String code, String address, String status) { }
    public record StaffView(Long id, String username, String displayName, boolean enabled) { }
    public record StoreRequest(@NotBlank @Size(max = 255) String name,
                               @NotBlank @Size(max = 80) String code,
                               @Size(max = 500) String address) { }
    public record CreateStaffRequest(@NotBlank @Size(max = 120) String username, @NotBlank @Size(max = 255) String password,
                                     @NotBlank @Size(max = 255) String displayName, @NotBlank String role) { }
    public record StatusRequest(@NotBlank String status) { }
}
