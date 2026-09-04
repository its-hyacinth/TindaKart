package com.ddev.tindakart;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbcTemplate;

    public AppUserDetailsService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<UserDetails> users = jdbcTemplate.query(
                "SELECT username, password_hash, enabled FROM users WHERE LOWER(username) = LOWER(?)",
                (rs, rowNum) -> User.withUsername(rs.getString("username"))
                        .password(rs.getString("password_hash"))
                        .disabled(!rs.getBoolean("enabled"))
                        .authorities(loadAuthorities(rs.getString("username")))
                        .build(), username);
        if (users.isEmpty()) throw new UsernameNotFoundException("User not found");
        return users.getFirst();
    }

    private List<GrantedAuthority> loadAuthorities(String username) {
        return jdbcTemplate.query(
                "SELECT r.name FROM roles r JOIN user_roles ur ON ur.role_id = r.id "
                        + "JOIN users u ON u.id = ur.user_id WHERE LOWER(u.username) = LOWER(?)",
                (rs, rowNum) -> new SimpleGrantedAuthority("ROLE_" + rs.getString("name")), username);
    }
}
