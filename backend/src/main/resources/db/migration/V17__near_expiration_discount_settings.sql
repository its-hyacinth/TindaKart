ALTER TABLE business_settings
    ADD COLUMN near_expiration_discount_percent NUMERIC(5, 2) NOT NULL DEFAULT 0;

ALTER TABLE business_settings
    ADD CONSTRAINT business_settings_expiration_discount_check
        CHECK (near_expiration_discount_percent >= 0 AND near_expiration_discount_percent <= 100);
