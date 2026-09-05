package com.ddev.tindakart;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class PermissionAccessServiceTest {
    @Mock JdbcTemplate jdbcTemplate;

    @Test
    void superAdminBypassesTenantPermissionQuery() {
        var authentication = authentication("ROLE_SUPER_ADMIN");
        var service = new PermissionAccessService(jdbcTemplate);

        assertTrue(service.has(authentication, 1L, 2L, "POS_USE"));
        assertDoesNotThrow(() -> service.require(authentication, 1L, 2L, "POS_USE"));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void assignedPermissionIsAllowed() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(2L), eq(1L), eq("cashier"), eq("POS_USE")))
                .thenReturn(1);
        var service = new PermissionAccessService(jdbcTemplate);

        assertTrue(service.has(authentication("ROLE_CASHIER", "cashier"), 1L, 2L, "POS_USE"));
    }

    @Test
    void missingPermissionIsDenied() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(2L), eq(1L), eq("cashier"), eq("REPORTS_VIEW")))
                .thenReturn(0);
        var service = new PermissionAccessService(jdbcTemplate);

        assertFalse(service.has(authentication("ROLE_CASHIER", "cashier"), 1L, 2L, "REPORTS_VIEW"));
        assertThrows(AccessDeniedException.class,
                () -> service.require(authentication("ROLE_CASHIER", "cashier"), 1L, 2L, "REPORTS_VIEW"));
    }

    private UsernamePasswordAuthenticationToken authentication(String authority) {
        return authentication(authority, "user");
    }

    private UsernamePasswordAuthenticationToken authentication(String authority, String username) {
        return new UsernamePasswordAuthenticationToken(username, "ignored",
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
