package com.ddev.tindakart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicPackageController {
    private final JdbcTemplate jdbcTemplate;

    public PublicPackageController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @GetMapping("/packages")
    public List<PublicPackage> listPackages() {
        return jdbcTemplate.query("SELECT id, name, description, monthly_price, annual_price FROM packages WHERE active = TRUE ORDER BY monthly_price, name",
                (rs, rowNum) -> new PublicPackage(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                        rs.getBigDecimal("monthly_price"), rs.getBigDecimal("annual_price"),
                        jdbcTemplate.query("SELECT feature_key FROM package_features WHERE package_id = ? AND enabled = TRUE ORDER BY feature_key",
                                (featureRs, featureRow) -> featureRs.getString("feature_key"), rs.getLong("id")),
                        jdbcTemplate.query("SELECT limit_key, limit_value FROM package_limits WHERE package_id = ? ORDER BY limit_key",
                                (limitRs, limitRow) -> Map.entry(limitRs.getString("limit_key"), limitRs.getInt("limit_value")), rs.getLong("id")).stream()
                                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
    }

    public record PublicPackage(Long id, String name, String description, BigDecimal monthlyPrice,
                                BigDecimal annualPrice, List<String> features, Map<String, Integer> limits) { }
}
