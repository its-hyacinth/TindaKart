CREATE TABLE business_settings (
    vendor_id BIGINT PRIMARY KEY REFERENCES vendors(id) ON DELETE CASCADE,
    business_name VARCHAR(255),
    business_address VARCHAR(500),
    tin VARCHAR(80),
    vat_registered BOOLEAN NOT NULL DEFAULT FALSE,
    vat_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    near_expiration_days INTEGER NOT NULL DEFAULT 30,
    receipt_footer VARCHAR(500),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT business_settings_vat_check CHECK (vat_rate >= 0 AND vat_rate <= 100),
    CONSTRAINT business_settings_expiration_check CHECK (near_expiration_days >= 0)
);
