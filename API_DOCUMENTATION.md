# TindaKart API (local PWA migration)

Base URL: `http://localhost:8080`

All protected endpoints use the server session cookie. Before a mutating request, obtain `/api/auth/csrf` and send the returned token using the returned header name. The PWA API client does this automatically.

## Authentication

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/auth/csrf` | Get the CSRF token and header name |
| POST | `/api/auth/login` | Start a session with `{ username, password }` |
| GET | `/api/auth/me` | Return the current user, roles, vendors, and stores |
| GET | `/api/auth/context` | Read the current session vendor/store context |
| PUT | `/api/auth/context` | Set a vendor/store context after server-side scope validation |
| POST | `/api/auth/password` | Change the authenticated user password |
| POST | `/api/auth/logout` | End the current session |

Operational endpoints enforce both tenant scope and the seeded role-permission matrix. Migration V16 assigns
`POS_USE`, `INVENTORY_VIEW`, `INVENTORY_MANAGE`, `DEBT_MANAGE`, and `DELIVERY_MANAGE` to the corresponding staff roles.
Super Admin bypasses tenant permission checks, while Vendor Admin receives the vendor operational permissions.

## Tenant and administration

Super Admin routes use `/api/super-admin`. Vendor routes require the authenticated user to have access to the vendor; store routes additionally validate the vendor/store relationship.

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/api/super-admin/vendors` | List or create vendors |
| PATCH | `/api/super-admin/vendors/{vendorId}/status` | Activate or suspend a vendor |
| GET/POST | `/api/vendors/{vendorId}/stores` | List or create stores |
| PATCH | `/api/vendors/{vendorId}/stores/{storeId}/status` | Activate or disable a store |
| GET/POST | `/api/vendors/{vendorId}/staff` | List or create vendor staff |
| PATCH | `/api/vendors/{vendorId}/staff/{userId}/status` | Enable or disable staff |
| PUT | `/api/vendors/{vendorId}/staff/{userId}/stores` | Replace staff store assignments |
| GET | `/api/super-admin/audit` | Read recent audit events (Super Admin only) |

## Catalog, inventory, and POS

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/api/vendors/{vendorId}/categories` | List or create categories |
| GET/POST | `/api/vendors/{vendorId}/products` | Search or create products |
| GET | `/api/vendors/{vendorId}/products/barcode/{barcode}` | Resolve a barcode |
| GET | `/api/vendors/{vendorId}/stores/{storeId}/sales/customers` | List customers available for POS credit sales within the authorized store context |
| GET | `/api/vendors/{vendorId}/stores/{storeId}/inventory` | List current inventory batches |
| GET | `/api/vendors/{vendorId}/stores/{storeId}/inventory/movements` | List recent inventory movements |
| POST | `/api/vendors/{vendorId}/stores/{storeId}/inventory/receive` | Receive stock |
| POST | `/api/vendors/{vendorId}/stores/{storeId}/inventory/adjust` | Adjust stock with a reason |
| POST | `/api/vendors/{vendorId}/stores/{storeId}/sales` | Complete a sale and deduct stock atomically |
| POST | `/api/vendors/{vendorId}/stores/{storeId}/sales/{saleId}/void` | Void a completed sale and restore stock |
| GET | `/api/vendors/{vendorId}/stores/{storeId}/sales/{saleId}/receipt` | Read receipt data |

## Debt, deliveries, reports, and billing

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/vendors/{vendorId}/debt/accounts` | List debt accounts |
| GET/POST | `/api/vendors/{vendorId}/debt/customers` | List or create debt customers |
| GET | `/api/vendors/{vendorId}/debt/accounts/{accountId}/payments` | Read payment history |
| POST | `/api/vendors/{vendorId}/debt/accounts/{accountId}/payments` | Record a debt payment |
| GET | `/api/vendors/{vendorId}/debt/aging` | Return debt aging buckets |
| GET/POST | `/api/vendors/{vendorId}/stores/{storeId}/deliveries` | List or create deliveries |
| PATCH | `/api/vendors/{vendorId}/stores/{storeId}/deliveries/{deliveryId}/status` | Advance delivery status |
| PUT | `/api/vendors/{vendorId}/stores/{storeId}/deliveries/{deliveryId}/receive` | Receive all ordered quantities |
| PUT | `/api/vendors/{vendorId}/stores/{storeId}/deliveries/{deliveryId}/receive-details` | Receive with missing/damaged quantities |
| GET/POST | `/api/vendors/{vendorId}/suppliers` | List or create suppliers |
| GET | `/api/vendors/{vendorId}/stores/{storeId}/reports/{report}` | Sales, stock, expiration, delivery, payment, profit, and best-selling reports |
| GET/PUT | `/api/vendors/{vendorId}/settings` | Read or update business/VAT/expiration settings, POS payment methods, camera scanning, and receipt output mode |
| GET | `/api/vendors/{vendorId}/entitlements` | Read package feature and limit entitlements |
| GET/POST | `/api/super-admin/packages` | List or create subscription packages |
| PUT | `/api/super-admin/packages/{packageId}` | Update a subscription package |
| GET | `/api/vendors/{vendorId}/packages` | List packages available to the vendor |
| PUT | `/api/vendors/{vendorId}/subscription` | Select a package |
| POST | `/api/vendors/{vendorId}/billing/checkout` | Create a subscription checkout session |
| GET | `/api/vendors/{vendorId}/billing/checkout` | Read billing history |
| POST | `/api/billing/webhooks/paymongo` | Receive a signed PayMongo event; public at the HTTP layer, HMAC signature required, idempotent by provider event ID |
| POST | `/api/vendors/{vendorId}/stores/{storeId}/sales/{saleId}/receipt/printed` | Record controlled receipt printing/reprinting |

## Operational rules

- Vendor and store IDs are always checked on the backend; client-supplied IDs cannot cross tenant boundaries.
- Package feature and numeric limits are enforced server-side.
- Expired inventory cannot be sold.
- Near-expiration discounts are vendor-configured, limited to 0–100%, and applied only when all allocated stock for a line is within the configured warning window.
- Sales, stock deduction, movement records, and receipt numbering are transactional.
- POS credit sales require a vendor customer and future due date; the backend creates or updates the corresponding debt account.
- Subscription billing is separate from store POS payment methods.
- POS payment methods default to `CASH`, `CARD`, `EWALLET`, and `CREDIT`, but Vendor Admin can configure the allowed list per vendor. The backend enforces the list.
- Receipt output mode is vendor-configurable as `BROWSER`, `THERMAL_BRIDGE`, or `MANUAL`; the current frontend implements browser preview while hardware integration remains an acceptance item.
- PayMongo webhook requests bypass browser session/CSRF checks because they are server-to-server; the backend validates the `Paymongo-Signature` HMAC before processing.
- PayMongo secrets remain backend-only and are never returned to the PWA.

## Health checks

- `GET /api/health`
- `GET /api/health/database`
