# TindaKart PWA POS — Frontend Codex Instructions

## 1. Purpose

This file defines the frontend implementation, design, UX, PWA, API-integration, and Codex-skill rules for **TindaKart**.

TindaKart is a multi-tenant Progressive Web App POS system for:

- Cashiers
- Vendor administrators
- Store managers
- Inventory staff
- Delivery staff
- Super administrators

Primary frontend priorities, in order:

1. Correctness
2. Transaction speed
3. Error prevention
4. Backend contract compliance
5. Accessibility
6. Touch and keyboard usability
7. Responsive behavior
8. PWA resilience
9. Visual consistency
10. Visual polish

The POS must feel like a fast operational tool, not a marketing dashboard.

---

# 2. Authoritative Backend Contract

`API_DOCUMENTATION.md` is the authoritative frontend/backend integration contract.

Before implementing any API call:

1. Read the relevant section of `API_DOCUMENTATION.md`.
2. Reuse the documented endpoint.
3. Preserve authentication, tenant-scope, permission, and business-rule requirements.
4. Do not invent an endpoint if an equivalent endpoint already exists.
5. Do not move backend-enforced business rules into the frontend.
6. If an operation is not documented, do not assume backend support.
7. If the frontend needs behavior that the backend does not document, stop and surface the gap.

The frontend improves UX.

The backend remains authoritative for:

- Authentication
- Authorization
- Vendor/store scope
- Subscription entitlements
- Package limits
- Product availability
- Expired inventory rules
- Near-expiration discounts
- Sales validation
- Inventory deduction
- Inventory movements
- Credit-sale validation
- Debt accounts
- Payment-method enforcement
- Receipt numbering
- Receipt-output rules
- Billing
- Transactional consistency

---

# 3. Current Backend Base URL

Development API:

```text
http://localhost:8080
```

Do not hardcode this throughout the frontend.

Use one environment variable, for example:

```env
VITE_API_URL=http://localhost:8080
```

Expose it through one application config module.

Example:

```ts
export const appConfig = {
  apiUrl: import.meta.env.VITE_API_URL,
};
```

---

# 4. Frontend Technology Stack

Use:

- React
- Vite
- TypeScript
- Tailwind CSS
- shadcn/ui
- Lucide React
- React Router
- TanStack Query
- React Hook Form
- Zod
- Dexie
- IndexedDB
- vite-plugin-pwa
- Playwright

Recommended supporting packages:

```bash
npm install react-router-dom
npm install @tanstack/react-query
npm install react-hook-form zod @hookform/resolvers
npm install dexie
npm install lucide-react
npm install vite-plugin-pwa
```

Initialize shadcn/ui if not already configured:

```bash
npx shadcn@latest init
```

Do not introduce another general-purpose UI framework unless explicitly approved.

Do not mix:

- Material UI
- Mantine
- Ant Design
- Chakra UI
- Bootstrap
- daisyUI

with the primary shadcn/ui + Tailwind design system.

---

# 5. Codex UI Skill Stack

Use the following skill pipeline:

```text
frontend-design
      ↓
implementation
      ↓
ui-ux-pro-max
      ↓
make-interfaces-feel-better
      ↓
Playwright
```

Each skill has a distinct responsibility.

Do not ask all skills to redesign the same screen independently.

---

# 6. Installing Codex Skills

## 6.1 frontend-design

Install:

```bash
npx skills add https://github.com/anthropics/skills \
  --skill frontend-design \
  --agent codex
```

Verify that Codex can see a skill directory containing:

```text
SKILL.md
```

A project-local structure may resemble:

```text
.agents/
└── skills/
    └── frontend-design/
        └── SKILL.md
```

---

## 6.2 ui-ux-pro-max

Install the CLI:

```bash
npm install -g uipro-cli
```

Initialize it for Codex from the project root:

```bash
uipro init --ai codex
```

Verify that the resulting skill is available to Codex, commonly under:

```text
.agents/
└── skills/
    └── ui-ux-pro-max/
        └── SKILL.md
```

Keep the complete skill package if it includes supporting resources.

---

## 6.3 make-interfaces-feel-better

Install through the skills installer when available:

```bash
npx skills add jakubkrehel/make-interfaces-feel-better \
  --skill make-interfaces-feel-better \
  --agent codex
```

If installing manually, copy the complete skill folder, not only `SKILL.md`.

Example:

```text
.agents/
└── skills/
    └── make-interfaces-feel-better/
        ├── SKILL.md
        └── supporting reference files...
```

Do not remove referenced supporting files.

---

# 7. How to Use Each Skill

## frontend-design

Use when:

- Creating a new page
- Designing a major workflow
- Creating a new dashboard
- Designing the main POS screen
- Establishing hierarchy
- Establishing layout
- Establishing responsive behavior
- Creating a new admin area

It answers:

> What should this interface look and feel like?

Use it before large UI implementation.

---

## ui-ux-pro-max

Use after the initial layout or implementation.

It answers:

> Is this interface usable, accessible, responsive, and safe?

Focus on:

- Accessibility
- Touch targets
- Keyboard use
- Form usability
- Checkout safety
- Loading states
- Empty states
- Error states
- Responsive behavior
- Dialog behavior
- Focus management
- Navigation
- Repeated cashier actions

Do not use it to completely replace the established visual direction unless there is a serious usability problem.

---

## make-interfaces-feel-better

Use as a polish pass.

Focus on:

- Typography
- Spacing inconsistencies
- Optical alignment
- Icon sizing
- Border radii
- Border contrast
- Surface hierarchy
- Button feedback
- Hover/focus/pressed states
- Table density
- Micro-interactions

For TindaKart, motion must remain restrained.

Do not add decorative animation to high-frequency cashier interactions.

---

## Playwright

Use after implementation.

Validate the actual rendered UI for:

- Overflow
- Clipping
- Dialog positioning
- Sticky cart behavior
- Responsive layout
- Touch target size
- Keyboard navigation
- Focus order
- Product search
- Barcode-like keyboard input
- Checkout
- Credit-sale fields
- Error states
- Offline states
- Subscription/entitlement restrictions

Do not assume correct code means correct UI.

---

# 8. Skill Workflow by Task Size

## Small change

Example:

> Add an SKU column.

Workflow:

```text
implement
→ verify
```

Do not invoke every design skill.

---

## Medium UI feature

Example:

> Add a payment-method dialog.

Workflow:

```text
frontend-design
→ implement
→ ui-ux-pro-max
→ Playwright
```

---

## Major screen or redesign

Example:

> Redesign the full POS workspace.

Workflow:

```text
frontend-design
→ implement
→ ui-ux-pro-max
→ make-interfaces-feel-better
→ Playwright
```

---

# 9. TindaKart Visual Direction

The attached FastCart references are **inspiration**, not a pixel-perfect implementation target.

Do not copy branding, logo, names, product content, or exact screen composition.

Extract the following visual principles:

- Light blue application canvas
- White operational surfaces
- Strong cyan/sky-blue primary accent
- Dark navy text
- Soft neutral borders
- Rounded but not pill-heavy UI
- Spacious but compact desktop density
- Fixed left navigation for desktop
- Utility top bar
- Large central work surface
- Persistent action areas
- Simple outline icons
- Product imagery where it materially improves recognition
- High-contrast selected rows/cards using the primary accent
- Subtle status colors
- Clear totals and action buttons

The final product must feel like **TindaKart**, not FastCart.

---

# 10. Recommended Design Tokens

Use semantic tokens instead of scattered hardcoded values.

Suggested direction:

```css
:root {
  --background: 205 60% 96%;
  --foreground: 205 55% 16%;

  --card: 0 0% 100%;
  --card-foreground: 205 55% 16%;

  --primary: 195 70% 55%;
  --primary-foreground: 0 0% 100%;

  --muted: 205 45% 94%;
  --muted-foreground: 205 15% 45%;

  --border: 205 28% 88%;

  --success: 145 60% 42%;
  --warning: 38 95% 52%;
  --destructive: 0 72% 54%;
  --info: 205 80% 55%;
}
```

These are directional starting points only.

When actual brand tokens exist, use them.

Do not hardcode raw hex values repeatedly inside components.

---

# 11. Shape Language

Use a consistent radius system.

Recommended:

```text
Inputs / buttons:      10–12px
Panels:                14–16px
Large shell surfaces:  18–20px
Badges:                compact, not oversized
```

Avoid:

- Excessive pills
- Fully rounded everything
- Nested rounded cards
- Huge floating surfaces

Use borders and spacing to create hierarchy before adding shadows.

---

# 12. Shadow Rules

The inspiration uses very soft depth.

Use shadows sparingly.

Preferred:

- Most panels: border only
- Menus/popovers/dialogs: subtle shadow
- Sticky cart/action regions: subtle separation only

Avoid:

- Large blur shadows
- Colored glow
- Neon effects
- Stacked shadows
- Floating-everything aesthetic

---

# 13. Typography

Use one primary UI font unless TindaKart brand guidelines specify otherwise.

Good choices:

- Inter
- Geist
- Manrope

Use a restrained hierarchy.

Example:

```text
Page title:       24–28px / semibold
Section heading:  18–20px / semibold
Body:             14–16px
Table/body dense: 13–14px
Metadata:         12–13px
```

Do not use oversized SaaS-style headings inside operational screens.

Monetary values should use tabular numerals when possible.

---

# 14. Desktop Application Shell

Primary desktop shell:

```text
┌─────────────────────────────────────────────────────────────────┐
│ Sidebar │ Top utility bar                                       │
│         ├─────────────────────────────────────────────────────────┤
│         │ Main work area                                        │
│         │                                                       │
│         │                                                       │
│         │                                                       │
└─────────────────────────────────────────────────────────────────┘
```

Recommended desktop behavior:

- Left sidebar: fixed or sticky
- Top bar: sticky where useful
- Main content: scrollable
- Avoid full-page nested scrolling traps
- Keep operational actions within predictable locations

---

# 15. Sidebar Navigation

The inspiration uses a clean, wide sidebar with clear active states.

TindaKart navigation must be role-aware.

Potential groups:

```text
POS
Dashboard / Reports
Inventory
Debt / Customers
Deliveries
Suppliers
Stores
Staff
Subscription
Settings
Audit
```

Do not show items the user cannot use.

However:

> Hidden navigation is UX only, not security.

The backend still decides authorization.

Active navigation should use:

- Primary accent surface
- High-contrast icon/text
- No ambiguous active state

---

# 16. Top Utility Bar

The top bar may contain:

- Global/product search where relevant
- Barcode action
- New sale / POS shortcut
- Offline/online status
- Pending sync status
- Notifications
- Current vendor/store
- Current user
- Profile/session menu

Do not place irrelevant global controls on every page.

For example:

A subscription-management page does not need a dominant barcode scanner action.

Contextualize the top bar per route where appropriate.

---

# 17. Main POS Screen

For cashier use, prioritize a split workspace.

Recommended:

```text
┌───────────────────────────────────────┬─────────────────────────┐
│ Product Search / Barcode              │ Cart                    │
├───────────────────────────────────────┤                         │
│ Category Filters                      │ Items                   │
│                                       │                         │
│ Product Grid / Search Results         │                         │
│                                       ├─────────────────────────┤
│                                       │ Totals                  │
│                                       │ Payment / Checkout      │
└───────────────────────────────────────┴─────────────────────────┘
```

The cart should remain visible on desktop and landscape tablet when space allows.

Do not force the cashier to navigate to a separate cart page for every sale.

---

# 18. POS Product Grid

Use product imagery only when it improves recognition.

Each product tile should prioritize:

1. Product name
2. Price
3. Availability
4. Optional image
5. Optional compact category/variant information

Avoid oversized product images.

The POS is not an ecommerce storefront.

Selected/added feedback should be immediate.

---

# 19. Barcode Workflow

The backend exposes a barcode-resolution endpoint.

Expected cashier interaction:

```text
Scan barcode
     ↓
Resolve product
     ↓
Available?
 ┌───┴────┐
Yes      No
 ↓        ↓
Add      Show actionable message
cart
```

Do not show a confirmation dialog after every successful scan.

USB barcode scanners often act like fast keyboards.

Barcode handling must:

- Accept rapid input
- Avoid interfering with normal typing
- Detect completion reliably
- Handle unknown barcodes
- Prevent duplicate-event bugs
- Support explicit scanner UI where configured

---

# 20. Search

Product search should support the backend product search contract.

Prioritize:

- Product name
- SKU
- Barcode

Use debounced search only where appropriate.

For high-frequency local product lookup, safe cached reference data may be used when it does not bypass backend authority.

---

# 21. Cart Rules

The cart must be fast and predictable.

Support:

- Add
- Increase quantity
- Decrease quantity
- Enter quantity
- Remove
- Clear
- Discount display when server-derived
- Customer selection for credit sale
- Payment method
- Suspend locally only if product requirements explicitly support it

Do not invent backend "suspend sale" functionality unless it exists in the contract.

The active cart may be persisted locally to survive:

- Accidental refresh
- PWA restart
- Temporary network interruption

Persistence of an active cart is not the same as finalizing an offline sale.

---

# 22. Checkout Rules

Always keep the following visually clear:

```text
Subtotal
Discounts
Tax / VAT
Other valid charges
Grand Total
Payment Method
```

For cash payment, if the UX supports amount tendered:

```text
Grand Total
Amount Received
Change
```

Prevent accidental duplicate submission.

During final submission:

```text
disable relevant controls
→ submit
→ wait for authoritative backend response
→ show success or actionable failure
```

Do not present a failed/unsent sale as finalized.

---

# 23. Payment Methods

Do not hardcode the payment-method list as authoritative.

The backend default set is:

```text
CASH
CARD
EWALLET
CREDIT
```

but Vendor Admin can configure allowed methods.

The UI must read the allowed payment methods from vendor settings.

If a method is disabled by the backend:

- Do not offer it in checkout.
- Do not attempt to bypass the restriction.

---

# 24. Credit Sales

When payment method is:

```text
CREDIT
```

the backend requires:

- A vendor customer
- A future due date

Use the documented POS customer endpoint for customer selection.

The frontend should make these fields required before attempting submission.

The backend remains authoritative and may reject invalid credit-sale data.

Do not manually create debt accounts from the frontend as a second request after a sale.

The backend handles the corresponding debt-account logic.

---

# 25. Sale Completion

Use the documented sale endpoint.

Do not implement sale completion as several frontend-coordinated mutations such as:

```text
create sale
then decrease inventory
then add movement
then create receipt
```

The backend completes sale + stock deduction + movement + receipt numbering transactionally.

The frontend sends the sale request and reacts to the authoritative response.

---

# 26. Voids

Void is a high-risk action.

Use the documented void endpoint.

UX requirements:

- Require appropriate permission
- Clearly identify the sale
- Explain the consequence
- Use confirmation
- Prevent accidental double submission
- Refresh affected sale/inventory state after success

Do not manually restock inventory in the frontend after voiding.

The backend restores stock.

---

# 27. Receipts

Receipt data comes from the backend.

Vendor receipt output mode may be:

```text
BROWSER
THERMAL_BRIDGE
MANUAL
```

Do not assume every successful sale should immediately call browser print.

Current browser behavior may use receipt preview.

When the receipt has been printed/reprinted through a controlled workflow, use the documented printed endpoint where appropriate.

Do not invent unsupported thermal-bridge behavior.

---

# 28. Inventory

Inventory screens should prioritize:

- Product
- Batch/stock state where exposed
- Available quantity
- Expiration
- Recent movement
- Adjustment action
- Receive-stock action

Use the documented inventory and movement endpoints.

Do not maintain a separate authoritative client inventory count.

After mutations, refresh or invalidate the relevant cached queries.

---

# 29. Expiration Rules

The backend prohibits expired inventory from being sold.

Near-expiration discounts are backend-controlled.

The frontend may display:

- Expired
- Near expiry
- Discount applied
- Warning state

but must not become authoritative for:

- Sale eligibility
- Discount percentage
- Discount applicability

If cached UI state disagrees with the server, the server wins.

---

# 30. Deliveries

Use the documented delivery endpoints.

Expected UI areas:

- Delivery list
- Create delivery
- Status progression
- Receive all
- Receive with missing/damaged details

Do not fabricate delivery statuses not supported by the backend.

Status changes should be visually explicit and role-protected.

---

# 31. Debt

Debt UI may include:

- Customer list
- Debt accounts
- Payment history
- Record payment
- Aging report

Use the documented debt endpoints.

Do not derive authoritative debt balances only in the client.

After recording a payment, invalidate affected account/payment/aging queries.

---

# 32. Reports

Reports are backend-provided.

Use the documented report endpoint.

Do not replicate heavy business reporting logic in the browser if the backend already provides the report.

The frontend owns:

- Filter UI
- Date controls
- Visualization
- Tables
- Export UX when supported

The backend owns report data and calculations.

---

# 33. Subscription and Entitlements

The backend exposes:

- Package entitlements
- Available packages
- Subscription selection
- Checkout
- Billing history

The frontend must use entitlements to determine feature UX.

Do not rely only on disabled/hidden UI.

The backend enforces package features and limits.

Subscription UI should take inspiration from the reference's clean pricing layout, but must use TindaKart plan data from the backend.

Do not hardcode reference-image prices or plan names.

---

# 34. Authentication

The backend uses a server session cookie.

Do NOT implement JWT/localStorage authentication unless the backend contract changes.

Authentication flow uses documented auth endpoints.

Protected mutating requests require CSRF protection.

The existing PWA API client should:

1. Obtain CSRF data when needed.
2. Send the returned token using the returned header name.
3. Include server session cookies.

Do not store session secrets in:

- localStorage
- IndexedDB
- exposed application state

Use secure browser cookie/session behavior provided by the backend.

---

# 35. API Client

Centralize HTTP logic.

Suggested structure:

```text
src/
└── lib/
    └── api/
        ├── client.ts
        ├── csrf.ts
        ├── errors.ts
        └── types.ts
```

The API client should consistently handle:

- Base URL
- Credentials/cookies
- CSRF
- JSON parsing
- Non-2xx responses
- Auth expiration
- Structured errors
- Request cancellation where useful

Do not repeat raw `fetch()` configuration throughout page components.

---

# 36. Tenant Context

The operational context is:

```text
vendor
+
store
```

Use the documented auth/context endpoints.

Do not trust arbitrary client-provided vendor/store IDs.

The server validates context.

The frontend should:

- Load allowed vendors/stores from the current user/session.
- Allow context switch only through the documented context endpoint.
- Store only non-sensitive UI convenience state locally if needed.
- Refresh scoped queries after context changes.

On context switch, invalidate:

- Products
- Inventory
- Sales
- Customers
- Deliveries
- Reports
- Other store/vendor-scoped data

---

# 37. Permissions

Backend-defined permissions include operational capabilities such as:

```text
POS_USE
INVENTORY_VIEW
INVENTORY_MANAGE
DEBT_MANAGE
DELIVERY_MANAGE
```

Use permissions for UX:

```text
allowed?
→ show / enable feature
```

But backend permission enforcement remains authoritative.

Do not treat route guards as security boundaries.

---

# 38. PWA Rules

Use `vite-plugin-pwa`.

Support:

- Installability
- App-shell caching
- Static-asset caching
- Safe offline startup
- Update detection
- Network recovery
- Persistent current cart
- Safe cached reference data

Do not blindly cache all API requests.

Do not cache mutating financial requests as ordinary offline requests.

---

# 39. Offline Behavior

The current API documentation does not define a completed offline-sale synchronization contract.

Therefore:

Allowed:

- Cache application shell
- Cache safe product/reference data
- Persist current cart
- Persist non-sensitive preferences
- Show offline state
- Show reconnect state

Do NOT assume support for:

- Finalizing offline sales
- Queueing financial mutations
- Queueing voids
- Queueing debt payments
- Queueing stock adjustments
- Generating final receipt numbers offline

Until the backend explicitly defines synchronization/idempotency behavior, final transactional operations that require server authority should require backend reachability.

If offline:

```text
Sale is still in cart
Connection unavailable
Reconnect to complete checkout
```

Do not silently discard cart contents.

---

# 40. IndexedDB / Dexie

Dexie may persist safe client state such as:

```text
catalogCache
categoryCache
currentCart
uiPreferences
syncMetadata
```

Do not store:

- Passwords
- Session cookies
- CSRF secrets longer than necessary
- PayMongo secrets
- Sensitive payment credentials

The local DB is a UX cache, not the source of truth.

---

# 41. Network State

Do not assume:

```ts
navigator.onLine
```

means the API is reachable.

Use it only as a hint.

Actual API calls must handle:

- Timeout
- Backend unavailable
- Session expired
- Permission denied
- Tenant mismatch
- Validation failure
- Conflict
- Server error

Use clear user-facing language.

BAD:

```text
POST /api/vendors/... 403
```

GOOD:

```text
You no longer have permission to complete this action.
Refresh your session or contact an administrator.
```

---

# 42. Error UX

Every operational error should answer:

1. What happened?
2. Is the user's work safe?
3. What can they do next?

Examples:

```text
Could not complete the sale.
Your cart is still available.
Check the connection and try again.
```

```text
This product can no longer be sold from the selected inventory.
Refresh the cart and review the updated availability.
```

---

# 43. Loading UX

Avoid full-screen spinners for routine page actions.

Prefer localized loading.

Examples:

- Product list skeleton while cart remains usable
- Button loading state during mutation
- Inline table loading
- Background query refresh indicator

Critical mutations may temporarily lock related controls.

---

# 44. Required UI States

All data-driven features should consider:

```text
default
loading
empty
error
success
disabled
offline
permission-restricted
entitlement-restricted
```

Interactive controls should consider:

```text
hover
focus-visible
active
pressed
disabled
loading
```

Do not implement only the ideal success state.

---

# 45. Touch Targets

Frequent POS controls should target approximately:

```text
44–48px minimum interactive height/area
```

Especially:

- Product tiles
- Checkout
- Category filters
- Quantity controls
- Payment methods
- Barcode button
- Cart actions

Do not create tiny `+` / `−` controls.

---

# 46. Keyboard Support

Desktop POS must support efficient keyboard use.

Potential shortcuts:

```text
F2       Focus product search
F4       Open checkout
Esc      Close non-destructive dialog
Enter    Confirm current safe action
↑ / ↓    Navigate search results
```

Do not override important browser/system shortcuts without a strong reason.

Show shortcuts in tooltips or labels where useful.

---

# 47. Accessibility

Target WCAG AA where practical.

Requirements:

- Semantic HTML
- Visible focus
- Keyboard navigation
- Accessible names
- Form labels
- Appropriate ARIA only where needed
- Sufficient contrast
- Error association with fields
- Reduced-motion support

Icon-only buttons require accessible labels.

Example:

```tsx
<Button
  variant="ghost"
  size="icon"
  aria-label="Remove item"
>
  <Trash2 />
</Button>
```

Never communicate state through color alone.

---

# 48. Responsive Targets

Always verify important screens at:

```text
375 × 812
768 × 1024
1024 × 768
1280 × 800
1366 × 768
1440 × 900
1920 × 1080
```

Primary POS optimization:

```text
1024 × 768
1280 × 800
1366 × 768
1920 × 1080
```

Mobile phone support matters more for admin/reporting views than for the primary cashier workspace unless requirements say otherwise.

No unintended horizontal page overflow.

---

# 49. Inspiration-Specific Responsive Behavior

The visual references are wide desktop layouts.

Do not simply shrink the desktop shell on mobile.

Desktop:

```text
Sidebar + Topbar + Main workspace + Persistent cart
```

Tablet:

```text
Collapsible sidebar
Main workspace
Persistent or slide-over cart depending on width
```

Mobile admin:

```text
Top navigation / drawer
Single-column content
Bottom or sheet actions where appropriate
```

Primary cashier phone usage is not a first-class target unless explicitly required.

---

# 50. Cards and Panels

The inspiration uses many bordered panels.

Use panels intentionally, but avoid nested card overload.

Prefer:

- One major panel per functional region
- Internal rows separated by borders/dividers
- Background hierarchy
- Spacing
- Headings

Avoid:

```text
Card
 └─ Card
     └─ Card
```

for ordinary layout.

---

# 51. Tables and Dense Lists

Operational tables should be compact and scan-friendly.

Alignment:

```text
Text      left
Numbers   right
Currency  right
Actions   consistent edge
```

Use:

- Sticky headers where useful
- Search
- Filters
- Pagination
- Sort
- Clear status

Do not use giant card rows for datasets that are better represented as tables.

---

# 52. Status Colors

Use semantic status colors.

Examples:

```text
Active / Synced / In stock      success
Warning / Low stock             warning
Error / Critical / Failed       destructive
Info / Processing               info
```

Always include text or icons.

BAD:

```text
●
```

GOOD:

```text
● Low stock
```

---

# 53. Charts

Charts should support decisions, not decoration.

Use charts for:

- Sales trends
- Revenue
- Stock aging
- Best sellers
- Debt aging
- Profit
- Delivery trends

Avoid:

- 3D charts
- Decorative gradients
- Too many colors
- Unlabeled metrics
- Tiny unreadable charts

Use backend report data.

Do not invent unsupported analytics.

---

# 54. Login Screen

The reference login screen suggests:

- Large visual area
- Clean sign-in panel
- Strong TindaKart branding
- Simple fields
- Clear submit action

For TindaKart:

- Do not include account creation unless backend/product requirements support self-registration.
- Do not include "Forgot Password?" unless the backend supports a recovery flow.
- The current documented backend supports login, current-user session, password change, and logout.

Do not fake unsupported auth flows.

---

# 55. Subscription Screen

The reference shows a clean four-column pricing design.

For TindaKart:

- Render actual package data.
- Highlight the current plan.
- Highlight recommended upgrades only when product rules define them.
- Display limits and entitlements clearly.
- Use actual backend package/entitlement data.
- Billing checkout must use the documented backend flow.

Do not hardcode inspiration-image pricing.

---

# 56. Inventory Screen

Take inspiration from the reference:

- Main product/stock list
- Restock/delivery context
- Expiration/waste awareness
- Clear status labels

But map it to actual TindaKart capabilities.

Suggested desktop inventory layout:

```text
Header / filters
──────────────────────────────
Inventory table/list
──────────────────────────────
Recent movements / expiration insights
```

Use dedicated receive-stock and adjustment flows rather than forcing every operation into one giant page.

---

# 57. Dashboard / Reports Screen

Use the reference's clean KPI + chart structure, but avoid fake metrics.

Only display data supported by actual report endpoints.

Good structure:

```text
KPI row
Sales trend
Best sellers
Low stock / expiration warnings
Relevant operational table
```

Do not build "AI-powered insights" unless the backend/product explicitly supports them.

---

# 58. Customer / Debt Screens

The inspiration's customer page is visually useful, but TindaKart customer behavior is primarily tied to POS credit/debt features.

Do not invent loyalty tiers, send-offers workflows, retention scores, or marketing automation unless product/backend requirements support them.

Use actual debt/customer data.

---

# 59. Feature Honesty Rule

Never implement UI that implies unsupported functionality.

Examples that must not be added without backend/product support:

- Forgot-password workflow
- Customer self-registration
- Loyalty program
- Send offers
- Employee calling
- AI insights
- Offline finalized sales
- Thermal printing integration
- Advanced analytics not provided by backend
- Automatic supplier ordering
- Email/SMS automation

The inspiration controls the visual direction, not the feature set.

---

# 60. Suggested Frontend Structure

```text
src/
├── app/
│   ├── router.tsx
│   ├── providers.tsx
│   ├── query-client.ts
│   └── config.ts
│
├── components/
│   ├── ui/
│   ├── layout/
│   └── shared/
│
├── lib/
│   ├── api/
│   │   ├── client.ts
│   │   ├── csrf.ts
│   │   ├── errors.ts
│   │   └── types.ts
│   ├── db/
│   │   └── local-db.ts
│   ├── auth/
│   └── permissions/
│
├── modules/
│   ├── auth/
│   ├── context/
│   ├── pos/
│   ├── catalog/
│   ├── inventory/
│   ├── debt/
│   ├── deliveries/
│   ├── suppliers/
│   ├── reports/
│   ├── settings/
│   ├── subscription/
│   ├── billing/
│   ├── staff/
│   ├── stores/
│   ├── super-admin/
│   └── audit/
│
├── hooks/
├── types/
└── utils/
```

---

# 61. Feature Module Structure

Example POS module:

```text
modules/pos/
├── pages/
│   ├── PosPage.tsx
│   ├── SaleReceiptPage.tsx
│   └── SaleDetailsPage.tsx
│
├── components/
│   ├── ProductSearch.tsx
│   ├── CategoryFilter.tsx
│   ├── ProductGrid.tsx
│   ├── ProductTile.tsx
│   ├── Cart.tsx
│   ├── CartItem.tsx
│   ├── CartTotals.tsx
│   ├── PaymentDialog.tsx
│   ├── CustomerSelector.tsx
│   ├── OfflineBanner.tsx
│   └── ReceiptPreview.tsx
│
├── hooks/
│   ├── useCart.ts
│   ├── useBarcodeScanner.ts
│   └── useCheckout.ts
│
├── api/
│   └── pos.api.ts
│
├── schemas/
│   └── checkout.schema.ts
│
└── types/
    └── pos.types.ts
```

---

# 62. Existing Components First

Before creating a component:

1. Search the repository.
2. Check `components/ui`.
3. Check `components/shared`.
4. Check the relevant feature module.
5. Reuse or extend existing components.

Do not create:

```text
Button
PrimaryButton
BlueButton
SubmitButton
ActionButton
```

when one configurable `Button` is enough.

---

# 63. Presentational vs Business Logic

Keep business logic out of presentational components.

Prefer:

```text
UI Component
     ↓
Hook
     ↓
Feature service / API function
     ↓
Central API client
```

Example:

```text
PaymentDialog
     ↓
useCheckout()
     ↓
pos.api.ts
     ↓
api/client.ts
```

---

# 64. Forms

Use:

- React Hook Form
- Zod
- shadcn form controls

Frontend validation improves UX.

Backend validation remains authoritative.

Do not duplicate complex backend business rules into Zod unless they are stable client-side input rules.

Examples suitable for client validation:

- Required field
- Valid date format
- Positive quantity
- Non-empty payment method
- Credit sale requires customer and due date

---

# 65. Query and Mutation State

Use TanStack Query for server state.

Use:

- `useQuery` for reads
- `useMutation` for writes
- Query invalidation after writes

Do not use global state as a replacement for the server cache.

Suggested query-key pattern:

```ts
['vendor', vendorId, 'products']
['vendor', vendorId, 'store', storeId, 'inventory']
['vendor', vendorId, 'store', storeId, 'sales']
['vendor', vendorId, 'debt', 'accounts']
```

Context changes must invalidate scoped data.

---

# 66. Avoid Over-Abstraction

Do not create abstractions before repeated patterns exist.

Avoid:

```text
GenericEnterpriseDataPanelFactory
UniversalEntityManager
MegaCrudRenderer
```

Prefer explicit feature components.

Refactor after meaningful repetition is proven.

---

# 67. Performance

Prioritize POS responsiveness.

Avoid:

- Huge dependency additions
- Fetching massive historical datasets
- Re-rendering the whole product grid on small cart changes
- Large unoptimized product images
- Blocking unrelated UI during API refresh
- Excessive animation

Prefer:

- Query caching
- Route lazy loading
- Local active-cart state
- Memoization only where justified
- Optimized images
- Paginated server data where supported

---

# 68. Security

Never expose:

- PayMongo secrets
- Server credentials
- Session secrets
- Sensitive backend configuration

PayMongo subscription payment processing is backend-controlled.

The browser uses the documented billing endpoints.

Do not call PayMongo secret APIs directly from frontend code.

---

# 69. frontend-design Prompt Template

Use:

```text
$frontend-design

Design and implement a TindaKart PWA POS screen.

Before coding:
1. Read AGENTS.md.
2. Read the relevant API_DOCUMENTATION.md section.
3. Inspect existing layout, tokens, and reusable components.
4. Do not invent unsupported backend functionality.

Visual inspiration:
- light blue application canvas
- white operational panels
- cyan/sky-blue primary accent
- dark navy typography
- soft borders
- restrained radii
- simple outline icons
- clean desktop POS density
- strong selected states
- minimal shadows

Do not copy FastCart branding or content.

Product principles:
- operational
- fast
- predictable
- touch-friendly
- keyboard-friendly
- accessible
- responsive
- PWA-aware

Avoid:
- excessive cards
- nested cards
- gradients
- glassmorphism
- giant headings
- neon
- excessive pills
- decorative animations
- fake features
- hardcoded API data

Before implementation define:
- visual hierarchy
- information architecture
- responsive behavior
- high-frequency actions
- loading state
- empty state
- error state
- offline state
- permission state
- entitlement state

Then implement using:
React
TypeScript
Tailwind
shadcn/ui
Lucide
TanStack Query
```

---

# 70. POS-Specific frontend-design Prompt

```text
$frontend-design

Design and implement the main TindaKart Point of Sale workspace.

Read AGENTS.md and API_DOCUMENTATION.md first.

Primary user:
Cashier on desktop or touchscreen terminal.

Priorities:
1. transaction speed
2. checkout correctness
3. barcode efficiency
4. readable cart totals
5. touch usability
6. keyboard usability

Desktop layout:
- left application navigation
- top utility bar
- central product search/catalog
- persistent right cart
- cart footer with totals and checkout

Use the TindaKart visual direction:
- pale blue canvas
- white surfaces
- cyan primary accent
- dark navy text
- soft neutral borders
- 12–16px radii
- minimal shadow
- strong selected states

Required behavior must match the backend contract:
- documented product search
- documented barcode resolution
- vendor/store context
- configured payment methods
- credit customer selection
- future due date for CREDIT
- server-authoritative checkout
- receipt preview behavior
- offline cart preservation

Do not implement finalized offline sales unless the backend contract explicitly supports them.

Avoid:
- ecommerce storefront styling
- oversized product imagery
- unnecessary modal steps
- confirmation after every scan
- fake payment methods
- fake loyalty features
```

---

# 71. ui-ux-pro-max Prompt Template

```text
$ui-ux-pro-max

Audit the existing TindaKart implementation.

Read AGENTS.md first.

Do not redesign the established visual identity unless there is a serious usability issue.

Audit:
- transaction speed
- accessibility
- keyboard navigation
- focus order
- touch target size
- barcode behavior
- cart behavior
- checkout safety
- credit-sale requirements
- configured payment methods
- duplicate-submit prevention
- loading states
- empty states
- errors
- offline state
- permission-restricted state
- entitlement-restricted state
- responsive behavior
- dialogs and sheets

Classify findings:
CRITICAL
HIGH
MEDIUM
LOW

Fix CRITICAL and HIGH issues.

Preserve:
- React
- Tailwind
- shadcn/ui
- current design tokens
- backend contract
```

---

# 72. make-interfaces-feel-better Prompt

```text
$make-interfaces-feel-better full TindaKart POS

Polish the existing implementation.

Do not change:
- backend behavior
- information architecture
- feature scope
- current design system

Focus on:
- typography
- number alignment
- spacing
- icon sizing
- icon alignment
- border consistency
- radius consistency
- surface contrast
- button feedback
- focus-visible styles
- hover/pressed states
- table density
- subtle transitions

TindaKart is an operational POS.

Keep motion minimal and immediate.

Do not add:
- decorative animations
- glow
- gradients
- excessive shadows
- oversized cards
```

---

# 73. Playwright QA Prompt

```text
Use Playwright to test the TindaKart frontend.

Read AGENTS.md first.

Test desktop sizes:
1024x768
1280x800
1366x768
1440x900
1920x1080

Also test:
768x1024
375x812

For the POS flow test:
- page load
- vendor/store context
- product search
- barcode-like input
- add product
- increment quantity
- decrement quantity
- remove product
- empty cart
- configured payment methods
- CASH checkout UI
- CREDIT customer selection
- CREDIT due-date validation
- duplicate submission protection
- backend validation error
- permission error
- offline state
- reconnect state
- receipt preview

Inspect:
- horizontal overflow
- clipped labels
- tiny controls
- unreadable totals
- cart overflow
- broken sticky elements
- dialog/sheet overflow
- focus visibility
- keyboard usability
- inconsistent spacing

Fix discovered problems without changing the established TindaKart visual direction.
```

---

# 74. Development Checklist

Before finishing a feature:

## Backend Contract

- [ ] Relevant API documentation read
- [ ] Endpoint exists
- [ ] Correct vendor/store scope used
- [ ] CSRF/session behavior preserved
- [ ] Permissions respected
- [ ] Entitlements respected
- [ ] No backend business rules moved into the client

## Functional

- [ ] Main workflow works
- [ ] Validation works
- [ ] Duplicate mutation prevented
- [ ] Loading state exists
- [ ] Empty state exists
- [ ] Error state exists
- [ ] Offline state considered
- [ ] Auth-expired state considered
- [ ] Permission state considered
- [ ] Entitlement state considered

## POS

- [ ] Product search works
- [ ] Barcode flow works
- [ ] Cart persists appropriately
- [ ] Cart total is clear
- [ ] Payment methods come from settings
- [ ] Credit requires customer + future due date
- [ ] Sale submission is server-authoritative
- [ ] Receipt behavior follows vendor settings

## Visual

- [ ] TindaKart visual direction followed
- [ ] No copied FastCart branding
- [ ] No unnecessary gradients
- [ ] No excessive cards
- [ ] No decorative animation
- [ ] Consistent radii
- [ ] Consistent borders
- [ ] Strong active state
- [ ] Totals prominent

## Accessibility

- [ ] Keyboard accessible
- [ ] Focus visible
- [ ] Labels present
- [ ] Icon-only buttons named
- [ ] Contrast acceptable
- [ ] No color-only state
- [ ] Reduced motion respected

## Responsive

- [ ] 1024×768 tested
- [ ] 1280×800 tested
- [ ] 1366×768 tested
- [ ] 1440×900 tested
- [ ] 1920×1080 tested
- [ ] Tablet tested
- [ ] No unintended horizontal overflow

## Quality

- [ ] Typecheck passes
- [ ] Lint passes
- [ ] Tests pass
- [ ] Production build passes
- [ ] Playwright critical flow passes

---

# 75. Final Rule

When implementing TindaKart, prioritize:

```text
backend correctness
    >
transaction safety
    >
cashier speed
    >
error prevention
    >
accessibility
    >
consistency
    >
responsive behavior
    >
visual polish
    >
decorative design
```

A visually attractive POS that invents unsupported backend behavior is wrong.

A fast POS that misrepresents a failed sale as successful is wrong.

A PWA that loses the active cart during a temporary disconnection is poor UX.

A strong TindaKart frontend should be:

**fast, clear, resilient, backend-aligned, accessible, and visually disciplined.**
