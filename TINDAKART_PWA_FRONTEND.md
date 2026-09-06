# TindaKart PWA — Frontend UI/UX & Codex Implementation Guide

> Scope: frontend structure and interactive mock implementation only.
> Stack: React + TypeScript + Vite + Tailwind CSS + shadcn/ui.
> Product: Philippine general-store / neighborhood-retail POS PWA.

---

## 1. Core Product Model

The new frontend MUST use this hierarchy:

```text
Super Admin
   └── Stores
       └── Staff
```

Do **not** implement a Vendor layer.

Everything previously attached to a Vendor moves to the Store level, including:

- subscription/package
- billing visibility
- store profile and business details
- products and categories
- prices
- inventory
- suppliers
- purchase orders
- deliveries
- customers
- debt/credit
- staff
- permissions
- reports
- receipt settings
- payment settings
- audit logs

The only exception:

- **Only the Super Admin can create a Store.**

A Store Manager is a staff role, not another tenant/entity layer.

---

# 2. Role Model

## Super Admin

Platform-wide access.

Can:

- view platform dashboard
- create stores
- edit stores
- activate/suspend stores
- assign or change packages
- see subscription state
- manage package definitions
- view all stores
- inspect platform audit logs
- inspect platform-wide analytics
- manage platform settings

## Store Manager

Store-scoped staff role.

Can be allowed to:

- manage store staff
- manage products/categories
- manage pricing
- receive inventory
- adjust inventory
- manage suppliers
- create purchase orders
- receive deliveries
- manage debt/customer credit
- configure store and receipt settings
- view reports
- see subscription information

Store Manager **cannot create a Store**.

## Cashier

Can be allowed to:

- use POS
- scan/search products
- create sales
- process approved discounts
- accept supported payment methods
- print receipts
- view own/relevant transaction history
- create credit sales only when permitted

## Inventory Staff

Can be allowed to:

- view products
- receive stock
- adjust stock
- view batches
- review expiration
- manage delivery receiving

## Debt / Collection Staff

Can be allowed to:

- search customers
- view debt balances
- record payments
- view payment history
- create payment receipt records
- send reminders when feature is enabled

---

# 3. Frontend Technology

Use:

```text
React
TypeScript
Vite
Tailwind CSS
shadcn/ui
React Router
TanStack Query
React Hook Form
Zod
MSW
Zustand
Lucide React
Recharts
vite-plugin-pwa
```

Use shadcn/ui as the component foundation.

Do not add MUI, Mantine, Chakra, Ant Design, or another competing component framework.

---

# 4. Product Design Direction

TindaKart should look like a polished retail SaaS product, not a generic admin template.

Design keywords:

```text
professional
calm
fast
trustworthy
retail-focused
mobile-first where needed
dense but readable
operational
touch-friendly
accessible
```

Avoid:

- giant rounded dashboard cards everywhere
- excessive gradients
- neon colors
- glassmorphism
- weak gray-on-gray contrast
- huge hero sections inside business screens
- decorative charts without purpose
- floating elements that block POS usage
- tiny mobile controls
- wide desktop tables forced into mobile
- inconsistent page spacing
- different design styles between modules

---

# 5. Visual Design System

## Color

Suggested semantic direction:

```text
Primary         deep emerald / retail green
Background      very light neutral gray
Surface         white
Foreground      charcoal / near-black
Muted           neutral gray
Success         green
Warning         amber
Danger          red
Info            blue
```

Use CSS variables/design tokens.

Example concept:

```css
--background
--foreground
--card
--card-foreground
--popover
--popover-foreground
--primary
--primary-foreground
--secondary
--secondary-foreground
--muted
--muted-foreground
--accent
--accent-foreground
--destructive
--border
--input
--ring
```

Support dark mode only if it can be implemented cleanly without hurting POS readability. Light mode is the primary experience.

## Typography

Prefer:

```text
Inter
or
Geist
```

Desktop:

```text
Page title       24–30px
Section title    18–20px
Body             14–16px
Table text       14px minimum
Supporting text  12–13px
```

Mobile:

```text
Page title       22–26px
Body             14–16px
Touch labels     14–16px
```

Use tabular numerals where appropriate for prices and totals.

## Spacing

Use a consistent 4px-based spacing scale.

Recommended:

```text
Mobile page padding       16px
Tablet page padding       20–24px
Desktop page padding      24–32px
Section gap               20–24px
Panel padding             16–24px
Form field gap            16px
Dense data row height     44–52px
Touch target minimum      ~44px
```

---

# 6. Philippine Retail Context

The UI must feel natural for Philippine store operations.

Use:

```text
Currency: Philippine Peso
Locale: en-PH
Timezone: Asia/Manila
```

Money formatter:

```ts
new Intl.NumberFormat("en-PH", {
  style: "currency",
  currency: "PHP",
}).format(value);
```

Support mock payment methods:

```text
Cash
GCash
Maya
Card
Credit
```

Do not assume all stores are VAT registered.

Store/receipt settings must support:

```text
Business name
Branch name
Address
Phone
Email
TIN
VAT registered toggle
Receipt footer
Receipt numbering
```

Barcode behavior must support:

- USB/Bluetooth keyboard-style scanner
- mobile camera scan entry point
- direct search fallback

---

# 7. Responsive Application Shell

## Desktop

Use:

```text
Left sidebar
Top app bar
Breadcrumb/context row
Main content
Optional contextual right panel
```

Sidebar should be collapsible.

Top app bar may include:

- Store switcher
- Search/command trigger
- Network status
- Notifications
- User menu

## Tablet

- collapsible sidebar
- reduced padding
- 2-column layouts where space permits
- POS remains optimized for touch

## Mobile

Use:

```text
Compact top app bar
Bottom navigation for frequent staff destinations
More drawer/sheet for secondary destinations
```

Recommended store-level mobile nav:

```text
Home
POS
Inventory
Customers
More
```

Cashier variant:

```text
Home
POS
Sales
More
```

---

# 8. Navigation

## Store navigation

```text
Overview
  Dashboard

Sales
  Point of Sale
  Transactions
  Returns & Refunds

Inventory
  Products
  Categories
  Stock
  Receiving
  Adjustments
  Expiration

Customers
  Customers
  Credit & Debt

Supply
  Suppliers
  Purchase Orders
  Deliveries

Reports
  Sales
  Inventory
  Debt
  Profit

Administration
  Staff
  Store Settings
  Receipt Settings
  Payment Settings
  Subscription
  Audit Log
```

Navigation is permission-aware.

Never show inaccessible links unless there is a deliberate disabled/upsell state.

---

# 9. Routing

Use React Router nested routes.

```text
/
├── login
├── forgot-password
│
├── admin
│   ├── dashboard
│   ├── stores
│   │   ├── index
│   │   ├── new
│   │   └── :storeId
│   │       ├── overview
│   │       ├── subscription
│   │       ├── billing
│   │       └── audit
│   ├── packages
│   ├── subscriptions
│   ├── audit
│   └── settings
│
└── stores
    └── :storeId
        ├── dashboard
        ├── pos
        ├── sales
        │   ├── transactions
        │   ├── :saleId
        │   └── returns
        ├── inventory
        │   ├── products
        │   ├── products/new
        │   ├── products/:productId
        │   ├── categories
        │   ├── stock
        │   ├── receiving
        │   ├── adjustments
        │   └── expiration
        ├── customers
        │   ├── index
        │   ├── :customerId
        │   └── debt
        ├── suppliers
        ├── purchase-orders
        │   ├── index
        │   ├── new
        │   └── :purchaseOrderId
        ├── deliveries
        │   ├── index
        │   └── :deliveryId
        ├── reports
        │   ├── sales
        │   ├── inventory
        │   ├── debt
        │   └── profit
        ├── staff
        │   ├── index
        │   └── :staffId
        ├── settings
        │   ├── business
        │   ├── receipt
        │   ├── payments
        │   └── permissions
        ├── subscription
        └── audit
```

Route rules:

```text
/admin/**                 SUPER_ADMIN only
/stores/:storeId/**       assigned-store access required
specific page             permission required
```

Also implement:

- 404
- Unauthorized
- preserved intended route after login
- store-switch route preservation where possible

---

# 10. Folder Structure

Use feature-first organization.

```text
src/
├── app/
│   ├── App.tsx
│   ├── providers.tsx
│   ├── router.tsx
│   ├── routes.ts
│   └── route-guards/
│       ├── auth-guard.tsx
│       ├── admin-guard.tsx
│       ├── store-guard.tsx
│       └── permission-guard.tsx
│
├── assets/
│
├── components/
│   ├── ui/
│   ├── common/
│   │   ├── app-logo.tsx
│   │   ├── page-header.tsx
│   │   ├── empty-state.tsx
│   │   ├── error-state.tsx
│   │   ├── loading-state.tsx
│   │   ├── status-badge.tsx
│   │   ├── money.tsx
│   │   ├── responsive-data-list.tsx
│   │   └── confirm-dialog.tsx
│   └── layout/
│       ├── app-sidebar.tsx
│       ├── app-header.tsx
│       ├── mobile-nav.tsx
│       ├── store-switcher.tsx
│       └── breadcrumbs.tsx
│
├── features/
│   ├── auth/
│   ├── super-admin/
│   │   ├── dashboard/
│   │   ├── stores/
│   │   ├── packages/
│   │   ├── subscriptions/
│   │   ├── audit/
│   │   └── settings/
│   ├── store-dashboard/
│   ├── pos/
│   ├── sales/
│   ├── inventory/
│   │   ├── products/
│   │   ├── categories/
│   │   ├── stock/
│   │   ├── receiving/
│   │   ├── adjustments/
│   │   └── expiration/
│   ├── customers/
│   ├── debt/
│   ├── suppliers/
│   ├── purchase-orders/
│   ├── deliveries/
│   ├── reports/
│   ├── staff/
│   ├── store-settings/
│   ├── subscription/
│   └── audit/
│
├── hooks/
│
├── lib/
│   ├── cn.ts
│   ├── currency.ts
│   ├── dates.ts
│   ├── permissions.ts
│   ├── storage.ts
│   └── constants.ts
│
├── mocks/
│   ├── browser.ts
│   ├── handlers/
│   ├── fixtures/
│   └── db/
│
├── services/
│   ├── api-client.ts
│   └── query-client.ts
│
├── stores/
│   ├── auth-store.ts
│   ├── cart-store.ts
│   └── ui-store.ts
│
├── types/
│   ├── auth.ts
│   ├── store.ts
│   ├── staff.ts
│   ├── product.ts
│   ├── inventory.ts
│   ├── sale.ts
│   ├── customer.ts
│   ├── debt.ts
│   └── subscription.ts
│
├── styles/
│   └── globals.css
│
└── main.tsx
```

Inside each feature:

```text
feature/
├── api/
├── components/
├── hooks/
├── pages/
├── schemas/
├── types/
└── utils/
```

---

# 11. Data & State Strategy

## TanStack Query

Use for server-like data:

- stores
- staff
- products
- inventory
- customers
- debts
- sales
- suppliers
- purchase orders
- deliveries
- reports
- subscription
- audit

## React Hook Form + Zod

Use for:

- login
- store creation
- store edit
- product forms
- checkout
- receiving
- stock adjustment
- customer forms
- debt payment
- supplier forms
- purchase orders
- delivery receiving
- staff forms
- settings

## Zustand

Use only for:

- POS cart
- held carts
- small UI state
- persisted current-sale state

Do not mirror TanStack Query collections into Zustand.

---

# 12. Mock API Architecture

Use MSW.

Do not put mock arrays directly inside page components.

Suggested API surface:

```text
GET    /api/me

GET    /api/admin/dashboard
GET    /api/admin/stores
POST   /api/admin/stores
GET    /api/admin/stores/:storeId
PATCH  /api/admin/stores/:storeId
GET    /api/admin/packages
GET    /api/admin/subscriptions
GET    /api/admin/audit

GET    /api/stores/:storeId/dashboard

GET    /api/stores/:storeId/products
POST   /api/stores/:storeId/products
GET    /api/stores/:storeId/products/:productId
PATCH  /api/stores/:storeId/products/:productId

GET    /api/stores/:storeId/inventory
POST   /api/stores/:storeId/inventory/receive
POST   /api/stores/:storeId/inventory/adjustments

GET    /api/stores/:storeId/sales
POST   /api/stores/:storeId/sales
GET    /api/stores/:storeId/sales/:saleId
POST   /api/stores/:storeId/sales/:saleId/refund

GET    /api/stores/:storeId/customers
GET    /api/stores/:storeId/customers/:customerId

GET    /api/stores/:storeId/debt-accounts
POST   /api/stores/:storeId/debt-accounts/:id/payments

GET    /api/stores/:storeId/suppliers
GET    /api/stores/:storeId/purchase-orders
POST   /api/stores/:storeId/purchase-orders
GET    /api/stores/:storeId/deliveries

GET    /api/stores/:storeId/staff
POST   /api/stores/:storeId/staff
PATCH  /api/stores/:storeId/staff/:staffId

GET    /api/stores/:storeId/reports/sales
GET    /api/stores/:storeId/reports/inventory
GET    /api/stores/:storeId/reports/debt
GET    /api/stores/:storeId/reports/profit

GET    /api/stores/:storeId/settings
PATCH  /api/stores/:storeId/settings

GET    /api/stores/:storeId/subscription
GET    /api/stores/:storeId/audit
```

---

# 13. Realistic Mock Data

Use fictional Philippine store data.

Stores:

```text
TindaKart Mabolo
TindaKart Lahug
TindaKart Guadalupe
```

Products can include:

```text
Lucky Me! Pancit Canton
Nescafé 3-in-1
Bear Brand Powdered Milk
Century Tuna
Datu Puti Soy Sauce
Silver Swan Vinegar
Surf Detergent
Safeguard Soap
Coca-Cola 1.5L
Nature Spring 1L
SkyFlakes Crackers
Piattos Cheese
Eggs
Rice per kilo
Cooking Oil 1L
```

Use realistic but fictional:

- SKU
- barcode
- quantity
- category
- retail price
- bulk price
- reorder level
- expiration batch
- supplier

Do not use real customer private data.

---

# 14. Mock Accounts

```text
superadmin@tindakart.local
Role: SUPER_ADMIN
```

```text
manager@tindakart.local
Role: STORE_MANAGER
Stores: Mabolo, Lahug
```

```text
cashier@tindakart.local
Role: CASHIER
Store: Mabolo
```

```text
inventory@tindakart.local
Role: INVENTORY_STAFF
Store: Mabolo
```

Permission examples:

```text
dashboard.view
pos.use
sales.view
sales.refund
inventory.view
inventory.manage
inventory.receive
inventory.adjust
customers.view
debt.view
debt.manage
suppliers.view
suppliers.manage
purchase_orders.view
purchase_orders.manage
deliveries.view
deliveries.manage
staff.view
staff.manage
reports.view
settings.view
settings.manage
subscription.view
audit.view
```

Build navigation from permission config instead of scattered role checks.

---

# 15. Login Screen

## Desktop

Prefer a clean split layout or centered professional auth card.

Must include:

- TindaKart branding
- email
- password
- password visibility toggle
- remember session
- forgot password
- sign-in button
- loading state
- invalid credentials state
- inactive account state
- offline warning

Development-only:

- mock account quick-fill controls

## Mobile

- single column
- no giant illustration
- form should remain above fold where practical

---

# 16. Super Admin Dashboard

The dashboard should answer:

- How many stores are active?
- Which stores need attention?
- Which subscriptions are expiring or past due?
- Which stores were recently added?
- Is platform activity healthy?

Recommended layout:

```text
Header
KPI strip
Sales/usage trend
Subscription health
Recent stores
Alerts / recent admin events
```

Metrics:

- Total Stores
- Active Stores
- Trial Stores
- Past Due / Suspended
- Staff Accounts
- Platform Sales Snapshot

Primary CTA:

```text
+ Add Store
```

---

# 17. Super Admin — Stores

Toolbar:

- search
- status
- package
- subscription state
- location
- Add Store

Desktop columns:

```text
Store
Location
Manager
Staff
Package
Subscription
Last Activity
Status
Actions
```

Row actions:

- View
- Edit
- Change package
- Suspend/reactivate
- View subscription
- View audit

Mobile:

Use condensed store cards.

---

# 18. Add Store Flow

Only Super Admin gets this route and action.

Use a full route, not a tiny modal.

Sections:

## Store Identity

- store name
- branch label
- phone
- email

## Address

- region
- province
- city/municipality
- barangay
- detailed address

## Business

- receipt business name
- TIN
- VAT registration toggle

## Package

- package
- trial/active state
- package limits summary

## Initial Manager

- full name
- email
- role
- temporary password / invite status

Final step:

```text
Review → Create Store
```

---

# 19. Store Dashboard

The store dashboard should answer:

- sales today
- transaction count
- average basket
- estimated gross profit
- low stock
- near expiration
- overdue debt
- pending deliveries

Recommended page:

```text
Store context header
KPI strip
7-day sales chart
Operational attention panel
Top selling products
Recent activity
```

Primary CTA:

```text
New Sale
```

Avoid a wall of cards.

---

# 20. POS Design — Highest Priority

The POS is the most important operational screen.

## Desktop

Use a true workspace layout.

```text
┌────────────────────────────────────────────────────────────┐
│ Store / scanner status / held sales / user                │
├───────────────────────────────────┬────────────────────────┤
│ Search / barcode / camera         │ Current Sale           │
│ Categories                        │ Customer               │
│                                   │                        │
│ Product grid/list                 │ Cart lines             │
│                                   │                        │
│                                   │ Totals                 │
│                                   │ Payment CTA            │
└───────────────────────────────────┴────────────────────────┘
```

Approximate width:

```text
Products  62–68%
Cart      32–38%
```

Product tile:

- product name
- unit
- price
- optional bulk price
- stock
- low-stock or expiry warning when relevant

Do not rely on product images.

Cart row:

- name
- unit price
- quantity stepper
- subtotal
- remove
- stock warning

Totals:

```text
Subtotal
Discount
VAT if configured
TOTAL
```

Total must be visually strongest.

Actions:

```text
Hold
Clear
Pay
```

## POS Keyboard/Scanner Behavior

- barcode input is always easy to refocus
- scanner may append Enter
- matching barcode immediately adds/increments item
- unknown barcode opens clear “not found” state
- keyboard navigation cannot be trapped

## Mobile POS

Use:

- sticky search/scanner controls
- product list/grid
- sticky cart summary

Example:

```text
3 items • ₱420.50        View Cart
```

Cart opens as a full-height sheet/page.

Checkout is a dedicated screen/step.

---

# 21. Checkout

Desktop:

- focused dialog or side sheet

Mobile:

- full-screen flow

Steps:

```text
1. Review total
2. Choose method
3. Enter payment details
4. Confirm
5. Success
6. Receipt
```

Payment methods:

```text
Cash
GCash
Maya
Card
Credit
```

Cash UI:

- amount due
- amount tendered
- quick cash buttons
- exact amount
- change

Success:

- receipt number
- total
- payment
- change
- cashier
- timestamp

Actions:

```text
Print Receipt
New Sale
View Transaction
```

Mock completion must:

- add transaction
- reduce stock
- generate receipt number
- clear active cart only after success

---

# 22. Products

Toolbar:

- search
- category
- stock status
- expiry
- sort
- Add Product

Desktop columns:

```text
Product
SKU / Barcode
Category
Unit
Price
Stock
Reorder
Nearest Expiration
Status
Actions
```

Mobile product row/card:

- name
- price
- stock
- category
- warning badge
- action menu

Product detail tabs:

```text
Overview
Pricing
Batches / Stock
Movement History
Audit
```

---

# 23. Add / Edit Product

Sections:

```text
Basic Information
Barcode & SKU
Category & Unit
Pricing
Stock Rules
Expiration
```

Fields:

- name
- description
- SKU
- barcode(s)
- category
- unit
- cost
- retail
- bulk price optional
- bulk threshold optional
- reorder level
- expiration applicable
- active/archive

Do not make this a cramped dialog.

---

# 24. Inventory Receiving

Flow:

```text
Scan / search
      ↓
Product found?
      ↓
Existing product OR Create product
      ↓
Supplier
      ↓
Quantity
      ↓
Unit cost
      ↓
Selling price
      ↓
Batch
      ↓
Expiration if applicable
      ↓
Review
      ↓
Receive
```

Success must:

- update mock stock
- add batch
- create inventory movement

---

# 25. Stock Adjustment

Required:

- product
- current stock read-only
- adjustment type
- quantity
- reason
- note

Adjustment types:

```text
Add
Remove
Correction
Damage
Loss
Expired
Return
```

Prevent negative stock.

Create mock movement/audit record.

---

# 26. Expiration

Tabs:

```text
Near Expiration
Expired
All Batches
```

Columns/data:

- product
- batch
- quantity
- expiration
- days remaining
- status
- action

Expired batch:

- red/destructive status
- unavailable in POS

Near-expiry:

- warning status
- optionally allow authorized markdown/discount workflow

---

# 27. Sales

Transaction list:

- receipt
- timestamp
- cashier
- customer
- payment method
- item count
- total
- status

Filters:

- date range
- cashier
- payment
- status
- amount

Detail:

- receipt information
- items
- subtotal
- discounts
- VAT when enabled
- payment
- change
- audit timeline
- print/reprint
- refund

Refund requires:

- items
- quantities
- reason
- confirmation

---

# 28. Customers & Debt

## Customer List

- name
- phone
- balance
- last purchase
- status

## Customer Detail

Tabs/sections:

- profile
- sales
- credit
- payments
- notes

## Debt Dashboard

Metrics:

- total outstanding
- overdue
- collected this month
- customers with balance

Table:

```text
Customer
Total Credit
Paid
Balance
Due
Status
Last Payment
```

Statuses:

```text
Current
Due Soon
Overdue
Settled
```

Payment flow:

- amount
- method
- date
- reference
- note
- balance-after-payment preview

---

# 29. Suppliers

Supplier list:

- supplier
- contact
- active PO count
- incoming delivery count
- recent delivery
- status

Supplier detail:

- contact
- products supplied
- purchase orders
- delivery history
- purchase summary

---

# 30. Purchase Orders

Statuses:

```text
Draft
Submitted
Partially Received
Received
Cancelled
```

Create PO:

- supplier
- requested delivery date
- products
- quantity
- unit cost
- line subtotal
- total
- notes
- review

---

# 31. Deliveries

Statuses:

```text
Upcoming
In Transit
Received
Completed
```

Receiving interface:

```text
Expected
Received
Missing
Damaged
Batch
Expiration
```

Completing receiving must update mock inventory.

---

# 32. Staff Management

Store-scoped.

Columns:

- name
- role
- assigned store
- permissions summary
- last active
- status
- actions

Create/Edit:

- name
- email
- phone
- role
- assigned store(s) user is allowed to manage
- permissions
- account status

Desktop permission editor:

- grouped permission matrix

Mobile:

- grouped accordion sections

---

# 33. Store Settings

Tabs:

```text
Business
Receipt
Payments
Permissions
Subscription
```

## Business

- name
- branch
- address
- contact
- TIN
- VAT
- timezone

## Receipt

- receipt name
- address
- TIN
- footer
- optional fields
- preview

On desktop show live receipt preview beside controls.

## Payments

Enable/disable:

- Cash
- GCash
- Maya
- Card
- Credit

## Subscription

Show:

- package
- status
- renewal
- limits
- features
- usage
- billing history

Store users cannot arbitrarily change package unless permission/product rules allow it.

---

# 34. Reports

Global controls:

- date
- compare period
- export
- store only for platform/multi-store context

## Sales

- revenue
- transactions
- average basket
- gross profit estimate
- trend
- top products
- payment mix
- detailed table

## Inventory

- inventory value
- low stock
- out of stock
- near expiry
- expired
- movement summary
- detailed table

## Debt

- outstanding
- overdue
- collected
- aging
- detailed customer balances

## Profit

- revenue
- estimated COGS
- gross profit
- gross margin
- trend
- product/category breakdown

Charts must always have a useful accompanying table where appropriate.

---

# 35. Loading, Error & Empty States

Every data route must support:

```text
Loading
Error
Retry
Empty
Content
```

Examples:

```text
No products yet
Add your first product to start selling.
[ Add Product ]
```

```text
No results
No products match your current filters.
[ Clear Filters ]
```

Offline status:

- persistent but unobtrusive
- clear indicator in app shell
- explain limited production behavior

---

# 36. Forms & Dialogs

Use shadcn:

- Button
- Input
- Label
- Form
- Textarea
- Select
- Combobox
- Checkbox
- Switch
- Tabs
- Dialog
- AlertDialog
- Sheet
- Drawer
- DropdownMenu
- Command
- Popover
- Tooltip
- Badge
- Table
- Skeleton
- Breadcrumb
- Separator
- Pagination
- Sonner

Rules:

- destructive actions require confirmation
- large forms use a page or large sheet
- errors display beside fields
- prevent duplicate submission
- keep values after validation failure
- placeholders are not labels
- disabled controls explain why when unclear

---

# 37. Tables

Desktop:

- sticky header for long tables
- right-aligned money and numeric columns
- compact row action menu
- filters
- pagination
- active filter badges
- clear filters action
- avoid excessive vertical borders

Mobile:

Do not squeeze desktop tables.

Use:

- responsive cards
- stacked data rows
- optional horizontal scrolling only when necessary

---

# 38. Accessibility

Target WCAG 2.2 AA where practical.

Required:

- semantic page landmarks
- real headings
- labels
- keyboard access
- visible focus
- accessible dialogs
- accessible names
- tooltips / aria-label on icon buttons
- adequate contrast
- reduced motion
- 44px touch target
- status not dependent on color
- skip-to-content
- correct table semantics
- scanner input does not hijack normal typing permanently

---

# 39. PWA

Configure:

- manifest
- standalone display
- theme/background colors
- icons
- service worker
- app-shell caching
- offline route
- online/offline state
- update available toast/banner
- install prompt where supported

Mock phase:

- preserve POS cart using localStorage
- cache app shell
- expose connectivity
- do not fake conflict-free offline sales sync

---

# 40. Reusable Components

Good candidates:

```text
PageHeader
StoreSwitcher
PermissionGate
KpiMetric
StatusBadge
StockBadge
ExpirationBadge
Money
QuantityStepper
ProductSearch
BarcodeInput
PaymentMethodPicker
ReceiptPreview
FilterBar
ResponsiveDataList
EmptyState
ErrorState
ConfirmDialog
```

Do not prematurely create a massive generic component abstraction.

---

# 41. Suggested Installation

If starting from an empty frontend directory:

```bash
pnpm create vite frontend --template react-ts
cd frontend

pnpm add react-router-dom
pnpm add @tanstack/react-query
pnpm add zustand
pnpm add react-hook-form zod @hookform/resolvers
pnpm add lucide-react
pnpm add recharts
pnpm add msw
pnpm add -D vite-plugin-pwa
```

Then initialize shadcn using its current CLI.

Add only needed shadcn components.

---

# 42. Implementation Sequence

## Phase 1 — Foundation

1. Vite + React + TypeScript
2. Tailwind
3. shadcn
4. tokens/theme
5. aliases
6. providers
7. React Router
8. TanStack Query
9. MSW
10. mock auth
11. route guards
12. permission system
13. desktop shell
14. mobile shell
15. PWA configuration
16. loading/error/empty patterns

## Phase 2 — Super Admin

1. Dashboard
2. Stores
3. Add Store
4. Store detail
5. Packages
6. Subscriptions
7. Audit
8. Settings

## Phase 3 — Core Store Operations

1. Dashboard
2. POS
3. Checkout
4. Receipt
5. Transactions
6. Products
7. Categories
8. Stock
9. Receiving
10. Adjustments
11. Expiration

## Phase 4 — Extended Operations

1. Customers
2. Debt
3. Suppliers
4. Purchase Orders
5. Deliveries
6. Staff
7. Settings
8. Subscription
9. Audit

## Phase 5 — Reports & QA

1. Sales report
2. Inventory report
3. Debt report
4. Profit report
5. keyboard flow
6. scanner flow
7. mobile QA
8. accessibility review
9. PWA/offline shell
10. final consistency pass

---

# 43. Required Mock Functionality

Do not create fake screens with dead buttons.

The mock frontend must support:

- mock login
- role-based routes
- permission-aware navigation
- Super Admin creates Store
- Staff cannot create Store
- Store switcher
- create/edit/deactivate staff
- create/edit/archive products
- categories
- POS search
- POS barcode lookup
- quantity updates
- bulk-price behavior
- discount mock behavior
- payment selection
- cash tender/change
- receipt generation
- cart persistence
- completed sale in transaction history
- stock deduction after sale
- insufficient-stock rejection
- expired-stock rejection
- receiving stock
- inventory adjustment
- movement history
- customer list
- credit sale
- debt payment
- suppliers
- PO lifecycle
- delivery receiving
- delivery receiving updates stock
- reports
- settings
- subscription display
- audit activity
- 404
- unauthorized
- loading
- error
- empty states
- responsive desktop/tablet/mobile navigation

---

# 44. Definition of Done

The frontend milestone is complete only when:

- no Vendor entity exists
- no Vendor routes exist
- no Vendor types exist
- no Vendor navigation exists
- no Vendor mock endpoints exist
- Store owns the former Vendor-level business configuration
- only Super Admin can add Stores
- routes are nested correctly
- protected routes work
- permissions work
- mock CRUD is interactive
- POS is fully usable with mock data
- inventory changes are reflected across affected screens
- debt changes are reflected across affected screens
- delivery receiving changes stock
- data formatting follows Philippine locale
- responsive layout works at mobile, tablet and desktop widths
- PWA shell works
- TypeScript passes
- build passes
- lint passes
- no core page is left as “Coming Soon”
- no dead buttons are present
- no direct PostgreSQL assumptions exist in frontend code

---

# 45. Design Reference Screens

Generate and use four reference designs.

## A. Super Admin / Stores Desktop

Show:

- left sidebar
- top bar
- platform KPI strip
- stores table
- subscription/status badges
- alerts
- Add Store primary action
- clean professional SaaS layout

## B. Store Dashboard Desktop

Show:

- store context
- sales KPIs
- 7-day trend
- low-stock panel
- near-expiry panel
- debt attention
- incoming deliveries
- top products
- recent activity
- New Sale primary CTA

## C. POS Desktop

Show:

- 65/35 split
- search
- barcode entry
- category pills
- compact product tiles
- cart
- quantity controls
- totals
- payment CTA
- store/cashier context
- minimal visual clutter

## D. Mobile POS / Inventory

Show:

- 390px smartphone PWA
- sticky product search
- scan control
- product list
- sticky cart summary
- bottom nav
- separate inventory screen concept
- touch-friendly controls
- offline indicator

Use the same design system across all four.

---

# 46. Master Codex Prompt

```text
You are the senior frontend architect, senior web/mobile product designer, and UX engineer for TindaKart.

Read this entire instruction file before changing code.

Rebuild the TindaKart frontend as a production-structured responsive PWA using:

- React
- TypeScript
- Vite
- Tailwind CSS
- shadcn/ui
- React Router
- TanStack Query
- React Hook Form
- Zod
- Zustand only for small client state such as the POS cart
- MSW for the mock API
- vite-plugin-pwa
- Lucide React
- Recharts when useful

The hierarchy is:

Super Admin → Stores → Staff

Vendor no longer exists.

Delete Vendor from the frontend mental model, routes, navigation, types, permissions, services, mock endpoints, dashboards, terminology, subscription ownership, business settings, products, inventory, staff, suppliers, deliveries, reports, and audit context.

Move every former Vendor-level frontend responsibility to Store.

The only exception is Store creation: only Super Admin can add/create a Store.

Store Manager is a Staff role.

Build a real interactive mock frontend, not static mockups.

Use MSW so UI code calls realistic API-like endpoints. Do not hardcode feature data arrays directly inside pages. Use TanStack Query for mock server state.

Mock actions must have observable consequences. Examples:

- completing a sale creates a transaction and reduces stock
- stock receiving increases inventory and creates a movement
- adjustment changes stock and creates a movement
- expired items cannot be sold
- insufficient stock cannot be sold
- a debt payment changes the customer's balance and history
- receiving a delivery changes inventory
- Super Admin adding a Store immediately updates the Stores list
- staff add/edit/deactivate actions update Staff pages

Treat the POS as the highest-priority screen.

Optimize desktop POS for keyboard and USB/Bluetooth barcode scanners.

Optimize mobile POS for touch and camera scanning.

Use Philippine Peso formatting and Asia/Manila context.

Support Cash, GCash, Maya, Card, and Credit as configurable mock payment methods.

Do not assume VAT is always enabled.

Follow the exact route architecture and feature-first folder structure from this file.

Use nested layouts and route guards.

Implement:
- AuthGuard
- AdminGuard
- StoreGuard
- PermissionGuard

Keep navigation permission-aware.

Use a persistent sidebar on desktop and bottom navigation / drawer patterns on mobile.

Follow the spacing, typography, visual hierarchy, accessibility, form, table, loading, empty, error, and responsive rules in this file.

Do not add another component framework.

Do not leave dead buttons.

Do not leave core pages as placeholders.

Do not make every page a grid of rounded cards.

Do not put large edit forms inside small dialogs.

Do not force desktop tables into mobile widths.

Do not use generic Lorem Ipsum.

Before implementation:
1. inspect the repository
2. identify existing dependencies and conventions
3. create a concise implementation plan
4. establish the app foundation first

Then implement in this order:

1. foundation
2. authentication
3. application shells
4. Super Admin
5. Store dashboard
6. POS + checkout + receipt
7. inventory
8. customers + debt
9. suppliers + purchase orders + deliveries
10. staff
11. settings + subscription + audit
12. reports
13. responsive QA
14. accessibility QA
15. PWA/offline shell

After meaningful batches:
- run TypeScript
- run lint
- run build
- repair all introduced errors

Continue until the complete interactive mock frontend described by this file is implemented coherently.

Do not stop after scaffolding.
```
