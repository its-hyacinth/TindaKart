# TindaKart Frontend Revamp and Interactive UX Plan

## 1. Purpose

Revamp the current React PWA into a modern, responsive, task-focused retail operations interface for:

- Super Admins managing the TindaKart platform
- Vendor Admins managing a store business
- Staff and cashiers performing daily operations

The revamp must preserve the existing backend contracts, vendor/store isolation, package entitlements, and role permissions. It should improve the way the existing capabilities are discovered and used; it is not a rewrite of the business rules in the browser.

This plan is implementation-only for the frontend. It does not authorize changes to `legacy/` or to the database schema unless a separate backend requirement is identified and approved.

## 2. Current frontend baseline

The frontend is a React 19 + TypeScript + Vite PWA with these current areas:

- `main.tsx` — authentication gate, shell, vendor/store context, navigation
- `operations.tsx` — POS, inventory, catalog, debt, delivery, and reports workspace
- `admin.tsx` — staff, business settings, packages, billing, and audit views
- `advancedReports.tsx` and `debtAging.tsx` — reporting subviews
- `deliveryReceiving.tsx` — delivery receiving workflow
- `styles.css` — current global responsive styling
- `api.ts` — fetch client and typed API contracts

The present UI is functional but dense and component-heavy. The main revamp risk is changing presentation without breaking context selection, package feature gates, permission behavior, or transaction state.

## 3. UX goals

- Make the next correct action obvious on every screen.
- Make POS fast enough for repetitive cashier use.
- Make inventory, debt, delivery, and reporting information scannable.
- Use the same visual language across desktop, tablet, and phone layouts.
- Make destructive or financial actions deliberate and recoverable.
- Surface backend errors in plain language without hiding technical details from support users.
- Preserve unsaved POS cart state and clearly show synchronization/connectivity state.
- Keep role and package restrictions understandable rather than showing confusing dead ends.

## 4. Product and interaction principles

1. **Task first:** each page has one primary job and one primary action.
2. **Progressive disclosure:** show common fields first; reveal advanced settings when needed.
3. **Immediate feedback:** every save, scan, receive, payment, and sale gives a visible result.
4. **Safe defaults:** use backend-provided settings and entitlements; never duplicate business rules in UI-only conditionals.
5. **Consistent state handling:** loading, empty, error, success, offline, and permission-denied states use shared patterns.
6. **Touch friendly:** interactive targets are at least 44px, with spacing suitable for store counters and phones.
7. **Keyboard friendly:** barcode scanners and keyboard workflows remain first-class on desktop.
8. **Accessible by default:** semantic controls, visible focus, readable contrast, labels, and screen-reader status messages.

## 5. Target information architecture

### Global shell

- Brand and current business/store context
- Connectivity indicator
- Notification/alert center for low stock, near expiration, failed actions, and pending work
- User menu: profile, change password, sign out
- Desktop sidebar; tablet compact rail; mobile bottom navigation or menu drawer

### Super Admin workspace

- Platform overview
- Vendors
- Packages and entitlements
- Subscription/billing status
- Audit log
- Platform health

### Vendor Admin workspace

- Business dashboard
- POS
- Inventory
- Catalog and categories
- Debt and customers
- Deliveries and suppliers
- Reports
- Staff and store management
- Business, tax, receipt, payment-method, camera, and print settings
- Subscription and billing

### Staff workspace

Show only the modules allowed by role permissions and active package entitlements:

- POS
- Assigned-store inventory
- Catalog lookup
- Debt, delivery, or reports only when permitted

The frontend must treat backend authorization as authoritative. Hidden navigation is a usability feature, not a security boundary.

## 6. Page and flow designs

### 6.1 Login and session entry

- Branded, compact login card
- Clear username/password validation
- Loading state on submit
- Lockout and invalid-credential messages that do not disclose sensitive account details
- Session-expiry recovery that returns the user to login without losing safe local POS cart state
- Responsive layout suitable for phone portrait and desktop

### 6.2 Dashboard

Create role-specific summary cards rather than showing a generic CRUD landing page.

Vendor/Staff cards may include:

- Today’s sales and transaction count
- Low-stock count
- Near-expiration count
- Outstanding debt balance
- Upcoming deliveries
- Recent activity

Each card links to the filtered detail view. Dashboard data must remain vendor/store scoped.

### 6.3 POS

Desktop layout:

- Left: searchable/filterable product area
- Right: persistent cart and checkout panel

Mobile layout:

- Search/scan bar fixed near the top
- Product cards in a compact grid/list
- Cart summary bar fixed near the bottom
- Checkout opens as a focused sheet or full-screen step

Required interactions:

- Search by name, SKU, or barcode
- Keyboard scanner and camera scanner paths
- Category filter and A–Z sorting
- Product cards showing unit, retail price, and bulk price
- Quantity stepper with direct quantity entry where useful
- Per-piece and bulk pricing clearly labeled
- Line subtotal, automatic discount, manual discount, VAT when configured, and final total
- Configurable payment methods from vendor settings
- Credit sale customer and due-date selection
- Amount tendered and change for non-credit sales
- Clear sale, remove line, and authorized void flows
- Sale success state with receipt preview, print/reprint action, and new-sale action

Guardrails:

- Disable completion when the cart is empty or required fields are missing.
- Surface insufficient stock and expired-stock errors beside the affected action.
- Never calculate final financial values as authoritative UI-only values; display the backend response.

### 6.4 Inventory

Use a dashboard plus focused action panels:

- Stock summary cards: total items, low stock, near expiration, expired
- Search/filter by category, status, barcode, and expiration
- Inventory batch cards on phone; table with horizontal overflow or column selection on desktop
- Receive stock flow with scan-first product identification
- New-product path when a barcode is not found
- Stock adjustment flow with reason confirmation
- Movement history drawer or detail page

Make the distinction explicit:

- Inventory scan → receive or update stock
- POS scan → add to sale and deduct stock after completed sale

### 6.5 Catalog and categories

- Category management for authorized users
- Product list with A–Z sorting, category filter, and search
- Product form grouped into identity, pricing, stock policy, barcode, and expiration sections
- Duplicate-barcode feedback with “use existing product” guidance
- Clear unit mode for piece, pack, bottle, kilogram, liter, and other
- Bulk price and threshold fields shown together with an example

### 6.6 Debt and customers

- Summary cards for total credit, collected amount, outstanding balance, and overdue accounts
- Searchable customer/account list
- Account detail drawer/page with payment history and receipt numbers
- Payment form with balance-aware amount validation
- Credit-sale customer creation accessible to authorized users
- Clear status labels: active, paid, overdue, and cancelled where applicable
- Print payment receipt action using the shared receipt pattern

### 6.7 Deliveries and suppliers

- Upcoming/in-transit/received/completed status board
- Supplier and expected-date filters
- Delivery detail with ordered, received, missing, and damaged quantities
- Receiving form that prevents totals above ordered quantity
- Explicit inventory-update confirmation after receiving
- Timeline/activity display for delivery status changes

### 6.8 Reports

- Date-range toolbar shared by all reports
- Summary cards followed by charts/tables where they improve comprehension
- Mobile-friendly report cards instead of forcing wide tables
- Export actions grouped in one consistent menu: CSV, print, Save as PDF
- Empty states that explain what data is needed
- Vendor/store scope and permission indicators

### 6.9 Administration

Super Admin:

- Vendor list with status filters and activation/suspension actions
- Package editor with price, active state, feature inclusion, and numeric limits
- Subscription and billing history
- Audit log with filters and detail view

Vendor Admin:

- Staff list with role, status, assigned stores, and actions
- Store/branch management
- Business/tax/receipt settings
- Dynamic POS payment methods, camera scanning, receipt print mode, and near-expiration settings

Use confirmation dialogs for suspension, disabling accounts, changing package entitlements, and other high-impact actions.

## 7. Visual design system

### Design tokens

Define CSS variables for:

- Brand, accent, success, warning, danger, and neutral colors
- Surface/background colors
- Text and muted text colors
- Border radius levels
- Shadows
- Spacing scale
- Typography scale
- Focus ring
- Z-index layers for navigation, sheets, dialogs, and toasts

### Component library

Build reusable components before page-specific polish:

- `AppShell`
- `PageHeader`
- `ContextSwitcher`
- `Sidebar` / `MobileNav`
- `StatCard`
- `DataTable` / `MobileList`
- `SearchBar`
- `FilterBar`
- `StatusBadge`
- `EmptyState`
- `LoadingSkeleton`
- `ErrorState`
- `ConfirmDialog`
- `FormField`
- `Toast`
- `Drawer` / `BottomSheet`
- `Modal`
- `MoneyDisplay`
- `QuantityStepper`
- `ReceiptPreview`
- `ScannerAction`

Components must expose accessible labels and avoid page-specific duplicated styles.

## 8. Frontend architecture plan

### Suggested structure

Refactor gradually into feature folders without breaking current routes:

```text
frontend/src/
  app/
    AppShell.tsx
    session.ts
    navigation.ts
  components/
    layout/
    feedback/
    forms/
    data-display/
    commerce/
  features/
    auth/
    dashboard/
    pos/
    inventory/
    catalog/
    debt/
    deliveries/
    reports/
    administration/
  lib/
    api.ts
    formatting.ts
    permissions.ts
    storage.ts
  styles/
    tokens.css
    globals.css
```

The existing files can be migrated feature by feature. Avoid a big-bang rewrite.

### State boundaries

- Server state: API responses, loading, errors, and invalidation
- Session state: current user, vendor/store context, entitlements, connectivity
- Local workflow state: POS cart, form drafts, open dialogs
- Persistent local state: scoped POS cart only unless offline support is separately approved

Do not duplicate server authorization or pricing logic in a second frontend rules engine.

### API interaction standards

- Keep typed API contracts in one place.
- Add request cancellation for search and changing contexts.
- Normalize API errors into user-safe messages with a support detail option.
- Invalidate affected lists after create/update/receive/payment/sale actions.
- Prevent duplicate submissions with pending-action state.
- Show the selected vendor/store on every tenant-scoped page.

## 9. Responsive behavior targets

Test at minimum:

- Phone portrait: 360–430px
- Tablet portrait: 768px
- Tablet landscape: 1024px
- Laptop: 1366px
- Large desktop: 1440px and above

Rules:

- No clipped primary actions.
- No mandatory horizontal scrolling for POS checkout.
- Tables become cards or controlled horizontal regions on narrow screens.
- Dialogs become bottom sheets/full-screen flows on phones.
- Navigation remains reachable with one hand on mobile.
- Keyboard and scanner flows remain efficient on desktop.

## 10. Accessibility and quality requirements

- Keyboard navigation for every workflow
- Visible focus states
- Semantic headings and landmarks
- Labels for all fields and icon-only buttons
- `aria-live` feedback for scan, save, payment, and error results
- Color is never the only status indicator
- Minimum contrast target of WCAG AA
- Reduced-motion support for transitions
- Screen-reader-friendly tables and dialogs

## 11. Implementation phases

### Phase A — UX foundation and inventory

- [ ] Capture current screenshots and behavior notes for each existing page.
- [x] Map each backend endpoint to its frontend consumer and required UI state in `FRONTEND_API_MAP.md`.
- [x] Record the target navigation and role/package visibility matrix from existing authorization rules in `FRONTEND_API_MAP.md`.
- [x] Define design tokens, typography, icon strategy, and responsive breakpoints in `styles.css`.
- [x] Create the shared component inventory and migration order in `ui.tsx` and this plan.
- [x] Revamp the public landing and sign-in entry experience with Free-plan onboarding, PWA installation, responsive navigation, and clearer authentication states.

### Phase B — Shell and shared components

- [x] Extract reusable `PageHeader`, `ContextSwitcher`, and responsive `WorkspaceNavigation` primitives used by the authenticated workspace.
- [ ] Refactor the shell into reusable desktop, tablet, and mobile layouts.
- [ ] Implement the shared page header, context switcher, navigation, breadcrumbs, and user menu.
- [x] Implement shared loading, empty, error, toast, confirmation-dialog, and form-field components in `ui.tsx`.
- [x] Extract reusable `DataTable` and barcode-camera components and move catalog screens into `pages/catalog/CatalogPage.tsx`.
- [x] Add visible focus states, touch-sized controls, keyboard-compatible native controls, and live status/error messaging.
- [x] Add global responsive and print styles for the shared foundation.

### Phase C — POS-first redesign

- [x] Rebuild the POS layout foundation for desktop and phone workflows, including the responsive page header and cart count.
- [x] Add scanner/search feedback, product loading/empty states, and duplicate-submit protection.
- [x] Add cart, pricing, discount, VAT, payment, credit, and receipt interaction states.
- [x] Add sale success, void, reprint, and recovery states.
- [x] Verify POS behavior against backend transaction responses and integration tests.

### Phase D — Inventory, catalog, and expiration

- [x] Add responsive inventory and catalog page hierarchy with summary cards, searchable views, and shared state components.
- [x] Use consistent loading, empty, error, and success feedback in the inventory and catalog foundations.
- [ ] Rebuild inventory dashboard and batch views.
- [ ] Rebuild receive, scan, create-product, and adjustment flows.
- [x] Add inline category creation, category chips, organized product forms, and refresh the catalog after product creation.
- [ ] Add low-stock, near-expiration, and expired-stock alert presentation.
- [ ] Verify inventory actions against movement and stock-balance responses.

### Phase E — Debt, deliveries, and reports

- [x] Add shared page hierarchy, summary metrics, responsive states, and pending-action feedback to debt, delivery, and report views.
- [ ] Rebuild debt summary, account detail, payment, and receipt flows.
- [x] Add account-level payment-history loading with receipt number, payment method, notes, and timestamp display.
- [ ] Rebuild delivery board, detail, receiving-variance, and supplier flows.
- [ ] Rebuild reports with shared filters, summaries, responsive data views, and exports.
- [ ] Verify each flow against its existing API integration test and authorization rules.

### Phase F — Administration and platform UX

- [x] Add shared administration page hierarchy and scope presentation for Super Admin and Vendor Admin workspaces.
- [ ] Rebuild Super Admin vendor, package, billing, and audit screens.
- [ ] Rebuild Vendor Admin staff, store, and settings screens.
- [x] Add a visible entitlement-load warning when package permissions cannot be loaded.
- [ ] Add confirmation and audit feedback for high-impact administrative actions.

### Phase G — Polish and acceptance

- [ ] Add consistent motion, hover, pressed, focus, disabled, and pending states.
- [ ] Validate keyboard, mobile touch, scanner, camera, and browser-print paths.
- [ ] Run responsive checks at all target viewport sizes.
- [ ] Run accessibility and performance checks.
- [ ] Run the complete backend unit/integration suite and frontend production build.
- [ ] Conduct a guided client workflow review before marking the redesign accepted.

## 12. Verification matrix

For each feature, verify all of the following:

| Area | Required evidence |
|---|---|
| Navigation | Correct modules appear for role, vendor, store, and package |
| Data loading | Loading, success, empty, and error states render correctly |
| Mutations | Pending, success, duplicate-submit prevention, and failure states work |
| Authorization | Unauthorized actions are hidden or rejected by the backend |
| Responsive | Phone, tablet, laptop, and desktop layouts remain usable |
| Accessibility | Keyboard, focus, labels, contrast, and announcements work |
| Financial flows | Backend-returned totals, payment, change, debt, and receipt data are displayed accurately |
| Offline/connectivity | Connectivity state is visible and no unsafe false-success is shown |
| Print/export | Browser print, CSV, and Save as PDF actions remain available where supported |

## 13. Definition of done

The frontend revamp is ready for client review when:

- Every role sees a clear, purpose-built workspace.
- POS can be completed efficiently on desktop and phone sizes.
- Inventory scanning clearly differs from POS scanning.
- Product, category, debt, delivery, and report workflows are discoverable and consistent.
- Backend permissions, package features, and vendor/store context are reflected correctly.
- All important actions have loading, success, empty, error, and confirmation states.
- No financial total is presented as final until the backend response is received.
- The PWA remains installable and responsive.
- Accessibility and responsive checks pass for the agreed target devices.
- Existing backend unit and integration tests remain green.
- The client can complete representative workflows without developer guidance.

## 14. Recommended implementation order

1. Shared design tokens and shell
2. POS redesign
3. Inventory and catalog redesign
4. Debt and delivery redesign
5. Reports redesign
6. Administration redesign
7. Accessibility, responsive, performance, and client acceptance pass

This order prioritizes the revenue-critical workflow first while allowing every later module to reuse the same components and interaction patterns.

## 15. Clean frontend restart checkpoint

The previous frontend direction has been retired. The implementation is restarting from a small, reusable foundation so the new product experience can be built intentionally module by module.

- [x] Replace the previous frontend source implementation with a Tailwind CSS v4 and shadcn-style component foundation.
- [x] Add reusable `Button`, `Card`, `Badge`, `Input`, and `cn()` utilities.
- [x] Rebuild the public landing page with a responsive product story, PWA install action, free-plan messaging, and feature overview.
- [x] Rebuild login and free vendor registration screens around the current authentication APIs.
- [x] Preserve the backend API client, PWA configuration, and backend/database implementation.
- [x] Verify TypeScript compilation and a production Vite build.
- [ ] Rebuild the authenticated application shell and navigation.
- [ ] Rebuild the POS, inventory, catalog, debt, delivery, reports, and administration modules on this foundation.

### Vendor and store workspace checkpoint

- [x] Add a responsive vendor/store shell with vendor and store context selectors.
- [x] Add role-aware navigation for Vendor Admin and staff users.
- [x] Add operations entry points for POS, inventory, catalog, debt, deliveries, and reports.
- [x] Add Vendor Admin entry points for staff and business settings.
- [x] Replace the temporary authenticated workspace with the vendor/store portal.
- [x] Add an overview dashboard with store sales context and quick actions.
- [ ] Implement the complete POS transaction workflow.
- [ ] Implement inventory receiving, scanning, adjustment, and expiration workflows.
- [ ] Implement catalog, debt, delivery, reports, staff, and settings workflows.

### Vendor functional pass checkpoint

- [x] Wire product search, cart quantities, discounts, payment method, customer selection, and sale submission in POS.
- [x] Wire catalog product search and category creation.
- [x] Wire inventory batch loading and stock receiving with expiration dates.
- [x] Wire debt account loading and payment recording.
- [x] Wire delivery loading, supplier creation, status changes, and receiving.
- [x] Wire sales, low-stock, expiration, VAT, and payment summary data into reports foundations.
- [x] Wire staff creation/status management and business settings persistence.
- [ ] Add barcode scanner/camera UX and duplicate-product prompts.
- [ ] Add product creation/editing and inventory adjustment forms.
- [ ] Add detailed delivery receiving variance and supplier delivery creation.
- [ ] Add receipt printing, report exports, advanced report views, and offline mutation handling.

### Super Admin portal checkpoint

- [x] Add a responsive Super Admin shell with role-aware navigation and session controls.
- [x] Add platform overview dashboard with vendor activity and admin shortcuts.
- [x] Add vendor and store management structure with vendor creation and status actions.
- [x] Add package and custom add-on pricing management structure.
- [x] Add vendor billing activity and checkout-session history view.
- [x] Add platform audit-log view and refresh state.
- [x] Add platform settings placeholder for environment and access-policy visibility.
- [x] Add package editor for feature flags and numeric limits.
- [x] Add vendor detail view with staff, subscription, entitlement, store, and billing drill-down.

### Default commercial catalog checkpoint

- [x] Seed Free, Growth, and Pro packages through Flyway migration `V21`.
- [x] Seed default monthly and annual prices in Philippine pesos.
- [x] Seed staff-seat and feature add-on prices for custom vendor plans.
- [x] Seed package features and operating limits.
