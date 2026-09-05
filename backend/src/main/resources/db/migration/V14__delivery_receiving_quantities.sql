ALTER TABLE delivery_items ADD COLUMN quantity_missing NUMERIC(12, 3) NOT NULL DEFAULT 0;
ALTER TABLE delivery_items ADD COLUMN quantity_damaged NUMERIC(12, 3) NOT NULL DEFAULT 0;
ALTER TABLE delivery_items ADD CONSTRAINT delivery_items_receiving_quantities_check
    CHECK (quantity_received + quantity_missing + quantity_damaged <= quantity_ordered
           AND quantity_missing >= 0 AND quantity_damaged >= 0);
