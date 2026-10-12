# Architecture

## Transactions and identity

Each transaction has a generated string UUID primary key and a client-supplied `external_id`. PostgreSQL enforces `UNIQUE (external_id)` through `uk_transactions_external_id`; the service also checks for an existing value to return a quick 409 for ordinary duplicates. That check alone cannot prevent a race, so the database constraint is authoritative. If concurrent requests both pass the check, only one insert can commit; the other constraint violation is translated to HTTP 409 by `ApiExceptionHandler`. This guarantees one stored row per external ID.

## Relational data and typed metadata

Amounts are stored as integer minor units (`total_minor`, `amount_minor`) with a three-character currency code. Taxes and fees are distinct concepts and tables: `transaction_taxes` has name, optional decimal rate, and amount; `transaction_fees` has name and amount. Both reference their transaction. Invoice totals and summaries use corresponding `invoice_taxes` and `invoice_fees` tables.

Travel-specific attributes use typed metadata DTOs serialized as JSON text in `transactions.metadata`; there are no separate relational tables for each travel type. Flight metadata contains origin/destination (OD), fare, and coupons; each coupon carries its flight number and segment details. Hotel metadata contains the hotel name, address, dates, room rate, and extras. Rail metadata contains an ordered list of legs. `navan_fee` metadata contains `fee_kind` (`trip_fee` or `agent_call_fee`), description, and optional related external ID. The transaction `type` selects which metadata DTO is read and written.

## Invoice snapshots

An invoice is created from transaction IDs. The service locks those transactions in sorted ID order, checks existence, currency consistency, and prior invoicing, then builds the invoice from their current values. `invoice_lines` stores a copy of each transaction's type, external ID, currency, time, total, metadata, and tax/fee snapshots. Invoice tax and fee rows are also stored separately for summary totals. These are snapshots, so later changes to a source transaction cannot alter an existing invoice. A unique constraint on `invoice_lines.transaction_id` prevents a transaction from appearing on more than one invoice. Updating an invoice rebuilds its snapshot; deleting the invoice removes its invoice rows but leaves source transactions intact.

## High Volume

Under high traffic, database contention and expensive queries are likely to become the first performance bottlenecks. The API addresses these risks through database constraints, row-level locking, indexes, and bounded pagination.

- **Concurrent PATCH:** Simultaneous updates to the same transaction can cause lock contention and increased response times. Database transactions and row-level locking protect data consistency, including preventing modifications to invoiced transactions.
- **Duplicate POST:** Concurrent requests with the same `external_id` can bypass application-level duplicate checks. A database-level unique constraint guarantees that only one transaction is persisted, with duplicate requests handled through HTTP 409.
- **Large lists:** Unbounded queries can increase database load, memory usage, and response latency. Pagination is enforced at the database level (default 50, maximum 100). Indexes on primary keys, `(created_at, id)`, `external_id`, and invoice-line `transaction_id` support efficient lookups and listing.

These safeguards maintain data integrity and predictable resource usage under load. As a future enhancement, **cursor/keyset pagination** can improve performance for large datasets.