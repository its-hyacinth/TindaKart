package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
    private static final List<String> DEFAULT_PAYMENT_METHODS = List.of("CASH", "CARD", "EWALLET", "CREDIT");
    private static final Set<String> PAYMENT_METHODS = Set.copyOf(DEFAULT_PAYMENT_METHODS);
    private static final Set<String> PRINT_MODES = Set.of("BROWSER", "THERMAL_BRIDGE", "MANUAL");
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;

    public SettingsController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public BusinessSettings get(@PathVariable Long vendorId, Authentication authentication) {
        requireAccess(vendorId, authentication, false);
        return jdbcTemplate.query("SELECT business_name, business_address, tin, vat_registered, vat_rate, near_expiration_days, near_expiration_discount_percent, receipt_footer, pos_payment_methods, camera_scanning_enabled, receipt_print_mode FROM business_settings WHERE vendor_id = ?",
                (rs, rowNum) -> new BusinessSettings(rs.getString("business_name"), rs.getString("business_address"), rs.getString("tin"),
                        rs.getBoolean("vat_registered"), rs.getBigDecimal("vat_rate"), rs.getInt("near_expiration_days"),
                        rs.getBigDecimal("near_expiration_discount_percent"), rs.getString("receipt_footer"),
                        paymentMethods(rs), rs.getBoolean("camera_scanning_enabled"), rs.getString("receipt_print_mode")), vendorId)
                .stream().findFirst().orElse(defaultSettings());
    }

    @PutMapping
    @Transactional
    public BusinessSettings update(@PathVariable Long vendorId, @Valid @RequestBody BusinessSettings request,
                                   Authentication authentication) {
        requireAccess(vendorId, authentication, true);
        if (request.vatRegistered() && request.vatRate().signum() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VAT rate is required when VAT is enabled");
        }
        if (request.posPaymentMethods().stream().anyMatch(method -> !PAYMENT_METHODS.contains(method))
                || request.posPaymentMethods().size() != request.posPaymentMethods().stream().distinct().count()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "POS payment methods contain an unsupported or duplicate value");
        }
        if (!PRINT_MODES.contains(request.receiptPrintMode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported receipt print mode");
        }
        String paymentMethods = "{" + String.join(",", request.posPaymentMethods()) + "}";
        jdbcTemplate.update("INSERT INTO business_settings (vendor_id, business_name, business_address, tin, vat_registered, vat_rate, near_expiration_days, near_expiration_discount_percent, receipt_footer, pos_payment_methods, camera_scanning_enabled, receipt_print_mode) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::varchar[], ?, ?) ON CONFLICT (vendor_id) DO UPDATE SET business_name = EXCLUDED.business_name, business_address = EXCLUDED.business_address, "
                + "tin = EXCLUDED.tin, vat_registered = EXCLUDED.vat_registered, vat_rate = EXCLUDED.vat_rate, near_expiration_days = EXCLUDED.near_expiration_days, "
                + "near_expiration_discount_percent = EXCLUDED.near_expiration_discount_percent, receipt_footer = EXCLUDED.receipt_footer, pos_payment_methods = EXCLUDED.pos_payment_methods, "
                + "camera_scanning_enabled = EXCLUDED.camera_scanning_enabled, receipt_print_mode = EXCLUDED.receipt_print_mode, updated_at = CURRENT_TIMESTAMP",
                vendorId, request.businessName(), request.businessAddress(), request.tin(), request.vatRegistered(), request.vatRate(), request.nearExpirationDays(), request.nearExpirationDiscountPercent(), request.receiptFooter(), paymentMethods, request.cameraScanningEnabled(), request.receiptPrintMode());
        return get(vendorId, authentication);
    }

    private List<String> paymentMethods(java.sql.ResultSet rs) throws SQLException {
        java.sql.Array array = rs.getArray("pos_payment_methods");
        if (array == null || array.getArray() == null) return DEFAULT_PAYMENT_METHODS;
        return Arrays.stream((Object[]) array.getArray()).map(String::valueOf).toList();
    }

    private BusinessSettings defaultSettings() {
        return new BusinessSettings(null, null, null, false, BigDecimal.ZERO, 30, BigDecimal.ZERO, null,
                DEFAULT_PAYMENT_METHODS, true, "BROWSER");
    }

    private void requireAccess(Long vendorId, Authentication authentication, boolean write) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                || tenantAccessService.hasStoreRole(authentication, vendorId, "STORE_ADMIN")) return;
        if (!write && tenantAccessService.hasStoreAccess(authentication, vendorId)) return;
        throw new AccessDeniedException("Business settings access is required");
    }

    public record BusinessSettings(@Size(max = 255) String businessName, @Size(max = 500) String businessAddress,
                                    @Size(max = 80) String tin, boolean vatRegistered, @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal vatRate,
                                    @NotNull @PositiveOrZero Integer nearExpirationDays,
                                    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal nearExpirationDiscountPercent,
                                    @Size(max = 500) String receiptFooter,
                                    @NotEmpty List<String> posPaymentMethods, boolean cameraScanningEnabled,
                                    @NotNull String receiptPrintMode) {
        public BusinessSettings {
            posPaymentMethods = posPaymentMethods == null ? DEFAULT_PAYMENT_METHODS : posPaymentMethods;
            receiptPrintMode = receiptPrintMode == null ? "BROWSER" : receiptPrintMode;
        }
    }
}
