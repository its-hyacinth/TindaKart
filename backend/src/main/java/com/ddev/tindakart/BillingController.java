package com.ddev.tindakart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class BillingController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;
    private final SubscriptionPaymentProvider paymentProvider;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public BillingController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService, ObjectMapper objectMapper,
                             SubscriptionPaymentProvider paymentProvider,
                             @Value("${tindakart.billing.paymongo-webhook-secret:}") String webhookSecret) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
        this.objectMapper = objectMapper;
        this.paymentProvider = paymentProvider;
        this.webhookSecret = webhookSecret;
    }

    @GetMapping("/stores/{storeId}/billing/checkout")
    public List<CheckoutView> checkoutHistory(@PathVariable Long storeId, Authentication authentication) {
        requireStoreAdmin(storeId, authentication);
        return jdbcTemplate.query("SELECT id, provider_checkout_id, checkout_url, amount, currency, status, created_at FROM subscription_checkout_sessions "
                        + "WHERE store_subscription_id IN (SELECT id FROM store_subscriptions WHERE store_id = ?) ORDER BY created_at DESC",
                (rs, rowNum) -> new CheckoutView(rs.getLong("id"), rs.getString("provider_checkout_id"), rs.getString("checkout_url"),
                        rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()), storeId);
    }

    @PostMapping("/stores/{storeId}/billing/checkout")
    @Transactional
    public CheckoutView createCheckout(@PathVariable Long storeId, @Valid @RequestBody CheckoutRequest request,
                                        Authentication authentication) {
        requireStoreAdmin(storeId, authentication);
        if (!paymentProvider.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment provider is not configured");
        Subscription subscription = jdbcTemplate.query("SELECT vs.id, p.name, p.monthly_price, p.annual_price FROM store_subscriptions vs JOIN packages p ON p.id = vs.package_id "
                        + "WHERE vs.store_id = ? AND vs.status IN ('TRIAL', 'PAST_DUE') ORDER BY vs.created_at DESC LIMIT 1",
                (rs, rowNum) -> new Subscription(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("monthly_price"), rs.getBigDecimal("annual_price")), storeId)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a package before checkout"));
        BigDecimal amount = "ANNUAL".equals(request.billingCycle()) ? subscription.annualPrice() : subscription.monthlyPrice();
        if (amount.signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected package has no billable price");
        SubscriptionPaymentProvider.Checkout checkout = paymentProvider.createCheckout(subscription.packageName(), amount);
        try {
            String checkoutId = checkout.providerCheckoutId();
            String checkoutUrl = checkout.checkoutUrl();
            jdbcTemplate.update("INSERT INTO subscription_checkout_sessions (store_subscription_id, provider, provider_checkout_id, checkout_url, amount) VALUES (?, 'PAYMONGO', ?, ?, ?)",
                    subscription.id(), checkoutId, checkoutUrl, amount);
            return jdbcTemplate.query("SELECT id, provider_checkout_id, checkout_url, amount, currency, status, created_at FROM subscription_checkout_sessions WHERE provider_checkout_id = ?",
                    (rs, rowNum) -> new CheckoutView(rs.getLong("id"), rs.getString("provider_checkout_id"), rs.getString("checkout_url"), rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()), checkoutId).getFirst();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from PayMongo");
        }
    }

    @PostMapping("/stores/{storeId}/billing/custom-checkout")
    @Transactional
    public CheckoutView createCustomCheckout(@PathVariable Long storeId, @Valid @RequestBody CustomCheckoutRequest request,
                                              Authentication authentication) {
        requireStoreAdmin(storeId, authentication);
        if (!paymentProvider.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment provider is not configured");
        Subscription subscription = jdbcTemplate.query("SELECT vs.id, p.name, p.monthly_price, p.annual_price FROM store_subscriptions vs JOIN packages p ON p.id = vs.package_id "
                        + "WHERE vs.store_id = ? AND vs.status IN ('TRIAL', 'ACTIVE') ORDER BY vs.created_at DESC LIMIT 1",
                (rs, rowNum) -> new Subscription(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("monthly_price"), rs.getBigDecimal("annual_price")), storeId)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active store plan found"));
        List<String> featureKeys = request.featureKeys() == null ? List.of() : request.featureKeys().stream().map(key -> key.trim().toUpperCase()).distinct().toList();
        if (featureKeys.stream().anyMatch("STAFF_SEAT"::equals)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use staffSeats for staff pricing");
        List<BigDecimal> featurePrices = featureKeys.isEmpty() ? List.of() : jdbcTemplate.query("SELECT monthly_price FROM platform_addon_prices WHERE feature_key = ANY (?::varchar[]) AND active = TRUE",
                (rs, rowNum) -> rs.getBigDecimal("monthly_price"), (Object) featureKeys.toArray(String[]::new));
        if (featurePrices.size() != featureKeys.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more feature add-ons are unavailable");
        BigDecimal seatPrice = jdbcTemplate.queryForObject("SELECT monthly_price FROM platform_addon_prices WHERE feature_key = 'STAFF_SEAT' AND active = TRUE", BigDecimal.class);
        BigDecimal monthly = seatPrice.multiply(BigDecimal.valueOf(request.staffSeats()));
        for (BigDecimal price : featurePrices) monthly = monthly.add(price);
        BigDecimal amount = "ANNUAL".equals(request.billingCycle()) ? monthly.multiply(BigDecimal.valueOf(12)) : monthly;
        if (amount.signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one paid staff seat or feature");
        String metadata = "staffSeats=" + request.staffSeats() + ";features=" + String.join(",", featureKeys);
        SubscriptionPaymentProvider.Checkout checkout = paymentProvider.createCheckout("TindaKart custom add-ons", amount);
        try {
            jdbcTemplate.update("INSERT INTO subscription_checkout_sessions (store_subscription_id, provider, provider_checkout_id, checkout_url, amount, purchase_type, purchase_metadata) VALUES (?, 'PAYMONGO', ?, ?, ?, 'CUSTOM_ADDONS', ?)",
                    subscription.id(), checkout.providerCheckoutId(), checkout.checkoutUrl(), amount, metadata);
            return jdbcTemplate.query("SELECT id, provider_checkout_id, checkout_url, amount, currency, status, created_at FROM subscription_checkout_sessions WHERE provider_checkout_id = ?",
                    (rs, rowNum) -> new CheckoutView(rs.getLong("id"), rs.getString("provider_checkout_id"), rs.getString("checkout_url"), rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()), checkout.providerCheckoutId()).getFirst();
        } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from PayMongo"); }
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
            Long subscriptionId = jdbcTemplate.query("SELECT store_subscription_id FROM subscription_checkout_sessions WHERE provider_checkout_id = ?",
                    (rs, rowNum) -> rs.getLong(1), resourceId).stream().findFirst().orElse(null);
            jdbcTemplate.update("INSERT INTO subscription_events (store_subscription_id, provider, provider_event_id, event_type, payload, processed_at) VALUES (?, 'PAYMONGO', ?, ?, ?, CURRENT_TIMESTAMP)",
                    subscriptionId, eventId, eventType, payload);
            String normalizedType = eventType.toLowerCase();
            if (normalizedType.contains("paid") && subscriptionId != null) {
                jdbcTemplate.update("UPDATE subscription_checkout_sessions SET status = 'PAID', completed_at = CURRENT_TIMESTAMP WHERE provider_checkout_id = ?", resourceId);
                jdbcTemplate.update("UPDATE store_subscriptions SET status = 'ACTIVE' WHERE id = ?", subscriptionId);
                BigDecimal amount = jdbcTemplate.queryForObject("SELECT amount FROM subscription_checkout_sessions WHERE provider_checkout_id = ?", BigDecimal.class, resourceId);
                jdbcTemplate.update("INSERT INTO subscription_payments (store_subscription_id, provider, provider_payment_id, amount, status, paid_at) "
                + "VALUES (?, 'PAYMONGO', ?, ?, 'PAID', CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING", subscriptionId, resourceId, amount);
                applyCustomAddons(resourceId, subscriptionId);
            } else if ((normalizedType.contains("failed") || normalizedType.contains("cancel") || normalizedType.contains("expired"))
                    && subscriptionId != null) {
                String status = normalizedType.contains("expired") ? "EXPIRED" : normalizedType.contains("cancel") ? "CANCELLED" : "PAST_DUE";
                String checkoutStatus = normalizedType.contains("expired") ? "EXPIRED" : normalizedType.contains("cancel") ? "CANCELLED" : "FAILED";
                jdbcTemplate.update("UPDATE subscription_checkout_sessions SET status = ? WHERE provider_checkout_id = ?", checkoutStatus, resourceId);
                jdbcTemplate.update("UPDATE store_subscriptions SET status = ? WHERE id = ?", status, subscriptionId);
            }
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook payload"); }
    }

    private void applyCustomAddons(String providerCheckoutId, Long subscriptionId) {
        CheckoutMetadata metadata = jdbcTemplate.query("SELECT purchase_type, purchase_metadata FROM subscription_checkout_sessions WHERE provider_checkout_id = ?",
                (rs, rowNum) -> new CheckoutMetadata(rs.getString("purchase_type"), rs.getString("purchase_metadata")), providerCheckoutId)
                .stream().findFirst().orElse(null);
        if (metadata == null || !"CUSTOM_ADDONS".equals(metadata.purchaseType())) return;
        Long storeId = jdbcTemplate.queryForObject("SELECT store_id FROM store_subscriptions WHERE id = ?", Long.class, subscriptionId);
        String raw = metadata.purchaseMetadata() == null ? "" : metadata.purchaseMetadata();
        int staffSeats = 0;
        List<String> features = List.of();
        for (String part : raw.split(";")) {
            if (part.startsWith("staffSeats=")) staffSeats = Integer.parseInt(part.substring("staffSeats=".length()));
            if (part.startsWith("features=")) features = part.substring("features=".length()).isBlank() ? List.of() : List.of(part.substring("features=".length()).split(","));
        }
        BigDecimal seatPrice = jdbcTemplate.queryForObject("SELECT monthly_price FROM platform_addon_prices WHERE feature_key = 'STAFF_SEAT'", BigDecimal.class);
        jdbcTemplate.update("INSERT INTO store_staff_seats (store_id, seat_count, monthly_unit_price) VALUES (?, ?, ?) ON CONFLICT (store_id) DO UPDATE SET seat_count = store_staff_seats.seat_count + EXCLUDED.seat_count, monthly_unit_price = EXCLUDED.monthly_unit_price, updated_at = CURRENT_TIMESTAMP",
                storeId, staffSeats, seatPrice);
        for (String feature : features) {
            BigDecimal price = jdbcTemplate.queryForObject("SELECT monthly_price FROM platform_addon_prices WHERE feature_key = ?", BigDecimal.class, feature);
            jdbcTemplate.update("INSERT INTO store_feature_addons (store_id, feature_key, monthly_price, active) VALUES (?, ?, ?, TRUE) ON CONFLICT (store_id, feature_key) DO UPDATE SET active = TRUE, monthly_price = EXCLUDED.monthly_price",
                    storeId, feature, price);
        }
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

    private void requireStoreAdmin(Long storeId, Authentication authentication) {
        if (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                && !tenantAccessService.hasStoreRole(authentication, storeId, "STORE_ADMIN")) throw new AccessDeniedException("Store Admin permission is required");
    }

    private record Subscription(Long id, String packageName, BigDecimal monthlyPrice, BigDecimal annualPrice) { }
    private record CheckoutMetadata(String purchaseType, String purchaseMetadata) { }
    public record CheckoutRequest(@NotBlank String billingCycle) { }
    public record CustomCheckoutRequest(@NotBlank String billingCycle, @jakarta.validation.constraints.Min(0) int staffSeats,
                                        List<String> featureKeys) { }
    public record CheckoutView(Long id, String providerCheckoutId, String checkoutUrl, BigDecimal amount, String currency, String status, java.time.Instant createdAt) { }
}
