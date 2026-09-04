CREATE TABLE inventory_batches (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    batch_reference VARCHAR(120),
    quantity_received NUMERIC(12, 3) NOT NULL,
    quantity_on_hand NUMERIC(12, 3) NOT NULL,
    cost_price NUMERIC(12, 2) NOT NULL,
    retail_price NUMERIC(12, 2),
    bulk_price NUMERIC(12, 2),
    expiration_date DATE,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_batches_quantities_check CHECK (quantity_received > 0 AND quantity_on_hand >= 0 AND quantity_on_hand <= quantity_received),
    CONSTRAINT inventory_batches_prices_check CHECK (cost_price >= 0 AND (retail_price IS NULL OR retail_price >= 0) AND (bulk_price IS NULL OR bulk_price >= 0))
);

CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    batch_id BIGINT REFERENCES inventory_batches(id) ON DELETE SET NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity_delta NUMERIC(12, 3) NOT NULL,
    reason VARCHAR(500),
    reference_type VARCHAR(50),
    reference_id VARCHAR(120),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_movements_type_check CHECK (movement_type IN ('RECEIVE', 'SALE', 'ADJUSTMENT', 'RETURN', 'VOID')),
    CONSTRAINT inventory_movements_delta_check CHECK (quantity_delta <> 0)
);

CREATE INDEX inventory_batches_store_product_idx ON inventory_batches(store_id, product_id);
CREATE INDEX inventory_batches_expiration_idx ON inventory_batches(expiration_date);
CREATE INDEX inventory_movements_store_product_idx ON inventory_movements(store_id, product_id, created_at);
