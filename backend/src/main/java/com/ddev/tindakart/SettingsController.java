package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vendors/{vendorId}/settings")
public class SettingsController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;

    public SettingsController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public BusinessSettings get(@PathVariable Long vendorId, Authentication authentication) {
        requireAccess(vendorId, authentication, false);
        return jdbcTemplate.query("SELECT business_name, business_address, tin, vat_registered, vat_rate, near_expiration_days, receipt_footer FROM business_settings WHERE vendor_id = ?",
                (rs, rowNum) -> new BusinessSettings(rs.getString("business_name"), rs.getString("business_address"), rs.getString("tin"),
                        rs.getBoolean("vat_registered"), rs.getBigDecimal("vat_rate"), rs.getInt("near_expiration_days"), rs.getString("receipt_footer")), vendorId)
                .stream().findFirst().orElse(new BusinessSettings(null, null, null, false, BigDecimal.ZERO, 30, null));
    }

    @PutMapping
    @Transactional
    public BusinessSettings update(@PathVariable Long vendorId, @Valid @RequestBody BusinessSettings request,
                                   Authentication authentication) {
        requireAccess(vendorId, authentication, true);
        if (request.vatRegistered() && request.vatRate().signum() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VAT rate is required when VAT is enabled");
        }
        jdbcTemplate.update("INSERT INTO business_settings (vendor_id, business_name, business_address, tin, vat_registered, vat_rate, near_expiration_days, receipt_footer) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (vendor_id) DO UPDATE SET business_name = EXCLUDED.business_name, business_address = EXCLUDED.business_address, "
                + "tin = EXCLUDED.tin, vat_registered = EXCLUDED.vat_registered, vat_rate = EXCLUDED.vat_rate, near_expiration_days = EXCLUDED.near_expiration_days, receipt_footer = EXCLUDED.receipt_footer, updated_at = CURRENT_TIMESTAMP",
                vendorId, request.businessName(), request.businessAddress(), request.tin(), request.vatRegistered(), request.vatRate(), request.nearExpirationDays(), request.receiptFooter());
        return get(vendorId, authentication);
    }

    private void requireAccess(Long vendorId, Authentication authentication, boolean write) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                || tenantAccessService.hasVendorRole(authentication, vendorId, "VENDOR_ADMIN")) return;
        if (!write && tenantAccessService.vendorsFor(authentication).stream().anyMatch(v -> v.id().equals(vendorId))) return;
        throw new AccessDeniedException("Business settings access is required");
    }

    public record BusinessSettings(@Size(max = 255) String businessName, @Size(max = 500) String businessAddress,
                                    @Size(max = 80) String tin, boolean vatRegistered, @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal vatRate,
                                    @NotNull @PositiveOrZero Integer nearExpirationDays, @Size(max = 500) String receiptFooter) { }
}
