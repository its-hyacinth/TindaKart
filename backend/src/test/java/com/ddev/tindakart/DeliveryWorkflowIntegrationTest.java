package com.ddev.tindakart;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class DeliveryWorkflowIntegrationTest {
    private static final String USERNAME = "delivery-integration-" + UUID.randomUUID();
    private static final String PASSWORD = "integration-password";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    private Long vendorId;
    private Long storeId;
    private Long userId;
    private Long packageId;
    private Long supplierId;
    private Long productId;
    private Long deliveryId;
    private Long deliveryItemId;

    @BeforeEach
    void setUpFixture() {
        vendorId = jdbcTemplate.queryForObject("INSERT INTO vendors (name, status) VALUES (?, 'ACTIVE') RETURNING id", Long.class,
                "Delivery Integration Vendor " + USERNAME);
        storeId = jdbcTemplate.queryForObject("INSERT INTO stores (vendor_id, name, code) VALUES (?, ?, ?) RETURNING id", Long.class,
                vendorId, "Delivery Integration Store", "DELIVERY-" + USERNAME);
        userId = jdbcTemplate.queryForObject("INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?) RETURNING id", Long.class,
                USERNAME, passwordEncoder.encode(PASSWORD), "Delivery Integration Admin");
        Long vendorAdminRoleId = roleId("VENDOR_ADMIN");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, vendorAdminRoleId);
        jdbcTemplate.update("INSERT INTO vendor_user_roles (user_id, vendor_id, role_id) VALUES (?, ?, ?)", userId, vendorId, vendorAdminRoleId);
        packageId = jdbcTemplate.queryForObject("INSERT INTO packages (name, monthly_price, annual_price) VALUES (?, 0, 0) RETURNING id", Long.class,
                "Delivery Integration Package " + USERNAME);
        jdbcTemplate.update("INSERT INTO package_features (package_id, feature_key, enabled) VALUES (?, 'DELIVERY', TRUE)", packageId);
        jdbcTemplate.update("INSERT INTO vendor_subscriptions (vendor_id, package_id, status) VALUES (?, ?, 'ACTIVE')", vendorId, packageId);
        supplierId = jdbcTemplate.queryForObject("INSERT INTO suppliers (vendor_id, name) VALUES (?, ?) RETURNING id", Long.class,
                vendorId, "Delivery Integration Supplier");
        productId = jdbcTemplate.queryForObject("INSERT INTO products (vendor_id, name, sku, retail_price, unit_type) VALUES (?, ?, ?, 12.00, 'PIECE') RETURNING id", Long.class,
                vendorId, "Delivery Integration Product", "DELIVERY-SKU-" + USERNAME);
        deliveryId = jdbcTemplate.queryForObject("INSERT INTO deliveries (vendor_id, store_id, supplier_id, expected_date, notes) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class,
                vendorId, storeId, supplierId, LocalDate.now(), "Integration delivery");
        deliveryItemId = jdbcTemplate.queryForObject("INSERT INTO delivery_items (delivery_id, product_id, quantity_ordered, cost_price, retail_price, expiration_date, batch_reference) VALUES (?, ?, 6, 5.00, 12.00, ?, ?) RETURNING id", Long.class,
                deliveryId, productId, LocalDate.now().plusDays(60), "DELIVERY-BATCH-" + USERNAME);
    }

    @AfterEach
    void removeFixture() {
        if (vendorId == null) return;
        jdbcTemplate.update("DELETE FROM inventory_movements WHERE vendor_id = ?", vendorId);
        jdbcTemplate.update("DELETE FROM inventory_batches WHERE vendor_id = ?", vendorId);
        jdbcTemplate.update("DELETE FROM delivery_items WHERE delivery_id = ?", deliveryId);
        jdbcTemplate.update("DELETE FROM deliveries WHERE id = ?", deliveryId);
        jdbcTemplate.update("DELETE FROM suppliers WHERE id = ?", supplierId);
        jdbcTemplate.update("DELETE FROM products WHERE id = ?", productId);
        jdbcTemplate.update("DELETE FROM audit_logs WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM login_attempts WHERE LOWER(username) = LOWER(?)", USERNAME);
        jdbcTemplate.update("DELETE FROM vendor_user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        jdbcTemplate.update("DELETE FROM vendor_subscriptions WHERE vendor_id = ?", vendorId);
        jdbcTemplate.update("DELETE FROM package_features WHERE package_id = ?", packageId);
        jdbcTemplate.update("DELETE FROM packages WHERE id = ?", packageId);
        jdbcTemplate.update("DELETE FROM stores WHERE id = ?", storeId);
        jdbcTemplate.update("DELETE FROM vendors WHERE id = ?", vendorId);
    }

    @Test
    void receivingVarianceCreatesStockAndMovement() throws Exception {
        MockHttpSession session = login();
        String request = "{\"items\":[{\"deliveryItemId\":" + deliveryItemId + ",\"received\":4,\"missing\":1,\"damaged\":1}]}";

        mockMvc.perform(put("/api/vendors/{vendorId}/stores/{storeId}/deliveries/{deliveryId}/receive-details", vendorId, storeId, deliveryId)
                        .session(session).with(csrf()).contentType("application/json").content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.items[0].quantityOrdered").value(6.0))
                .andExpect(jsonPath("$.items[0].quantityReceived").value(4.0))
                .andExpect(jsonPath("$.items[0].quantityMissing").value(1.0))
                .andExpect(jsonPath("$.items[0].quantityDamaged").value(1.0));

        BigDecimal stock = jdbcTemplate.queryForObject("SELECT quantity_on_hand FROM inventory_batches WHERE vendor_id = ? AND store_id = ? AND product_id = ?", BigDecimal.class,
                vendorId, storeId, productId);
        Integer movements = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_movements WHERE vendor_id = ? AND movement_type = 'RECEIVE' AND reference_id = ?", Integer.class,
                vendorId, deliveryId.toString());
        if (stock == null || stock.compareTo(new BigDecimal("4.000")) != 0 || movements == null || movements != 1) {
            throw new AssertionError("Receiving did not create the expected stock movement");
        }
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
