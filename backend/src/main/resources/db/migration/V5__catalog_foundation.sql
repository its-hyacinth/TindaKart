CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT categories_vendor_name_unique UNIQUE (vendor_id, name)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(120) NOT NULL,
    unit_type VARCHAR(30) NOT NULL DEFAULT 'PIECE',
    cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    retail_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    bulk_price NUMERIC(12, 2),
    bulk_threshold INTEGER,
    reorder_level INTEGER NOT NULL DEFAULT 0,
    expiration_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT products_vendor_sku_unique UNIQUE (vendor_id, sku),
    CONSTRAINT products_unit_type_check CHECK (unit_type IN ('PIECE', 'PACK', 'BOTTLE', 'KILOGRAM', 'LITER', 'OTHER')),
    CONSTRAINT products_prices_nonnegative CHECK (cost_price >= 0 AND retail_price >= 0 AND (bulk_price IS NULL OR bulk_price >= 0)),
    CONSTRAINT products_bulk_fields_check CHECK ((bulk_price IS NULL AND bulk_threshold IS NULL) OR (bulk_price IS NOT NULL AND bulk_threshold > 0)),
    CONSTRAINT products_reorder_nonnegative CHECK (reorder_level >= 0)
);

CREATE TABLE product_barcodes (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    barcode VARCHAR(120) NOT NULL UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX products_vendor_name_idx ON products(vendor_id, name);
CREATE INDEX product_barcodes_product_idx ON product_barcodes(product_id);
