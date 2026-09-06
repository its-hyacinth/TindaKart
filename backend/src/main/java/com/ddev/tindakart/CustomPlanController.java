package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class CustomPlanController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;

    public CustomPlanController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/super-admin/custom-pricing")
    public List<AddonPrice> prices(Authentication authentication) {
        requireSuperAdmin(authentication);
        return listPrices();
    }

    @PutMapping("/super-admin/custom-pricing/{featureKey}")
    public AddonPrice updatePrice(@PathVariable String featureKey, @Valid @RequestBody PriceRequest request,
                                  Authentication authentication) {
        requireSuperAdmin(authentication);
        int updated = jdbcTemplate.update("UPDATE platform_addon_prices SET display_name = ?, description = ?, monthly_price = ?, active = TRUE, updated_at = CURRENT_TIMESTAMP WHERE feature_key = ?",
                request.displayName().trim(), request.description(), request.monthlyPrice(), featureKey.trim().toUpperCase());
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom pricing item not found");
        return listPrices().stream().filter(item -> item.featureKey().equals(featureKey.trim().toUpperCase())).findFirst().orElseThrow();
    }

    @GetMapping("/vendors/{vendorId}/custom-plan")
    public CustomPlan plan(@PathVariable Long vendorId, Authentication authentication) {
        requireVendorAccess(authentication, vendorId);
        Integer seats = jdbcTemplate.query("SELECT seat_count FROM vendor_staff_seats WHERE vendor_id = ?",
                (rs, rowNum) -> rs.getInt("seat_count"), vendorId).stream().findFirst().orElse(0);
        List<String> features = jdbcTemplate.query("SELECT feature_key FROM vendor_feature_addons WHERE vendor_id = ? AND active = TRUE ORDER BY feature_key",
                (rs, rowNum) -> rs.getString("feature_key"), vendorId);
        return new CustomPlan(seats, features, listPrices());
    }

    private List<AddonPrice> listPrices() {
        return jdbcTemplate.query("SELECT feature_key, display_name, description, monthly_price, active FROM platform_addon_prices WHERE active = TRUE ORDER BY display_name",
                (rs, rowNum) -> new AddonPrice(rs.getString("feature_key"), rs.getString("display_name"), rs.getString("description"), rs.getBigDecimal("monthly_price"), rs.getBoolean("active")));
    }

    private void requireSuperAdmin(Authentication authentication) {
        if (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            throw new AccessDeniedException("Super Admin permission is required");
        }
    }

    private void requireVendorAccess(Authentication authentication, Long vendorId) {
        if (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                && tenantAccessService.vendorsFor(authentication).stream().noneMatch(v -> v.id().equals(vendorId))) {
            throw new AccessDeniedException("Vendor access is required");
        }
    }

    public record PriceRequest(@NotBlank String displayName, String description,
                               @NotNull @DecimalMin("0.00") BigDecimal monthlyPrice) { }
    public record AddonPrice(String featureKey, String displayName, String description, BigDecimal monthlyPrice, boolean active) { }
    public record CustomPlan(int staffSeats, List<String> features, List<AddonPrice> prices) { }
}
