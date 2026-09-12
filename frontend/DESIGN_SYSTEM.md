# Nova HR — front-end design system

This document is the single source of truth for how every page in this
product should look and behave. Follow it exactly when building or
extending any screen. Where this document is silent, make the smallest
possible extension consistent with the rules below rather than inventing
a new pattern.

## 1. Color

Pure black / white / grey base. No blue, purple, amber, or any other
accent color anywhere in the product. Color is reserved entirely for two
semantic states:

- **Green** — approved / success / completed. Nothing else.
- **Red (light tint, not saturated)** — requires action from a specific
  person right now (pending approval, failed payment, expiring
  contract). Red is a call to action, not decoration.
  - Red never appears on a button, nav item, logo, or purely
    informational badge.
  - Aim for at most one red element visible on screen at a time.
- **Grey** — the default for status, structure, and "informational but
  not urgent" states.

Do not introduce a UI accent color (no blue, teal, indigo, etc.) even
for focus rings, active nav states, or links. Active/selected states are
shown with a light grey fill (`surface-1`) and a border, plus font
weight, never with color.

Surfaces: white cards on a very light grey page background, with
hairline (0.5px) borders — never drop shadows.

**Exception — the login/auth screen only:** dark near-black background
as a deliberate "front door" moment. Still black/white/grey only, just
inverted.

## 2. Typography

Typeface: **Public Sans** (Google Fonts), loaded for the whole app.
Weights used: 400 (body/regular) and 500 or 700 (labels/headings/
numbers) only.

- Page heading / greeting: 20px, weight 500-700
- Card label / eyebrow text: 12px, text-secondary
- Stat numbers: 20px, weight 500
- Body / row text: 13px, weight 400
- Muted / meta text: 11px, text-muted

Sentence case everywhere. No ALL CAPS labels.

## 3. Spacing

Comfortable scale throughout:
- Card padding: 14px
- Grid/section gaps: 10px
- Vertical rhythm between sections: 1.25-1.75rem

## 4. Motion — the frost rule, with restraint

Every hoverable, clickable surface gets a frost transition (~0.2-0.25s
ease shift from surface-1 to surface-2, plus a hairline border):

- **Full frost** (background lighten + border + 1px lift via
  translateY(-1px)) — buttons, metric/stat cards, nav items, standalone
  cards.
- **Flat hover** (background tint only) — rows inside dense
  lists/tables (20+ items).

Two named patterns used only in their specific contexts:
- **Sand-fill** — button background fills left-to-right during file
  generation/download, swaps to checkmark, resets. Not a generic
  spinner.
- **Comet ring** — login screen only. Static dashes with two glowing
  comets chasing each other (4s rotation).

No gradients, no glassmorphism, no drop shadows anywhere.

## 5. Layout

- Fixed-width sidebar (~200px) + fluid content area. Don't center
  content in a narrow column.
- Vary page structure by content type — table-heavy pages look
  different from stat/summary pages.
- Corner radius: modest (8-12px). Never large bubbly radii (20px+) on
  structural containers — reserve pill radii (20px) for small controls
  (login inputs/buttons, status badges).

## 6. Icons

Functional wayfinding only, never decorative filler.

## 7. Empty / loading / error states

- **Empty**: same container shape as populated state, centered icon +
  one line of specific copy + one action button.
- **Loading**: skeleton shapes in surface-1 matching the exact shape of
  incoming content — not a generic spinner (except sand-fill for
  downloads).
- **Error**: shown inline next to what failed, using the reserved red,
  never a full-page takeover or banner. Plain language, no apology, no
  raw error strings.

## 8. Content and copy

Real names, dates, numbers everywhere — never lorem ipsum. Sentence
case, active voice, verb-first buttons ("Download payslip," not
"Submit").

## 9. What to avoid

- Purple/indigo gradients or glassmorphism
- Generic default sans-serif with no hierarchy
- Center-aligned identical card grids on every page
- Rounded corners + drop shadows on everything
- Icon-heavy decoration that doesn't aid scanning
- Untouched component-library defaults
- Lorem-ipsum placeholder content
