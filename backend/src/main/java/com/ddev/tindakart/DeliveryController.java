package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vendors/{vendorId}/stores/{storeId}/deliveries")
public class DeliveryController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;
    private final PackageAccessService packageAccessService;

    public DeliveryController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService, PackageAccessService packageAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
        this.packageAccessService = packageAccessService;
    }


    @GetMapping
    public List<DeliveryView> list(@PathVariable Long vendorId, @PathVariable Long storeId, Authentication authentication) {
        requireStoreAccess(vendorId, storeId, authentication, false);
        packageAccessService.requireFeature(authentication, vendorId, "DELIVERY");
        return jdbcTemplate.query("SELECT d.id, d.supplier_id, s.name AS supplier_name, d.expected_date, d.status, d.notes "
                        + "FROM deliveries d JOIN suppliers s ON s.id = d.supplier_id WHERE d.vendor_id = ? AND d.store_id = ? "
                        + "ORDER BY d.expected_date, d.id", (rs, rowNum) -> delivery(rs, items(rs.getLong("id"))), vendorId, storeId);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<DeliveryView> create(@PathVariable Long vendorId, @PathVariable Long storeId,
                                                @Valid @RequestBody CreateDeliveryRequest request,
                                                Authentication authentication) {
        requireStoreAccess(vendorId, storeId, authentication, true);
        packageAccessService.requireFeature(authentication, vendorId, "DELIVERY");
        validateSupplier(vendorId, request.supplierId());
        for (DeliveryItemRequest item : request.items()) validateProduct(vendorId, item.productId());
        Long id;
        try {
            id = jdbcTemplate.queryForObject("INSERT INTO deliveries (vendor_id, store_id, supplier_id, expected_date, notes) "
                    + "VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, vendorId, storeId, request.supplierId(), request.expectedDate(), blankToNull(request.notes()));
            for (DeliveryItemRequest item : request.items()) jdbcTemplate.update("INSERT INTO delivery_items (delivery_id, product_id, quantity_ordered, cost_price, retail_price, bulk_price, expiration_date, batch_reference) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", id, item.productId(), item.quantity(), item.costPrice(), item.retailPrice(), item.bulkPrice(), item.expirationDate(), blankToNull(item.batchReference()));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid supplier or delivery data");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(find(vendorId, storeId, id));
    }

    @PatchMapping("/{deliveryId}/status")
    public DeliveryView status(@PathVariable Long vendorId, @PathVariable Long storeId, @PathVariable Long deliveryId,
                               @Valid @RequestBody StatusRequest request, Authentication authentication) {
        requireStoreAccess(vendorId, storeId, authentication, true);
        packageAccessService.requireFeature(authentication, vendorId, "DELIVERY");
        if (!List.of("UPCOMING", "IN_TRANSIT", "COMPLETED").contains(request.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use receive endpoint for RECEIVED status");
        }
        String current = jdbcTemplate.query("SELECT status FROM deliveries WHERE id = ? AND vendor_id = ? AND store_id = ?",
                (rs, rowNum) -> rs.getString("status"), deliveryId, vendorId, storeId).stream().findFirst()
                .orElseThrow(() -> notFound("Delivery not found"));
        if (!validTransition(current, request.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid delivery transition from " + current + " to " + request.status());
        }
        jdbcTemplate.update("UPDATE deliveries SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND vendor_id = ? AND store_id = ?",
                request.status(), deliveryId, vendorId, storeId);
        return find(vendorId, storeId, deliveryId);
    }

    @PutMapping("/{deliveryId}/receive")
    @Transactional
    public DeliveryView receive(@PathVariable Long vendorId, @PathVariable Long storeId, @PathVariable Long deliveryId,
                                Authentication authentication) {
        requireStoreAccess(vendorId, storeId, authentication, true);
        packageAccessService.requireFeature(authentication, vendorId, "DELIVERY");
        String current = jdbcTemplate.query("SELECT status FROM deliveries WHERE id = ? AND vendor_id = ? AND store_id = ? FOR UPDATE",
                (rs, rowNum) -> rs.getString("status"), deliveryId, vendorId, storeId).stream().findFirst()
                .orElseThrow(() -> notFound("Delivery not found"));
        if ("RECEIVED".equals(current) || "COMPLETED".equals(current)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Delivery was already received");
        List<DeliveryItem> items = jdbcTemplate.query("SELECT id, product_id, quantity_ordered, cost_price, retail_price, bulk_price, expiration_date, batch_reference FROM delivery_items WHERE delivery_id = ? FOR UPDATE",
                (rs, rowNum) -> new DeliveryItem(rs.getLong("id"), rs.getLong("product_id"), rs.getBigDecimal("quantity_ordered"), rs.getBigDecimal("cost_price"), rs.getBigDecimal("retail_price"), rs.getBigDecimal("bulk_price"), rs.getObject("expiration_date", LocalDate.class), rs.getString("batch_reference")), deliveryId);
        Long userId = userId(authentication);
        for (DeliveryItem item : items) {
            Long batchId = jdbcTemplate.queryForObject("INSERT INTO inventory_batches (vendor_id, store_id, product_id, batch_reference, quantity_received, quantity_on_hand, cost_price, retail_price, bulk_price, expiration_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                    Long.class, vendorId, storeId, item.productId(), item.batchReference(), item.quantity(), item.quantity(), item.costPrice(), item.retailPrice(), item.bulkPrice(), item.expirationDate());
            jdbcTemplate.update("UPDATE delivery_items SET quantity_received = quantity_ordered WHERE id = ?", item.id());
            jdbcTemplate.update("INSERT INTO inventory_movements (vendor_id, store_id, product_id, batch_id, movement_type, quantity_delta, reason, reference_type, reference_id, created_by) VALUES (?, ?, ?, ?, 'RECEIVE', ?, 'Delivery received', 'DELIVERY', ?, ?)", vendorId, storeId, item.productId(), batchId, item.quantity(), deliveryId.toString(), userId);
        }
        jdbcTemplate.update("UPDATE deliveries SET status = 'RECEIVED', updated_at = CURRENT_TIMESTAMP WHERE id = ?", deliveryId);
        return find(vendorId, storeId, deliveryId);
    }

    private DeliveryView find(Long vendorId, Long storeId, Long deliveryId) {
        return jdbcTemplate.query("SELECT d.id, d.supplier_id, s.name AS supplier_name, d.expected_date, d.status, d.notes FROM deliveries d JOIN suppliers s ON s.id = d.supplier_id WHERE d.id = ? AND d.vendor_id = ? AND d.store_id = ?",
                (rs, rowNum) -> delivery(rs, items(rs.getLong("id"))), deliveryId, vendorId, storeId).stream().findFirst().orElseThrow(() -> notFound("Delivery not found"));
    }

    private List<DeliveryItemView> items(Long deliveryId) {
        return jdbcTemplate.query("SELECT di.id, di.product_id, p.name AS product_name, di.quantity_ordered, di.quantity_received, di.cost_price, di.expiration_date FROM delivery_items di JOIN products p ON p.id = di.product_id WHERE di.delivery_id = ? ORDER BY p.name",
                (rs, rowNum) -> new DeliveryItemView(rs.getLong("id"), rs.getLong("product_id"), rs.getString("product_name"), rs.getBigDecimal("quantity_ordered"), rs.getBigDecimal("quantity_received"), rs.getBigDecimal("cost_price"), rs.getObject("expiration_date", LocalDate.class)), deliveryId);
    }

    private DeliveryView delivery(java.sql.ResultSet rs, List<DeliveryItemView> items) throws java.sql.SQLException {
        return new DeliveryView(rs.getLong("id"), rs.getLong("supplier_id"), rs.getString("supplier_name"), rs.getObject("expected_date", LocalDate.class), rs.getString("status"), rs.getString("notes"), items);
    }

    private void requireStoreAccess(Long vendorId, Long storeId, Authentication authentication, boolean write) {
        Integer belongs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores WHERE id = ? AND vendor_id = ?", Integer.class, storeId, vendorId);
        if (belongs == null || belongs == 0) throw notFound("Store not found");
        boolean allowed = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                || tenantAccessService.hasVendorRole(authentication, vendorId, "VENDOR_ADMIN")
                || (!write && tenantAccessService.hasStoreAccess(authentication, storeId))
                || (write && tenantAccessService.hasStoreRole(authentication, storeId, "DELIVERY_STAFF"));
        if (!allowed) throw new AccessDeniedException("Delivery access is required for this store");
    }

    private void validateSupplier(Long vendorId, Long supplierId) { if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM suppliers WHERE id = ? AND vendor_id = ?", Integer.class, supplierId, vendorId) == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier does not belong to this vendor"); }
    private void validateProduct(Long vendorId, Long productId) { if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE id = ? AND vendor_id = ? AND active = TRUE", Integer.class, productId, vendorId) == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product does not belong to this vendor"); }
    private Long userId(Authentication authentication) { return jdbcTemplate.queryForObject("SELECT id FROM users WHERE LOWER(username) = LOWER(?)", Long.class, authentication.getName()); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private boolean validTransition(String current, String next) {
        return ("UPCOMING".equals(current) && "IN_TRANSIT".equals(next))
                || ("RECEIVED".equals(current) && "COMPLETED".equals(next));
    }

    private record DeliveryItem(Long id, Long productId, BigDecimal quantity, BigDecimal costPrice, BigDecimal retailPrice, BigDecimal bulkPrice, LocalDate expirationDate, String batchReference) { }
    public record DeliveryView(Long id, Long supplierId, String supplierName, LocalDate expectedDate, String status, String notes, List<DeliveryItemView> items) { }
    public record DeliveryItemView(Long id, Long productId, String productName, BigDecimal quantityOrdered, BigDecimal quantityReceived, BigDecimal costPrice, LocalDate expirationDate) { }
    public record CreateDeliveryRequest(@NotNull Long supplierId, @NotNull @FutureOrPresent LocalDate expectedDate, @Size(max = 500) String notes, @NotEmpty List<@Valid DeliveryItemRequest> items) { }
    public record DeliveryItemRequest(@NotNull Long productId, @NotNull @DecimalMin("0.001") BigDecimal quantity, @NotNull @DecimalMin("0.00") BigDecimal costPrice, @DecimalMin("0.00") BigDecimal retailPrice, @DecimalMin("0.00") BigDecimal bulkPrice, LocalDate expirationDate, @Size(max = 120) String batchReference) { }
    public record StatusRequest(@NotBlank String status) { }
}
