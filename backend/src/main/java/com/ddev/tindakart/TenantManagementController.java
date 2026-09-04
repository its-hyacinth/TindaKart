package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
public class TenantManagementController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;

    public TenantManagementController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/super-admin/vendors")
    public List<VendorView> listVendors(Authentication authentication) {
        requireRole(authentication, "ROLE_SUPER_ADMIN");
        return jdbcTemplate.query("SELECT id, name, status FROM vendors ORDER BY name",
                (rs, rowNum) -> new VendorView(rs.getLong("id"), rs.getString("name"), rs.getString("status")));
    }

    @PostMapping("/super-admin/vendors")
    public ResponseEntity<VendorView> createVendor(
            @Valid @RequestBody VendorRequest request, Authentication authentication) {
        requireRole(authentication, "ROLE_SUPER_ADMIN");
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO vendors (name) VALUES (?) RETURNING id", Long.class, request.name().trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(findVendor(id));
    }

    @PatchMapping("/super-admin/vendors/{vendorId}/status")
    public VendorView updateVendorStatus(@PathVariable Long vendorId,
                                         @Valid @RequestBody StatusRequest request,
                                         Authentication authentication) {
        requireRole(authentication, "ROLE_SUPER_ADMIN");
        validateVendorStatus(request.status());
        int updated = jdbcTemplate.update("UPDATE vendors SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.status(), vendorId);
        if (updated == 0) throw notFound("Vendor not found");
        return findVendor(vendorId);
    }

    @GetMapping("/vendors/{vendorId}/stores")
    public List<StoreView> listStores(@PathVariable Long vendorId, Authentication authentication) {
        requireVendorAccess(authentication, vendorId);
        return jdbcTemplate.query("SELECT id, vendor_id, name, code, address, status FROM stores "
                        + "WHERE vendor_id = ? ORDER BY name", (rs, rowNum) -> store(rs), vendorId);
    }

    @PostMapping("/vendors/{vendorId}/stores")
    public ResponseEntity<StoreView> createStore(@PathVariable Long vendorId,
                                                   @Valid @RequestBody StoreRequest request,
                                                   Authentication authentication) {
        requireVendorAccess(authentication, vendorId);
        Long id;
        try {
            id = jdbcTemplate.queryForObject("INSERT INTO stores (vendor_id, name, code, address) "
                    + "VALUES (?, ?, ?, ?) RETURNING id", Long.class, vendorId, request.name().trim(),
                    request.code().trim().toUpperCase(), blankToNull(request.address()));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Store code already exists for this vendor");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(findStore(vendorId, id));
    }

    @PatchMapping("/vendors/{vendorId}/stores/{storeId}/status")
    public StoreView updateStoreStatus(@PathVariable Long vendorId, @PathVariable Long storeId,
                                       @Valid @RequestBody StatusRequest request,
                                       Authentication authentication) {
        requireVendorAccess(authentication, vendorId);
        if (!List.of("ACTIVE", "INACTIVE").contains(request.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Store status must be ACTIVE or INACTIVE");
        }
        int updated = jdbcTemplate.update("UPDATE stores SET status = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND vendor_id = ?", request.status(), storeId, vendorId);
        if (updated == 0) throw notFound("Store not found");
        return findStore(vendorId, storeId);
    }

    private VendorView findVendor(Long id) {
        return jdbcTemplate.query("SELECT id, name, status FROM vendors WHERE id = ?",
                (rs, rowNum) -> new VendorView(rs.getLong("id"), rs.getString("name"), rs.getString("status")), id)
                .stream().findFirst().orElseThrow(() -> notFound("Vendor not found"));
    }

    private StoreView findStore(Long vendorId, Long id) {
        return jdbcTemplate.query("SELECT id, vendor_id, name, code, address, status FROM stores "
                        + "WHERE id = ? AND vendor_id = ?", (rs, rowNum) -> store(rs), id, vendorId)
                .stream().findFirst().orElseThrow(() -> notFound("Store not found"));
    }

    private StoreView store(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new StoreView(rs.getLong("id"), rs.getLong("vendor_id"), rs.getString("name"),
                rs.getString("code"), rs.getString("address"), rs.getString("status"));
    }

    private void requireVendorAccess(Authentication authentication, Long vendorId) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) return;
        boolean allowed = tenantAccessService.vendorsFor(authentication).stream()
                .anyMatch(vendor -> vendor.id().equals(vendorId));
        if (!allowed) throw new AccessDeniedException("You do not have access to this vendor");
    }

    private void requireRole(Authentication authentication, String role) {
        if (!hasRole(authentication, role)) throw new AccessDeniedException("Insufficient permission");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    private void validateVendorStatus(String status) {
        if (!List.of("PENDING", "ACTIVE", "SUSPENDED", "CANCELLED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid vendor status");
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record VendorView(Long id, String name, String status) { }
    public record StoreView(Long id, Long vendorId, String name, String code, String address, String status) { }

    public record VendorRequest(@NotBlank @Size(max = 255) String name) { }

    public record StoreRequest(@NotBlank @Size(max = 255) String name,
                               @NotBlank @Size(max = 80) String code,
                               @Size(max = 500) String address) { }

    public record StatusRequest(@NotBlank String status) { }
}
