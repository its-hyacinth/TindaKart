package com.ddev.tindakart;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
import org.springframework.transaction.annotation.Transactional;
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
    private final TenantContextService tenantContextService;

    public AuthController(AuthenticationManager authenticationManager, TenantAccessService tenantAccessService,
                          JdbcTemplate jdbcTemplate, AuditService auditService, PasswordEncoder passwordEncoder,
                          TenantContextService tenantContextService) {
        this.authenticationManager = authenticationManager;
        this.tenantAccessService = tenantAccessService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.tenantContextService = tenantContextService;
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

    @PostMapping("/register-store")
    @Transactional
    public ResponseEntity<?> registerStore(@Valid @RequestBody StoreRegistrationRequest request) {
        Long userId;
        try {
            userId = jdbcTemplate.queryForObject("INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?) RETURNING id",
                    Long.class, request.username().trim(), passwordEncoder.encode(request.password()), request.displayName().trim());
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already exists"));
        }
        Long storeId = jdbcTemplate.queryForObject("INSERT INTO stores (name, code) VALUES (?, ?) RETURNING id",
                Long.class, request.storeName().trim(), request.storeCode().trim().toUpperCase());
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE name = 'STORE_ADMIN' ON CONFLICT DO NOTHING", userId);
        jdbcTemplate.update("INSERT INTO store_user_roles (user_id, store_id, role_id) SELECT ?, ?, id FROM roles WHERE name = 'STORE_ADMIN'", userId, storeId);
        Long freePackageId = jdbcTemplate.queryForObject("SELECT id FROM packages WHERE name = 'Free' AND active = TRUE", Long.class);
        jdbcTemplate.update("INSERT INTO store_subscriptions (store_id, package_id, status) VALUES (?, ?, 'ACTIVE')", storeId, freePackageId);
        jdbcTemplate.update("INSERT INTO store_staff_seats (store_id, seat_count, monthly_unit_price) VALUES (?, 0, 0) ON CONFLICT (store_id) DO NOTHING", storeId);
        auditService.record(request.username().trim(), "STORE_REGISTERED", "STORE", storeId.toString(), request.storeName().trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("username", request.username().trim(), "storeId", storeId, "plan", "Free"));
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
    public ContextView context(HttpServletRequest request, Authentication authentication) {
        return tenantContextService.resolve(request, authentication);
    }

    @PutMapping("/context")
    public ContextView setContext(@Valid @RequestBody ContextRequest context, Authentication authentication,
                                  HttpServletRequest request) {
        return tenantContextService.set(request, authentication, context.storeId());
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
                "stores", tenantAccessService.storesFor(authentication));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }
    public record StoreRegistrationRequest(@NotBlank @Size(max = 120) String username,
                                            @NotBlank @Size(min = 8, max = 255) String password,
                                            @NotBlank @Size(max = 255) String displayName,
                                            @NotBlank @Size(max = 255) String storeName,
                                            @NotBlank @Size(max = 80) String storeCode) { }
    public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank @jakarta.validation.constraints.Size(min = 8, max = 255) String newPassword) { }
    public record ContextRequest(@jakarta.validation.constraints.NotNull Long storeId) { }
    public record ContextView(Long storeId) { }
}
