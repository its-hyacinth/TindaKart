# TindaKart PWA Migration Plan

## Implementation verification status

Checked items below mean the capability exists in the repository and has been verified by inspection or build validation. Partial capabilities remain unchecked until the complete workflow and tests exist.

- [x] Module 0: backend and PWA foundations created.
- [x] Module 1: database-backed authentication foundation created.
- [x] Multitenant foundation schema created.
- [x] Vendor/store API foundation created.
- [x] Backend Java compilation verified successfully.
- [x] Database-backed authentication and multistore context-isolation integration test added and passed; complete workflow integration coverage remains in Phase 13.
- [x] Complete scoped-role and package-feature authorization; end-to-end denial tests remain in Phase 13.
- [x] Frontend operations screen foundations for POS, Inventory, Catalog, Debt, Deliveries, and Reports.
- [x] POS, inventory, debt, delivery, receipts, billing, and reporting workflows implemented; end-to-end verification remains in Phase 13.
- [x] Centralized API error response and server logging foundation added.
- [x] Desktop sidebar and mobile compact navigation added to the operations shell.
- [x] Vendor Admin staff controls and business receipt/tax settings UI added.
- [x] Catalog category and product creation UI added.
- [x] Subscription package creation, selection, checkout, and billing-history UI foundation added.
- [x] Vendor entitlement API and package-based operations navigation added.
- [x] Backend entitlement endpoint compilation and full test suite verified.
- [x] PostgreSQL-backed runtime smoke verified: Flyway validated all 18 migrations and health/database endpoints returned UP.
- [x] Authenticated shell shows connectivity status; offline transaction support remains intentionally pending.
- [x] Inventory batches and movement history show explicit loading states.
- [x] Sales, stock, expiration, and advanced report views show explicit loading states.
- [x] Catalog, debt, and delivery tables show explicit loading states.
- [x] API client requests fail with a bounded timeout and actionable connectivity message.
- [x] POS browser printing records the receipt printed event before opening print preview.
- [x] Receipt preview includes configured business identity, TIN, VAT context, payment/change, and footer fields.
- [x] Protected frontend mutations obtain and send the backend CSRF token automatically.
- [x] POS completed-sale void control restores stock batches and records reversal movements.
- [x] Delivery receiving variance UI and debt-aging UI wired into Operations.
- [x] Expiration classification extracted into a backend policy with boundary unit tests.
- [x] Server-validated vendor/store context persists in the authenticated session and restores on PWA reload.
- [x] Local PostgreSQL backup was restored into an isolated temporary database and validated before cleanup.
- [x] Authenticated runtime smoke verified PostgreSQL health, Super Admin login, and initial session context.
- [x] Vendor-configured near-expiration discount percentage is validated server-side and applied only to qualifying non-expired stock.

## Purpose

Migrate TindaKart from its current Java Swing desktop application into a responsive Progressive Web App (PWA) that works on:

- Laptop and desktop browsers
- Android phones
- Tablets
- iOS browsers, with optional installation to the home screen

The current PostgreSQL database, business requirements, and useful domain concepts will be preserved where practical. The Swing interface will serve as a reference implementation, not as the mobile frontend.

## Current Architecture

```text
Java Swing application
        |
        | direct JDBC connection
        v
PostgreSQL database
```

Current areas include:

- Sales/POS
- Inventory
- Debt tracking
- Customer profiles
- Reports
- Demo authentication
- Automatic schema creation and sample-data seeding

Current limitations:

- The UI is desktop-only.
- The frontend connects directly to PostgreSQL.
- Authentication is hard-coded and demo-only.
- Business rules are mixed into Swing event handlers.
- Barcode, category, expiration, delivery, receipt, and payment workflows are incomplete.
- The database schema is not yet sufficient for the complete requested system.

## Target Architecture

```text
PWA frontend
  - responsive POS, inventory, debt, delivery, reports
  - barcode keyboard/camera input
  - installable on supported devices
        |
        | HTTPS REST/JSON API
        v
Backend service
  - authentication and authorization
  - validation and business rules
  - transactions and audit logging
  - receipt and reporting services
        |
        | database connection
        v
PostgreSQL
```

The browser must not connect directly to PostgreSQL. All database access should go through the backend API.

### Multi-tenant business model

TindaKart is a multi-tenant SaaS platform with this hierarchy:

```text
Super Admin — TindaKart platform owner
        |
        +-- Vendors — subscribing store owners/businesses
                    |
                    +-- Stores/Branches
                                |
                                +-- Staff
```

Operational data must be isolated by vendor and store. A Vendor Admin can manage all stores under their vendor account, while staff can operate only in their assigned store(s). The Super Admin can manage the platform and view platform-wide data.

Package access must be evaluated together with user permissions:

```text
User role permission + Vendor package feature = Final allowed action
```

## Recommended Migration Strategy

Use an incremental migration rather than attempting to convert Swing screens directly.

```text
Document current system
        ↓
Design target data/API model
        ↓
Build backend beside existing app
        ↓
Build responsive PWA shell
        ↓
Migrate one workflow at a time
        ↓
Test old and new workflows against controlled data
        ↓
Deploy PWA and retire Swing application
```

The Swing application should remain available as a temporary reference until the new POS, inventory, debt, and reporting workflows pass acceptance testing.

## Phase 0: Decisions and Scope

- [x] Confirm whether the PWA will run locally, on a store LAN, or in the cloud. (Local development/deployment selected.)
- [x] Confirm whether the system supports one store or multiple stores. (Multistore selected.)
- [x] Confirm the vendor-to-store relationship and whether one vendor can have multiple branches. (Vendor can own multiple stores.)
- [x] Support multi-store staff assignment as a configurable default; Vendor Admin can assign and reassign staff across the vendor's stores.
- [x] Confirm Super Admin responsibilities and platform ownership.
- [x] Implement configurable vendor lifecycle states (pending, active, suspended, cancelled); cancellation is the recoverable soft-deactivation policy and can be changed through Super Admin status controls.
- [x] Confirm package names, prices, limits, inclusions, and feature permissions as platform-managed data.
- [x] Confirm that vendors pay TindaKart subscription fees online using a PH sandbox provider during development.
- [x] Keep POS customer payments provider-neutral and configurable per vendor (cash, card, e-wallet, or credit); PayMongo remains dedicated to TindaKart subscription billing.
- [ ] Confirm the number of simultaneous users and cashiers.
- [ ] Confirm whether internet loss must be supported.
- [x] Make receipt output strategy configurable per vendor (browser preview, thermal bridge, or manual); physical printer compatibility testing remains pending.
- [x] Make camera barcode scanning configurable per vendor and enable it by default on supported devices.
- [x] Provide a standards-based installable PWA path, including iOS-compatible manifest metadata where supported; iPhone/iPad acceptance testing remains pending.
- [x] Provide configurable VAT, TIN, receipt footer, and receipt-print settings; owner/accountant validation remains required before production tax use.
- [x] Implement data-driven role and permission enforcement with configurable package feature gates; final client role mapping can be adjusted without code changes.
- [ ] Confirm the acceptance criteria for the first release.

### Initial MVP recommendation

The first PWA release should include:

- Login and role permissions
- Super Admin, Vendor Admin, and Staff account scopes
- Vendor and store isolation
- Package and subscription visibility
- Product categories and product management
- Barcode lookup
- POS cart and payment
- Stock deduction
- Inventory receiving and adjustment
- Basic receipts
- Debt balances and payments
- Expiration blocking
- Basic sales reports

Delivery management, SMS reminders, advanced analytics, and native mobile packaging can follow after the core POS is stable.

## Phase 1: Protect and Document Existing Data

- [x] Back up the existing PostgreSQL database; a local custom-format archive was created and verified.
- [x] Create a restore procedure and test it against an isolated temporary database; the active database was not modified.
- [x] Export the current table definitions.
- [x] Document existing columns, constraints, indexes, and relationships.
- [x] Identify duplicate or incomplete products; local audit found none (`DATABASE_AUDIT_REPORT.md`).
- [x] Identify debt records without matching customer profiles; local audit found none (`DATABASE_AUDIT_REPORT.md`).
- [x] Identify products that have sales history; local audit found none (`DATABASE_AUDIT_REPORT.md`).
- [ ] Record the existing sample/demo data separately from real client data.
- [ ] Decide whether current IDs must be preserved during migration.
- [x] Record the current PostgreSQL version and connection settings without exposing credentials.

No migration should run against the client database without a verified backup and rollback plan.
Read-only data-quality queries are documented in `DATABASE_AUDIT_PROCEDURE.md`; their result still requires owner/reviewer sign-off before the related checklist items can be checked.

## Phase 2: Backend Foundation

- [x] Create a separate backend service project.
- [x] Add environment-based configuration for database credentials.
- [x] Add database migrations instead of startup-only schema creation.
- [x] Add connection pooling.
- [x] Add structured application logging foundation.
- [x] Add centralized exception handling.
- [x] Add request validation.
- [x] Add consistent API error response format.
- [x] Add health-check endpoint.
- [x] Add database transaction boundaries in the backend.
- [x] Add API documentation for the implemented PWA/backend contracts.
- [x] Add development, test, and production configuration profiles.
- [x] Add tenant context resolution and validation from the authenticated user session (`TenantContextService`); stale/tampered contexts are rejected and cleared in integration coverage.
- [x] Enforce vendor/store scope in backend queries and service methods.
- [x] Prevent clients from selecting arbitrary vendor or store IDs.
- [x] Add platform, vendor, and store-level authorization checks.

### Backend rules

The backend must own these rules:

- Never allow negative stock.
- Never sell expired inventory.
- Recalculate prices on the server.
- Validate discounts and permissions.
- Deduct stock and record a sale atomically.
- Use exact numeric types for money.
- Preserve product history instead of deleting sold products.
- Record important changes in an audit log.

## Phase 3: Authentication and Authorization

- [x] Replace hard-coded accounts with database users.
- [x] Hash passwords securely.
- [x] Implement login sessions or secure token authentication.
- [x] Implement logout and session expiry.
- [x] Add staff account activation/deactivation controls with backend enforcement.
- [x] Add authenticated password-change flow; password-reset-by-email remains pending.
- [x] Add role-based permissions foundation.
- [x] Protect implemented admin-only API routes on the backend.
- [x] Add login attempt protection (five failures within a 15-minute window locks the username temporarily).
- [x] Add login success/failure audit logging foundation.
- [x] Add platform-level, vendor-level, and store-level scope schema.
- [x] Add package feature and limit schema.
- [x] Add dynamic vendor operating settings for POS payment methods, camera scanning, and receipt output mode.
- [x] Verify dynamic vendor operating settings round-trip through the API and PostgreSQL (`PackageManagementIntegrationTest`).
- [x] Add a current vendor/store context to authenticated sessions.

### Role hierarchy and scopes

#### Super Admin — platform scope

- Manage vendors and vendor onboarding
- Manage packages, pricing, inclusions, limits, and feature permissions
- Manage subscriptions and billing status
- Suspend or reactivate vendors
- Manage platform settings
- View platform-wide reports and audit logs

#### Vendor Admin — vendor scope

- Manage stores/branches under the vendor
- Manage vendor staff
- Manage products and categories
- Manage prices and discounts
- Adjust inventory for permitted stores
- Manage suppliers and deliveries
- View vendor-wide reports
- Configure vendor receipt and business settings

#### Staff — assigned store scope

- Use POS
- Scan products
- Process sales
- Print receipts
- Perform assigned operational tasks only
- Create credit sales only if permitted by role and package

#### Debt/Collection Staff

- View customer debt
- Record payments
- View payment history
- Send payment reminders if enabled

## Phase 4: Database Model Expansion

The current `inventory`, `sales`, `debt`, and `debt_customer` tables are not enough for the complete system. The target database should be designed and migrated deliberately.

All tenant-owned tables must include the appropriate `vendor_id` and/or `store_id` foreign key. Store and vendor scope must be enforced by the backend, not only by frontend filters.

### Users and business configuration

- `users`
- `roles`
- `permissions`
- `role_permissions`
- `user_roles`
- `vendors`
- `stores`
- `vendor_users`
- `store_users`
- `business_settings` (implemented in V10)
- `audit_logs`

### Packages and subscriptions

- `packages`
- `package_features`
- `package_limits`
- `vendor_subscriptions`
- `subscription_events`
- `subscription_payments`

Packages should be data-driven. The Super Admin must be able to change pricing, limits, inclusions, and feature permissions without changing application source code.

Possible package controls include maximum stores, maximum staff, product limits, barcode scanning, debt tracking, delivery tracking, advanced reports, receipt printing, and camera scanning.

### Tenant relationships

- One vendor can own multiple stores/branches.
- A Vendor Admin belongs to a vendor and can access its stores.
- Staff belongs to a vendor and is assigned to one or more stores.
- Products may be vendor-wide, while stock is store-specific.
- Sales, inventory movements, deliveries, and reports must be store-specific.
- Super Admin may view data across all vendors.

### Products and inventory

- `categories` (implemented in V5)
- `products` (implemented in V5)
- `product_barcodes` (implemented in V5)
- `product_prices`
- `inventory_batches` (implemented in V6)
- `inventory_movements` (implemented in V6)
- `stock_adjustments` (represented by adjustment movements in V6; dedicated table pending)

Important product concepts:

- Product name
- SKU
- One or more barcodes
- Category
- Unit type: piece, pack, bottle, kilo, liter, and so on
- Cost price
- Retail price
- Bulk price
- Bulk threshold
- Reorder level
- Expiration applicable flag
- Active/archived status

Expiration dates should belong to inventory batches because the same product can arrive with different expiration dates.

### Sales and receipts

- `sales` (implemented in V7)
- `sale_items` (implemented in V7)
- `payments` (implemented in V7)
- `discounts`
- `receipts` (implemented in V7)

Each sale should record:

- Receipt/invoice number
- Cashier
- Date/time
- Subtotal
- Discount
- VAT amount when applicable
- Total
- Payment method
- Amount tendered
- Change

### Debt and credit

- `debt_accounts` (implemented in V8)
- `credit_sales` (implemented in V8)
- `debt_payments` (implemented in V8)
- `debt_payment_allocations` (implemented in V8)
- `customer_profiles` (implemented in V8)

This supports total credit, total paid, remaining balance, payment history, due dates, and overdue status.

### Suppliers and delivery

- `suppliers` (implemented in V9)
- `purchase_orders` (pending)
- `purchase_order_items` (pending)
- `deliveries` (implemented in V9)
- `delivery_items` (implemented in V9)

Delivery lifecycle:

```text
Upcoming → In Transit → Received → Completed
```

Receiving a delivery should create inventory movement records automatically.

### Payment provider records

- `payment_provider_customers`
- `subscription_checkout_sessions` (implemented in V11)
- `payment_provider_events`
- `payment_provider_refunds`

Store POS payments and TindaKart subscription payments must be represented separately.

## Phase 5: API Design

Create APIs around business actions rather than exposing database tables directly.

Example API areas:

- `/auth`
- `/users`
- `/categories` (vendor-scoped implementation)
- `/products` (vendor-scoped implementation)
- `/products/barcode/{barcode}` (vendor-scoped implementation)
- `/inventory` (store-scoped batch listing foundation)
- `/inventory/receive` (store-scoped receiving foundation)
- `/inventory/adjustments` (store-scoped adjustment foundation)
- `/sales` (completed-sale transaction foundation)
- `/sales/{id}/receipt` (receipt preview and print-status API foundation)
- `/payments` (recorded within sale transaction)
- `/debt-accounts` (vendor-scoped API foundation)
- `/debt-accounts/{id}/payments` (payment history and allocation foundation)
- `/suppliers` (vendor-scoped API foundation)
- `/deliveries` (store-scoped lifecycle and receiving API foundation)
- `/reports` (store-scoped sales, low-stock, and expiration API foundation)
- `/settings` (vendor business/tax settings API foundation)
- `/super-admin/vendors`
- `/super-admin/packages`
- `/super-admin/subscriptions`
- `/vendors/{vendorId}/stores`
- `/vendors/{vendorId}/staff`
- `/billing/checkout`
- `/billing/webhooks/paymongo`

The backend must validate the authenticated user's vendor/store scope for every tenant-owned endpoint.

## Phase 5A: Vendor, Store, Package, and Subscription Management

- [x] Build Super Admin vendor management API foundation.
- [x] Build vendor onboarding and activation flow.
- [x] Build vendor suspension/reactivation flow.
- [x] Build vendor store/branch management API foundation.
- [x] Build Vendor Admin staff management API foundation.
- [x] Build store assignment for staff API foundation.
- [x] Add multi-store staff creation and reassignment controls in the PWA; final staff assignment policy remains owner-confirmed.
- [x] Build package management API foundation for Super Admin.
- [x] Add package prices, features, and limits API support.
- [x] Add Super Admin PWA controls for package feature inclusions and numeric limits.
- [x] Add Super Admin PWA controls to edit existing package prices, features, active state, and numeric limits.
- [x] Build vendor package selection API foundation.
- [x] Add subscription status: trial, active, past due, suspended, cancelled.
- [x] Enforce package feature gates and initial numeric limits in the backend (stores, staff, and products).
- [x] Hide unavailable PWA modules based on package features (backend enforcement remains authoritative).
- [x] Add initial Vendor Admin staff and business-settings management UI.
- [x] Add vendor-level, store-level, and staff-management audit events.
- [x] Add Super Admin audit-log review API and PWA view.
- [x] Verify Super Admin package creation and editing, including persisted feature and limit controls (`PackageManagementIntegrationTest`).
- [x] Seed the owner-operated Free plan with POS/Catalog access, one store, zero additional staff seats, and automatic subscription assignment.
- [x] Add vendor self-registration that provisions the vendor owner, first store, Vendor Admin role, and Free subscription.
- [x] Exclude the vendor owner from the paid staff-seat limit calculation.
- [x] Add Super Admin-managed custom add-on price records for staff seats and feature unlocks.
- [x] Add vendor custom-plan visibility and PayMongo checkout metadata for selected staff seats and feature add-ons.

## Phase 5B: PayMongo Subscription Billing

PayMongo should first be used for vendors paying TindaKart subscription fees:

```text
Vendor selects package
        ↓
Backend creates PayMongo checkout/payment request
        ↓
Vendor completes payment in PHP
        ↓
PayMongo webhook reaches backend
        ↓
Backend verifies and records the event
        ↓
Vendor subscription becomes active
        ↓
Package features and limits are enforced
```

- [x] Create a payment-provider abstraction so PayMongo can be replaced later.
- [x] Store PayMongo secret keys only in backend environment configuration.
- [x] Never expose secret keys to the PWA.
- [x] Create subscription checkout sessions from the backend.
- [x] Store provider checkout IDs.
- [x] Implement verified webhook handling foundation; valid and invalid HMAC cases are integration-tested.
- [x] Make webhook processing idempotent; duplicate events create one event/payment record.
- [x] Handle successful, failed, cancelled, and expired payments in the webhook foundation.
- [x] Update subscription status from verified provider events in the webhook foundation.
- [x] Add authorized POS cancellation/void handling with stock restoration; external payment refunds remain pending.
- [x] Add billing history for Vendor Admin; Super Admin package controls are available.
- [x] Configure PayMongo sandbox/test transactions for development.
- [ ] Confirm PayMongo account activation and supported payment methods before production.

PayMongo subscription billing is separate from store POS payments. POS payment methods are vendor-configurable and currently record cash, card, e-wallet, or credit transactions; online customer payment collection can be enabled later without changing the POS contract.

### Custom add-on billing model

The Free plan is an owner-operated starting point. Vendors can purchase additional staff seats and feature add-ons individually. Super Admin controls the monthly price of each staff seat and feature add-on. A custom checkout records the selected add-ons in checkout metadata; verified PayMongo payment events activate the purchased seats/features. The vendor owner is not counted as a purchased staff seat.

If TindaKart later collects and distributes customer payments among multiple vendors, connected-merchant or payment-splitting capabilities must be evaluated separately and activated with the provider before implementation.

### POS transaction flow

```text
Scan/search product
        ↓
Backend verifies product, price, status, and expiration
        ↓
Add to cart
        ↓
Calculate subtotal, bulk price, discount, VAT, and total
        ↓
Accept payment
        ↓
Backend transaction:
  record sale
  record sale items
  record payment
  deduct stock
  create receipt number
        ↓
Return receipt data
        ↓
Print or display receipt
```

### Inventory receiving flow

```text
Inventory screen
        ↓
Scan barcode
        ↓
Existing product?
  Yes → select product and add stock batch
  No  → create product and stock batch
        ↓
Record supplier, quantity, cost, price, and expiration
        ↓
Create inventory movement
```

## Phase 6: PWA Frontend Foundation

- [x] Create a responsive application shell foundation.
- [x] Add desktop sidebar navigation.
- [x] Add mobile compact menu navigation.
- [x] Add responsive breakpoints.
- [x] Add loading states across authenticated shell, administration, operations, reports, and inventory views.
- [x] Add error states to the implemented PWA workflows.
- [x] Add empty states to tables and alert panels.
- [x] Add reusable buttons, inputs, tables, badges, and cards foundation.
- [x] Add authentication state handling.
- [x] Add protected application view/authentication gate.
- [x] Add installable PWA manifest.
- [x] Add service worker.
- [x] Add app icons; splash assets remain platform/browser dependent.
- [x] Add a public responsive landing page with PWA install/download guidance and sign-in entry point.
- [ ] Configure HTTPS for deployed environments.
- [ ] Add responsive testing for laptop, tablet, Android, and iOS browser sizes.

### Mobile behavior

- Use large touch targets.
- Keep the cart visible and easy to reach.
- Avoid wide tables on phones.
- Convert tables to cards or horizontally scrollable sections where needed.
- Keep scanning and checkout accessible with minimal navigation.
- Preserve unsaved cart state during accidental navigation.
- [x] Preserve unsaved POS cart state in scoped browser storage across tab/navigation changes.

## Phase 7: POS Implementation

- [x] Add product search.
- [x] Add category filter.
- [x] Add A–Z sorting to the POS product display.
- [x] Add product cards or organized product list.
- [x] Add product creation form with piece/bulk and expiration fields.
- [x] Add barcode input for USB/Bluetooth keyboard-style scanners.
- [x] Add camera scanning foundation for supported mobile devices.
- [x] Add cart item quantity controls.
- [x] Add piece/unit display.
- [x] Add bulk pricing and quantity thresholds backend/frontend foundation.
- [x] Display item subtotal.
- [x] Display transaction subtotal.
- [x] Display discount.
- [x] Display VAT only when configured on the backend transaction.
- [x] Display total clearly.
- [x] Add payment method.
- [x] Apply vendor-configured POS payment methods with safe defaults and backend enforcement.
- [x] Add amount tendered and change.
- [x] Add remove item and void transaction controls (including authorized backend void with stock restoration).
- [x] Add insufficient-stock validation (backend rejects insufficient stock atomically; frontend surfaces the API error).
- [x] Add expired-product blocking.
- [x] Add successful sale confirmation.
- [x] Add receipt preview and browser printing; thermal layout/printer acceptance remains pending.

## Phase 8: Inventory and Expiration

- [x] Create a dedicated inventory dashboard foundation.
- [x] Show name, barcode, category, unit, quantity, retail/bulk prices, expiration, and status.
- [x] Add stock receiving.
- [x] Add stock adjustment with required reason.
- [x] Add inventory barcode input/camera scan that identifies an existing product before receiving stock.
- [x] Add scoped inventory movement history API and PWA view.
- [x] Add low-stock warnings.
- [x] Add near-expiration section.
- [ ] Define near-expiration threshold with the owner.
- [x] Allow authorized near-expiration discounts through Vendor Admin business settings; owner threshold confirmation remains pending.
- [x] Mark expired stock clearly.
- [x] Block expired stock at POS.
- [x] Add expired and near-expiration reports using the configured vendor threshold.

## Phase 9: Receipts and Printing

- [ ] Confirm business name, address, TIN, VAT status, and receipt requirements.
- [ ] Design receipt/invoice layout with the owner.
- [x] Add receipt numbering.
- [x] Add subtotal, discount, VAT, and total fields.
- [x] Add payment and change details.
- [x] Support browser print preview foundation.
- [x] Store configurable receipt output mode (browser, thermal bridge, or manual) for future device-specific printing.
- [ ] Test the selected thermal printer.
- [ ] Decide between browser printing, local print bridge, or desktop print helper.
- [x] Add controlled reprint capability.
- [x] Store generated receipt metadata.

Do not add VAT until the business tax setup is confirmed.

## Phase 10: Debt and Credit

- [x] Create a debt dashboard foundation.
- [x] Show total credit, total paid, and balance.
- [x] Show due date and account status API foundation.
- [x] Record credit sales from POS.
- [x] Add POS customer selection and due-date controls for credit sales.
- [x] Record full and partial payments API foundation.
- [x] Show payment history API foundation.
- [x] Add payment receipt numbering and API metadata; final print layout remains pending owner acceptance.
- [x] Add customer search.
- [x] Add Vendor Admin/Super Admin customer creation UI for POS credit sales.
- [x] Add debt aging report API and PWA view.
- [ ] Add SMS/reminder integration only if required.

## Phase 11: Delivery

- [x] Add supplier management API foundation.
- [x] Add delivery creation API foundation.
- [x] Add delivery creation and supplier selection UI.
- [x] Add expected delivery date.
- [x] Add products and quantities.
- [x] Add delivery status.
- [x] Add received, missing, and damaged quantities with controlled receiving API.
- [x] Update inventory when delivery is received.
- [x] Add upcoming deliveries dashboard foundation.
- [x] Add delivery history API foundation.

## Phase 12: Reporting

- [x] Daily sales API foundation.
- [x] Weekly sales API foundation.
- [x] Monthly sales API foundation.
- [x] Best-selling products.
- [x] Low-stock products API foundation.
- [x] Expired products API foundation.
- [x] Near-expiration products API foundation.
- [x] Debt balances API foundation.
- [x] Payments collected.
- [x] Delivery history report API, CSV inclusion, and PWA view.
- [x] Profit estimate.
- [x] VAT summary report when applicable; disabled-business state is displayed without adding VAT.
- [x] Browser print/Save as PDF and CSV export are implemented in the PWA; server-generated PDF remains optional.

## Phase 13: Testing

### Unit tests

- [x] Pricing calculations.
- [x] Piece and bulk pricing.
- [x] Discounts.
- [x] VAT calculations.
- [x] Debt balances.
- [x] Expiration rules.
- [x] Role permissions evaluator and package entitlement rules have unit coverage; end-to-end role isolation remains in integration testing.

### Integration tests

- [x] Sale records and deducts stock atomically (`PosWorkflowIntegrationTest`).
- [x] Insufficient stock is rejected without creating a sale (`PosWorkflowIntegrationTest`).
- [x] Expired products cannot be sold (`PosWorkflowIntegrationTest`).
- [x] Duplicate barcode behavior is correct: a duplicate is rejected with conflict and no second product is created (`CatalogBarcodeIntegrationTest`).
- [x] Delivery receiving updates inventory and records received/missing/damaged quantities (`DeliveryWorkflowIntegrationTest`).
- [x] Credit sales create debt records (`DebtWorkflowIntegrationTest`).
- [x] Debt payments update balances and payment history (`DebtWorkflowIntegrationTest`).
- [x] Unauthorized users cannot perform protected actions (`MultitenantAuthorizationIntegrationTest`).
- [x] Verify every declared `/api` route resolves through Spring's handler mapping (`ApiEndpointSmokeIntegrationTest`); functional workflow coverage remains in the endpoint-specific integration tests.

### Device and acceptance tests

- [ ] Laptop browser.
- [ ] Android phone.
- [ ] Android tablet.
- [ ] iPhone/iPad browser.
- [ ] USB barcode scanner.
- [ ] Bluetooth barcode scanner.
- [ ] Camera barcode scanner.
- [ ] Thermal receipt printer.
- [ ] Slow network behavior.
- [x] Database backup and restore tested with a temporary isolated database.

## Phase 14: Deployment and Handover

- [ ] Create development, staging, and production environments.
- [x] Store secrets outside source control: `.env` is ignored/untracked and application profiles consume environment-injected credentials.
- [ ] Configure HTTPS.
- [ ] Configure database backups.
- [ ] Configure monitoring and logs.
- [x] Create local deployment instructions in `HANDOVER_GUIDE.md`; production deployment remains pending.
- [x] Create user and role setup instructions.
- [x] Create cashier training guide.
- [x] Create troubleshooting guide.
- [x] Create database restore guide in `BACKUP_PROCEDURE.md`.
- [ ] Perform client acceptance testing.
- [ ] Run a controlled pilot before full rollout.

## Definition of Done for PWA Migration

The migration is ready for client use when:

- Users can log in with database-backed accounts.
- Super Admin, Vendor Admin, and Staff roles are enforced.
- Vendor and store data is isolated by backend authorization.
- Staff can access only assigned stores.
- Package features and limits control available vendor capabilities.
- Vendor subscriptions have active, failed, suspended, and cancelled states.
- PayMongo webhook events are verified and processed idempotently in sandbox testing.
- POS customer payments are kept separate from TindaKart subscription billing.
- Roles prevent unauthorized actions.
- Products can be searched and filtered by category.
- Barcode scanning works in POS and Inventory contexts.
- POS calculates quantity, price, subtotal, discount, VAT, and total correctly.
- Completed sales deduct inventory exactly once.
- Expired items cannot be sold.
- Inventory receiving creates traceable stock movements.
- Debt payments update balances and history correctly.
- Receipts can be generated and printed on the selected device.
- The interface works on laptop, tablet, and Android phone sizes.
- The PWA can be installed where supported.
- Database backup and restore has been tested.
- The owner has approved the workflows and receipt format.

## First Implementation Sprint

Before implementing features, complete these items:

- [x] Confirm the deployment model: local development first; LAN/cloud deployment remains a later decision.
- [x] Confirm PWA framework and backend framework.
- [x] Back up the current PostgreSQL database.
- [x] Document the current schema.
- [x] Design the target schema through versioned Flyway migrations.
- [x] Define the Super Admin → Vendor → Store → Staff hierarchy.
- [x] Define vendor, store, and user scopes.
- [x] Define permissions and package feature rules.
- [x] Define packages, pricing, limits, inclusions, and subscription statuses.
- [x] Define TindaKart subscription billing versus store POS payments.
- [ ] Confirm PayMongo sandbox/test account requirements.
- [x] Define user roles.
- [x] Define POS and inventory API contracts.
- [x] Create the backend project skeleton.
- [x] Create database migration tooling.
- [x] Create the PWA project skeleton.
- [x] Build a login page and protected application shell.
- [x] Build a health-check endpoint and database connection test.
- [x] Validate the first API request from the PWA to the backend.
- [x] Validate vendor/store isolation with a database-backed authorization integration test.
- [x] Validate the initial Super Admin bootstrap flow.

Only after this sprint should feature migration begin.
