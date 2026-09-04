package com.ddev.tindakart;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/vendors/{vendorId}/stores/{storeId}/reports")
public class ReportController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;
    private final PackageAccessService packageAccessService;

    public ReportController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService, PackageAccessService packageAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
        this.packageAccessService = packageAccessService;
    }

    @GetMapping("/sales")
    public SalesSummary sales(@PathVariable Long vendorId, @PathVariable Long storeId,
                              @RequestParam(required = false) LocalDate from,
                              @RequestParam(required = false) LocalDate to,
                              Authentication authentication) {
        requireAccess(vendorId, storeId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "REPORTS");
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start : to;
        if (end.isBefore(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report end date must not be before start date");
        return jdbcTemplate.query("SELECT COUNT(*) AS sale_count, COALESCE(SUM(subtotal), 0) AS subtotal, "
                        + "COALESCE(SUM(discount_amount), 0) AS discount, COALESCE(SUM(vat_amount), 0) AS vat, "
                        + "COALESCE(SUM(total_amount), 0) AS total FROM sales WHERE vendor_id = ? AND store_id = ? "
                        + "AND status = 'COMPLETED' AND created_at >= ?::date AND created_at < (?::date + INTERVAL '1 day')",
                (rs, rowNum) -> new SalesSummary(rs.getLong("sale_count"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"),
                        rs.getBigDecimal("vat"), rs.getBigDecimal("total"), start, end), vendorId, storeId, start, end).getFirst();
    }

    @GetMapping("/low-stock")
    public List<StockAlert> lowStock(@PathVariable Long vendorId, @PathVariable Long storeId, Authentication authentication) {
        requireAccess(vendorId, storeId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "REPORTS");
        return jdbcTemplate.query("SELECT p.id, p.name, p.sku, p.reorder_level, COALESCE(SUM(b.quantity_on_hand), 0) AS quantity "
                        + "FROM products p LEFT JOIN inventory_batches b ON b.product_id = p.id AND b.store_id = ? "
                        + "WHERE p.vendor_id = ? AND p.active = TRUE GROUP BY p.id, p.name, p.sku, p.reorder_level "
                        + "HAVING COALESCE(SUM(b.quantity_on_hand), 0) <= p.reorder_level ORDER BY quantity, p.name",
                (rs, rowNum) -> new StockAlert(rs.getLong("id"), rs.getString("name"), rs.getString("sku"),
                        rs.getBigDecimal("quantity"), rs.getInt("reorder_level")), storeId, vendorId);
    }

    @GetMapping("/expiration")
    public List<ExpirationAlert> expiration(@PathVariable Long vendorId, @PathVariable Long storeId, Authentication authentication) {
        requireAccess(vendorId, storeId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "REPORTS");
        return jdbcTemplate.query("SELECT b.id, p.name, p.sku, b.quantity_on_hand, b.expiration_date FROM inventory_batches b "
                        + "JOIN products p ON p.id = b.product_id WHERE b.vendor_id = ? AND b.store_id = ? "
                        + "AND b.quantity_on_hand > 0 AND b.expiration_date IS NOT NULL "
                        + "AND b.expiration_date <= CURRENT_DATE + INTERVAL '30 days' ORDER BY b.expiration_date, p.name",
                (rs, rowNum) -> new ExpirationAlert(rs.getLong("id"), rs.getString("name"), rs.getString("sku"),
                        rs.getBigDecimal("quantity_on_hand"), rs.getObject("expiration_date", LocalDate.class)), vendorId, storeId);
    }

    private void requireAccess(Long vendorId, Long storeId, Authentication authentication) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores WHERE id = ? AND vendor_id = ?", Integer.class, storeId, vendorId);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found");
        boolean allowed = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                || tenantAccessService.hasVendorRole(authentication, vendorId, "VENDOR_ADMIN")
                || tenantAccessService.hasStoreAccess(authentication, storeId);
        if (!allowed) throw new AccessDeniedException("Report access is required for this store");
    }

    public record SalesSummary(long saleCount, BigDecimal subtotal, BigDecimal discount, BigDecimal vat, BigDecimal total,
                               LocalDate from, LocalDate to) { }
    public record StockAlert(Long productId, String productName, String sku, BigDecimal quantity, int reorderLevel) { }
    public record ExpirationAlert(Long batchId, String productName, String sku, BigDecimal quantity, LocalDate expirationDate) { }
}
