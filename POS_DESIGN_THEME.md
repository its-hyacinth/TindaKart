# TindaKart POS Design and Theme Direction

## Purpose

This document defines the visual direction for the TindaKart POS and operations PWA. It is a design reference for implementation, not a pixel-for-pixel copy of another product.

The recommended direction is a calm, high-contrast retail workspace: a compact navigation rail, a fast product-browsing area, and a persistent cart/checkout panel. It should feel modern without becoming decorative or slow during repetitive cashier work.

## Inspiration references

The following references were reviewed for layout, component organization, responsive behavior, and visual hierarchy:

- [MasterPOS — Point of Sales Admin Dashboard Figma](https://themeforest.net/item/masterpos-point-of-sales-admin-dashboard-figma/48662343) — broad retail/admin screen coverage, including tablet and mobile layouts.
- [MPOS — POS Dashboard Template](https://rxdevelopers.gumroad.com/l/ozcbv) — responsive POS patterns, organized Figma components, and mobile/desktop screen planning.
- [Hayabusha — Point of Sales UI Kit](https://caraka.io/products/hayabusha-point-of-sales-ui-kit) — product/category browsing, inventory screens, payment methods, and reusable design-system structure.
- [CloudPOS — Point of Sale UI Kit](https://dribbble.com/shots/26122410-Cloud-POS-UI-Kit-Modern-POS-Design-System) — restrained blue theme, light/dark variation, and data-oriented dashboard hierarchy.
- [Point of Sale Dashboard — DjectStudio](https://djectstudio.com/store/point-of-sale-dashboard) — calm spacing, clear hierarchy, and responsive-friendly layout principles.
- [Retail POS Analytics Dashboard case study](https://contra.com/p/6m66ZbPU-retail-pos-analytics-dashboard-case-study) — KPI-first reporting, low-stock visibility, and a collapsible navigation model.

These references are inspiration only. TindaKart must retain its own branding, Philippine retail context, vendor/store scope, package entitlements, and existing backend behavior.

## Recommended visual direction

### Design keywords

`calm` · `fast` · `trustworthy` · `operational` · `warm retail` · `high contrast` · `responsive`

### Core layout

#### Desktop

```text
┌──────────────┬───────────────────────────────────────────────┐
│ Brand        │ Store context / notifications / user           │
│ Dashboard    ├───────────────────────────────────────────────┤
│ POS          │ Search + barcode scan                          │
│ Inventory    │ Category filters                               │
│ Catalog      │ Product grid                  │ Current cart   │
│ Debt         │ Product cards                  │ totals         │
│ Deliveries   │                               │ payment        │
│ Reports      │                               │ complete sale  │
└──────────────┴───────────────────────────────────────────────┘
```

- Persistent cart on the right at widths of 1,024px and above.
- Product grid is the main visual area; avoid showing a dense table as the default POS view.
- Search and barcode input remain the first keyboard focus target.
- The cart total and primary checkout action remain visible without scrolling where possible.

#### Tablet

- Compact navigation rail or top navigation.
- Product grid remains primary.
- Cart becomes a slide-over or bottom sheet when space is limited.
- Keep quantity controls and checkout actions touch-sized.

#### Phone

- Bottom navigation for POS, Inventory, Catalog, and More.
- Product search and category chips at the top.
- Cart becomes a dedicated drawer/sheet with a sticky total and checkout button.
- Use cards instead of wide tables; expose secondary details through expandable rows or detail sheets.

## Theme tokens

The current CSS already has a strong base. The target theme should evolve those tokens consistently rather than introducing one-off colors in individual pages.

### Color roles

| Role | Target | Usage |
|---|---|---|
| Ink | `#18202A` | Primary text and strong headings |
| Brand navy | `#333652` | Navigation, headings, primary identity |
| Action indigo | `#5961A8` | Primary actions, selected navigation, links |
| Action soft | `#EEF0FF` | Selected states, chips, subtle emphasis |
| Surface | `#FFFFFF` | Cards, forms, cart, dialogs |
| Canvas | `#F7F7FB` | App background |
| Border | `#E6E6EF` | Dividers and input borders |
| Success | `#26734D` | Completed, paid, received, healthy |
| Warning | `#8A5A00` | Low stock, near expiration, pending |
| Danger | `#9D2D22` | Expired, void, destructive errors |
| Accent warm | `#E58A4A` | Optional retail highlight, promotions, near-expiration discount |

Rules:

- Color must communicate state, not decoration.
- Never use red as the only indication of an invalid/expired state; pair it with text or an icon.
- Keep primary actions visually dominant; secondary actions should not compete with checkout.
- Use the warm accent sparingly for promotions and discounted near-expiration inventory.

### Typography

- Primary font: Inter, with system fallbacks.
- Page title: 28–40px, weight 700–800.
- Section heading: 18–22px, weight 700.
- Body: 14–16px, weight 400–500.
- Metadata: 12–13px, muted color.
- Money totals: 24–36px, weight 800; use tabular numerals where supported.
- Avoid all-caps for normal content; reserve uppercase for compact status badges and eyebrows.

### Spacing and shape

- Base spacing unit: 4px.
- Common gaps: 8px, 12px, 16px, 24px, 32px.
- Card radius: 14–20px.
- Input/button radius: 10–12px.
- Use subtle borders and shadows. Avoid heavy gradients and excessive floating cards.
- Minimum interactive target: 44px.

## POS component system

### Required components

- `AppShell`: desktop sidebar, tablet rail, mobile navigation.
- `ContextSwitcher`: vendor/store context and scope indicator.
- `PageHeader`: eyebrow, title, description, actions.
- `SearchBar`: text search, barcode keyboard input, camera scan action.
- `CategoryFilter`: horizontal chips on mobile, select/segmented control on desktop.
- `ProductCard`: name, SKU, unit, retail price, bulk price, stock/availability state.
- `CartPanel`: line items, quantity controls, subtotal, discount, VAT, total.
- `PaymentPanel`: payment method, tendered amount, credit customer, due date.
- `SaleSuccess`: receipt number, totals, print, void, and recovery actions.
- `StatusBadge`: success, warning, danger, neutral.
- `StatCard`: compact operational metric with optional detail and tone.
- `DataTable`: desktop table with responsive card/row treatment on small screens.
- `ConfirmDialog`: void, disable, status changes, and other high-impact actions.
- `Toast`/`InlineMessage`: success, failure, pending, and permission feedback.

### Product card states

- Default: available product with price and unit.
- Bulk eligible: show bulk threshold and bulk price without hiding retail price.
- Low stock: show a warning badge.
- Expired: disabled from POS selection and clearly marked.
- Barcode match: briefly highlight the matched card after scanning.
- Unavailable: keep the product discoverable but make the reason explicit.

### Cart states

- Empty cart: explain how to search or scan.
- Active cart: show quantity, unit price, bulk pricing, line subtotal, and remove action.
- Discount applied: show discount as a separate line, never bury it in the total.
- Credit sale: require customer and due date before enabling completion.
- Pending checkout: disable duplicate submission and show progress.
- Success: show receipt number, final totals, payment method, change, print, and void.
- Failure: preserve the cart and explain whether retry is safe.

## Navigation and hierarchy

### Staff-first navigation

1. POS
2. Inventory
3. Catalog
4. Debt
5. Deliveries
6. Reports

Admin-only areas should be visually separated under an Administration group:

- Staff
- Stores
- Business/tax/receipt settings
- Packages and billing
- Audit log

The UI must hide modules that are not available to the role/package, but must show a concise explanation when a user encounters an unavailable entitlement or a failed entitlement lookup.

## Page themes beyond POS

### Inventory

- Summary cards first: low stock, near expiration, expired, tracked batches.
- Primary action: receive stock.
- Secondary action: adjust stock with a reason.
- Use warning/danger treatment for stock conditions.
- On mobile, use inventory cards with a “details” expansion instead of forcing a wide table.

### Catalog

- Product creation and category management are separate cards.
- Category chips/filter should be sorted A–Z.
- Product list should be searchable and sorted A–Z by default.
- Keep barcode, unit type, retail price, bulk price, and expiration applicability visible.

### Debt

- Summary metrics: total credit, outstanding balance, open accounts.
- Account rows show customer, balance, status, and “View history”.
- Payment history opens inline or in a detail panel, not a separate confusing workflow.

### Deliveries

- Use a status progression: Upcoming → In Transit → Received → Completed.
- Show supplier, expected date, item count, and receiving variance.
- Receiving must distinguish received, missing, and damaged quantities.

### Reports

- KPI cards first; detailed tables second.
- Low-stock and expiration alerts should be action-oriented.
- Keep VAT/tax information clearly labeled and tied to the vendor settings.

## Interaction and motion

- Use short transitions, approximately 120–180ms, for hover, focus, selected, and drawer states.
- Do not animate totals continuously during checkout.
- Use a brief highlight when a scanned product is added.
- Loading states should preserve layout to prevent content jumping.
- Destructive actions require confirmation and should explain the effect, such as restoring stock after voiding a sale.

## Accessibility and device rules

- Maintain visible keyboard focus.
- Use native input/select controls where they are faster and more accessible.
- Keep barcode scanner keyboard input working without requiring a mouse.
- Support camera scanning as an optional enhancement, not the only scan path.
- Ensure contrast for muted text, status badges, and disabled controls.
- Test at 320px, 375px, 768px, 1024px, 1280px, and 1440px widths.
- Test portrait and landscape tablet layouts.

## Implementation mapping

| Design area | Frontend target |
|---|---|
| Tokens | `frontend/src/styles.css` |
| Shared UI | `frontend/src/ui.tsx` and `frontend/src/components/` |
| POS page | `frontend/src/pages/pos/` |
| Inventory page | `frontend/src/pages/inventory/` |
| Catalog page | `frontend/src/pages/catalog/CatalogPage.tsx` |
| Debt page | `frontend/src/pages/debt/` |
| Delivery page | `frontend/src/pages/deliveries/` |
| Reports page | `frontend/src/pages/reports/` |
| Admin pages | `frontend/src/pages/admin/` |
| PWA icons/static assets | `frontend/public/` |

## Figma file organization recommendation

If a Figma design file is created, use this page structure:

1. `00 Cover and project notes`
2. `01 Foundations` — colors, typography, spacing, elevation, icons
3. `02 Components` — buttons, inputs, badges, cards, tables, dialogs
4. `03 POS Desktop`
5. `04 POS Tablet`
6. `05 POS Mobile`
7. `06 Inventory and Catalog`
8. `07 Debt and Deliveries`
9. `08 Reports and Admin`
10. `09 Prototype flows`

Use Auto Layout, named components, variants for state, and shared variables for colors and spacing. Design desktop, tablet, and mobile states together so responsive behavior is intentional rather than a late adaptation.

## Acceptance criteria for the visual direction

- A cashier can search or scan, add a product, edit quantity, select payment, and complete a sale without hunting through the interface.
- The current cart, total, and next action remain obvious on desktop and mobile.
- Inventory warnings are visible at a glance and do not require opening a report first.
- Vendor/store context is always visible before an operational action.
- Package and role restrictions are understandable.
- Shared components look and behave consistently across POS, inventory, catalog, debt, delivery, reports, and admin.
- The design remains usable with keyboard, barcode scanner, touch, camera, and browser printing.
