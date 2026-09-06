# TindaKart PWA Migration Plan

## Product model

TindaKart is a store operations PWA. The only tenant is a store. A store can have
many staff members, each with one or more operational roles.

```text
Super Admin
└── Stores
    └── Staff
```

There is no vendor, parent business, branch, or multi-store owner layer. Store
data is isolated by the backend. A staff member can only use stores explicitly
assigned to that account.

## Product priorities

1. Correctness and backend contract compliance
2. Fast, safe POS transactions
3. Error prevention and clear recovery
4. Accessibility and keyboard/touch usability
5. Responsive and offline-resilient PWA behavior
6. Consistent, restrained visual polish

## Target architecture

- React, Vite, TypeScript, Tailwind CSS, shadcn/ui, Lucide React
- React Router, TanStack Query, React Hook Form, Zod
- Dexie/IndexedDB for safe local reference data and active-cart recovery
- `vite-plugin-pwa` for installability and service-worker support
- Playwright for rendered UI and device-size validation
- Backend base URL exposed only through `VITE_API_URL`
- Spring Boot and PostgreSQL remain authoritative for auth, permissions,
  store scope, pricing, stock, sales, debt, receipts, billing, and entitlements

## Store and staff rules

- Super Admin manages stores, plans, billing controls, and platform audit logs.
- Store Admin manages one store's staff, settings, catalog, and operations.
- Staff access only assigned stores and permitted modules.
- A store may have multiple staff accounts.
- The Store Admin is not counted as a paid staff seat.
- No client-provided parent-store or vendor identifier is trusted for scope.
- Every tenant-owned query is scoped by the authenticated store assignment.

## Backend contract migration

- Replace vendor-scoped routes with store-scoped routes.
- Replace vendor role names with `STORE_ADMIN` and store-scoped staff roles.
- Replace vendor subscription/settings/seat records with store records.
- Remove the parent-business relationship from the final schema.
- Preserve existing data through an explicit Flyway migration where possible;
  do not silently discard stores or staff assignments.
- Update `API_DOCUMENTATION.md` before frontend API integration.
- Add integration coverage for store isolation, staff assignment, and the
  Super Admin → stores → staff workflow.

## Frontend workflows

### Public

- Landing page with install guidance and sign-in entry point.
- Login and store registration.

### Super Admin

- Store list, creation, activation/suspension, and audit history.
- Store subscription packages, limits, feature controls, and billing events.

### Store workspace

- POS with product search, SKU/barcode input, product grid, persistent cart,
  payment, receipt preview, and safe error handling.
- Inventory, receiving, adjustments, expiration, and movements.
- Catalog, debt/customers, deliveries, reports, settings, and staff.
- Staff creation, role selection, activation/deactivation, and assignment to
  the current store.

## PWA and interaction requirements

- Keep the desktop cart visible beside products when space allows.
- Preserve an active cart through refresh and PWA restart; never imply that a
  queued cart is an offline-completed sale.
- Support keyboard-style barcode scanners without disrupting normal typing.
- Use visible focus rings, a skip link, semantic forms, and 44px touch targets.
- Provide loading, empty, error, offline, and restricted-entitlement states.
- Respect reduced motion; do not animate high-frequency cashier actions.
- Test at 375px, 768px, 1024px, and 1440px widths.

## Visual direction

- Light blue application canvas, white operational surfaces, cyan/sky primary,
  dark navy text, neutral borders, and restrained status colors.
- Fixed/sticky desktop navigation, contextual utility bar, and predictable
  action areas.
- Inputs/buttons 10–12px radius; panels 14–16px; shell surfaces 18–20px.
- Prefer borders and spacing over heavy shadows or decorative effects.
- Use Lucide outline icons with consistent stroke weight and tabular numerals.
- The UI should feel like TindaKart: a fast store tool, not an ecommerce site.

## Skill workflow

For the major frontend rebuild:

```text
frontend-design → implement → ui-ux-pro-max → make-interfaces-feel-better → Playwright
```

The design-system search is run before implementation. UX checks happen after
the first implementation. The polish pass covers typography, surfaces, icons,
states, motion restraint, and numeric stability. Playwright validates the
rendered experience rather than source code alone.

## Definition of done

- No vendor terminology or parent-business routes remain in the plan, API
  contract, backend source, migrations, or frontend.
- Store isolation and staff assignment are enforced server-side.
- Super Admin → stores → staff flows work end to end.
- POS, inventory, debt, delivery, reporting, billing, and settings remain
  backed by documented endpoints.
- Frontend builds successfully and passes unit/integration checks available in
  the repository.
- Playwright covers login, store selection, staff management, POS search/cart,
  checkout validation, responsive layout, and offline/error states.
