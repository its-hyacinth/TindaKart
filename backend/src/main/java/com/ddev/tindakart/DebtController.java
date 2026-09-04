package com.ddev.tindakart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vendors/{vendorId}/debt")
public class DebtController {
    private final JdbcTemplate jdbcTemplate;
    private final TenantAccessService tenantAccessService;
    private final PackageAccessService packageAccessService;

    public DebtController(JdbcTemplate jdbcTemplate, TenantAccessService tenantAccessService, PackageAccessService packageAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantAccessService = tenantAccessService;
        this.packageAccessService = packageAccessService;
    }

    @GetMapping("/customers")
    public List<CustomerView> customers(@PathVariable Long vendorId, Authentication authentication) {
        requireVendorAccess(vendorId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "DEBT");
        return jdbcTemplate.query("SELECT id, name, phone, address FROM customer_profiles WHERE vendor_id = ? ORDER BY name",
                (rs, rowNum) -> new CustomerView(rs.getLong("id"), rs.getString("name"), rs.getString("phone"), rs.getString("address")), vendorId);
    }

    @PostMapping("/customers")
    public ResponseEntity<CustomerView> createCustomer(@PathVariable Long vendorId, @Valid @RequestBody CustomerRequest request,
                                                        Authentication authentication) {
        requireVendorAdmin(vendorId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "DEBT");
        Long id = jdbcTemplate.queryForObject("INSERT INTO customer_profiles (vendor_id, name, phone, address) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, vendorId, request.name().trim(), blankToNull(request.phone()), blankToNull(request.address()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CustomerView(id, request.name().trim(), request.phone(), request.address()));
    }

    @GetMapping("/accounts")
    public List<DebtView> accounts(@PathVariable Long vendorId, Authentication authentication) {
        requireVendorAccess(vendorId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "DEBT");
        return jdbcTemplate.query("SELECT da.id, cp.id AS customer_id, cp.name, cp.phone, da.status, "
                        + "COALESCE((SELECT SUM(principal_amount) FROM credit_sales cs WHERE cs.debt_account_id = da.id), 0) AS total_credit, "
                        + "COALESCE((SELECT SUM(dpa.amount) FROM debt_payment_allocations dpa JOIN credit_sales cs2 ON cs2.id = dpa.credit_sale_id "
                        + "WHERE cs2.debt_account_id = da.id), 0) AS total_paid FROM debt_accounts da JOIN customer_profiles cp ON cp.id = da.customer_id "
                        + "WHERE da.vendor_id = ? ORDER BY cp.name", (rs, rowNum) -> debt(rs), vendorId);
    }

    @GetMapping("/accounts/{accountId}/payments")
    public List<PaymentView> payments(@PathVariable Long vendorId, @PathVariable Long accountId, Authentication authentication) {
        requireVendorAccess(vendorId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "DEBT");
        requireAccount(vendorId, accountId);
        return jdbcTemplate.query("SELECT id, amount, payment_method, notes, paid_at FROM debt_payments "
                        + "WHERE debt_account_id = ? ORDER BY paid_at DESC", (rs, rowNum) -> new PaymentView(rs.getLong("id"),
                        rs.getBigDecimal("amount"), rs.getString("payment_method"), rs.getString("notes"), rs.getTimestamp("paid_at").toInstant()), accountId);
    }

    @PostMapping("/accounts/{accountId}/payments")
    @Transactional
    public PaymentView recordPayment(@PathVariable Long vendorId, @PathVariable Long accountId,
                                     @Valid @RequestBody PaymentRequest request, Authentication authentication) {
        requireDebtWrite(vendorId, authentication);
        packageAccessService.requireFeature(authentication, vendorId, "DEBT");
        requireAccount(vendorId, accountId);
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        List<CreditBalance> balances = jdbcTemplate.query("SELECT cs.id, cs.principal_amount - COALESCE((SELECT SUM(amount) FROM debt_payment_allocations WHERE credit_sale_id = cs.id), 0) AS balance "
                        + "FROM credit_sales cs WHERE cs.debt_account_id = ? AND cs.principal_amount > COALESCE((SELECT SUM(amount) FROM debt_payment_allocations WHERE credit_sale_id = cs.id), 0) "
                        + "ORDER BY cs.due_date, cs.id FOR UPDATE", (rs, rowNum) -> new CreditBalance(rs.getLong("id"), rs.getBigDecimal("balance")), accountId);
        BigDecimal outstanding = balances.stream().map(CreditBalance::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.compareTo(outstanding) > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment exceeds outstanding balance");
        Long paymentId = jdbcTemplate.queryForObject("INSERT INTO debt_payments (debt_account_id, amount, payment_method, notes, recorded_by) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, accountId, amount, request.paymentMethod(), blankToNull(request.notes()), userId(authentication));
        BigDecimal remaining = amount;
        for (CreditBalance balance : balances) {
            if (remaining.signum() == 0) break;
            BigDecimal allocation = remaining.min(balance.balance());
            jdbcTemplate.update("INSERT INTO debt_payment_allocations (payment_id, credit_sale_id, amount) VALUES (?, ?, ?)", paymentId, balance.creditSaleId(), allocation);
            remaining = remaining.subtract(allocation);
        }
        jdbcTemplate.update("UPDATE debt_accounts SET status = CASE WHEN ? = (SELECT COALESCE(SUM(principal_amount), 0) - COALESCE((SELECT SUM(amount) FROM debt_payment_allocations dpa JOIN credit_sales cs2 ON cs2.id = dpa.credit_sale_id WHERE cs2.debt_account_id = ?), 0) FROM credit_sales WHERE debt_account_id = ?) THEN 'PAID' ELSE 'OPEN' END WHERE id = ?",
                BigDecimal.ZERO, accountId, accountId, accountId);
        return jdbcTemplate.query("SELECT id, amount, payment_method, notes, paid_at FROM debt_payments WHERE id = ?",
                (rs, rowNum) -> new PaymentView(rs.getLong("id"), rs.getBigDecimal("amount"), rs.getString("payment_method"), rs.getString("notes"), rs.getTimestamp("paid_at").toInstant()), paymentId)
                .getFirst();
    }

    private DebtView debt(java.sql.ResultSet rs) throws java.sql.SQLException {
        BigDecimal total = rs.getBigDecimal("total_credit");
        BigDecimal paid = rs.getBigDecimal("total_paid");
        return new DebtView(rs.getLong("id"), rs.getLong("customer_id"), rs.getString("name"), rs.getString("phone"),
                rs.getString("status"), total, paid, total.subtract(paid).setScale(2, RoundingMode.HALF_UP));
    }

    private void requireAccount(Long vendorId, Long accountId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM debt_accounts WHERE id = ? AND vendor_id = ?", Integer.class, accountId, vendorId);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Debt account not found");
    }

    private void requireDebtWrite(Long vendorId, Authentication authentication) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN") || tenantAccessService.hasVendorRole(authentication, vendorId, "VENDOR_ADMIN")) return;
        boolean allowed = tenantAccessService.vendorsFor(authentication).stream().anyMatch(vendor -> vendor.id().equals(vendorId));
        if (!allowed) throw new AccessDeniedException("Debt permission is required");
    }

    private void requireVendorAdmin(Long vendorId, Authentication authentication) {
        if (!hasRole(authentication, "ROLE_SUPER_ADMIN") && !tenantAccessService.hasVendorRole(authentication, vendorId, "VENDOR_ADMIN")) {
            throw new AccessDeniedException("Vendor Admin permission is required");
        }
    }

    private void requireVendorAccess(Long vendorId, Authentication authentication) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) return;
        if (tenantAccessService.vendorsFor(authentication).stream().noneMatch(vendor -> vendor.id().equals(vendorId))) {
            throw new AccessDeniedException("You do not have access to this vendor");
        }
    }

    private boolean hasRole(Authentication authentication, String role) { return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority())); }
    private Long userId(Authentication authentication) { return jdbcTemplate.queryForObject("SELECT id FROM users WHERE LOWER(username) = LOWER(?)", Long.class, authentication.getName()); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private record CreditBalance(Long creditSaleId, BigDecimal balance) { }
    public record CustomerView(Long id, String name, String phone, String address) { }
    public record DebtView(Long id, Long customerId, String customerName, String phone, String status,
                           BigDecimal totalCredit, BigDecimal totalPaid, BigDecimal balance) { }
    public record PaymentView(Long id, BigDecimal amount, String paymentMethod, String notes, java.time.Instant paidAt) { }
    public record CustomerRequest(@NotBlank @Size(max = 255) String name, @Size(max = 40) String phone, @Size(max = 500) String address) { }
    public record PaymentRequest(@NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank String paymentMethod, @Size(max = 500) String notes) { }
}
