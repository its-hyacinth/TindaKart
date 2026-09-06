# TindaKart API contract

Development base URL: `http://localhost:8080`

The backend is authoritative for authentication, authorization, store scope,
entitlements, pricing, stock, sales, debt, receipts, billing, and audit data.
The frontend must not invent routes or duplicate business rules.

## Authentication

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Issue the CSRF cookie |
| POST | `/api/auth/login` | Create a session |
| POST | `/api/auth/logout` | End a session |
| GET | `/api/auth/me` | Current account and assigned stores |
| GET/PUT | `/api/auth/context` | Read or set the current store context |
| POST | `/api/auth/password` | Change the signed-in password |

## Store hierarchy

| Method | Route | Purpose |
| --- | --- | --- |
| GET/POST | `/api/super-admin/stores` | List or create stores |
| PATCH | `/api/super-admin/stores/{storeId}/status` | Activate or suspend a store |
| GET/POST | `/api/stores/{storeId}/staff` | List or create staff for a store |
| PATCH | `/api/stores/{storeId}/staff/{userId}/status` | Enable or disable staff |

Super Admin can manage every store. Store Admin can manage staff only for the
assigned store. Staff access is always checked server-side through the store
assignment and role permissions.

## Store operations

| Method | Route | Purpose |
| --- | --- | --- |
| GET/POST | `/api/stores/{storeId}/categories` | List or create categories |
| GET/POST | `/api/stores/{storeId}/products` | Search or create products |
| GET | `/api/stores/{storeId}/products/barcode/{barcode}` | Resolve a barcode |
| GET | `/api/stores/{storeId}/inventory` | Current inventory |
| POST | `/api/stores/{storeId}/inventory/receive` | Receive stock |
| POST | `/api/stores/{storeId}/inventory/adjust` | Adjust stock with a reason |
| POST | `/api/stores/{storeId}/sales` | Complete a sale atomically |
| POST | `/api/stores/{storeId}/sales/{saleId}/void` | Void a sale and restore stock |
| GET | `/api/stores/{storeId}/sales/{saleId}/receipt` | Read receipt data |
| GET | `/api/stores/{storeId}/reports/{report}` | Read an operational report |
| GET/PUT | `/api/stores/{storeId}/settings` | Store business and POS settings |

The remaining operational controller migration must preserve the existing
validation rules: expired stock cannot be sold, stock deductions are atomic,
credit sales require a customer and future due date, and payment methods are
enforced by the store settings.

## Subscription and billing

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/api/stores/{storeId}/entitlements` | Read package features and limits |
| GET | `/api/stores/{storeId}/packages` | List available packages |
| PUT | `/api/stores/{storeId}/subscription` | Select a package |
| POST | `/api/stores/{storeId}/billing/checkout` | Create a subscription checkout |
| GET | `/api/stores/{storeId}/billing/checkout` | Read billing history |

Subscription payments are separate from customer POS payments. Provider secret
keys stay in backend environment configuration and webhook events are verified
and processed idempotently.

## Error and security rules

- The client must send the session cookie and CSRF token for state-changing calls.
- A client-supplied store ID is accepted only after server-side authorization.
- `401` means the session is missing or expired; `403` means the account lacks
  store permission or entitlement; `409` means a business uniqueness conflict;
  `422`/`400` means validation failed.
- Error bodies expose an actionable message but never secrets or credentials.
