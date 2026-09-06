package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vendors/{vendorId}")
public class CatalogController {
    private static final List<String> UNIT_TYPES = List.of("PIECE", "PACK", "BOTTLE", "KILOGRAM", "LITER", "OTHER");
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;
    private final PackageAccessService packageAccessService;
    private final PermissionAccessService permissionAccessService;

    public CatalogController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService,
                             PackageAccessService packageAccessService, PermissionAccessService permissionAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
        this.packageAccessService = packageAccessService;
        this.permissionAccessService = permissionAccessService;
    }

    @GetMapping("/categories")
    public List<CategoryView> listCategories(@PathVariable Long vendorId, Authentication authentication) {
        requireVendorAccess(authentication, vendorId);
        packageAccessService.requireFeature(authentication, vendorId, "CATALOG");
        return jdbcTemplate.query("SELECT id, vendor_id, name, active FROM categories WHERE vendor_id = ? "
                        + "ORDER BY LOWER(name), id", (rs, rowNum) -> new CategoryView(rs.getLong("id"),
                        rs.getLong("vendor_id"), rs.getString("name"), rs.getBoolean("active")), vendorId);
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryView> createCategory(@PathVariable Long vendorId,
                                                        @Valid @RequestBody CategoryRequest request,
                                                        Authentication authentication) {
        requireVendorAdmin(authentication, vendorId);
        packageAccessService.requireFeature(authentication, vendorId, "CATALOG");
        try {
            Long id = jdbcTemplate.queryForObject("INSERT INTO categories (vendor_id, name) VALUES (?, ?) RETURNING id",
                    Long.class, vendorId, request.name().trim());
            return ResponseEntity.status(HttpStatus.CREATED).body(findCategory(vendorId, id));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists for this vendor");
        }
    }

    @GetMapping("/products")
    public List<ProductView> listProducts(@PathVariable Long vendorId,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String search,
                                           Authentication authentication) {
        requireVendorAccess(authentication, vendorId);
        packageAccessService.requireFeature(authentication, vendorId, "CATALOG");
        String normalizedSearch = search == null ? "" : search.trim();
        String select = "SELECT p.id, p.vendor_id, p.category_id, c.name AS category_name, p.name, p.sku, "
                + "p.unit_type, p.cost_price, p.retail_price, p.bulk_price, p.bulk_threshold, p.reorder_level, "
                + "p.expiration_applicable, p.active FROM products p LEFT JOIN categories c ON c.id = p.category_id ";
        String searchClause = "AND (? = '' OR LOWER(p.name) LIKE LOWER(?) OR LOWER(p.sku) LIKE LOWER(?)) "
                + "ORDER BY LOWER(p.name), p.id";
        var mapper = (org.springframework.jdbc.core.RowMapper<ProductView>) (rs, rowNum) -> product(rs, barcodes(rs.getLong("id")));
        if (categoryId == null) {
            return jdbcTemplate.query(select + "WHERE p.vendor_id = ? " + searchClause, mapper,
                    vendorId, normalizedSearch, "%" + normalizedSearch + "%", "%" + normalizedSearch + "%");
        }
        return jdbcTemplate.query(select + "WHERE p.vendor_id = ? AND p.category_id = ? " + searchClause, mapper,
                vendorId, categoryId, normalizedSearch, "%" + normalizedSearch + "%", "%" + normalizedSearch + "%");
    }

    @GetMapping("/products/barcode/{barcode}")
    public ProductView findByBarcode(@PathVariable Long vendorId, @PathVariable String barcode,
                                     Authentication authentication) {
        requireVendorAccess(authentication, vendorId);
        packageAccessService.requireFeature(authentication, vendorId, "CATALOG");
        return jdbcTemplate.query("SELECT p.id, p.vendor_id, p.category_id, c.name AS category_name, p.name, p.sku, "
                        + "p.unit_type, p.cost_price, p.retail_price, p.bulk_price, p.bulk_threshold, p.reorder_level, "
                        + "p.expiration_applicable, p.active FROM products p JOIN product_barcodes pb ON pb.product_id = p.id "
                        + "LEFT JOIN categories c ON c.id = p.category_id WHERE p.vendor_id = ? AND pb.barcode = ?",
                (rs, rowNum) -> product(rs, barcodes(rs.getLong("id"))), vendorId, barcode.trim())
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @GetMapping("/products/count")
    public long countProducts(@PathVariable Long vendorId, Authentication authentication) {
        requireVendorAccess(authentication, vendorId);
        packageAccessService.requireFeature(authentication, vendorId, "CATALOG");
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE vendor_id = ?", Long.class, vendorId);
        return count == null ? 0 : count;
    }

    @PostMapping("/products")
    @Transactional
    public ResponseEntity<ProductView> createProduct(@PathVariable Long vendorId,
                                                      @Valid @RequestBody ProductRequest request,
                                                      Authentication authentication) {
        requireVendorAdmin(authentication, vendorId);
        packageAccessService.requireFeature(authentication, vendorId, "CATALOG");
        long currentProducts = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE vendor_id = ?", Long.class, vendorId);
        if (!packageAccessService.withinLimit(vendorId, "MAX_PRODUCTS", currentProducts, 1)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your package product limit has been reached");
        }
        validateProduct(request);
        validateCategory(vendorId, request.categoryId());
        try {
            Long id = jdbcTemplate.queryForObject("INSERT INTO products (vendor_id, category_id, name, sku, unit_type, "
                    + "cost_price, retail_price, bulk_price, bulk_threshold, reorder_level, expiration_applicable) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id", Long.class, vendorId, request.categoryId(),
                    request.name().trim(), request.sku().trim().toUpperCase(), request.unitType(), request.costPrice(),
                    request.retailPrice(), request.bulkPrice(), request.bulkThreshold(), request.reorderLevel(), request.expirationApplicable());
            for (String barcode : request.barcodes()) {
                jdbcTemplate.update("INSERT INTO product_barcodes (product_id, barcode, is_primary) VALUES (?, ?, ?)",
                        id, barcode.trim(), barcode.equals(request.barcodes().getFirst()));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(findProduct(vendorId, id));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU or barcode already exists");
        }
    }

    @PutMapping("/products/{productId}")
    @Transactional
    public ProductView updateProduct(@PathVariable Long vendorId, @PathVariable Long productId,
                                     @Valid @RequestBody ProductRequest request, Authentication authentication) {
        requireVendorAdmin(authentication, vendorId);
        packageAccessService.requireFeature(authentication, vendorId, "CATALOG");
        validateProduct(request);
        validateCategory(vendorId, request.categoryId());
        if (jdbcTemplate.update("UPDATE products SET category_id = ?, name = ?, sku = ?, unit_type = ?, cost_price = ?, retail_price = ?, bulk_price = ?, bulk_threshold = ?, reorder_level = ?, expiration_applicable = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND vendor_id = ?",
                request.categoryId(), request.name().trim(), request.sku().trim().toUpperCase(), request.unitType(), request.costPrice(), request.retailPrice(), request.bulkPrice(), request.bulkThreshold(), request.reorderLevel(), request.expirationApplicable(), productId, vendorId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        jdbcTemplate.update("DELETE FROM product_barcodes WHERE product_id = ?", productId);
        for (String barcode : request.barcodes()) {
            jdbcTemplate.update("INSERT INTO product_barcodes (product_id, barcode, is_primary) VALUES (?, ?, ?)", productId, barcode.trim(), barcode.equals(request.barcodes().getFirst()));
        }
        return findProduct(vendorId, productId);
    }

    @DeleteMapping("/products/{productId}")
    public void deactivateProduct(@PathVariable Long vendorId, @PathVariable Long productId, Authentication authentication) {
        requireVendorAdmin(authentication, vendorId);
        packageAccessService.requireFeature(authentication, vendorId, "CATALOG");
        if (jdbcTemplate.update("UPDATE products SET active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND vendor_id = ?", productId, vendorId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
    }

    private ProductView findProduct(Long vendorId, Long id) {
        return jdbcTemplate.query("SELECT p.id, p.vendor_id, p.category_id, c.name AS category_name, p.name, p.sku, "
                        + "p.unit_type, p.cost_price, p.retail_price, p.bulk_price, p.bulk_threshold, p.reorder_level, "
                        + "p.expiration_applicable, p.active FROM products p LEFT JOIN categories c ON c.id = p.category_id "
                        + "WHERE p.vendor_id = ? AND p.id = ?", (rs, rowNum) -> product(rs, barcodes(rs.getLong("id"))), vendorId, id)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private CategoryView findCategory(Long vendorId, Long id) {
        return jdbcTemplate.query("SELECT id, vendor_id, name, active FROM categories WHERE vendor_id = ? AND id = ?",
                (rs, rowNum) -> new CategoryView(rs.getLong("id"), rs.getLong("vendor_id"), rs.getString("name"),
                        rs.getBoolean("active")), vendorId, id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private List<String> barcodes(Long productId) {
        return jdbcTemplate.query("SELECT barcode FROM product_barcodes WHERE product_id = ? ORDER BY is_primary DESC, barcode",
                (rs, rowNum) -> rs.getString("barcode"), productId);
    }

    private ProductView product(java.sql.ResultSet rs, List<String> barcodes) throws java.sql.SQLException {
        return new ProductView(rs.getLong("id"), rs.getLong("vendor_id"), rs.getObject("category_id", Long.class),
                rs.getString("category_name"), rs.getString("name"), rs.getString("sku"), rs.getString("unit_type"),
                rs.getBigDecimal("cost_price"), rs.getBigDecimal("retail_price"), rs.getBigDecimal("bulk_price"),
                rs.getObject("bulk_threshold", Integer.class), rs.getInt("reorder_level"),
                rs.getBoolean("expiration_applicable"), rs.getBoolean("active"), barcodes);
    }

    private void validateProduct(ProductRequest request) {
        if (!UNIT_TYPES.contains(request.unitType())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid unit type");
        if (request.bulkPrice() == null && request.bulkThreshold() != null || request.bulkPrice() != null && request.bulkThreshold() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bulk price and threshold must be provided together");
        }
        if (request.barcodes().stream().anyMatch(code -> code == null || code.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Barcode cannot be blank");
        }
    }

    private void validateCategory(Long vendorId, Long categoryId) {
        if (categoryId == null) return;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories WHERE id = ? AND vendor_id = ?",
                Integer.class, categoryId, vendorId);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category does not belong to this vendor");
    }

    private void requireVendorAdmin(Authentication authentication, Long vendorId) {
        permissionAccessService.requireVendor(authentication, vendorId, "CATALOG_MANAGE");
    }

    private void requireVendorAccess(Authentication authentication, Long vendorId) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) return;
        if (!tenantAccessService.hasStoreAccess(authentication, vendorId)) {
            throw new AccessDeniedException("You do not have access to this vendor");
        }
    }

    public record CategoryView(Long id, Long vendorId, String name, boolean active) { }
    public record ProductView(Long id, Long vendorId, Long categoryId, String categoryName, String name, String sku,
                              String unitType, BigDecimal costPrice, BigDecimal retailPrice, BigDecimal bulkPrice,
                              Integer bulkThreshold, int reorderLevel, boolean expirationApplicable, boolean active,
                              List<String> barcodes) { }
    public record CategoryRequest(@NotBlank @Size(max = 120) String name) { }
    public record ProductRequest(@NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 120) String sku,
                                 @NotBlank String unitType, Long categoryId,
                                 @NotNull @DecimalMin("0.00") BigDecimal costPrice,
                                 @NotNull @DecimalMin("0.00") BigDecimal retailPrice,
                                 @DecimalMin("0.00") BigDecimal bulkPrice, @PositiveOrZero Integer bulkThreshold,
                                 @PositiveOrZero int reorderLevel, boolean expirationApplicable,
                                 @NotNull @Size(min = 1) List<String> barcodes) { }
}
