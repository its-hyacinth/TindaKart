package com.ddev.tindakart;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-integration")
class MultitenantAuthorizationIntegrationTest {
    private static final String USERNAME = "integration-" + UUID.randomUUID();
    private static final String PASSWORD = "integration-password";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    private Long vendorA;
    private Long storeA;
    private Long vendorB;
    private Long storeB;
    private Long userId;

    @BeforeEach
    void setUpFixture() {
        vendorA = jdbcTemplate.queryForObject(
                "INSERT INTO vendors (name, status) VALUES (?, 'ACTIVE') RETURNING id", Long.class,
                "Integration Vendor A " + USERNAME);
        storeA = jdbcTemplate.queryForObject(
                "INSERT INTO stores (vendor_id, name, code) VALUES (?, ?, ?) RETURNING id", Long.class,
                vendorA, "Integration Store A", "INT-A-" + USERNAME);
        vendorB = jdbcTemplate.queryForObject(
                "INSERT INTO vendors (name, status) VALUES (?, 'ACTIVE') RETURNING id", Long.class,
                "Integration Vendor B " + USERNAME);
        storeB = jdbcTemplate.queryForObject(
                "INSERT INTO stores (vendor_id, name, code) VALUES (?, ?, ?) RETURNING id", Long.class,
                vendorB, "Integration Store B", "INT-B-" + USERNAME);
        userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?) RETURNING id", Long.class,
                USERNAME, passwordEncoder.encode(PASSWORD), "Integration Staff");
        Long staffRoleId = roleId("STAFF");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, staffRoleId);
        jdbcTemplate.update("INSERT INTO store_user_roles (user_id, store_id, role_id) VALUES (?, ?, ?)", userId, storeA, staffRoleId);
    }

    @AfterEach
    void removeFixture() {
        if (userId == null) return;
        jdbcTemplate.update("DELETE FROM audit_logs WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM login_attempts WHERE LOWER(username) = LOWER(?)", USERNAME);
        jdbcTemplate.update("DELETE FROM store_user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM vendor_user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        jdbcTemplate.update("DELETE FROM stores WHERE id IN (?, ?)", storeA, storeB);
        jdbcTemplate.update("DELETE FROM vendors WHERE id IN (?, ?)", vendorA, vendorB);
    }

    @Test
    void staffCanSelectAssignedStoreButCannotSelectAnotherVendorsStore() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("STAFF"))
                .andExpect(jsonPath("$.stores", hasSize(1)))
                .andExpect(jsonPath("$.stores[0].id").value(storeA))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(put("/api/auth/context")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"vendorId\":" + vendorA + ",\"storeId\":" + storeA + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorId").value(vendorA))
                .andExpect(jsonPath("$.storeId").value(storeA));

        mockMvc.perform(get("/api/auth/context").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendorId").value(vendorA))
                .andExpect(jsonPath("$.storeId").value(storeA));

        session.setAttribute("CURRENT_VENDOR_ID", vendorB);
        session.setAttribute("CURRENT_STORE_ID", storeB);
        mockMvc.perform(get("/api/auth/context").session(session))
                .andExpect(status().isForbidden());
        if (session.getAttribute("CURRENT_VENDOR_ID") != null || session.getAttribute("CURRENT_STORE_ID") != null) {
            throw new AssertionError("Invalid saved context was not cleared");
        }

        mockMvc.perform(put("/api/auth/context")
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"vendorId\":" + vendorB + ",\"storeId\":" + storeB + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/vendors/{vendorId}/stores/{storeId}/sales", vendorB, storeB)
                        .session(session)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}],\"paymentMethod\":\"CASH\",\"discountAmount\":0,\"amountTendered\":10}"))
                .andExpect(status().isForbidden());
    }

    private Long roleId(String role) {
        return jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, role);
    }
}
