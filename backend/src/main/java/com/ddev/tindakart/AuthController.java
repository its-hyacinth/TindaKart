package com.ddev.tindakart;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final TenantAccessService tenantAccessService;
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, TenantAccessService tenantAccessService,
                          JdbcTemplate jdbcTemplate, AuditService auditService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tenantAccessService = tenantAccessService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.username().trim();
        if (isLocked(username)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Too many failed attempts. Try again later."));
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            clearFailedAttempts(username);
            auditService.record(username, "LOGIN_SUCCESS", "USER", username, "Authenticated session created");
            return ResponseEntity.ok(userResponse(authentication));
        } catch (BadCredentialsException ex) {
            recordFailedAttempt(username);
            auditService.record(username, "LOGIN_FAILURE", "USER", username, "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    private boolean isLocked(String username) {
        return jdbcTemplate.query("SELECT locked_until FROM login_attempts WHERE LOWER(username) = LOWER(?)",
                (rs, rowNum) -> rs.getObject("locked_until", OffsetDateTime.class), username).stream()
                .findFirst().map(value -> value != null && value.isAfter(OffsetDateTime.now(ZoneOffset.UTC))).orElse(false);
    }

    private void recordFailedAttempt(String username) {
        jdbcTemplate.update("INSERT INTO login_attempts (username, failed_count, first_failed_at, updated_at) VALUES (?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                + "ON CONFLICT (username) DO UPDATE SET failed_count = CASE WHEN login_attempts.first_failed_at < CURRENT_TIMESTAMP - INTERVAL '15 minutes' THEN 1 ELSE login_attempts.failed_count + 1 END, "
                + "first_failed_at = CASE WHEN login_attempts.first_failed_at < CURRENT_TIMESTAMP - INTERVAL '15 minutes' THEN CURRENT_TIMESTAMP ELSE login_attempts.first_failed_at END, "
                + "locked_until = CASE WHEN login_attempts.failed_count + 1 >= 5 THEN CURRENT_TIMESTAMP + INTERVAL '15 minutes' ELSE login_attempts.locked_until END, updated_at = CURRENT_TIMESTAMP", username);
    }

    private void clearFailedAttempts(String username) {
        jdbcTemplate.update("DELETE FROM login_attempts WHERE LOWER(username) = LOWER(?)", username);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return userResponse(authentication);
    }

    @GetMapping("/context")
    public ContextView context(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return new ContextView(session == null ? null : (Long) session.getAttribute("CURRENT_VENDOR_ID"),
                session == null ? null : (Long) session.getAttribute("CURRENT_STORE_ID"));
    }

    @PutMapping("/context")
    public ContextView setContext(@Valid @RequestBody ContextRequest context, Authentication authentication,
                                  HttpServletRequest request) {
        if (!tenantAccessService.storeBelongsToVendor(context.vendorId(), context.storeId())
                || (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))
                && !tenantAccessService.hasStoreAccess(authentication, context.storeId()))) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have access to this vendor/store context");
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("CURRENT_VENDOR_ID", context.vendorId());
        session.setAttribute("CURRENT_STORE_ID", context.storeId());
        return new ContextView(context.vendorId(), context.storeId());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                             Authentication authentication, HttpServletRequest httpRequest) {
        String username = authentication.getName();
        String hash = jdbcTemplate.query("SELECT password_hash FROM users WHERE LOWER(username) = LOWER(?)",
                (rs, rowNum) -> rs.getString("password_hash"), username).stream().findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), hash)) {
            auditService.record(username, "PASSWORD_CHANGE_FAILURE", "USER", username, "Current password did not match");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Current password is incorrect"));
        }
        jdbcTemplate.update("UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE LOWER(username) = LOWER(?)",
                passwordEncoder.encode(request.newPassword()), username);
        auditService.record(username, "PASSWORD_CHANGED", "USER", username, "Password updated");
        HttpSession session = httpRequest.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> userResponse(Authentication authentication) {
        return Map.of("username", authentication.getName(), "roles", authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", "")).toList(),
                "vendors", tenantAccessService.vendorsFor(authentication),
                "stores", tenantAccessService.storesFor(authentication));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }
    public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank @jakarta.validation.constraints.Size(min = 8, max = 255) String newPassword) { }
    public record ContextRequest(@jakarta.validation.constraints.NotNull Long vendorId,
                                 @jakarta.validation.constraints.NotNull Long storeId) { }
    public record ContextView(Long vendorId, Long storeId) { }
}
