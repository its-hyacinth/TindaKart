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
    private final PackageAccessService packageAccessService;
    private final PermissionAccessService permissionAccessService;

    public ReportController(JdbcTemplate jdbcTemplate,
                            PackageAccessService packageAccessService, PermissionAccessService permissionAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.packageAccessService = packageAccessService;
        this.permissionAccessService = permissionAccessService;
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
                        + "AND b.expiration_date <= CURRENT_DATE + (? * INTERVAL '1 day') ORDER BY b.expiration_date, p.name",
                (rs, rowNum) -> new ExpirationAlert(rs.getLong("id"), rs.getString("name"), rs.getString("sku"),
                        rs.getBigDecimal("quantity_on_hand"), rs.getObject("expiration_date", LocalDate.class)), vendorId, storeId,
                nearExpirationDays(vendorId));
    }

    @GetMapping("/best-selling")
    public List<BestSeller> bestSelling(@PathVariable Long vendorId, @PathVariable Long storeId,
                                        @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                        Authentication authentication) {
        requireAccess(vendorId, storeId, authentication); packageAccessService.requireFeature(authentication, vendorId, "REPORTS");
        LocalDate start = from == null ? LocalDate.now() : from, end = to == null ? start : to;
        return jdbcTemplate.query("SELECT p.id, p.name, SUM(si.quantity) AS quantity, SUM(si.subtotal) AS revenue FROM sale_items si JOIN sales s ON s.id = si.sale_id JOIN products p ON p.id = si.product_id WHERE s.vendor_id = ? AND s.store_id = ? AND s.status = 'COMPLETED' AND s.created_at >= ?::date AND s.created_at < (?::date + INTERVAL '1 day') GROUP BY p.id, p.name ORDER BY quantity DESC, p.name LIMIT 10",
                (rs, rowNum) -> new BestSeller(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("quantity"), rs.getBigDecimal("revenue")), vendorId, storeId, start, end);
    }

    @GetMapping("/payments")
    public List<PaymentSummary> payments(@PathVariable Long vendorId, @PathVariable Long storeId,
                                         @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                         Authentication authentication) {
        requireAccess(vendorId, storeId, authentication); packageAccessService.requireFeature(authentication, vendorId, "REPORTS");
        LocalDate start = from == null ? LocalDate.now() : from, end = to == null ? start : to;
        return jdbcTemplate.query("SELECT p.payment_method, COUNT(*) AS payment_count, SUM(p.amount) AS amount FROM payments p JOIN sales s ON s.id = p.sale_id WHERE s.vendor_id = ? AND s.store_id = ? AND s.status = 'COMPLETED' AND p.created_at >= ?::date AND p.created_at < (?::date + INTERVAL '1 day') GROUP BY p.payment_method ORDER BY p.payment_method",
                (rs, rowNum) -> new PaymentSummary(rs.getString("payment_method"), rs.getLong("payment_count"), rs.getBigDecimal("amount")), vendorId, storeId, start, end);
    }

    @GetMapping("/profit")
    public ProfitSummary profit(@PathVariable Long vendorId, @PathVariable Long storeId,
                                @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                Authentication authentication) {
        requireAccess(vendorId, storeId, authentication); packageAccessService.requireFeature(authentication, vendorId, "REPORTS");
        LocalDate start = from == null ? LocalDate.now() : from, end = to == null ? start : to;
        return jdbcTemplate.query("SELECT COALESCE(SUM(si.subtotal), 0) AS revenue, COALESCE(SUM(si.quantity * b.cost_price), 0) AS cost FROM sale_items si JOIN sales s ON s.id = si.sale_id JOIN inventory_batches b ON b.id = si.batch_id WHERE s.vendor_id = ? AND s.store_id = ? AND s.status = 'COMPLETED' AND s.created_at >= ?::date AND s.created_at < (?::date + INTERVAL '1 day')",
                (rs, rowNum) -> new ProfitSummary(rs.getBigDecimal("revenue"), rs.getBigDecimal("cost")), vendorId, storeId, start, end).getFirst();
    }

    @GetMapping("/delivery-history")
    public List<DeliverySummary> deliveryHistory(@PathVariable Long vendorId, @PathVariable Long storeId,
                                                 Authentication authentication) {
        requireAccess(vendorId, storeId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "REPORTS");
        return jdbcTemplate.query("SELECT d.id, s.name AS supplier_name, d.expected_date, d.status, "
                        + "COUNT(di.id) AS item_count, COALESCE(SUM(di.quantity_ordered), 0) AS quantity_ordered, "
                        + "COALESCE(SUM(di.quantity_received), 0) AS quantity_received "
                        + "FROM deliveries d JOIN suppliers s ON s.id = d.supplier_id LEFT JOIN delivery_items di ON di.delivery_id = d.id "
                        + "WHERE d.vendor_id = ? AND d.store_id = ? GROUP BY d.id, s.name, d.expected_date, d.status "
                        + "ORDER BY d.expected_date DESC, d.id DESC LIMIT 100",
                (rs, rowNum) -> new DeliverySummary(rs.getLong("id"), rs.getString("supplier_name"),
                        rs.getObject("expected_date", LocalDate.class), rs.getString("status"), rs.getLong("item_count"),
                        rs.getBigDecimal("quantity_ordered"), rs.getBigDecimal("quantity_received")), vendorId, storeId);
    }

    @GetMapping("/vat-summary")
    public VatSummary vatSummary(@PathVariable Long vendorId, @PathVariable Long storeId,
                                 @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                 Authentication authentication) {
        requireAccess(vendorId, storeId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "REPORTS");
        LocalDate start = from == null ? LocalDate.now() : from, end = to == null ? start : to;
        BusinessTaxSettings settings = jdbcTemplate.query("SELECT vat_registered, vat_rate FROM business_settings WHERE vendor_id = ?",
                (rs, rowNum) -> new BusinessTaxSettings(rs.getBoolean("vat_registered"), rs.getBigDecimal("vat_rate")), vendorId)
                .stream().findFirst().orElse(new BusinessTaxSettings(false, BigDecimal.ZERO));
        return jdbcTemplate.query("SELECT COALESCE(SUM(subtotal), 0) AS net_sales, COALESCE(SUM(vat_amount), 0) AS vat_amount, "
                        + "COALESCE(SUM(total_amount), 0) AS gross_sales FROM sales WHERE vendor_id = ? AND store_id = ? "
                        + "AND status = 'COMPLETED' AND created_at >= ?::date AND created_at < (?::date + INTERVAL '1 day')",
                (rs, rowNum) -> new VatSummary(settings.vatRegistered(), settings.vatRate(), rs.getBigDecimal("net_sales"),
                        rs.getBigDecimal("vat_amount"), rs.getBigDecimal("gross_sales"), start, end), vendorId, storeId, start, end).getFirst();
    }

    private void requireAccess(Long vendorId, Long storeId, Authentication authentication) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores WHERE id = ? AND vendor_id = ?", Integer.class, storeId, vendorId);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found");
        permissionAccessService.require(authentication, vendorId, storeId, "REPORTS_VIEW");
    }

    private int nearExpirationDays(Long vendorId) {
        Integer days = jdbcTemplate.query("SELECT near_expiration_days FROM business_settings WHERE vendor_id = ?",
                (rs, rowNum) -> rs.getInt(1), vendorId).stream().findFirst().orElse(30);
        return Math.max(0, days);
    }

    public record SalesSummary(long saleCount, BigDecimal subtotal, BigDecimal discount, BigDecimal vat, BigDecimal total,
                               LocalDate from, LocalDate to) { }
    public record StockAlert(Long productId, String productName, String sku, BigDecimal quantity, int reorderLevel) { }
    public record ExpirationAlert(Long batchId, String productName, String sku, BigDecimal quantity, LocalDate expirationDate) { }
    public record BestSeller(Long productId, String productName, BigDecimal quantity, BigDecimal revenue) { }
    public record PaymentSummary(String paymentMethod, long paymentCount, BigDecimal amount) { }
    public record ProfitSummary(BigDecimal revenue, BigDecimal cost) { }
    public record DeliverySummary(Long id, String supplierName, LocalDate expectedDate, String status, long itemCount,
                                  BigDecimal quantityOrdered, BigDecimal quantityReceived) { }
    private record BusinessTaxSettings(boolean vatRegistered, BigDecimal vatRate) { }
    public record VatSummary(boolean vatRegistered, BigDecimal vatRate, BigDecimal netSales, BigDecimal vatAmount,
                             BigDecimal grossSales, LocalDate from, LocalDate to) { }
}
