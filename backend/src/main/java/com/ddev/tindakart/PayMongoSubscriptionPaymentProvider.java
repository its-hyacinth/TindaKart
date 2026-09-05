package com.ddev.tindakart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PayMongoSubscriptionPaymentProvider implements SubscriptionPaymentProvider {
    private final RestClient restClient; private final ObjectMapper objectMapper; private final String secretKey;
    public PayMongoSubscriptionPaymentProvider(ObjectMapper objectMapper, @Value("${tindakart.billing.paymongo-secret-key:}") String secretKey, @Value("${tindakart.billing.paymongo-base-url:https://api.paymongo.com}") String baseUrl) {
        this.objectMapper = objectMapper; this.secretKey = secretKey; this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }
    @Override public boolean configured() { return !secretKey.isBlank(); }
    @Override public Checkout createCheckout(String packageName, BigDecimal amount) {
        String escaped = packageName.replace("\\", "\\\\").replace("\"", "\\\"");
        String payload = "{\"data\":{\"attributes\":{\"line_items\":[{\"currency\":\"PHP\",\"amount\":" + amount.movePointRight(2).intValueExact() + ",\"description\":\"TindaKart " + escaped + " subscription\",\"name\":\"TindaKart subscription\",\"quantity\":1}],\"payment_method_types\":[\"card\",\"gcash\",\"paymaya\"],\"description\":\"TindaKart subscription\",\"send_email_receipt\":false,\"show_line_items\":true}}}";
        try { String response = restClient.post().uri("/v1/checkout_sessions").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8))).body(payload).retrieve().body(String.class); JsonNode data = objectMapper.readTree(response).path("data"); String id = data.path("id").asText(), url = data.path("attributes").path("checkout_url").asText(); if (id.isBlank() || url.isBlank()) throw new IllegalStateException(); return new Checkout(id, url); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PayMongo checkout could not be created"); }
    }
}
