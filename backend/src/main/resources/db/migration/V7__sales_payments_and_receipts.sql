CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    cashier_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    receipt_number VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    subtotal NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sales_status_check CHECK (status IN ('COMPLETED', 'VOIDED')),
    CONSTRAINT sales_amounts_check CHECK (subtotal >= 0 AND discount_amount >= 0 AND vat_amount >= 0 AND total_amount >= 0)
);

CREATE TABLE sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    batch_id BIGINT NOT NULL REFERENCES inventory_batches(id) ON DELETE RESTRICT,
    quantity NUMERIC(12, 3) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    subtotal NUMERIC(12, 2) NOT NULL,
    CONSTRAINT sale_items_quantity_check CHECK (quantity > 0),
    CONSTRAINT sale_items_amounts_check CHECK (unit_price >= 0 AND discount_amount >= 0 AND subtotal >= 0)
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE RESTRICT,
    payment_method VARCHAR(30) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    amount_tendered NUMERIC(12, 2),
    change_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT payments_method_check CHECK (payment_method IN ('CASH', 'CARD', 'EWALLET')),
    CONSTRAINT payments_amount_check CHECK (amount > 0 AND change_amount >= 0)
);

CREATE TABLE receipts (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL UNIQUE REFERENCES sales(id) ON DELETE RESTRICT,
    receipt_number VARCHAR(80) NOT NULL UNIQUE,
    printed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX sales_store_created_idx ON sales(store_id, created_at);
CREATE INDEX sale_items_product_idx ON sale_items(product_id);
