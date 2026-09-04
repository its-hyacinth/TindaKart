package com.ddev.tindakart;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public BootstrapAdminRunner(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
            @Value("${tindakart.bootstrap.admin-username:}") String username,
            @Value("${tindakart.bootstrap.admin-password:}") String password) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) return;
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (userCount != null && userCount > 0) return;
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?) RETURNING id",
                Long.class, username.trim(), passwordEncoder.encode(password), username.trim());
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) "
                + "SELECT ?, id FROM roles WHERE name = 'SUPER_ADMIN'", userId);
        System.out.println("TindaKart: initial SUPER_ADMIN account created for " + username.trim());
    }
}
