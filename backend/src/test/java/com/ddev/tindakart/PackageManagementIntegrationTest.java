package com.ddev.tindakart;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-integration")
class PackageManagementIntegrationTest {
    private final String username = "package-admin-" + UUID.randomUUID();
    private final String password = "package-admin-password";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    private Long userId;
    private Long packageId;
    private Long vendorId;

    @BeforeEach
    void setUpFixture() {
        userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?) RETURNING id",
                Long.class, username, passwordEncoder.encode(password), "Package Admin");
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = 'SUPER_ADMIN'", Long.class);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        vendorId = jdbcTemplate.queryForObject(
                "INSERT INTO vendors (name, status) VALUES (?, 'ACTIVE') RETURNING id", Long.class,
                "Settings Integration Vendor " + username);
    }

    @AfterEach
    void removeFixture() {
        if (packageId != null) {
            jdbcTemplate.update("DELETE FROM package_features WHERE package_id = ?", packageId);
            jdbcTemplate.update("DELETE FROM package_limits WHERE package_id = ?", packageId);
            jdbcTemplate.update("DELETE FROM packages WHERE id = ?", packageId);
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM audit_logs WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM login_attempts WHERE LOWER(username) = LOWER(?)", username);
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
        if (vendorId != null) {
            jdbcTemplate.update("DELETE FROM business_settings WHERE vendor_id = ?", vendorId);
            jdbcTemplate.update("DELETE FROM vendors WHERE id = ?", vendorId);
        }
    }

    @Test
    void superAdminCanCreateAndUpdatePackageControls() throws Exception {
        MockHttpSession session = login();

        MvcResult created = mockMvc.perform(post("/api/super-admin/packages")
                        .session(session).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"Integration Package %s",
                                  "description":"Initial package",
                                  "monthlyPrice":199.00,
                                  "annualPrice":1990.00,
                                  "active":true,
                                  "features":{"CATALOG":true,"POS":false},
                                  "limits":{"MAX_STORES":2,"MAX_STAFF":10}
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.features.CATALOG").value(true))
                .andExpect(jsonPath("$.features.POS").value(false))
                .andExpect(jsonPath("$.limits.MAX_STORES").value(2))
                .andReturn();
        Number createdId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        packageId = createdId.longValue();

        mockMvc.perform(put("/api/super-admin/packages/{id}", packageId)
                        .session(session).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"Updated Integration Package %s",
                                  "description":"Updated package",
                                  "monthlyPrice":249.00,
                                  "annualPrice":2490.00,
                                  "active":false,
                                  "features":{"CATALOG":true,"POS":true,"REPORTS":true},
                                  "limits":{"MAX_STORES":5,"MAX_STAFF":20,"MAX_PRODUCTS":500}
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Integration Package " + username))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.features.POS").value(true))
                .andExpect(jsonPath("$.limits.MAX_PRODUCTS").value(500));

        Number monthlyPrice = jdbcTemplate.queryForObject("SELECT monthly_price FROM packages WHERE id = ?", Number.class, packageId);
        Integer featureCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM package_features WHERE package_id = ?", Integer.class, packageId);
        Integer limitCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM package_limits WHERE package_id = ?", Integer.class, packageId);
        if (monthlyPrice == null || monthlyPrice.doubleValue() != 249.00 || featureCount != 3 || limitCount != 3) {
            throw new AssertionError("Package controls were not persisted consistently");
        }
    }

    @Test
    void operationalSettingsArePersistedAsDynamicVendorPolicy() throws Exception {
        MockHttpSession session = login();
        String settings = """
                {
                  "businessName":"Dynamic Store",
                  "businessAddress":"Local address",
                  "tin":"TAX-123",
                  "vatRegistered":false,
                  "vatRate":0,
                  "nearExpirationDays":14,
                  "nearExpirationDiscountPercent":10,
                  "receiptFooter":"Thank you",
                  "posPaymentMethods":["CASH","CREDIT"],
                  "cameraScanningEnabled":false,
                  "receiptPrintMode":"THERMAL_BRIDGE"
                }
                """;
        mockMvc.perform(put("/api/vendors/{vendorId}/settings", vendorId)
                        .session(session).with(csrf()).contentType("application/json").content(settings))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posPaymentMethods[0]").value("CASH"))
                .andExpect(jsonPath("$.posPaymentMethods[1]").value("CREDIT"))
                .andExpect(jsonPath("$.cameraScanningEnabled").value(false))
                .andExpect(jsonPath("$.receiptPrintMode").value("THERMAL_BRIDGE"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/vendors/{vendorId}/settings", vendorId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nearExpirationDays").value(14))
                .andExpect(jsonPath("$.receiptPrintMode").value("THERMAL_BRIDGE"));
    }

    private MockHttpSession login() throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .with(csrf()).contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
}
