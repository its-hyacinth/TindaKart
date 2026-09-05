# TindaKart PWA handover guide

This guide covers the verified local deployment and the operating rules that must be confirmed before client rollout.

## Local startup

1. Install Java 21, Node.js/npm, and PostgreSQL.
2. Create the `tindakart` database and copy `.env.example` to the ignored root `.env`.
3. Set database credentials and a strong initial admin password in `.env`.
4. From the repository root, run:

   ```powershell
   $env:GRADLE_USER_HOME = "$PWD\.gradle-home"
   .\gradlew.bat :backend:bootRun
   ```

   Flyway applies pending migrations on startup.

5. In another terminal, run:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

Open `http://localhost:5173`. Confirm `/api/health` and `/api/health/database` return `UP` before testing operations.

If Java fails to start, `JAVA_HOME` must point to the JDK directory itself, such as `C:\Program Files\Java\jdk-21.0.12`, not its `bin` subdirectory.

## Platform setup

The hierarchy is:

- Super Admin: manages vendors, packages, entitlements, platform audit logs, and platform billing configuration.
- Vendor Admin: manages the vendor's stores, staff, catalog, settings, and subscription checkout.
- Staff: operates only assigned stores and receives only the assigned operational permissions.

Create the vendor and store first, select a package, then create staff accounts with the minimum required role:

- `CASHIER`: POS and receipt operations.
- `INVENTORY_STAFF`: inventory viewing, receiving, and adjustments.
- `DEBT_STAFF`: debt accounts and payments.
- `DELIVERY_STAFF`: suppliers and deliveries.

Store assignments are enforced by the backend. Do not rely on hiding a menu item as an authorization control.

## Daily operating flows

- POS: select the vendor/store context, search or scan a product, confirm quantity and payment, complete the sale, then print the receipt.
- Inventory: receive or adjust stock with a reason; review movement history and expiration status.
- Delivery: create the delivery, move it through its statuses, and receive quantities with missing/damaged variance.
- Debt: create a credit sale against a customer, record payments, and review aging/balances.
- Reports: review sales, stock, expiration, VAT, payments, profit, best sellers, and delivery history.

Do not sell expired batches. Near-expiration discounting is controlled by Vendor Admin business settings and applies only to qualifying non-expired stock.

## Backup and recovery

Follow [BACKUP_PROCEDURE.md](BACKUP_PROCEDURE.md). Backups may contain client data and must remain outside source control. Test restoration into an isolated database before any production recovery. Never use destructive reset migrations against a client database.

## Troubleshooting

- Database health is down: verify PostgreSQL is running, `.env` values are correct, and port 5432 is available.
- Flyway validation fails: stop the backend, inspect the migration history, and do not edit an already-applied migration. Add a new forward migration after taking a backup.
- Login fails: confirm the username is enabled and the account has the expected role/store assignment; repeated failures temporarily lock the username.
- A module is unavailable: check both the user's role permission and the vendor's active package entitlement.
- POS cannot sell an item: check stock, expiration, store context, and package POS entitlement.
- Receipt printing does not work: use browser print preview first; thermal printer/bridge support requires device acceptance testing.

## Deployment prerequisites

Before client rollout, the owner must approve the vendor/store policy, roles, tax/receipt layout, payment methods, printer, offline behavior, and acceptance criteria. A deployment must additionally provide HTTPS, environment-injected secrets, scheduled database backups, monitoring/log retention, and a controlled pilot. These are not assumed to be complete from local verification.

Recommended verification commands:

```powershell
$env:GRADLE_USER_HOME = "$PWD\.gradle-home"
.\gradlew.bat :backend:test --no-daemon --console=plain
cd frontend
.\node_modules\.bin\tsc.cmd -p tsconfig.app.json --noEmit --incremental false
npx vite build --configLoader runner
```
