CREATE SEQUENCE debt_payment_receipt_seq START WITH 1;
ALTER TABLE debt_payments ADD COLUMN receipt_number VARCHAR(80);
CREATE UNIQUE INDEX debt_payments_receipt_number_idx ON debt_payments (receipt_number) WHERE receipt_number IS NOT NULL;
