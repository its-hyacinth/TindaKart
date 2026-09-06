package com.ddev.tindakart;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
class CatalogBarcodeIntegrationTest {
    private static final String USERNAME = "catalog-integration-" + UUID.randomUUID();
    private static final String PASSWORD = "integration-password";
    private static final String BARCODE = "DUPLICATE-" + USERNAME;

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    private Long vendorId;
    private Long userId;
    private Long packageId;
    private Long existingProductId;

    @BeforeEach
    void setUpFixture() {
        vendorId = jdbcTemplate.queryForObject("INSERT INTO vendors (name, status) VALUES (?, 'ACTIVE') RETURNING id", Long.class,
                "Catalog Integration Vendor " + USERNAME);
        userId = jdbcTemplate.queryForObject("INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?) RETURNING id", Long.class,
                USERNAME, passwordEncoder.encode(PASSWORD), "Catalog Integration Admin");
        Long vendorAdminRoleId = roleId("VENDOR_ADMIN");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, vendorAdminRoleId);
        jdbcTemplate.update("INSERT INTO vendor_user_roles (user_id, vendor_id, role_id) VALUES (?, ?, ?)", userId, vendorId, vendorAdminRoleId);
        packageId = jdbcTemplate.queryForObject("INSERT INTO packages (name, monthly_price, annual_price) VALUES (?, 0, 0) RETURNING id", Long.class,
                "Catalog Integration Package " + USERNAME);
        jdbcTemplate.update("INSERT INTO package_features (package_id, feature_key, enabled) VALUES (?, 'CATALOG', TRUE)", packageId);
        jdbcTemplate.update("INSERT INTO vendor_subscriptions (vendor_id, package_id, status) VALUES (?, ?, 'ACTIVE')", vendorId, packageId);
        existingProductId = jdbcTemplate.queryForObject("INSERT INTO products (vendor_id, name, sku, cost_price, retail_price) VALUES (?, ?, ?, 1.00, 2.00) RETURNING id", Long.class,
                vendorId, "Existing Barcode Product", "EXISTING-" + USERNAME);
        jdbcTemplate.update("INSERT INTO product_barcodes (product_id, barcode, is_primary) VALUES (?, ?, TRUE)", existingProductId, BARCODE);
    }

    @AfterEach
    void removeFixture() {
        if (vendorId == null) return;
        jdbcTemplate.update("DELETE FROM product_barcodes WHERE product_id IN (SELECT id FROM products WHERE vendor_id = ?)", vendorId);
        jdbcTemplate.update("DELETE FROM products WHERE vendor_id = ?", vendorId);
        jdbcTemplate.update("DELETE FROM audit_logs WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM login_attempts WHERE LOWER(username) = LOWER(?)", USERNAME);
        jdbcTemplate.update("DELETE FROM vendor_user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        jdbcTemplate.update("DELETE FROM vendor_subscriptions WHERE vendor_id = ?", vendorId);
        jdbcTemplate.update("DELETE FROM package_features WHERE package_id = ?", packageId);
        jdbcTemplate.update("DELETE FROM packages WHERE id = ?", packageId);
        jdbcTemplate.update("DELETE FROM vendors WHERE id = ?", vendorId);
    }

    @Test
    void duplicateBarcodeIsRejectedWithoutCreatingAnotherProduct() throws Exception {
        MockHttpSession session = login();
        String request = "{\"name\":\"Duplicate Attempt\",\"sku\":\"DUP-SKU-" + USERNAME
                + "\",\"unitType\":\"PIECE\",\"costPrice\":1,\"retailPrice\":2,\"reorderLevel\":0,\"expirationApplicable\":false,\"barcodes\":[\""
                + BARCODE + "\"]}";

        mockMvc.perform(post("/api/vendors/{vendorId}/products", vendorId)
                        .session(session).with(csrf()).contentType("application/json").content(request))
                .andExpect(status().isConflict());

        Integer products = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE vendor_id = ?", Integer.class, vendorId);
        Integer barcodeOwners = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_barcodes WHERE barcode = ?", Integer.class, BARCODE);
        if (products == null || products != 1 || barcodeOwners == null || barcodeOwners != 1) {
            throw new AssertionError("Duplicate barcode attempt changed catalog state");
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
