package com.ddev.tindakart;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-integration")
class PosWorkflowIntegrationTest {
    private static final String USERNAME = "pos-integration-" + UUID.randomUUID();
    private static final String PASSWORD = "integration-password";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    private Long vendorId;
    private Long storeId;
    private Long userId;
    private Long packageId;
    private Long productId;
    private Long expiredProductId;
    private Long stockBatchId;
    private Long expiredBatchId;
    private Long customerId;

    @BeforeEach
    void setUpFixture() {
        vendorId = jdbcTemplate.queryForObject("INSERT INTO vendors (name, status) VALUES (?, 'ACTIVE') RETURNING id", Long.class,
                "POS Integration Vendor " + USERNAME);
        storeId = jdbcTemplate.queryForObject("INSERT INTO stores (vendor_id, name, code) VALUES (?, ?, ?) RETURNING id", Long.class,
                vendorId, "POS Integration Store", "POS-" + USERNAME);
        userId = jdbcTemplate.queryForObject("INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?) RETURNING id", Long.class,
                USERNAME, passwordEncoder.encode(PASSWORD), "POS Integration Cashier");
        Long cashierRoleId = roleId("CASHIER");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, cashierRoleId);
        jdbcTemplate.update("INSERT INTO store_user_roles (user_id, store_id, role_id) VALUES (?, ?, ?)", userId, storeId, cashierRoleId);

        packageId = jdbcTemplate.queryForObject("INSERT INTO packages (name, monthly_price, annual_price) VALUES (?, 0, 0) RETURNING id", Long.class,
                "POS Integration Package " + USERNAME);
        jdbcTemplate.update("INSERT INTO package_features (package_id, feature_key, enabled) VALUES (?, 'POS', TRUE)", packageId);
        Long subscriptionId = jdbcTemplate.queryForObject("INSERT INTO vendor_subscriptions (vendor_id, package_id, status) VALUES (?, ?, 'ACTIVE') RETURNING id", Long.class,
                vendorId, packageId);
        if (subscriptionId == null) throw new IllegalStateException("Subscription fixture was not created");
        jdbcTemplate.update("INSERT INTO business_settings (vendor_id, pos_payment_methods) VALUES (?, ARRAY['CASH']::varchar[])", vendorId);

        productId = jdbcTemplate.queryForObject("INSERT INTO products (vendor_id, name, sku, retail_price, unit_type) VALUES (?, ?, ?, 10.00, 'PIECE') RETURNING id", Long.class,
                vendorId, "POS Integration Product", "POS-SKU-" + USERNAME);
        jdbcTemplate.update("INSERT INTO product_barcodes (product_id, barcode, is_primary) VALUES (?, ?, TRUE)", productId, "POS-BARCODE-" + USERNAME);
        stockBatchId = jdbcTemplate.queryForObject("INSERT INTO inventory_batches (vendor_id, store_id, product_id, batch_reference, quantity_received, quantity_on_hand, cost_price, retail_price, expiration_date) VALUES (?, ?, ?, ?, 5, 5, 5.00, 10.00, ?) RETURNING id", Long.class,
                vendorId, storeId, productId, "POS-STOCK-" + USERNAME, LocalDate.now().plusDays(30));

        expiredProductId = jdbcTemplate.queryForObject("INSERT INTO products (vendor_id, name, sku, retail_price, unit_type) VALUES (?, ?, ?, 8.00, 'PIECE') RETURNING id", Long.class,
                vendorId, "POS Expired Product", "POS-EXPIRED-" + USERNAME);
        expiredBatchId = jdbcTemplate.queryForObject("INSERT INTO inventory_batches (vendor_id, store_id, product_id, batch_reference, quantity_received, quantity_on_hand, cost_price, retail_price, expiration_date) VALUES (?, ?, ?, ?, 2, 2, 4.00, 8.00, ?) RETURNING id", Long.class,
                vendorId, storeId, expiredProductId, "POS-EXPIRED-STOCK-" + USERNAME, LocalDate.now().minusDays(1));
        customerId = jdbcTemplate.queryForObject("INSERT INTO customer_profiles (vendor_id, name, phone) VALUES (?, ?, ?) RETURNING id", Long.class,
                vendorId, "POS Credit Customer", "09000000000");
    }

    @AfterEach
    void removeFixture() {
        if (vendorId == null) return;
        jdbcTemplate.update("DELETE FROM receipts WHERE sale_id IN (SELECT id FROM sales WHERE vendor_id = ?)", vendorId);
        jdbcTemplate.update("DELETE FROM payments WHERE sale_id IN (SELECT id FROM sales WHERE vendor_id = ?)", vendorId);
        jdbcTemplate.update("DELETE FROM sale_items WHERE sale_id IN (SELECT id FROM sales WHERE vendor_id = ?)", vendorId);
        jdbcTemplate.update("DELETE FROM inventory_movements WHERE vendor_id = ?", vendorId);
        jdbcTemplate.update("DELETE FROM sales WHERE vendor_id = ?", vendorId);
        jdbcTemplate.update("DELETE FROM product_barcodes WHERE product_id IN (?, ?)", productId, expiredProductId);
        jdbcTemplate.update("DELETE FROM inventory_batches WHERE id IN (?, ?)", stockBatchId, expiredBatchId);
        jdbcTemplate.update("DELETE FROM products WHERE id IN (?, ?)", productId, expiredProductId);
        jdbcTemplate.update("DELETE FROM customer_profiles WHERE id = ?", customerId);
        jdbcTemplate.update("DELETE FROM audit_logs WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM login_attempts WHERE LOWER(username) = LOWER(?)", USERNAME);
        jdbcTemplate.update("DELETE FROM store_user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        jdbcTemplate.update("DELETE FROM vendor_subscriptions WHERE id = (SELECT id FROM vendor_subscriptions WHERE vendor_id = ? AND package_id = ?)", vendorId, packageId);
        jdbcTemplate.update("DELETE FROM package_features WHERE package_id = ?", packageId);
        jdbcTemplate.update("DELETE FROM packages WHERE id = ?", packageId);
        jdbcTemplate.update("DELETE FROM stores WHERE id = ?", storeId);
        jdbcTemplate.update("DELETE FROM vendors WHERE id = ?", vendorId);
    }

    @Test
    void saleDeductsStockAndInsufficientOrExpiredStockIsRejected() throws Exception {
        MockHttpSession session = login();
        mockMvc.perform(get("/api/vendors/{vendorId}/stores/{storeId}/sales/customers", vendorId, storeId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].id").value(customerId));
        String sale = "{\"items\":[{\"productId\":" + productId + ",\"quantity\":2}],\"paymentMethod\":\"CASH\",\"discountAmount\":0,\"amountTendered\":20}";

        mockMvc.perform(post("/api/vendors/{vendorId}/stores/{storeId}/sales", vendorId, storeId)
                        .session(session).with(csrf()).contentType("application/json").content(sale))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(20.0))
                .andExpect(jsonPath("$.totalAmount").value(20.0));

        BigDecimal remaining = jdbcTemplate.queryForObject("SELECT quantity_on_hand FROM inventory_batches WHERE id = ?", BigDecimal.class, stockBatchId);
        if (remaining == null || remaining.compareTo(new BigDecimal("3.000")) != 0) {
            throw new AssertionError("Expected exactly 3 units after sale, got " + remaining);
        }
        Integer movements = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_movements WHERE batch_id = ? AND movement_type = 'SALE'", Integer.class, stockBatchId);
        if (movements == null || movements != 1) throw new AssertionError("Expected one sale movement");

        mockMvc.perform(post("/api/vendors/{vendorId}/stores/{storeId}/sales", vendorId, storeId)
                        .session(session).with(csrf()).contentType("application/json")
                        .content("{\"items\":[{\"productId\":" + productId + ",\"quantity\":10}],\"paymentMethod\":\"CASH\",\"discountAmount\":0,\"amountTendered\":100}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/vendors/{vendorId}/stores/{storeId}/sales", vendorId, storeId)
                        .session(session).with(csrf()).contentType("application/json")
                        .content("{\"items\":[{\"productId\":" + expiredProductId + ",\"quantity\":1}],\"paymentMethod\":\"CASH\",\"discountAmount\":0,\"amountTendered\":8}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/vendors/{vendorId}/stores/{storeId}/sales", vendorId, storeId)
                        .session(session).with(csrf()).contentType("application/json")
                        .content("{\"items\":[{\"productId\":" + productId + ",\"quantity\":1}],\"paymentMethod\":\"CARD\",\"discountAmount\":0,\"amountTendered\":10}"))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                        .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Long roleId(String role) {
        return jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, role);
    }
}
