# Implementation Plan: Landing Navigation Redesign

## Overview

Four-phase approach: (1) initialize shadcn/ui and install components, (2) customize shadcn components with Slack tokens, (3) rewrite NavigationHeader with floating pill + mega menu, (4) write tests and verify.

## Tasks

### Phase 1: shadcn/ui Setup

- [ ] 1. Initialize shadcn/ui in the frontend project
  - Run `npx shadcn@latest init` configured for Next.js 16 App Router, Tailwind CSS v4, React 19
  - Set up `components.json` with paths matching project structure (`@/components/ui` for shadcn, `@/lib` for utils)
  - Generate `lib/utils.ts` with `cn()` helper (required by shadcn components)
  - _Requirements: 6.1_

- [ ] 2. Add required shadcn components
  - Run `npx shadcn@latest add navigation-menu`
  - Run `npx shadcn@latest add sheet`
  - Run `npx shadcn@latest add accordion`
  - Run `npx shadcn@latest add badge`
  - Run `npx shadcn@latest add button` (shadcn Button, not to be confused with the project's custom Button)
  - Install `lucide-react` via `npm install lucide-react`
  - _Requirements: 6.2, 6.4_

- [ ] 3. Checkpoint — shadcn setup complete
  - Verify `frontend/components/ui/` has new folders: `navigation-menu.tsx`, `sheet.tsx`, `accordion.tsx`, `badge.tsx`, `button.tsx`
  - Verify `frontend/lib/utils.ts` exists with `cn()` export
  - Verify `frontend/package.json` contains `lucide-react`
  - Build succeeds with `npm run build`

### Phase 2: Customize shadcn Components with Slack Tokens

- [ ] 4. Customize `navigation-menu.tsx` colors
  - Replace all `bg-white` / `bg-background` → `bg-slack-surface-primary`
  - Replace all `bg-neutral-100` / `bg-muted` → `bg-slack-surface-tertiary`
  - Replace all `text-neutral-900` / `text-foreground` → `text-slack-text-primary`
  - Replace all `text-neutral-500` / `text-muted-foreground` → `text-slack-text-secondary`
  - Replace all `border-neutral-200` / `border` → `border-slack-border`
  - Replace all `hover:bg-neutral-100` / `hover:bg-accent` → `hover:bg-slack-surface-tertiary`
  - Adjust `NavigationMenuViewport` styling for pill-aligned rounded corners
  - _Requirements: 6.3, 1.5, 3.3_

- [ ] 5. Customize `sheet.tsx` colors
  - Replace all color tokens with Slack equivalents (same mapping as above)
  - Ensure dark mode works via `:root.dark` variable overrides
  - _Requirements: 6.3, 5.5_

- [ ] 6. Customize `accordion.tsx` and `badge.tsx` colors
  - Apply same Slack token mapping
  - _Requirements: 6.3_

- [ ] 7. Checkpoint — component customization complete
  - Visually inspect each shadcn component renders with Slack colors
  - Toggle dark mode and verify all components adapt

### Phase 3: Rewrite NavigationHeader

- [ ] 8. Create shared data file for nav items
  - Create or update `frontend/components/landing/navigationData.ts` with `MENU_ITEMS` and `SYSTEM_MODULES` constants
  - Add `lucide-react` icon names to `SYSTEM_MODULES` for the mega menu cards
  - _Requirements: 2.3, 3.2_

- [ ] 9. Rewrite `NavigationHeader.tsx` — floating pill structure
  - Outer `<header>` with `sticky top-0 z-40`
  - Centered container (`mx-auto max-w-5xl px-4`)
  - Pill div: `flex h-16 items-center justify-between gap-2 rounded-full border border-slack-border bg-slack-surface-primary shadow-slack-md`
  - Logo section on left (same as current)
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 10. Implement desktop NavigationMenu
  - Add `<NavigationMenu>` with `NavigationMenuList` inside the pill, hidden below `lg:`
  - Add `NavigationMenuLink` items for Home, Readiness, Integration, Community, Stories (simple scroll links)
  - Add `NavigationMenuTrigger` + `NavigationMenuContent` for System (mega menu)
  - Implement scroll-to-section in the click handler
  - Implement scroll-spy active section tracking (preserve existing `useEffect` with `getBoundingClientRect`)
  - _Requirements: 2.1, 2.2, 2.3_

- [ ] 11. Create `SystemMegaMenu.tsx` sub-component
  - 3-column grid layout inside `NavigationMenuContent`
  - Each column: icon in rounded box (lucide icon), title, role description, stat pills
  - Use Slack tokens throughout
  - Match styling of project's existing card patterns (rounded-2xl borders, etc.)
  - _Requirements: 3.1, 3.2, 3.3_

- [ ] 12. Implement desktop action items
  - `ThemeToggle` button
  - Optional icon button (e.g., `Search` or `Command` lucide icon)
  - "Sign In" link: `<Button variant="outline" className="border-slack-border">`
  - "Start Journey" CTA: `<Button className="bg-slack-primary text-slack-text-inverse">`
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 13. Implement mobile Sheet drawer
  - Add `<Sheet>` with `<SheetTrigger>` (hamburger icon, `lg:hidden`)
  - `<SheetContent side="right" className="w-[300px] sm:w-[400px]">` with Slack token colors
  - Sheet header: logo + brand name
  - Nav links stacked vertically; "System" uses `<Accordion>` to reveal Pulse/Relay/Aurora sub-items
  - Bottom: "Sign In" + "Start Journey" buttons
  - Dismiss on Escape, overlay click, close button
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [ ] 14. Preserve existing mobile focus management
  - Focus trapping inside Sheet (Tab/Shift+Tab cycling)
  - Focus returns to hamburger on close
  - Escape key handling
  - ARIA labels on hamburger, Sheet dialog, nav items
  - _Requirements: 7.5, 7.6_

- [ ] 15. Checkpoint — NavigationHeader complete
  - Manual visual check: pill renders centered with correct colors in light + dark mode
  - All 6 nav items scroll to correct sections
  - System mega menu opens and shows 3 module cards
  - Sheet opens/closes correctly on mobile
  - Sign In / Start Journey navigate correctly
  - Theme toggle works
  - Build passes

### Phase 4: Testing

- [ ] 16. Write unit tests for NavigationHeader
  - Renders all 6 nav items
  - Click handler calls `scrollIntoView` with correct id
  - Active section updates on simulated scroll events
  - Sheet opens/closes on hamburger click and Escape
  - _Requirements: 7.1, 7.5, 7.6_

- [ ] 17. Write integration tests for mega menu and mobile Sheet
  - Desktop: System mega menu renders 3 module cards with correct content
  - Mobile: Sheet contains all nav items and auth buttons
  - Slack token colors present in rendered output
  - _Requirements: 3.2, 5.3_

- [ ] 18. Accessibility audit
  - Keyboard navigation through all nav items
  - Focus trapping in Sheet
  - ARIA labels on interactive elements
  - No focus loss on close
  - _Requirements: 7.6_

- [ ] 19. Final checkpoint
  - Run `npm run build` — zero errors
  - All tests pass
  - Manual review: light + dark mode, all viewport sizes, all nav interactions
  - Confirm existing chat nav (sidebar + bottom tabs) unaffected
