# TindaKart backend handover guide

This guide covers the verified local deployment and the operating rules that must be confirmed before client rollout.

## Local startup

1. Install Java 21 and PostgreSQL.
2. Create the `tindakart` database and copy `.env.example` to the ignored root `.env`.
3. Set database credentials and a strong initial admin password in `.env`.
4. From the repository root, run:

   ```powershell
   $env:GRADLE_USER_HOME = "$PWD\.gradle-home"
   .\gradlew.bat :backend:bootRun
   ```

   Flyway applies pending migrations on startup.

5. Confirm `/api/health` and `/api/health/database` return `UP` before testing operations.

## Local PayMongo test mode

PayMongo checkout creation runs from the backend. Add the test credentials to
the ignored root `.env` file:

```properties
PAYMONGO_SECRET_KEY=sk_test_your_key
PAYMONGO_WEBHOOK_SECRET=your_webhook_signing_secret
PAYMONGO_BASE_URL=https://api.paymongo.com
NGROK_HOST=https://your-ngrok-host.ngrok-free.app
PAYMONGO_WEBHOOK_URL=https://your-ngrok-host.ngrok-free.app/api/billing/webhooks/paymongo
```

Start the backend normally. When a package or add-on checkout is started,
TindaKart calls PayMongo from `localhost:8080`. Test mode does not charge real
money.

To test the post-payment subscription update locally, expose the backend with a
tunnel such as ngrok:

```powershell
ngrok http 8080
```

In the PayMongo dashboard, create a test webhook pointing to:
`${PAYMONGO_WEBHOOK_URL}`. Subscribe to
`checkout_session.payment.paid` and the relevant failed, cancelled, or expired
events. Copy the webhook signing secret into `PAYMONGO_WEBHOOK_SECRET` and
restart the backend after changing `.env`.

Never commit `.env` or paste a live key into source control. Rotate a key
immediately if it is exposed.

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

For pre-migration data-quality review, run the read-only checks in [DATABASE_AUDIT_PROCEDURE.md](DATABASE_AUDIT_PROCEDURE.md) and retain the reviewed results with the backup record.

## Troubleshooting

- Database health is down: verify PostgreSQL is running, `.env` values are correct, and port 5432 is available.
- Flyway validation fails: stop the backend, inspect the migration history, and do not edit an already-applied migration. Add a new forward migration after taking a backup.
- Login fails: confirm the username is enabled and the account has the expected role/store assignment; repeated failures temporarily lock the username.
- A module is unavailable: check both the user's role permission and the vendor's active package entitlement.
- POS cannot sell an item: check stock, expiration, store context, and package POS entitlement.
- Receipt printing does not work: verify the configured receipt output mode and complete the required device acceptance testing for thermal printer/bridge support.

## Deployment prerequisites

Before client rollout, the owner must approve the vendor/store policy, roles, tax/receipt layout, payment methods, printer, and acceptance criteria. A deployment must additionally provide HTTPS, environment-injected secrets, scheduled database backups, monitoring/log retention, and a controlled pilot. These are not assumed to be complete from local verification.

The staging and production Spring profiles mark session cookies as Secure, HttpOnly, and SameSite=Lax. They must only be used behind HTTPS; local development keeps the Secure flag off so localhost sessions work.

Recommended verification commands:

```powershell
$env:GRADLE_USER_HOME = "$PWD\.gradle-home"
.\gradlew.bat :backend:test --no-daemon --console=plain
```

The normal test task excludes database-mutating integration tests. After configuring a disposable or backed-up local PostgreSQL database, run the opt-in multistore authorization check with:

```powershell
.\gradlew.bat :backend:integrationTest --no-daemon --console=plain
```

That test creates uniquely named fixture rows and removes them during teardown. Do not point it at a client production database.
