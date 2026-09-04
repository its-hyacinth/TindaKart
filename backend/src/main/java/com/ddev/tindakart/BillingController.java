package com.ddev.tindakart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class BillingController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String secretKey;
    private final String webhookSecret;

    public BillingController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService, ObjectMapper objectMapper,
                             @Value("${tindakart.billing.paymongo-secret-key:}") String secretKey,
                             @Value("${tindakart.billing.paymongo-webhook-secret:}") String webhookSecret,
                             @Value("${tindakart.billing.paymongo-base-url:https://api.paymongo.com}") String baseUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
        this.objectMapper = objectMapper;
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @GetMapping("/vendors/{vendorId}/billing/checkout")
    public List<CheckoutView> checkoutHistory(@PathVariable Long vendorId, Authentication authentication) {
        requireVendorAdmin(vendorId, authentication);
        return jdbcTemplate.query("SELECT id, provider_checkout_id, checkout_url, amount, currency, status, created_at FROM subscription_checkout_sessions "
                        + "WHERE vendor_subscription_id IN (SELECT id FROM vendor_subscriptions WHERE vendor_id = ?) ORDER BY created_at DESC",
                (rs, rowNum) -> new CheckoutView(rs.getLong("id"), rs.getString("provider_checkout_id"), rs.getString("checkout_url"),
                        rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()), vendorId);
    }

    @PostMapping("/vendors/{vendorId}/billing/checkout")
    @Transactional
    public CheckoutView createCheckout(@PathVariable Long vendorId, @Valid @RequestBody CheckoutRequest request,
                                        Authentication authentication) {
        requireVendorAdmin(vendorId, authentication);
        if (secretKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PayMongo sandbox key is not configured");
        Subscription subscription = jdbcTemplate.query("SELECT vs.id, p.name, p.monthly_price, p.annual_price FROM vendor_subscriptions vs JOIN packages p ON p.id = vs.package_id "
                        + "WHERE vs.vendor_id = ? AND vs.status IN ('TRIAL', 'PAST_DUE') ORDER BY vs.created_at DESC LIMIT 1",
                (rs, rowNum) -> new Subscription(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("monthly_price"), rs.getBigDecimal("annual_price")), vendorId)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a package before checkout"));
        BigDecimal amount = "ANNUAL".equals(request.billingCycle()) ? subscription.annualPrice() : subscription.monthlyPrice();
        if (amount.signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected package has no billable price");
        String payload = "{\"data\":{\"attributes\":{"
                + "\"line_items\":[{\"currency\":\"PHP\",\"amount\":" + amount.movePointRight(2).intValueExact()
                + ",\"description\":\"TindaKart " + escape(subscription.packageName()) + " subscription\",\"name\":\"TindaKart subscription\",\"quantity\":1}],"
                + "\"payment_method_types\":[\"card\",\"gcash\",\"paymaya\"],\"description\":\"TindaKart subscription\","
                + "\"send_email_receipt\":false,\"show_line_items\":true}}}";
        String response;
        try {
            response = restClient.post().uri("/v1/checkout_sessions").contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8)))
                    .body(payload).retrieve().body(String.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PayMongo checkout could not be created");
        }
        try {
            JsonNode data = objectMapper.readTree(response).path("data");
            String checkoutId = data.path("id").asText();
            String checkoutUrl = data.path("attributes").path("checkout_url").asText();
            if (checkoutId.isBlank() || checkoutUrl.isBlank()) throw new IllegalStateException();
            jdbcTemplate.update("INSERT INTO subscription_checkout_sessions (vendor_subscription_id, provider, provider_checkout_id, checkout_url, amount) VALUES (?, 'PAYMONGO', ?, ?, ?)",
                    subscription.id(), checkoutId, checkoutUrl, amount);
            return jdbcTemplate.query("SELECT id, provider_checkout_id, checkout_url, amount, currency, status, created_at FROM subscription_checkout_sessions WHERE provider_checkout_id = ?",
                    (rs, rowNum) -> new CheckoutView(rs.getLong("id"), rs.getString("provider_checkout_id"), rs.getString("checkout_url"), rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()), checkoutId).getFirst();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from PayMongo");
        }
    }

    @PostMapping(path = "/billing/webhooks/paymongo", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public void paymongoWebhook(@RequestBody String payload, @RequestHeader(value = "Paymongo-Signature", required = false) String signature) {
        if (webhookSecret.isBlank() || !validSignature(payload, signature)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        try {
            JsonNode root = objectMapper.readTree(payload).path("data");
            String eventId = root.path("id").asText();
            String eventType = root.path("attributes").path("type").asText();
            if (eventId.isBlank() || eventType.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook payload");
            Integer existing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscription_events WHERE provider = 'PAYMONGO' AND provider_event_id = ?", Integer.class, eventId);
            if (existing != null && existing > 0) return;
            String resourceId = root.path("attributes").path("data").path("id").asText();
            Long subscriptionId = jdbcTemplate.query("SELECT vendor_subscription_id FROM subscription_checkout_sessions WHERE provider_checkout_id = ?",
                    (rs, rowNum) -> rs.getLong(1), resourceId).stream().findFirst().orElse(null);
            jdbcTemplate.update("INSERT INTO subscription_events (vendor_subscription_id, provider, provider_event_id, event_type, payload, processed_at) VALUES (?, 'PAYMONGO', ?, ?, ?, CURRENT_TIMESTAMP)",
                    subscriptionId, eventId, eventType, payload);
            String normalizedType = eventType.toLowerCase();
            if (normalizedType.contains("paid") && subscriptionId != null) {
                jdbcTemplate.update("UPDATE subscription_checkout_sessions SET status = 'PAID', completed_at = CURRENT_TIMESTAMP WHERE provider_checkout_id = ?", resourceId);
                jdbcTemplate.update("UPDATE vendor_subscriptions SET status = 'ACTIVE' WHERE id = ?", subscriptionId);
                BigDecimal amount = jdbcTemplate.queryForObject("SELECT amount FROM subscription_checkout_sessions WHERE provider_checkout_id = ?", BigDecimal.class, resourceId);
                jdbcTemplate.update("INSERT INTO subscription_payments (vendor_subscription_id, provider, provider_payment_id, amount, status, paid_at) "
                + "VALUES (?, 'PAYMONGO', ?, ?, 'PAID', CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING", subscriptionId, resourceId, amount);
            } else if ((normalizedType.contains("failed") || normalizedType.contains("cancel") || normalizedType.contains("expired"))
                    && subscriptionId != null) {
                String status = normalizedType.contains("expired") ? "EXPIRED" : normalizedType.contains("cancel") ? "CANCELLED" : "PAST_DUE";
                String checkoutStatus = normalizedType.contains("expired") ? "EXPIRED" : normalizedType.contains("cancel") ? "CANCELLED" : "FAILED";
                jdbcTemplate.update("UPDATE subscription_checkout_sessions SET status = ? WHERE provider_checkout_id = ?", checkoutStatus, resourceId);
                jdbcTemplate.update("UPDATE vendor_subscriptions SET status = ? WHERE id = ?", status, subscriptionId);
            }
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook payload"); }
    }

    private boolean validSignature(String payload, String signature) {
        if (signature == null || signature.isBlank()) return false;
        String timestamp = value(signature, "t");
        String testSignature = value(signature, "te");
        String liveSignature = value(signature, "li");
        if (timestamp.isBlank()) return false;
        String expected;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            expected = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { return false; }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), testSignature.getBytes(StandardCharsets.UTF_8))
                || MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), liveSignature.getBytes(StandardCharsets.UTF_8));
    }

    private String value(String signature, String key) {
        for (String piece : signature.split(",")) if (piece.startsWith(key + "=")) return piece.substring(key.length() + 1);
        return "";
    }

    private void requireVendorAdmin(Long vendorId, Authentication authentication) {
        if (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                && !tenantAccessService.hasVendorRole(authentication, vendorId, "VENDOR_ADMIN")) throw new AccessDeniedException("Vendor Admin permission is required");
    }

    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private record Subscription(Long id, String packageName, BigDecimal monthlyPrice, BigDecimal annualPrice) { }
    public record CheckoutRequest(@NotBlank String billingCycle) { }
    public record CheckoutView(Long id, String providerCheckoutId, String checkoutUrl, BigDecimal amount, String currency, String status, java.time.Instant createdAt) { }
}
