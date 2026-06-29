# Requirements Document

## Introduction

Redesign the landing page `NavigationHeader` to match the floating pill aesthetic of the shadcn navigation-5 component, with navigation content tailored to the project's existing section structure and using the project's Slack-themed CSS custom properties for all colors. This requires introducing shadcn/ui components (`NavigationMenu`, `Sheet`, `Accordion`, `Badge`) alongside the existing custom component library.

## Glossary

- **Floating Pill Navbar**: A horizontal navigation bar with `rounded-full` shape, centered within the viewport, with a shadow/border that makes it appear to float above the page content rather than span edge-to-edge.
- **Mega Menu**: An expanded dropdown panel triggered by a nav item that displays multiple columns of links and content.
- **Sheet**: A slide-in panel component from shadcn (used here for the mobile drawer).
- **Scroll Spy**: The existing mechanism that tracks which page section is visible and highlights the corresponding nav item.
- **Slack CSS Tokens**: The custom CSS custom properties (`--slack-primary`, `--slack-surface-*`, `--slack-text-*`, `--slack-border`) defined in `globals.css` that form the project's design system. Used as Tailwind classes via `@theme` (e.g., `bg-slack-surface-primary`, `text-slack-text-primary`).

## Requirements

### Requirement 1: Floating Pill Navbar Container

**User Story:** As a visitor, I want the navigation bar to have a modern floating pill appearance.

#### Acceptance Criteria

1. WHEN the landing page loads, THE navigation bar SHALL render as a centered, horizontal pill with `rounded-full` corners, a border, a shadow, and a max-width constrained container.
2. WHEN the page scrolls, THE navigation bar SHALL remain fixed at the top (`sticky top-0 z-40`).
3. WHEN on desktop (`lg:` and above), THE pill SHALL contain logo on the left, nav links in the center, action items on the right.
4. THE navigation bar SHALL NOT span edge-to-edge; it SHALL be narrower than the viewport with horizontal padding.
5. **ALL colors SHALL use Slack CSS tokens** (`bg-slack-surface-primary`, `border-slack-border`, `text-slack-text-primary`, `text-slack-text-secondary`, `bg-slack-primary`, `bg-slack-surface-tertiary`, `shadow-slack-*`) — NOT `neutral-*`, `orange-*`, or `gray-*` classes.

### Requirement 2: Navigation Links with Scroll-to-Section

**User Story:** As a visitor, I want to click nav links to scroll to sections.

#### Acceptance Criteria

1. WHEN clicked, THE page SHALL smoothly scroll to the corresponding section by `id`.
2. THE active section's nav link SHALL be highlighted using `bg-slack-surface-tertiary` and `text-slack-text-primary`.
3. **THE nav labels SHALL be the project's section names**: Home, Readiness, System, Integration, Community, Stories.

### Requirement 3: Mega Menu / Dropdown for "System"

**User Story:** As a visitor, I want a rich dropdown for the System nav item showing module cards.

#### Acceptance Criteria

1. WHEN triggered, A dropdown SHALL appear with columns showing Pulse, Relay, and Aurora module cards.
2. THE dropdown SHALL use `bg-slack-surface-primary`, `border-slack-border`, and `text-slack-text-*` tokens.
3. THE dropdown SHALL have rounded corners and SHALL dismiss on outside click or Escape.

### Requirement 4: Right-Side Action Items

1. THE nav bar SHALL include `ThemeToggle`, a "Sign In" link (`/auth/login`), and a "Start Journey" CTA (`/auth/register`).
2. "Sign In" SHALL use `border-slack-border` secondary styling; "Start Journey" SHALL use `bg-slack-primary` primary styling.

### Requirement 5: Mobile Navigation via Sheet Drawer

1. Below `lg:`, THE pill SHALL show a hamburger button instead of center links.
2. Clicking it opens a shadcn `Sheet` from the right with logo, all 6 nav links (System uses `Accordion` for sub-items: Pulse, Relay, Aurora), Sign In, and Start Journey.
3. THE Sheet SHALL use Slack CSS tokens exclusively.
4. Dismissible via close button, overlay click, or Escape.

### Requirement 6: shadcn/ui Integration

1. Install with `npx shadcn@latest init` configured for Tailwind CSS v4, React 19, Next.js 16 App Router.
2. Add components: `navigation-menu`, `button`, `badge`, `sheet`, `accordion`.
3. Customize all shadcn components to use Slack CSS tokens instead of default `neutral-*` colors.
4. Existing custom `Button` component remains untouched.

### Requirement 7: Preservation of Existing Behavior

1. Scroll-to-section, scroll-spy (140px offset), theme toggle, auth links, and mobile nav functionality SHALL remain unchanged.
2. All accessibility features (keyboard navigation, ARIA labels, focus trapping) SHALL be preserved or improved.

## Scope

### In-Scope
- Rewrite `NavigationHeader.tsx` to floating pill aesthetic
- Install & configure shadcn/ui (`navigation-menu`, `button`, `badge`, `sheet`, `accordion`)
- Customize shadcn components to use Slack CSS tokens
- Nav labels: Home, Readiness, System, Integration, Community, Stories
- Mega menu dropdown for System (Pulse, Relay, Aurora)
- Desktop pill + mobile Sheet drawer with Accordion
- Add `lucide-react` dependency

### Out-of-Scope
- Chat app sidebar/bottom tab bar, splash screen, hero, features, footer
- Backend changes, custom UI component removal, CSS variable changes, section ID changes
