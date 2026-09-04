CREATE TABLE customer_profiles (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(40),
    address VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE debt_accounts (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    customer_id BIGINT NOT NULL REFERENCES customer_profiles(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT debt_accounts_status_check CHECK (status IN ('OPEN', 'PAID', 'OVERDUE')),
    CONSTRAINT debt_accounts_customer_unique UNIQUE (vendor_id, customer_id)
);

CREATE TABLE credit_sales (
    id BIGSERIAL PRIMARY KEY,
    debt_account_id BIGINT NOT NULL REFERENCES debt_accounts(id) ON DELETE RESTRICT,
    sale_id BIGINT NOT NULL UNIQUE REFERENCES sales(id) ON DELETE RESTRICT,
    principal_amount NUMERIC(12, 2) NOT NULL,
    due_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT credit_sales_amount_positive CHECK (principal_amount > 0)
);

CREATE TABLE debt_payments (
    id BIGSERIAL PRIMARY KEY,
    debt_account_id BIGINT NOT NULL REFERENCES debt_accounts(id) ON DELETE RESTRICT,
    amount NUMERIC(12, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL DEFAULT 'CASH',
    notes VARCHAR(500),
    paid_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT debt_payments_amount_positive CHECK (amount > 0)
);

CREATE TABLE debt_payment_allocations (
    payment_id BIGINT NOT NULL REFERENCES debt_payments(id) ON DELETE CASCADE,
    credit_sale_id BIGINT NOT NULL REFERENCES credit_sales(id) ON DELETE RESTRICT,
    amount NUMERIC(12, 2) NOT NULL,
    PRIMARY KEY (payment_id, credit_sale_id),
    CONSTRAINT debt_allocations_amount_positive CHECK (amount > 0)
);

CREATE INDEX credit_sales_account_due_idx ON credit_sales(debt_account_id, due_date);
CREATE INDEX debt_payments_account_date_idx ON debt_payments(debt_account_id, paid_at);

ALTER TABLE payments DROP CONSTRAINT payments_method_check;
ALTER TABLE payments ADD CONSTRAINT payments_method_check CHECK (payment_method IN ('CASH', 'CARD', 'EWALLET', 'CREDIT'));
