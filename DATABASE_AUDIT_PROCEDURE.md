# TindaKart database audit procedure

These checks are read-only. Run them against a verified backup or a disposable copy before migration or client rollout. They do not modify data and do not replace the backup/restore procedure.

## Product quality

```sql
-- Products without a barcode, or with an incomplete searchable identity.
SELECT p.id, p.vendor_id, p.name, p.sku
FROM products p
LEFT JOIN product_barcodes pb ON pb.product_id = p.id
WHERE NULLIF(BTRIM(p.name), '') IS NULL
   OR NULLIF(BTRIM(p.sku), '') IS NULL
   OR pb.id IS NULL
ORDER BY p.vendor_id, p.id;

-- Barcode values used by more than one product.
SELECT barcode, COUNT(DISTINCT product_id) AS product_count
FROM product_barcodes
GROUP BY barcode
HAVING COUNT(DISTINCT product_id) > 1
ORDER BY barcode;
```

## Debt/customer integrity

```sql
-- The foreign key should make this empty; retain the check for legacy imports.
SELECT da.id AS debt_account_id, da.vendor_id, da.customer_id
FROM debt_accounts da
LEFT JOIN customer_profiles cp ON cp.id = da.customer_id
WHERE cp.id IS NULL;

-- Credit sales whose account customer belongs to a different vendor.
SELECT cs.id AS credit_sale_id, da.vendor_id AS account_vendor_id, cp.vendor_id AS customer_vendor_id
FROM credit_sales cs
JOIN debt_accounts da ON da.id = cs.debt_account_id
JOIN customer_profiles cp ON cp.id = da.customer_id
WHERE da.vendor_id <> cp.vendor_id;
```

## Sales-history dependencies

```sql
-- Products that already have sales history and must not be hard-deleted.
SELECT p.id, p.vendor_id, p.name, COUNT(DISTINCT si.sale_id) AS sale_count
FROM products p
JOIN sale_items si ON si.product_id = p.id
GROUP BY p.id, p.vendor_id, p.name
ORDER BY p.vendor_id, p.id;
```

Record the result with the audit date, database/environment, reviewer, and migration decision. Preserve IDs when client reporting or external references depend on them; otherwise document any mapping before importing data.

The current migration schema uses foreign keys and restricted deletes for these relationships. That prevents accidental orphaning, but it does not decide whether a product is incomplete or whether a client wants its historical IDs preserved.
