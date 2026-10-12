ALTER TABLE transactions ADD CONSTRAINT uk_transactions_external_id UNIQUE (external_id);
ALTER TABLE invoice_lines ADD CONSTRAINT uk_invoice_line_transaction UNIQUE (transaction_id);
CREATE INDEX idx_transactions_created ON transactions (created_at, id);
CREATE INDEX idx_invoices_created ON invoices (created_at, id);
CREATE INDEX idx_transaction_taxes_transaction ON transaction_taxes (transaction_id);
CREATE INDEX idx_transaction_fees_transaction ON transaction_fees (transaction_id);
CREATE INDEX idx_invoice_lines_invoice ON invoice_lines (invoice_id);
CREATE INDEX idx_invoice_taxes_invoice ON invoice_taxes (invoice_id);
CREATE INDEX idx_invoice_fees_invoice ON invoice_fees (invoice_id);
