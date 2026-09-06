package com.ddev.tindakart;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-integration")
@TestPropertySource(properties = "tindakart.billing.paymongo-webhook-secret=integration-webhook-secret")
class BillingWebhookIntegrationTest {
    private static final String SECRET = "integration-webhook-secret";
    private static final String EVENT_ID = "evt-integration-" + UUID.randomUUID();
    private static final String CHECKOUT_ID = "cs-integration-" + UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long vendorId;
    private Long packageId;
    private Long subscriptionId;

    @BeforeEach
    void setUpFixture() {
        vendorId = jdbcTemplate.queryForObject("INSERT INTO vendors (name, status) VALUES (?, 'PENDING') RETURNING id", Long.class,
                "Billing Integration Vendor " + EVENT_ID);
        packageId = jdbcTemplate.queryForObject("INSERT INTO packages (name, monthly_price, annual_price) VALUES (?, 99.00, 999.00) RETURNING id", Long.class,
                "Billing Integration Package " + EVENT_ID);
        subscriptionId = jdbcTemplate.queryForObject("INSERT INTO vendor_subscriptions (vendor_id, package_id, status) VALUES (?, ?, 'TRIAL') RETURNING id", Long.class,
                vendorId, packageId);
        jdbcTemplate.update("INSERT INTO subscription_checkout_sessions (vendor_subscription_id, provider, provider_checkout_id, checkout_url, amount) VALUES (?, 'PAYMONGO', ?, 'https://example.test/checkout', 99.00)",
                subscriptionId, CHECKOUT_ID);
    }

    @AfterEach
    void removeFixture() {
        if (vendorId == null) return;
        jdbcTemplate.update("DELETE FROM subscription_payments WHERE vendor_subscription_id = ?", subscriptionId);
        jdbcTemplate.update("DELETE FROM subscription_events WHERE vendor_subscription_id = ?", subscriptionId);
        jdbcTemplate.update("DELETE FROM subscription_checkout_sessions WHERE vendor_subscription_id = ?", subscriptionId);
        jdbcTemplate.update("DELETE FROM vendor_subscriptions WHERE id = ?", subscriptionId);
        jdbcTemplate.update("DELETE FROM packages WHERE id = ?", packageId);
        jdbcTemplate.update("DELETE FROM vendors WHERE id = ?", vendorId);
    }

    @Test
    void signedPaidWebhookIsIdempotent() throws Exception {
        String payload = "{\"data\":{\"id\":\"" + EVENT_ID + "\",\"attributes\":{\"type\":\"checkout_session.payment.paid\",\"data\":{\"id\":\"" + CHECKOUT_ID + "\"}}}}";
        String timestamp = "1700000000";
        String signature = "t=" + timestamp + ",te=" + sign(timestamp + "." + payload);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/billing/webhooks/paymongo")
                            .with(csrf()).header("Paymongo-Signature", signature)
                            .contentType("application/json").content(payload))
                    .andExpect(status().isOk());
        }

        Integer events = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscription_events WHERE provider = 'PAYMONGO' AND provider_event_id = ?", Integer.class, EVENT_ID);
        Integer payments = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscription_payments WHERE vendor_subscription_id = ?", Integer.class, subscriptionId);
        String subscriptionStatus = jdbcTemplate.queryForObject("SELECT status FROM vendor_subscriptions WHERE id = ?", String.class, subscriptionId);
        String checkoutStatus = jdbcTemplate.queryForObject("SELECT status FROM subscription_checkout_sessions WHERE provider_checkout_id = ?", String.class, CHECKOUT_ID);
        if (events == null || events != 1 || payments == null || payments != 1
                || !"ACTIVE".equals(subscriptionStatus) || !"PAID".equals(checkoutStatus)) {
            throw new AssertionError("Webhook was not processed idempotently");
        }
    }

    @Test
    void invalidWebhookSignatureIsRejectedWithoutRecordingEvent() throws Exception {
        String payload = "{\"data\":{\"id\":\"" + EVENT_ID + "\",\"attributes\":{\"type\":\"checkout_session.payment.paid\",\"data\":{\"id\":\"" + CHECKOUT_ID + "\"}}}}";

        mockMvc.perform(post("/api/billing/webhooks/paymongo")
                        .header("Paymongo-Signature", "t=1700000000,te=invalid")
                        .contentType("application/json").content(payload))
                .andExpect(status().isUnauthorized());

        Integer events = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscription_events WHERE provider = 'PAYMONGO' AND provider_event_id = ?", Integer.class, EVENT_ID);
        if (events == null || events != 0) throw new AssertionError("Invalid webhook was recorded");
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
