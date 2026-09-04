package com.ddev.tindakart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vendors/{vendorId}/stores/{storeId}/sales/{saleId}/receipt")
public class ReceiptController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;

    public ReceiptController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ReceiptView get(@PathVariable Long vendorId, @PathVariable Long storeId, @PathVariable Long saleId,
                           Authentication authentication) {
        requireStoreAccess(vendorId, storeId, authentication);
        return find(vendorId, storeId, saleId);
    }

    @PostMapping("/printed")
    public ReceiptView markPrinted(@PathVariable Long vendorId, @PathVariable Long storeId, @PathVariable Long saleId,
                                   Authentication authentication) {
        requireStoreAccess(vendorId, storeId, authentication);
        int updated = jdbcTemplate.update("UPDATE receipts SET printed_at = CURRENT_TIMESTAMP WHERE sale_id = ? "
                + "AND EXISTS (SELECT 1 FROM sales WHERE id = ? AND vendor_id = ? AND store_id = ?)", saleId, saleId, vendorId, storeId);
        if (updated == 0) throw notFound("Receipt not found");
        return find(vendorId, storeId, saleId);
    }

    private ReceiptView find(Long vendorId, Long storeId, Long saleId) {
        return jdbcTemplate.query("SELECT s.id, s.receipt_number, s.created_at, s.subtotal, s.discount_amount, s.vat_amount, "
                        + "s.total_amount, p.payment_method, p.amount_tendered, p.change_amount, r.printed_at FROM sales s "
                        + "JOIN receipts r ON r.sale_id = s.id JOIN payments p ON p.sale_id = s.id WHERE s.id = ? "
                        + "AND s.vendor_id = ? AND s.store_id = ?", (rs, rowNum) -> new ReceiptView(rs.getLong("id"),
                        rs.getString("receipt_number"), rs.getTimestamp("created_at").toInstant(), rs.getBigDecimal("subtotal"),
                        rs.getBigDecimal("discount_amount"), rs.getBigDecimal("vat_amount"), rs.getBigDecimal("total_amount"),
                        rs.getString("payment_method"), rs.getBigDecimal("amount_tendered"), rs.getBigDecimal("change_amount"),
                        rs.getTimestamp("printed_at") == null ? null : rs.getTimestamp("printed_at").toInstant(), items(saleId)), saleId, vendorId, storeId)
                .stream().findFirst().orElseThrow(() -> notFound("Receipt not found"));
    }

    private List<ReceiptItem> items(Long saleId) {
        return jdbcTemplate.query("SELECT si.product_id, p.name, si.quantity, si.unit_price, si.subtotal FROM sale_items si "
                        + "JOIN products p ON p.id = si.product_id WHERE si.sale_id = ? ORDER BY si.id",
                (rs, rowNum) -> new ReceiptItem(rs.getLong("product_id"), rs.getString("name"), rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("unit_price"), rs.getBigDecimal("subtotal")), saleId);
    }

    private void requireStoreAccess(Long vendorId, Long storeId, Authentication authentication) {
        Integer belongs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores WHERE id = ? AND vendor_id = ?", Integer.class, storeId, vendorId);
        if (belongs == null || belongs == 0) throw notFound("Store not found");
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) return;
        if (!tenantAccessService.hasStoreAccess(authentication, storeId)) throw new AccessDeniedException("Store access is required");
    }

    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    public record ReceiptView(Long saleId, String receiptNumber, Instant createdAt, BigDecimal subtotal, BigDecimal discountAmount,
                              BigDecimal vatAmount, BigDecimal totalAmount, String paymentMethod, BigDecimal amountTendered,
                              BigDecimal changeAmount, Instant printedAt, List<ReceiptItem> items) { }
    public record ReceiptItem(Long productId, String productName, BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal) { }
}
