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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-integration")
class AuthRegistrationIntegrationTest {
    private final String username = "registration-" + UUID.randomUUID();
    private final String storeCode = "REG-" + UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void removeFixture() {
        jdbc.update("DELETE FROM login_attempts WHERE LOWER(username) = LOWER(?)", username);
        jdbc.update("DELETE FROM audit_logs WHERE user_id IN (SELECT id FROM users WHERE username = ?)", username);
        jdbc.update("DELETE FROM users WHERE username = ?", username);
        jdbc.update("DELETE FROM stores WHERE code = ?", storeCode);
    }

    @Test
    void registerLoginReadSessionAndSelectStore() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register-store")
                        .with(csrf()).contentType("application/json")
                        .content("""
                                {"username":"%s","password":"registration-password","displayName":"Registration Test","storeName":"Registration Store","storeCode":"%s"}
                                """.formatted(username, storeCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.plan").value("Free"))
                .andReturn();
        Number storeId = com.jayway.jsonpath.JsonPath.read(registration.getResponse().getContentAsString(), "$.storeId");

        MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"registration-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("STORE_ADMIN"))
                .andExpect(jsonPath("$.stores", hasSize(1)))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username));
        mockMvc.perform(put("/api/auth/context").session(session).with(csrf())
                        .contentType("application/json").content("{\"storeId\":" + storeId.longValue() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.storeId").value(storeId.longValue()));
    }
}
