package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vendors/{vendorId}/suppliers")
public class SupplierController {
    private final JdbcTemplate jdbcTemplate;
    private final PermissionAccessService permissionAccessService;

    public SupplierController(JdbcTemplate jdbcTemplate, PermissionAccessService permissionAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionAccessService = permissionAccessService;
    }

    @GetMapping
    public List<SupplierView> list(@PathVariable Long vendorId, Authentication authentication) {
        requireAccess(vendorId, authentication, false);
        return jdbcTemplate.query("SELECT id, name, phone, address FROM suppliers WHERE vendor_id = ? ORDER BY name",
                (rs, rowNum) -> new SupplierView(rs.getLong("id"), rs.getString("name"), rs.getString("phone"), rs.getString("address")), vendorId);
    }

    @PostMapping
    public ResponseEntity<SupplierView> create(@PathVariable Long vendorId, @Valid @RequestBody SupplierRequest request,
                                                Authentication authentication) {
        requireAccess(vendorId, authentication, true);
        try {
            Long id = jdbcTemplate.queryForObject("INSERT INTO suppliers (vendor_id, name, phone, address) VALUES (?, ?, ?, ?) RETURNING id",
                    Long.class, vendorId, request.name().trim(), blankToNull(request.phone()), blankToNull(request.address()));
            return ResponseEntity.status(HttpStatus.CREATED).body(new SupplierView(id, request.name().trim(), request.phone(), request.address()));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Supplier already exists for this vendor");
        }
    }

    private void requireAccess(Long vendorId, Authentication authentication, boolean write) {
        permissionAccessService.requireVendor(authentication, vendorId, "DELIVERY_MANAGE");
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public record SupplierView(Long id, String name, String phone, String address) { }
    public record SupplierRequest(@NotBlank @Size(max = 255) String name, @Size(max = 40) String phone, @Size(max = 500) String address) { }
}
