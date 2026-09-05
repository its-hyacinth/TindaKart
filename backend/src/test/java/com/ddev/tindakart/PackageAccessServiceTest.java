package com.ddev.tindakart;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PackageAccessServiceTest {
    @Mock JdbcTemplate jdbcTemplate;

    @Test
    void superAdminCanUseFeaturesWithoutVendorSubscription() {
        var service = new PackageAccessService(jdbcTemplate);
        var authentication = new UsernamePasswordAuthenticationToken("admin", "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        assertDoesNotThrow(() -> service.requireFeature(authentication, 99L, "REPORTS"));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void enabledSubscriptionFeatureIsAllowed() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(7L), eq("POS"))).thenReturn(1);
        var authentication = new UsernamePasswordAuthenticationToken("vendor-admin", "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_VENDOR_ADMIN")));

        assertDoesNotThrow(() -> new PackageAccessService(jdbcTemplate).requireFeature(authentication, 7L, "POS"));
    }

    @Test
    void missingSubscriptionFeatureIsForbidden() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(7L), eq("REPORTS"))).thenReturn(0);
        var authentication = new UsernamePasswordAuthenticationToken("cashier", "ignored",
                List.of(new SimpleGrantedAuthority("ROLE_CASHIER")));

        assertThrows(ResponseStatusException.class,
                () -> new PackageAccessService(jdbcTemplate).requireFeature(authentication, 7L, "REPORTS"));
    }
}
