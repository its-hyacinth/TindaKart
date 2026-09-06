ALTER TABLE business_settings
    ADD COLUMN pos_payment_methods VARCHAR(30)[] NOT NULL
        DEFAULT ARRAY['CASH', 'CARD', 'EWALLET', 'CREDIT']::VARCHAR[],
    ADD COLUMN camera_scanning_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN receipt_print_mode VARCHAR(20) NOT NULL DEFAULT 'BROWSER';

ALTER TABLE business_settings
    ADD CONSTRAINT business_settings_payment_methods_check
        CHECK (cardinality(pos_payment_methods) > 0),
    ADD CONSTRAINT business_settings_print_mode_check
        CHECK (receipt_print_mode IN ('BROWSER', 'THERMAL_BRIDGE', 'MANUAL'));
