# Travel Inventory Invoice API

A Spring Boot REST API designed to manage travel-related transactions, including flights, hotels, rail bookings, and Navan service fees. The application uses PostgreSQL for data persistence and supports invoice generation with immutable snapshots

## Requirements

- Java 17 or later
- PostgreSQL running locally with a database named `my_local_db`
- A database role with permission to create and drop schemas for integration tests
- Windows PowerShell with `curl.exe`
- Maven 3.9 or later

## Create the PostgreSQL database and run Flyway migrations

Flyway creates and updates the application's tables and indexes, but PostgreSQL requires the target database to exist first. Start the PostgreSQL server, then create the empty database named 'my_local_db'.

Set `DB_USERNAME` and `DB_PASSWORD` below to credentials for a PostgreSQL role that can connect to `my_local_db`. 

```powershell
$env:DB_USERNAME = "your-username"
$env:DB_PASSWORD = "your-local-password"
```

When the API starts, Spring Boot runs Flyway automatically using the configured `classpath:db/migration` location. Flyway applies `V1__create_tables.sql`, `V2__create_indexes.sql`, and `V3__metadata_to_jsonb.sql`, and records completed migrations in `flyway_schema_history`. The JPA setting `ddl-auto=validate` then checks that the schema matches the entities; Hibernate does not create the tables. V3 converts transaction and invoice snapshot metadata from text to PostgreSQL `JSONB`.

## Run the API

With PostgreSQL running and the credentials set, start the API with one command:

```powershell
mvn spring-boot:run
```

## Run integration tests

With PostgreSQL running and the credentials set above, run:

```powershell
mvn test
```

The tests create a uniquely named temporary PostgreSQL schema and drop it after each test class. Set `$env:TEST_DB_URL` to use another database URL; it defaults to `jdbc:postgresql://localhost:5432/my_local_db`.

## API examples

These examples use PowerShell. Each POST or PATCH body is written to a UTF-8 JSON file, then sent with `curl.exe -i -X POST` or `curl.exe -i -X PATCH` and `--data-binary "@file.json"`. 
Responses print directly in the console. 
For each follow-up request, replace the named placeholder such as `{flightId}` or `{invoiceId}` with the UUID printed by the matching create response before running it.
Run the snippets from the same directory so the request files are found. Use fresh `external_id` values if you repeat the create examples.

### Create transactions

Each successful create returns `201 Created` with its ID in the console response. Use a fresh `external_id` if you repeat a request.

#### Create flight

```powershell
@'
{
  "type": "flight",
  "external_id": "flight-TXL-LHR-2026-03-11",
  "currency": "EUR",
  "occurred_at": "2026-03-11T06:40:00Z",
  "total": 220.00,
  "tax_lines": [{ "name": "Airport tax", "amount": 15.00 }],
  "fee_lines": [{ "name": "YQ", "amount": 25.00 }],
  "metadata": {
    "origin": "TXL",
    "destination": "LHR",
    "fare": 180.00,
    "coupons": [{
      "carrier": "BA",
      "flight_number": "BA982",
      "from": "TXL",
      "to": "LHR",
      "departure": "2026-03-11T07:00:00Z",
      "arrival": "2026-03-11T08:20:00Z"
    }]
  }
}
'@ | Set-Content -Encoding utf8 flight-request.json

curl.exe -i -X POST "http://localhost:8080/transactions" `
  -H "Content-Type: application/json" `
  --data-binary "@flight-request.json"
```

#### Create hotel

```powershell
@'
{
  "type": "hotel",
  "external_id": "hotel-LON-2026-03-11",
  "currency": "EUR",
  "occurred_at": "2026-03-11T15:00:00Z",
  "total": 331.70,
  "tax_lines": [{ "name": "VAT", "rate": 0.07, "amount": 21.70 }],
  "fee_lines": [],
  "metadata": {
    "name": "Hotel Mitte London",
    "address": { "street": "12 Kingsway", "city": "London", "country": "GB" },
    "check_in": "2026-03-11",
    "check_out": "2026-03-13",
    "nights": 2,
    "room_rate_per_night": 140.00,
    "extras": [{ "name": "Breakfast", "amount": 30.00 }]
  }
}
'@ | Set-Content -Encoding utf8 hotel-request.json
curl.exe -i -X POST "http://localhost:8080/transactions" `
  -H "Content-Type: application/json" `
  --data-binary "@hotel-request.json"
```

#### Create rail

```powershell
@'
{
  "type": "rail",
  "external_id": "rail-STP-PAR-2026-03-13",
  "currency": "EUR",
  "occurred_at": "2026-03-13T09:00:00Z",
  "total": 94.00,
  "tax_lines": [],
  "fee_lines": [{ "name": "Booking fee", "amount": 4.00 }],
  "metadata": {
    "legs": [
      { "origin": "London St Pancras", "destination": "Lille Europe", "departure": "2026-03-13T09:00:00Z", "arrival": "2026-03-13T10:30:00Z", "train_number": "EST9108", "class": "standard" },
      { "origin": "Lille Europe", "destination": "Paris Nord", "departure": "2026-03-13T11:00:00Z", "arrival": "2026-03-13T12:05:00Z", "train_number": "EST9122", "class": "standard" }
    ]
  }
}
'@ | Set-Content -Encoding utf8 rail-request.json
curl.exe -i -X POST "http://localhost:8080/transactions" `
  -H "Content-Type: application/json" `
  --data-binary "@rail-request.json"
```

#### Create Navan trip fee

```powershell
@'
{
  "type": "navan_fee",
  "external_id": "navan-trip-fee-2026-03-11",
  "currency": "EUR",
  "occurred_at": "2026-03-11T06:40:00Z",
  "total": 25.00,
  "tax_lines": [],
  "fee_lines": [],
  "metadata": { "fee_kind": "trip_fee", "description": "Navan trip fee", "related_external_id": "flight-TXL-LHR-2026-03-11" }
}
'@ | Set-Content -Encoding utf8 trip-fee-request.json
curl.exe -i -X POST "http://localhost:8080/transactions" `
  -H "Content-Type: application/json" `
  --data-binary "@trip-fee-request.json"
```

#### Create Navan agent call fee

```powershell
@'
{
  "type": "navan_fee",
  "external_id": "navan-agent-call-2026-03-11",
  "currency": "EUR",
  "occurred_at": "2026-03-11T18:00:00Z",
  "total": 15.00,
  "tax_lines": [],
  "fee_lines": [],
  "metadata": { "fee_kind": "agent_call_fee", "description": "Agent fee for support call", "related_external_id": "hotel-LON-2026-03-11" }
}
'@ | Set-Content -Encoding utf8 agent-fee-request.json
curl.exe -i -X POST "http://localhost:8080/transactions" `
  -H "Content-Type: application/json" `
  --data-binary "@agent-fee-request.json"
```

### Duplicate create

Send the same request file a second time. It returns `409 Conflict`

```powershell
@'
{
  "type": "flight",
  "external_id": "flight-TXL-LHR-2026-03-11",
  "currency": "EUR",
  "occurred_at": "2026-03-11T06:40:00Z",
  "total": 220.00,
  "tax_lines": [{ "name": "Airport tax", "amount": 15.00 }],
  "fee_lines": [{ "name": "YQ", "amount": 25.00 }],
  "metadata": {
    "origin": "TXL",
    "destination": "LHR",
    "fare": 180.00,
    "coupons": [{
      "carrier": "BA",
      "flight_number": "BA982",
      "from": "TXL",
      "to": "LHR",
      "departure": "2026-03-11T07:00:00Z",
      "arrival": "2026-03-11T08:20:00Z"
    }]
  }
}
'@ | Set-Content -Encoding utf8 flight-request.json

curl.exe -i -X POST "http://localhost:8080/transactions" `
  -H "Content-Type: application/json" `
  --data-binary "@flight-request.json"
```

### Get and list transactions

```powershell
curl.exe -i "http://localhost:8080/transactions/{transactionId}"
```

```powershell
curl.exe -i "http://localhost:8080/transactions?limit=50&offset=0"
```

### Patch and delete transactions

Create a separate unbilled fee transaction to demonstrate successful PATCH and DELETE:

```powershell
@'
{
  "type": "navan_fee",
  "external_id": "support-call-demo-001",
  "currency": "EUR",
  "occurred_at": "2026-03-11T18:00:00Z",
  "total": 15.00,
  "tax_lines": [],
  "fee_lines": [],
  "metadata": { "fee_kind": "agent_call_fee", "description": "Support call" }
}
'@ | Set-Content -Encoding utf8 maintenance-request.json
curl.exe -i -X POST "http://localhost:8080/transactions" `
  -H "Content-Type: application/json" `
  --data-binary "@maintenance-request.json"
```

```powershell
@'
{ "total": 16.00, "occurred_at": "2026-03-11T18:30:00Z" }
'@ | Set-Content -Encoding utf8 transaction-patch.json
curl.exe -i -X PATCH "http://localhost:8080/transactions/{maintenanceTransactionId}" -H "Content-Type: application/json" --data-binary "@transaction-patch.json"
```

```powershell
curl.exe -i -X DELETE "http://localhost:8080/transactions/{maintenanceTransactionId}"
```

After creating the trip invoice below, replace `{flightId}` with the actual flight ID from its create response. PATCH and DELETE each return `409 Conflict`:

```powershell
curl.exe -i -X PATCH "http://localhost:8080/transactions/{flightId}" -H "Content-Type: application/json" --data-binary "@transaction-patch.json"
```

```powershell
curl.exe -i -X DELETE "http://localhost:8080/transactions/{flightId}"
```

### Invoices

Create an invoice for the five trip transactions. Its total is `685.70 EUR`.

```powershell
@'
{
  "transaction_ids": [
    "{flightId}",
    "{hotelId}",
    "{railId}",
    "{tripFeeId}",
    "{agentCallFeeId}"
  ]
}
'@ | Set-Content -Encoding utf8 invoice-request.json
curl.exe -i -X POST "http://localhost:8080/invoices" `
  -H "Content-Type: application/json" `
  --data-binary "@invoice-request.json"
```

#### Get invoice

```powershell
curl.exe -i "http://localhost:8080/invoices/{invoiceId}"
```

#### List invoices

```powershell
curl.exe -i "http://localhost:8080/invoices?limit=50&offset=0"
```

PATCH replaces the invoice transaction set and rebuilds the snapshot from current stored transactions. This example keeps the same set:

```powershell
curl.exe -i -X PATCH "http://localhost:8080/invoices/{invoiceId}" -H "Content-Type: application/json" --data-binary "@invoice-request.json"
```

Delete the invoice (`204 No Content`); its underlying transactions remain available:

```powershell
curl.exe -i -X DELETE "http://localhost:8080/invoices/{invoiceId}"
```
