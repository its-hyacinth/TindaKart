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
class DebtWorkflowIntegrationTest {
    private static final String USERNAME = "debt-integration-" + UUID.randomUUID();
    private static final String PASSWORD = "integration-password";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    private Long vendorId;
    private Long storeId;
    private Long userId;
    private Long packageId;
    private Long productId;
    private Long batchId;
    private Long customerId;
    private Long accountId;

    @BeforeEach
    void setUpFixture() {
        vendorId = jdbcTemplate.queryForObject("INSERT INTO vendors (name, status) VALUES (?, 'ACTIVE') RETURNING id", Long.class,
                "Debt Integration Vendor " + USERNAME);
        storeId = jdbcTemplate.queryForObject("INSERT INTO stores (vendor_id, name, code) VALUES (?, ?, ?) RETURNING id", Long.class,
                vendorId, "Debt Integration Store", "DEBT-" + USERNAME);
        userId = jdbcTemplate.queryForObject("INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?) RETURNING id", Long.class,
                USERNAME, passwordEncoder.encode(PASSWORD), "Debt Integration Admin");
        Long vendorAdminRoleId = roleId("VENDOR_ADMIN");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, vendorAdminRoleId);
        jdbcTemplate.update("INSERT INTO vendor_user_roles (user_id, vendor_id, role_id) VALUES (?, ?, ?)", userId, vendorId, vendorAdminRoleId);
        packageId = jdbcTemplate.queryForObject("INSERT INTO packages (name, monthly_price, annual_price) VALUES (?, 0, 0) RETURNING id", Long.class,
                "Debt Integration Package " + USERNAME);
        jdbcTemplate.update("INSERT INTO package_features (package_id, feature_key, enabled) VALUES (?, 'POS', TRUE), (?, 'DEBT', TRUE)", packageId, packageId);
        jdbcTemplate.update("INSERT INTO vendor_subscriptions (vendor_id, package_id, status) VALUES (?, ?, 'ACTIVE')", vendorId, packageId);
        productId = jdbcTemplate.queryForObject("INSERT INTO products (vendor_id, name, sku, retail_price, unit_type) VALUES (?, ?, ?, 10.00, 'PIECE') RETURNING id", Long.class,
                vendorId, "Debt Integration Product", "DEBT-SKU-" + USERNAME);
        batchId = jdbcTemplate.queryForObject("INSERT INTO inventory_batches (vendor_id, store_id, product_id, batch_reference, quantity_received, quantity_on_hand, cost_price, retail_price) VALUES (?, ?, ?, ?, 3, 3, 5.00, 10.00) RETURNING id", Long.class,
                vendorId, storeId, productId, "DEBT-STOCK-" + USERNAME);
        customerId = jdbcTemplate.queryForObject("INSERT INTO customer_profiles (vendor_id, name) VALUES (?, ?) RETURNING id", Long.class,
                vendorId, "Debt Integration Customer");
    }

    @AfterEach
    void removeFixture() {
        if (vendorId == null) return;
        jdbcTemplate.update("DELETE FROM debt_payment_allocations WHERE credit_sale_id IN (SELECT cs.id FROM credit_sales cs JOIN debt_accounts da ON da.id = cs.debt_account_id WHERE da.vendor_id = ?)", vendorId);
        jdbcTemplate.update("DELETE FROM debt_payments WHERE debt_account_id IN (SELECT id FROM debt_accounts WHERE vendor_id = ?)", vendorId);
        jdbcTemplate.update("DELETE FROM credit_sales WHERE debt_account_id IN (SELECT id FROM debt_accounts WHERE vendor_id = ?)", vendorId);
        jdbcTemplate.update("DELETE FROM debt_accounts WHERE vendor_id = ?", vendorId);
        jdbcTemplate.update("DELETE FROM receipts WHERE sale_id IN (SELECT id FROM sales WHERE vendor_id = ?)", vendorId);
        jdbcTemplate.update("DELETE FROM payments WHERE sale_id IN (SELECT id FROM sales WHERE vendor_id = ?)", vendorId);
        jdbcTemplate.update("DELETE FROM sale_items WHERE sale_id IN (SELECT id FROM sales WHERE vendor_id = ?)", vendorId);
        jdbcTemplate.update("DELETE FROM inventory_movements WHERE vendor_id = ?", vendorId);
        jdbcTemplate.update("DELETE FROM sales WHERE vendor_id = ?", vendorId);
        jdbcTemplate.update("DELETE FROM inventory_batches WHERE id = ?", batchId);
        jdbcTemplate.update("DELETE FROM products WHERE id = ?", productId);
        jdbcTemplate.update("DELETE FROM customer_profiles WHERE id = ?", customerId);
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
    void creditSaleCreatesDebtAndPaymentReducesBalance() throws Exception {
        MockHttpSession session = login();
        String sale = "{\"items\":[{\"productId\":" + productId + ",\"quantity\":1}],\"paymentMethod\":\"CREDIT\",\"discountAmount\":0,\"customerId\":"
                + customerId + ",\"dueDate\":\"" + LocalDate.now().plusDays(14) + "\"}";

        mockMvc.perform(post("/api/vendors/{vendorId}/stores/{storeId}/sales", vendorId, storeId)
                        .session(session).with(csrf()).contentType("application/json").content(sale))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(10.0));

        accountId = jdbcTemplate.queryForObject("SELECT id FROM debt_accounts WHERE vendor_id = ? AND customer_id = ?", Long.class, vendorId, customerId);
        BigDecimal principal = jdbcTemplate.queryForObject("SELECT principal_amount FROM credit_sales WHERE debt_account_id = ?", BigDecimal.class, accountId);
        if (principal == null || principal.compareTo(new BigDecimal("10.00")) != 0) throw new AssertionError("Credit sale was not recorded");

        mockMvc.perform(post("/api/vendors/{vendorId}/debt/accounts/{accountId}/payments", vendorId, accountId)
                        .session(session).with(csrf()).contentType("application/json")
                        .content("{\"amount\":4,\"paymentMethod\":\"CASH\",\"notes\":\"Partial payment\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(4.0));

        mockMvc.perform(get("/api/vendors/{vendorId}/debt/accounts", vendorId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].totalCredit").value(10.0))
                .andExpect(jsonPath("[0].totalPaid").value(4.0))
                .andExpect(jsonPath("[0].balance").value(6.0))
                .andExpect(jsonPath("[0].status").value("OPEN"));
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
