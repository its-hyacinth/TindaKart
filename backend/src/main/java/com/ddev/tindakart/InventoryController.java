package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;
    private final PackageAccessService packageAccessService;
    private final ExpirationPolicy expirationPolicy;
    private final PermissionAccessService permissionAccessService;

    public InventoryController(JdbcTemplate jdbcTemplate, PackageAccessService packageAccessService,
                               ExpirationPolicy expirationPolicy,
                               PermissionAccessService permissionAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.packageAccessService = packageAccessService;
        this.expirationPolicy = expirationPolicy;
        this.permissionAccessService = permissionAccessService;
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
                (rs, rowNum) -> batch(rs, nearExpirationDays(vendorId)), vendorId, storeId).stream()
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

    @GetMapping("/movements")
    public List<InventoryMovementView> movements(@PathVariable Long vendorId, @PathVariable Long storeId,
                                                 @RequestParam(defaultValue = "100") int limit,
                                                 Authentication authentication) {
        requireStoreAccess(authentication, vendorId, storeId, false);
        packageAccessService.requireFeature(authentication, vendorId, "INVENTORY");
        if (limit < 1 || limit > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 500");
        return jdbcTemplate.query("SELECT m.id, m.product_id, p.name AS product_name, m.batch_id, m.movement_type, "
                        + "m.quantity_delta, m.reason, m.reference_type, m.reference_id, m.created_at, u.display_name AS created_by "
                        + "FROM inventory_movements m JOIN products p ON p.id = m.product_id LEFT JOIN users u ON u.id = m.created_by "
                        + "WHERE m.vendor_id = ? AND m.store_id = ? ORDER BY m.created_at DESC, m.id DESC LIMIT ?",
                (rs, rowNum) -> new InventoryMovementView(rs.getLong("id"), rs.getLong("product_id"),
                        rs.getString("product_name"), rs.getObject("batch_id", Long.class), rs.getString("movement_type"),
                        rs.getBigDecimal("quantity_delta"), rs.getString("reason"), rs.getString("reference_type"),
                        rs.getString("reference_id"), rs.getObject("created_at", java.time.OffsetDateTime.class),
                        rs.getString("created_by")), vendorId, storeId, limit);
    }

    private InventoryBatchView findBatch(Long vendorId, Long storeId, Long batchId) {
        return jdbcTemplate.query("SELECT b.id, b.product_id, p.name AS product_name, p.sku, c.name AS category_name, p.unit_type, "
                        + "(SELECT string_agg(barcode, ',') FROM product_barcodes WHERE product_id = p.id) AS barcodes, b.batch_reference, "
                        + "b.quantity_on_hand, b.cost_price, COALESCE(b.retail_price, p.retail_price) AS retail_price, "
                        + "b.bulk_price, b.expiration_date, p.reorder_level FROM inventory_batches b JOIN products p ON p.id = b.product_id LEFT JOIN categories c ON c.id = p.category_id "
                        + "WHERE b.id = ? AND b.vendor_id = ? AND b.store_id = ?", (rs, rowNum) -> batch(rs, nearExpirationDays(vendorId)), batchId, vendorId, storeId)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory batch not found"));
    }

    private int nearExpirationDays(Long vendorId) {
        Integer days = jdbcTemplate.query("SELECT near_expiration_days FROM business_settings WHERE vendor_id = ?",
                (rs, rowNum) -> rs.getInt(1), vendorId).stream().findFirst().orElse(30);
        return Math.max(0, days);
    }

    private InventoryBatchView batch(java.sql.ResultSet rs, int nearExpirationDays) throws java.sql.SQLException {
        LocalDate expiration = rs.getObject("expiration_date", LocalDate.class);
        String status = expirationPolicy.status(expiration, LocalDate.now(), nearExpirationDays,
                rs.getBigDecimal("quantity_on_hand"), rs.getInt("reorder_level"));
        return new InventoryBatchView(rs.getLong("id"), rs.getLong("product_id"), rs.getString("product_name"),
                rs.getString("sku"), rs.getString("category_name"), rs.getString("unit_type"), rs.getString("barcodes"),
                rs.getString("batch_reference"), rs.getBigDecimal("quantity_on_hand"),
                rs.getBigDecimal("cost_price"), rs.getBigDecimal("retail_price"), rs.getBigDecimal("bulk_price"), expiration, status);
    }

    private void requireStoreAccess(Authentication authentication, Long vendorId, Long storeId, boolean write) {
        Integer belongs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores WHERE id = ? AND vendor_id = ?",
                Integer.class, storeId, vendorId);
        if (belongs == null || belongs == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found");
        permissionAccessService.require(authentication, vendorId, storeId, write ? "INVENTORY_MANAGE" : "INVENTORY_VIEW");
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
    public record InventoryMovementView(Long id, Long productId, String productName, Long batchId, String movementType,
                                       BigDecimal quantityDelta, String reason, String referenceType, String referenceId,
                                       java.time.OffsetDateTime createdAt, String createdBy) { }
    public record ReceiveRequest(@NotNull Long productId, @NotNull @DecimalMin("0.001") BigDecimal quantity,
                                 @NotNull @DecimalMin("0.00") BigDecimal costPrice, @DecimalMin("0.00") BigDecimal retailPrice,
                                 @DecimalMin("0.00") BigDecimal bulkPrice, @Size(max = 120) String batchReference,
                                 LocalDate expirationDate, @Size(max = 500) String reason) { }
    public record AdjustmentRequest(@NotNull Long batchId, @NotNull BigDecimal quantityDelta,
                                    @NotBlank @Size(max = 500) String reason) { }
}
