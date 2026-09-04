package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vendors/{vendorId}/stores/{storeId}/sales")
public class PosController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;
    private final PackageAccessService packageAccessService;
    private final PricingService pricingService;

    public PosController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService, PackageAccessService packageAccessService, PricingService pricingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
        this.packageAccessService = packageAccessService;
        this.pricingService = pricingService;
    }

    @PostMapping
    @Transactional
    public SaleView completeSale(@PathVariable Long vendorId, @PathVariable Long storeId, @Valid @RequestBody SaleRequest request,
                                 Authentication authentication) {
        requirePosAccess(vendorId, storeId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "POS");
        if (!List.of("CASH", "CARD", "EWALLET", "CREDIT").contains(request.paymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment method");
        }
        boolean creditSale = request.paymentMethod().equals("CREDIT");
        if (creditSale && (request.customerId() == null || request.dueDate() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit sales require a customer and due date");
        }
        if (creditSale && request.dueDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit sale due date cannot be in the past");
        }
        if (creditSale && jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer_profiles WHERE id = ? AND vendor_id = ?",
                Integer.class, request.customerId(), vendorId) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer does not belong to this vendor");
        }
        Long cashierId = userId(authentication);
        List<PreparedLine> lines = new ArrayList<>();
        for (SaleLineRequest line : request.items()) lines.add(prepareLine(vendorId, storeId, line));
        BigDecimal subtotal = lines.stream().map(PreparedLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = pricingService.money(request.discountAmount());
        BigDecimal taxable;
        try { taxable = pricingService.discountedSubtotal(subtotal, discount); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); }
        TaxSettings tax = taxSettings(vendorId);
        BigDecimal vat = pricingService.vat(taxable, tax.vatRegistered(), tax.vatRate());
        BigDecimal total = taxable.add(vat).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tendered = creditSale ? null : (request.amountTendered() == null ? total : money(request.amountTendered()));
        if (!creditSale && tendered.compareTo(total) < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount tendered is insufficient");
        BigDecimal change = creditSale ? BigDecimal.ZERO : tendered.subtract(total).setScale(2, RoundingMode.HALF_UP);
        String receiptNumber = "TK-" + storeId + "-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        Long saleId = jdbcTemplate.queryForObject("INSERT INTO sales (vendor_id, store_id, cashier_id, receipt_number, "
                + "subtotal, discount_amount, vat_amount, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id", Long.class,
                vendorId, storeId, cashierId, receiptNumber, subtotal, discount, vat, total);
        for (PreparedLine line : lines) {
            for (BatchAllocation allocation : line.allocations()) {
                jdbcTemplate.update("UPDATE inventory_batches SET quantity_on_hand = quantity_on_hand - ? WHERE id = ?",
                        allocation.quantity(), allocation.batchId());
                jdbcTemplate.update("INSERT INTO sale_items (sale_id, product_id, batch_id, quantity, unit_price, subtotal) "
                        + "VALUES (?, ?, ?, ?, ?, ?)", saleId, line.productId(), allocation.batchId(), allocation.quantity(),
                        line.unitPrice(), allocation.quantity().multiply(line.unitPrice()).setScale(2, RoundingMode.HALF_UP));
                jdbcTemplate.update("INSERT INTO inventory_movements (vendor_id, store_id, product_id, batch_id, movement_type, "
                        + "quantity_delta, reason, reference_type, reference_id, created_by) VALUES (?, ?, ?, ?, 'SALE', ?, ?, 'SALE', ?, ?)",
                        vendorId, storeId, line.productId(), allocation.batchId(), allocation.quantity().negate(), "POS sale", saleId.toString(), cashierId);
            }
        }
        jdbcTemplate.update("INSERT INTO payments (sale_id, payment_method, amount, amount_tendered, change_amount) VALUES (?, ?, ?, ?, ?)",
                saleId, request.paymentMethod(), total, tendered, change);
        jdbcTemplate.update("INSERT INTO receipts (sale_id, receipt_number) VALUES (?, ?)", saleId, receiptNumber);
        if (creditSale) {
            jdbcTemplate.update("INSERT INTO debt_accounts (vendor_id, customer_id) VALUES (?, ?) ON CONFLICT (vendor_id, customer_id) DO NOTHING",
                    vendorId, request.customerId());
            Long accountId = jdbcTemplate.queryForObject("SELECT id FROM debt_accounts WHERE vendor_id = ? AND customer_id = ?",
                    Long.class, vendorId, request.customerId());
            jdbcTemplate.update("INSERT INTO credit_sales (debt_account_id, sale_id, principal_amount, due_date) VALUES (?, ?, ?, ?)",
                    accountId, saleId, total, request.dueDate());
        }
        return new SaleView(saleId, receiptNumber, subtotal, discount, vat, total, request.paymentMethod(), tendered, change);
    }

    private PreparedLine prepareLine(Long vendorId, Long storeId, SaleLineRequest request) {
        ProductPricing product = jdbcTemplate.query("SELECT id, retail_price, bulk_price, bulk_threshold FROM products "
                        + "WHERE id = ? AND vendor_id = ? AND active = TRUE", (rs, rowNum) -> new ProductPricing(rs.getLong("id"),
                        rs.getBigDecimal("retail_price"), rs.getBigDecimal("bulk_price"), rs.getObject("bulk_threshold", Integer.class)),
                request.productId(), vendorId).stream().findFirst().orElseThrow(() -> notFound("Product not found"));
        BigDecimal unitPrice = pricingService.unitPrice(product.retailPrice(), product.bulkPrice(), product.bulkThreshold(), request.quantity());
        List<BatchAllocation> allocations = new ArrayList<>();
        BigDecimal remaining = request.quantity();
        List<StockBatch> batches = jdbcTemplate.query("SELECT id, quantity_on_hand, expiration_date FROM inventory_batches "
                        + "WHERE vendor_id = ? AND store_id = ? AND product_id = ? AND quantity_on_hand > 0 "
                        + "AND (expiration_date IS NULL OR expiration_date >= CURRENT_DATE) "
                        + "ORDER BY expiration_date NULLS LAST, received_at, id FOR UPDATE", (rs, rowNum) -> new StockBatch(rs.getLong("id"),
                        rs.getBigDecimal("quantity_on_hand"), rs.getObject("expiration_date", LocalDate.class)), vendorId, storeId, request.productId());
        for (StockBatch batch : batches) {
            if (remaining.signum() == 0) break;
            BigDecimal amount = remaining.min(batch.quantityOnHand());
            allocations.add(new BatchAllocation(batch.id(), amount));
            remaining = remaining.subtract(amount);
        }
        if (remaining.signum() > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient non-expired stock for product " + request.productId());
        return new PreparedLine(product.id(), unitPrice, request.quantity().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP), allocations);
    }

    private void requirePosAccess(Long vendorId, Long storeId, Authentication authentication) {
        Integer belongs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores WHERE id = ? AND vendor_id = ?", Integer.class, storeId, vendorId);
        if (belongs == null || belongs == 0) throw notFound("Store not found");
        boolean allowed = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                || tenantAccessService.hasVendorRole(authentication, vendorId, "VENDOR_ADMIN")
                || tenantAccessService.hasStoreRole(authentication, storeId, "CASHIER")
                || tenantAccessService.hasStoreRole(authentication, storeId, "STAFF");
        if (!allowed) throw new AccessDeniedException("POS access is required for this store");
    }

    private Long userId(Authentication authentication) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE LOWER(username) = LOWER(?)", Long.class, authentication.getName());
    }

    private BigDecimal money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }
    private TaxSettings taxSettings(Long vendorId) {
        return jdbcTemplate.query("SELECT vat_registered, vat_rate FROM business_settings WHERE vendor_id = ?",
                (rs, rowNum) -> new TaxSettings(rs.getBoolean("vat_registered"), rs.getBigDecimal("vat_rate")), vendorId)
                .stream().findFirst().orElse(new TaxSettings(false, BigDecimal.ZERO));
    }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }

    private record ProductPricing(Long id, BigDecimal retailPrice, BigDecimal bulkPrice, Integer bulkThreshold) { }
    private record StockBatch(Long id, BigDecimal quantityOnHand, LocalDate expirationDate) { }
    private record BatchAllocation(Long batchId, BigDecimal quantity) { }
    private record PreparedLine(Long productId, BigDecimal unitPrice, BigDecimal subtotal, List<BatchAllocation> allocations) { }
    private record TaxSettings(boolean vatRegistered, BigDecimal vatRate) { }

    public record SaleView(Long id, String receiptNumber, BigDecimal subtotal, BigDecimal discountAmount,
                           BigDecimal vatAmount, BigDecimal totalAmount, String paymentMethod,
                           BigDecimal amountTendered, BigDecimal changeAmount) { }
    public record SaleRequest(@NotEmpty List<@Valid SaleLineRequest> items,
                              @NotBlank String paymentMethod, @DecimalMin("0.00") BigDecimal discountAmount,
                              @DecimalMin("0.00") BigDecimal amountTendered, Long customerId, LocalDate dueDate) {
        public SaleRequest {
            discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        }
    }
    public record SaleLineRequest(@NotNull Long productId, @NotNull @DecimalMin("0.001") BigDecimal quantity) { }
}
