# TindaKart PWA foundation

Module 0 adds the new architecture beside the existing Swing desktop app.

## Local requirements

- Java 21
- Node.js and npm
- PostgreSQL running locally
- A PostgreSQL database named `tindakart`
- Root `.env` with the local database settings

## Run the backend

From the repository root:

```powershell
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
.\gradlew.bat :backend:bootRun
```

The API runs at `http://localhost:8080`.

Health checks:

- `http://localhost:8080/api/health`
- `http://localhost:8080/api/health/database`

## Run the frontend

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

The PWA runs at `http://localhost:5173` and proxies `/api` requests to the backend.

## Migrations

Flyway runs automatically when the backend starts. The current local database has migrations V1 through V11 applied. Versioned migrations are forward-only; do not add `DROP TABLE` statements to reset a client database.

Implemented API foundations include:

- `/api/super-admin/vendors` and `/api/vendors/{vendorId}/stores`
- `/api/vendors/{vendorId}/staff`
- `/api/super-admin/packages` and vendor subscriptions
- `/api/vendors/{vendorId}/categories` and products/barcodes
- Store inventory receiving, adjustments, and batch status
- Atomic POS sales, payments, credit sales, and receipts
- Debt accounts and payment allocation
- Suppliers and delivery receiving
- Business/tax settings, reports, and PayMongo checkout/webhooks

The current PWA includes responsive operations tabs for POS, Catalog, Inventory, Debt, Deliveries, and Reports. Hardware camera scanning, thermal printing, advanced exports, and production billing activation still require acceptance testing and configuration.

## Important

The current root `.env` is intentionally local and ignored by Git. Do not commit it. Use `.env.example` as the template for another machine.

## Module 1: first owner account

Add these two values to the root `.env` before the first backend startup:

```properties
INITIAL_ADMIN_USERNAME=admin
INITIAL_ADMIN_PASSWORD=choose-a-strong-local-password
```

When the `users` table is empty, the backend creates this account with the `SUPER_ADMIN` role. It does not overwrite existing users on later starts. Change or remove these values after the first account is created.
