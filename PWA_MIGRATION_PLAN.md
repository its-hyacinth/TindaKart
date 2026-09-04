# TindaKart PWA Migration Plan

## Implementation verification status

Checked items below mean the capability exists in the repository and has been verified by inspection or build validation. Partial capabilities remain unchecked until the complete workflow and tests exist.

- [x] Module 0: backend and PWA foundations created.
- [x] Module 1: database-backed authentication foundation created.
- [x] Multitenant foundation schema created.
- [x] Vendor/store API foundation created.
- [x] Backend Java compilation verified successfully.
- [ ] End-to-end migration and authentication integration tests.
- [ ] Complete scoped-role and package-feature authorization.
- [x] Frontend operations screen foundations for POS, Inventory, Catalog, Debt, Deliveries, and Reports.
- [ ] POS, inventory, debt, delivery, receipts, billing, and reporting workflows.
- [x] Centralized API error response and server logging foundation added.
- [x] Desktop sidebar and mobile compact navigation added to the operations shell.
- [x] Vendor Admin staff controls and business receipt/tax settings UI added.
- [x] Catalog category and product creation UI added.
- [x] Subscription package creation, selection, checkout, and billing-history UI foundation added.

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
- [ ] Confirm whether staff may be assigned to multiple stores.
- [x] Confirm Super Admin responsibilities and platform ownership.
- [ ] Confirm vendor onboarding, suspension, and deletion rules.
- [x] Confirm package names, prices, limits, inclusions, and feature permissions as platform-managed data.
- [x] Confirm that vendors pay TindaKart subscription fees online using a PH sandbox provider during development.
- [ ] Confirm whether POS customer payments will use PayMongo or remain cash/manual initially.
- [ ] Confirm the number of simultaneous users and cashiers.
- [ ] Confirm whether internet loss must be supported.
- [ ] Confirm the preferred receipt/thermal printer.
- [ ] Confirm whether camera barcode scanning is required on mobile.
- [ ] Confirm whether the first release needs iOS home-screen installation.
- [ ] Confirm VAT, TIN, invoice, and receipt requirements with the owner/accountant.
- [ ] Confirm staff roles and permissions.
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

- [ ] Back up the existing PostgreSQL database.
- [ ] Create a restore procedure and test it.
- [ ] Export the current table definitions.
- [ ] Document existing columns, constraints, indexes, and relationships.
- [ ] Identify duplicate or incomplete products.
- [ ] Identify debt records without matching customer profiles.
- [ ] Identify products that have sales history.
- [ ] Record the existing sample/demo data separately from real client data.
- [ ] Decide whether current IDs must be preserved during migration.
- [ ] Record the current PostgreSQL version and connection settings.

No migration should run against the client database without a verified backup and rollback plan.

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
- [ ] Add database transaction boundaries in the backend.
- [ ] Add API documentation.
- [ ] Add development, test, and production configuration profiles.
- [ ] Add tenant context resolution from the authenticated user.
- [ ] Enforce vendor/store scope in backend queries and service methods.
- [ ] Prevent clients from selecting arbitrary vendor or store IDs.
- [ ] Add platform, vendor, and store-level authorization checks.

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
- [ ] Add account activation/deactivation.
- [ ] Add password change/reset flow.
- [x] Add role-based permissions foundation.
- [x] Protect implemented admin-only API routes on the backend.
- [ ] Add login attempt protection.
- [ ] Add audit logging for login and sensitive actions.
- [x] Add platform-level, vendor-level, and store-level scope schema.
- [x] Add package feature and limit schema.
- [ ] Add a current vendor/store context to authenticated sessions.

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
- [ ] Build vendor onboarding and activation flow.
- [ ] Build vendor suspension/reactivation flow.
- [x] Build vendor store/branch management API foundation.
- [x] Build Vendor Admin staff management API foundation.
- [x] Build store assignment for staff API foundation.
- [x] Build package management API foundation for Super Admin.
- [x] Add package prices, features, and limits API support.
- [x] Build vendor package selection API foundation.
- [x] Add subscription status: trial, active, past due, suspended, cancelled.
- [x] Enforce package feature gates and initial numeric limits in the backend (stores, staff, and products).
- [ ] Hide or disable unavailable PWA modules based on package features.
- [x] Add initial Vendor Admin staff and business-settings management UI.
- [ ] Add vendor-level and store-level audit events.

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

- [ ] Create a payment-provider abstraction so PayMongo can be replaced later.
- [x] Store PayMongo secret keys only in backend environment configuration.
- [x] Never expose secret keys to the PWA.
- [x] Create subscription checkout sessions from the backend.
- [x] Store provider checkout IDs.
- [x] Implement verified webhook handling foundation.
- [x] Make webhook processing idempotent.
- [x] Handle successful, failed, cancelled, and expired payments in the webhook foundation.
- [x] Update subscription status from verified provider events in the webhook foundation.
- [ ] Add refund/cancellation handling.
- [x] Add billing history for Vendor Admin; Super Admin package controls are available.
- [x] Configure PayMongo sandbox/test transactions for development.
- [ ] Confirm PayMongo account activation and supported payment methods before production.

PayMongo subscription billing is separate from store POS payments. POS can initially support cash and manually recorded payment methods. Online customer payments can be added later.

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
- [ ] Add loading states.
- [ ] Add error states.
- [ ] Add empty states.
- [x] Add reusable buttons, inputs, tables, badges, and cards foundation.
- [x] Add authentication state handling.
- [x] Add protected application view/authentication gate.
- [x] Add installable PWA manifest.
- [x] Add service worker.
- [ ] Add app icons and splash assets.
- [ ] Configure HTTPS for deployed environments.
- [ ] Add responsive testing for laptop, tablet, Android, and iOS browser sizes.

### Mobile behavior

- Use large touch targets.
- Keep the cart visible and easy to reach.
- Avoid wide tables on phones.
- Convert tables to cards or horizontally scrollable sections where needed.
- Keep scanning and checkout accessible with minimal navigation.
- Preserve unsaved cart state during accidental navigation.

## Phase 7: POS Implementation

- [x] Add product search.
- [x] Add category filter.
- [ ] Add A–Z sorting.
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
- [x] Add amount tendered and change.
- [ ] Add remove item and void transaction controls.
- [ ] Add insufficient-stock validation.
- [x] Add expired-product blocking.
- [x] Add successful sale confirmation.
- [ ] Add receipt preview and printing.

## Phase 8: Inventory and Expiration

- [x] Create a dedicated inventory dashboard foundation.
- [ ] Show name, barcode, category, unit, quantity, prices, expiration, and status.
- [x] Add stock receiving.
- [x] Add stock adjustment with required reason.
- [ ] Add inventory movement history.
- [ ] Add low-stock warnings.
- [ ] Add near-expiration section.
- [ ] Define near-expiration threshold with the owner.
- [ ] Allow authorized near-expiration discounts.
- [ ] Mark expired stock clearly.
- [ ] Block expired stock at POS.
- [ ] Add expired and near-expiration reports.

## Phase 9: Receipts and Printing

- [ ] Confirm business name, address, TIN, VAT status, and receipt requirements.
- [ ] Design receipt/invoice layout with the owner.
- [x] Add receipt numbering.
- [x] Add subtotal, discount, VAT, and total fields.
- [x] Add payment and change details.
- [x] Support browser print preview foundation.
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
- [x] Record full and partial payments API foundation.
- [x] Show payment history API foundation.
- [ ] Add payment receipt.
- [x] Add customer search.
- [ ] Add debt aging report.
- [ ] Add SMS/reminder integration only if required.

## Phase 11: Delivery

- [x] Add supplier management API foundation.
- [x] Add delivery creation API foundation.
- [x] Add delivery creation and supplier selection UI.
- [x] Add expected delivery date.
- [x] Add products and quantities.
- [x] Add delivery status.
- [ ] Add received, missing, and damaged quantities.
- [x] Update inventory when delivery is received.
- [x] Add upcoming deliveries dashboard foundation.
- [x] Add delivery history API foundation.

## Phase 12: Reporting

- [x] Daily sales API foundation.
- [x] Weekly sales API foundation.
- [x] Monthly sales API foundation.
- [ ] Best-selling products.
- [x] Low-stock products API foundation.
- [x] Expired products API foundation.
- [x] Near-expiration products API foundation.
- [x] Debt balances API foundation.
- [ ] Payments collected.
- [ ] Delivery history.
- [ ] Profit estimate.
- [ ] VAT summary when applicable.
- [ ] CSV/PDF export.

## Phase 13: Testing

### Unit tests

- [x] Pricing calculations.
- [x] Piece and bulk pricing.
- [x] Discounts.
- [x] VAT calculations.
- [ ] Debt balances.
- [ ] Expiration rules.
- [ ] Role permissions.

### Integration tests

- [ ] Sale records and deducts stock atomically.
- [ ] Insufficient stock is rejected.
- [ ] Expired products cannot be sold.
- [ ] Duplicate barcode behavior is correct.
- [ ] Delivery receiving updates inventory.
- [ ] Credit sales create debt records.
- [ ] Debt payments update balances.
- [ ] Unauthorized users cannot perform protected actions.

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
- [ ] Database backup and restore.

## Phase 14: Deployment and Handover

- [ ] Create development, staging, and production environments.
- [ ] Store secrets outside source control.
- [ ] Configure HTTPS.
- [ ] Configure database backups.
- [ ] Configure monitoring and logs.
- [ ] Create deployment instructions.
- [ ] Create user and role setup instructions.
- [ ] Create cashier training guide.
- [ ] Create troubleshooting guide.
- [ ] Create database restore guide.
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

- [ ] Confirm the deployment model: local, LAN, or cloud.
- [ ] Confirm PWA framework and backend framework.
- [ ] Back up the current PostgreSQL database.
- [ ] Document the current schema.
- [ ] Design the target schema.
- [ ] Define the Super Admin → Vendor → Store → Staff hierarchy.
- [ ] Define vendor, store, and user scopes.
- [ ] Define permissions and package feature rules.
- [ ] Define packages, pricing, limits, inclusions, and subscription statuses.
- [ ] Define TindaKart subscription billing versus store POS payments.
- [ ] Confirm PayMongo sandbox/test account requirements.
- [ ] Define user roles.
- [ ] Define POS and inventory API contracts.
- [ ] Create the backend project skeleton.
- [ ] Create database migration tooling.
- [ ] Create the PWA project skeleton.
- [ ] Build a login page and protected application shell.
- [ ] Build a health-check endpoint and database connection test.
- [ ] Validate the first API request from the PWA to the backend.
- [ ] Validate vendor/store isolation with an authorization test.
- [ ] Validate the initial Super Admin bootstrap flow.

Only after this sprint should feature migration begin.
