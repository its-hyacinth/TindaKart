# TindaKart Frontend API and Visibility Map

This document records the current frontend consumers of the backend API. It is a planning and verification aid for the frontend revamp; backend authorization remains authoritative.

## Shared request behavior

`frontend/src/api.ts` centralizes requests and provides:

- Session cookies with `credentials: include`
- CSRF token acquisition for state-changing requests
- Request timeout and normalized API errors
- Typed response contracts

Every authenticated page should expose loading, empty, error, success, and pending-submission states for its calls.

## Endpoint consumer map

| Backend area | Frontend consumer | Primary UI state |
|---|---|---|
| `/api/auth/csrf`, `/api/auth/login`, `/api/auth/me`, `/api/auth/logout` | `main.tsx` login/session shell | Checking session, signing in, invalid credentials, signed-in state |
| `/api/auth/context` | `main.tsx`, `operations.tsx` | Context loading, valid vendor/store selection, scope rejection |
| `/api/auth/password` | `admin.tsx` `AccountSecurity` | Pending change, success/sign-out, validation error |
| `/api/super-admin/vendors` and vendor status | `main.tsx` `TenantDashboard` | Vendor list, create, activate/suspend, error feedback |
| `/api/vendors/{vendorId}/stores` and store status | `main.tsx` `TenantDashboard` | Store list, create, enable/disable, empty state |
| `/api/vendors/{vendorId}/staff` and store assignments | `admin.tsx` `StaffPanel` | Staff loading, create, assignment editor, enable/disable |
| `/api/vendors/{vendorId}/settings` | `admin.tsx` `VendorSettings`, `operations.tsx` POS | Settings loading, configurable VAT/payment/camera/print policy |
| `/api/super-admin/packages` | `admin.tsx` package panels | Package list, create, edit, feature/limit controls |
| `/api/vendors/{vendorId}/packages` and subscription | `admin.tsx` `BillingPanel` | Package selection, checkout, billing history |
| `/api/vendors/{vendorId}/billing/checkout` | `admin.tsx` `BillingPanel` | Checkout creation, provider redirect, history |
| `/api/vendors/{vendorId}/entitlements` | `main.tsx` `OperationsWorkspace` | Package feature visibility and unavailable-module filtering |
| `/api/vendors/{vendorId}/categories` | `operations.tsx` catalog forms | Category loading, filter, create-product selection |
| `/api/vendors/{vendorId}/products` | `operations.tsx` catalog/POS/inventory/delivery | Search, loading, empty, product selection |
| `/api/vendors/{vendorId}/products/barcode/{barcode}` | `operations.tsx` POS and inventory | Identified product, not-found feedback |
| `/api/vendors/{vendorId}/stores/{storeId}/sales/customers` | `operations.tsx` POS | Credit-customer loading and selection |
| `/api/vendors/{vendorId}/stores/{storeId}/sales` | `operations.tsx` POS | Pending checkout, success receipt, atomic error |
| `/api/vendors/{vendorId}/stores/{storeId}/sales/{saleId}/void` | `operations.tsx` POS | Confirmation, stock-restored success, error |
| `/api/vendors/{vendorId}/stores/{storeId}/sales/{saleId}/receipt` | Receipt preview/reprint flow | Receipt loading and print/reprint result |
| `/api/vendors/{vendorId}/stores/{storeId}/inventory` | `operations.tsx` `InventoryPage` | Batch list, summary cards, empty/error state |
| `/api/vendors/{vendorId}/stores/{storeId}/inventory/receive` | `operations.tsx` inventory receive form | Barcode identification, pending receive, success/error |
| `/api/vendors/{vendorId}/stores/{storeId}/inventory/adjust` | `operations.tsx` adjustment form | Reason validation, pending adjustment, success/error |
| `/api/vendors/{vendorId}/stores/{storeId}/inventory/movements` | `InventoryMovementHistory` | Loading, movement table, empty/error state |
| `/api/vendors/{vendorId}/debt/customers` | `operations.tsx` customer form | Create customer, success/error |
| `/api/vendors/{vendorId}/debt/accounts` | `operations.tsx` `DebtPage` | Summary cards, search, balance table |
| `/api/vendors/{vendorId}/debt/accounts/{accountId}/payments` | `operations.tsx` debt payment form | Pending payment, balance refresh, print receipt |
| `/api/vendors/{vendorId}/debt/aging` | `debtAging.tsx` | Aging loading, buckets, empty/error state |
| `/api/vendors/{vendorId}/suppliers` | `operations.tsx` delivery creation | Supplier selection and quick-create |
| `/api/vendors/{vendorId}/stores/{storeId}/deliveries` | `operations.tsx` delivery page | Delivery board/table, status actions |
| delivery receiving endpoints | `deliveryReceiving.tsx` | Variance form, receive success, inventory update error |
| store report endpoints | `operations.tsx`, `advancedReports.tsx` | Report loading, metrics, tables, exports |
| `/api/super-admin/audit` | `admin.tsx` `AuditLogPanel` | Audit loading, table, empty/error state |
| `/api/billing/webhooks/paymongo` | Provider-to-backend only | HMAC verification and idempotent processing; never called by the PWA |

## Visibility matrix

| Capability | Super Admin | Vendor Admin | Staff/Cashier |
|---|---:|---:|---:|
| Platform vendors | Yes | No | No |
| Packages and platform billing controls | Yes | No | No |
| Vendor staff and store assignments | Yes where vendor context exists | Yes | No |
| Business/tax/receipt settings | Yes | Yes | Read-only where allowed |
| POS | Platform bypass / selected vendor | Package + permission | Package + assigned-store permission |
| Inventory | Platform bypass / selected store | Package + permission | Package + assigned-store permission |
| Catalog/categories | Platform bypass / selected vendor | Package + permission | Permission-dependent |
| Debt | Platform bypass / selected vendor | Package + permission | Permission-dependent |
| Deliveries | Platform bypass / selected store | Package + permission | Permission-dependent |
| Reports | Platform/vendor scope | Vendor scope | Assigned-store or permission scope |
| Audit log | Yes | Vendor operational audit where exposed | No |

The UI may hide unavailable modules for clarity, but every mutation and scope check must still be enforced by the backend.

## State checklist for each consumer

- Loading state does not show stale data as current.
- Empty state explains the next useful action.
- API error state preserves the current form/cart when safe.
- Mutating controls disable while a request is pending.
- Success feedback names the affected entity or transaction.
- Vendor/store context is visible for tenant-owned data.
- Package or permission restrictions explain why an action is unavailable.
