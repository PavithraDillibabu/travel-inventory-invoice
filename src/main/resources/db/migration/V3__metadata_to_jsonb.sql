ALTER TABLE transactions
ALTER COLUMN metadata TYPE JSONB USING metadata::jsonb;

ALTER TABLE invoice_lines
ALTER COLUMN metadata TYPE JSONB USING metadata::jsonb;
