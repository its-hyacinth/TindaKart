package com.ddev.tindakart;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test-integration")
class DatabaseAuditIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void readOnlyDataQualityAuditRunsAgainstCurrentSchema() {
        List<Map<String, Object>> incompleteProducts = jdbcTemplate.queryForList("SELECT p.id, p.vendor_id, p.name, p.sku "
                + "FROM products p LEFT JOIN product_barcodes pb ON pb.product_id = p.id "
                + "WHERE NULLIF(BTRIM(p.name), '') IS NULL OR NULLIF(BTRIM(p.sku), '') IS NULL OR pb.id IS NULL "
                + "ORDER BY p.vendor_id, p.id");
        List<Map<String, Object>> duplicateBarcodes = jdbcTemplate.queryForList("SELECT barcode, COUNT(DISTINCT product_id) AS product_count "
                + "FROM product_barcodes GROUP BY barcode HAVING COUNT(DISTINCT product_id) > 1 ORDER BY barcode");
        List<Map<String, Object>> orphanDebt = jdbcTemplate.queryForList("SELECT da.id AS debt_account_id, da.vendor_id, da.customer_id "
                + "FROM debt_accounts da LEFT JOIN customer_profiles cp ON cp.id = da.customer_id WHERE cp.id IS NULL");
        List<Map<String, Object>> crossVendorDebt = jdbcTemplate.queryForList("SELECT cs.id AS credit_sale_id, da.vendor_id AS account_vendor_id, cp.vendor_id AS customer_vendor_id "
                + "FROM credit_sales cs JOIN debt_accounts da ON da.id = cs.debt_account_id JOIN customer_profiles cp ON cp.id = da.customer_id "
                + "WHERE da.vendor_id <> cp.vendor_id");
        List<Map<String, Object>> productsWithSales = jdbcTemplate.queryForList("SELECT p.id, p.vendor_id, p.name, COUNT(DISTINCT si.sale_id) AS sale_count "
                + "FROM products p JOIN sale_items si ON si.product_id = p.id GROUP BY p.id, p.vendor_id, p.name ORDER BY p.vendor_id, p.id");

        System.out.printf("DATA_AUDIT incompleteProducts=%d duplicateBarcodes=%d orphanDebt=%d crossVendorDebt=%d productsWithSales=%d%n",
                incompleteProducts.size(), duplicateBarcodes.size(), orphanDebt.size(), crossVendorDebt.size(), productsWithSales.size());
        assertTrue(incompleteProducts.size() >= 0 && duplicateBarcodes.size() >= 0 && orphanDebt.size() >= 0
                && crossVendorDebt.size() >= 0 && productsWithSales.size() >= 0);
    }
}
