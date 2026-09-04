package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vendors/{vendorId}/stores/{storeId}/inventory")
public class InventoryController {
    private static final long NEAR_EXPIRATION_DAYS = 30;
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;
    private final PackageAccessService packageAccessService;

    public InventoryController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService, PackageAccessService packageAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
        this.packageAccessService = packageAccessService;
    }

    @GetMapping
    public List<InventoryBatchView> list(@PathVariable Long vendorId, @PathVariable Long storeId,
                                        @RequestParam(required = false) String status,
                                        Authentication authentication) {
        requireStoreAccess(authentication, vendorId, storeId, false);
        packageAccessService.requireFeature(authentication, vendorId, "INVENTORY");
        return jdbcTemplate.query("SELECT b.id, b.product_id, p.name AS product_name, p.sku, c.name AS category_name, p.unit_type, "
                        + "(SELECT string_agg(barcode, ',') FROM product_barcodes WHERE product_id = p.id) AS barcodes, b.batch_reference, "
                        + "b.quantity_on_hand, b.cost_price, COALESCE(b.retail_price, p.retail_price) AS retail_price, "
                        + "b.bulk_price, b.expiration_date, p.reorder_level FROM inventory_batches b "
                        + "JOIN products p ON p.id = b.product_id LEFT JOIN categories c ON c.id = p.category_id WHERE b.vendor_id = ? AND b.store_id = ? "
                        + "AND b.quantity_on_hand > 0 ORDER BY p.name, b.expiration_date NULLS LAST, b.id",
                (rs, rowNum) -> batch(rs), vendorId, storeId).stream()
                .filter(item -> status == null || status.equals(item.status())).toList();
    }

    @PostMapping("/receive")
    @Transactional
    public ResponseEntity<InventoryBatchView> receive(@PathVariable Long vendorId, @PathVariable Long storeId,
                                                       @Valid @RequestBody ReceiveRequest request,
                                                       Authentication authentication) {
        requireStoreAccess(authentication, vendorId, storeId, true);
        packageAccessService.requireFeature(authentication, vendorId, "INVENTORY");
        validateProductBelongs(vendorId, request.productId());
        Long userId = userId(authentication);
        Long batchId = jdbcTemplate.queryForObject("INSERT INTO inventory_batches (vendor_id, store_id, product_id, "
                + "batch_reference, quantity_received, quantity_on_hand, cost_price, retail_price, bulk_price, expiration_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id", Long.class, vendorId, storeId, request.productId(),
                blankToNull(request.batchReference()), request.quantity(), request.quantity(), request.costPrice(),
                request.retailPrice(), request.bulkPrice(), request.expirationDate());
        jdbcTemplate.update("INSERT INTO inventory_movements (vendor_id, store_id, product_id, batch_id, movement_type, "
                + "quantity_delta, reason, reference_type, created_by) VALUES (?, ?, ?, ?, 'RECEIVE', ?, ?, 'RECEIVING', ?)",
                vendorId, storeId, request.productId(), batchId, request.quantity(), blankToNull(request.reason()), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(findBatch(vendorId, storeId, batchId));
    }

    @PostMapping("/adjust")
    @Transactional
    public InventoryBatchView adjust(@PathVariable Long vendorId, @PathVariable Long storeId,
                                     @Valid @RequestBody AdjustmentRequest request,
                                     Authentication authentication) {
        requireStoreAccess(authentication, vendorId, storeId, true);
        packageAccessService.requireFeature(authentication, vendorId, "INVENTORY");
        if (request.quantityDelta().signum() == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjustment cannot be zero");
        List<BigDecimal> current = jdbcTemplate.query("SELECT quantity_on_hand FROM inventory_batches WHERE id = ? "
                        + "AND vendor_id = ? AND store_id = ? FOR UPDATE", (rs, rowNum) -> rs.getBigDecimal(1),
                request.batchId(), vendorId, storeId);
        if (current.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory batch not found");
        BigDecimal updated = current.getFirst().add(request.quantityDelta());
        if (updated.signum() < 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Adjustment would create negative stock");
        jdbcTemplate.update("UPDATE inventory_batches SET quantity_on_hand = ? WHERE id = ?", updated, request.batchId());
        jdbcTemplate.update("INSERT INTO inventory_movements (vendor_id, store_id, product_id, batch_id, movement_type, "
                + "quantity_delta, reason, reference_type, created_by) SELECT ?, ?, product_id, ?, 'ADJUSTMENT', ?, ?, 'ADJUSTMENT', ? "
                + "FROM inventory_batches WHERE id = ?", vendorId, storeId, request.batchId(), request.quantityDelta(), request.reason(),
                userId(authentication), request.batchId());
        return findBatch(vendorId, storeId, request.batchId());
    }

    private InventoryBatchView findBatch(Long vendorId, Long storeId, Long batchId) {
        return jdbcTemplate.query("SELECT b.id, b.product_id, p.name AS product_name, p.sku, c.name AS category_name, p.unit_type, "
                        + "(SELECT string_agg(barcode, ',') FROM product_barcodes WHERE product_id = p.id) AS barcodes, b.batch_reference, "
                        + "b.quantity_on_hand, b.cost_price, COALESCE(b.retail_price, p.retail_price) AS retail_price, "
                        + "b.bulk_price, b.expiration_date, p.reorder_level FROM inventory_batches b JOIN products p ON p.id = b.product_id LEFT JOIN categories c ON c.id = p.category_id "
                        + "WHERE b.id = ? AND b.vendor_id = ? AND b.store_id = ?", (rs, rowNum) -> batch(rs), batchId, vendorId, storeId)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory batch not found"));
    }

    private InventoryBatchView batch(java.sql.ResultSet rs) throws java.sql.SQLException {
        LocalDate expiration = rs.getObject("expiration_date", LocalDate.class);
        String status = expiration != null && expiration.isBefore(LocalDate.now()) ? "EXPIRED"
                : expiration != null && ChronoUnit.DAYS.between(LocalDate.now(), expiration) <= NEAR_EXPIRATION_DAYS ? "NEAR_EXPIRATION"
                : rs.getBigDecimal("quantity_on_hand").compareTo(BigDecimal.valueOf(rs.getInt("reorder_level"))) <= 0 ? "LOW_STOCK" : "IN_STOCK";
        return new InventoryBatchView(rs.getLong("id"), rs.getLong("product_id"), rs.getString("product_name"),
                rs.getString("sku"), rs.getString("category_name"), rs.getString("unit_type"), rs.getString("barcodes"),
                rs.getString("batch_reference"), rs.getBigDecimal("quantity_on_hand"),
                rs.getBigDecimal("cost_price"), rs.getBigDecimal("retail_price"), rs.getBigDecimal("bulk_price"), expiration, status);
    }

    private void requireStoreAccess(Authentication authentication, Long vendorId, Long storeId, boolean write) {
        Integer belongs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores WHERE id = ? AND vendor_id = ?",
                Integer.class, storeId, vendorId);
        if (belongs == null || belongs == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found");
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) return;
        boolean allowed = write ? tenantAccessService.hasVendorRole(authentication, vendorId, "VENDOR_ADMIN")
                || tenantAccessService.hasStoreRole(authentication, storeId, "INVENTORY_STAFF")
                : tenantAccessService.hasStoreAccess(authentication, storeId);
        if (!allowed) throw new AccessDeniedException("You do not have inventory access to this store");
    }

    private void validateProductBelongs(Long vendorId, Long productId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE id = ? AND vendor_id = ? AND active = TRUE",
                Integer.class, productId, vendorId);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product does not belong to this vendor");
    }

    private Long userId(Authentication authentication) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE LOWER(username) = LOWER(?)", Long.class, authentication.getName());
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record InventoryBatchView(Long id, Long productId, String productName, String sku, String categoryName, String unitType,
                                     String barcodes, String batchReference,
                                     BigDecimal quantityOnHand, BigDecimal costPrice, BigDecimal retailPrice,
                                     BigDecimal bulkPrice, LocalDate expirationDate, String status) { }
    public record ReceiveRequest(@NotNull Long productId, @NotNull @DecimalMin("0.001") BigDecimal quantity,
                                 @NotNull @DecimalMin("0.00") BigDecimal costPrice, @DecimalMin("0.00") BigDecimal retailPrice,
                                 @DecimalMin("0.00") BigDecimal bulkPrice, @Size(max = 120) String batchReference,
                                 LocalDate expirationDate, @Size(max = 500) String reason) { }
    public record AdjustmentRequest(@NotNull Long batchId, @NotNull BigDecimal quantityDelta,
                                    @NotBlank @Size(max = 500) String reason) { }
}
