CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(40),
    address VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT suppliers_vendor_name_unique UNIQUE (vendor_id, name)
);

CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    expected_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT deliveries_status_check CHECK (status IN ('UPCOMING', 'IN_TRANSIT', 'RECEIVED', 'COMPLETED'))
);

CREATE TABLE delivery_items (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity_ordered NUMERIC(12, 3) NOT NULL,
    quantity_received NUMERIC(12, 3) NOT NULL DEFAULT 0,
    cost_price NUMERIC(12, 2) NOT NULL,
    retail_price NUMERIC(12, 2),
    bulk_price NUMERIC(12, 2),
    expiration_date DATE,
    batch_reference VARCHAR(120),
    CONSTRAINT delivery_items_quantity_check CHECK (quantity_ordered > 0 AND quantity_received >= 0 AND quantity_received <= quantity_ordered),
    CONSTRAINT delivery_items_cost_check CHECK (cost_price >= 0)
);

CREATE INDEX deliveries_store_date_idx ON deliveries(store_id, expected_date);
CREATE INDEX delivery_items_delivery_idx ON delivery_items(delivery_id);
