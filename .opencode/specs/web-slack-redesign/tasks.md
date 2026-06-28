# Implementation Plan: Cross-Platform Slack-Inspired Design Implementation

## Overview

Six phases covering token infrastructure, UI base components, auth screens, chat interface, landing/demo pages, and final cross-platform validation. Each phase ends with a visual checkpoint to verify correctness before proceeding.

## Tasks

### Phase 1: Token Infrastructure

- [x] 1. **Add Slack dark mode CSS custom properties to `globals.css`**
  - Define `:root` with all light-mode Slack token values
  - Add `@media (prefers-color-scheme: dark)` block with dark-mode values
  - Ensure every token from `design.md` has both light and dark definitions
  - _Requirements: 1.1, 1.2, 1.5, 5.1, 5.2_

- [x] 2. **Update `tailwind.config.ts` to use CSS variables**
  - Replace all hardcoded hex values in `slack.*` namespace with `var(--slack-*)`
  - Update `fontFamily` to reference `var(--font-noto-sans)` / `var(--font-noto-sans-display)`
  - Verify `rounded-pill`, spacing, shadows still work
  - _Requirements: 1.3, 1.4_

- [x] 3. **Replace Geist Sans with Noto Sans fonts**
  - Install `next/font/google` imports for `Noto_Sans` and `Noto_Sans_Display`
  - Add CSS variable declarations in root `layout.tsx`
  - Update `globals.css` `@theme` to include `--font-noto-sans` / `--font-noto-sans-display`
  - Remove Geist font loading
  - _Requirements: 1.4_

- [x] 4. **Checkpoint — Token Infrastructure Verified**
  - Run `npx tsc --noEmit` — no new errors
  - Verify all `--slack-*` CSS vars resolve in both light and dark mode
  - Verify Noto Sans fonts render on all pages
  - Ask user if questions arise before proceeding

### Phase 2: UI Base Components

- [x] 5. **Redesign `Button.tsx` with Slack tokens**
  - Primary variant: `bg-slack-primary text-slack-text-inverse rounded-pill`
  - Secondary variant: `bg-transparent text-slack-accent-blue border border-slack-accent-blue rounded-pill`
  - Danger variant: `bg-slack-accent-red text-white rounded-pill`
  - Ghost variant: `bg-transparent text-slack-text-secondary`
  - All get `rounded-pill` and `min-h-[44px]` for consistency
  - _Requirements: 2.2_

- [x] 6. **Redesign `Input.tsx` with Slack tokens**
  - Background: `bg-slack-surface-tertiary`
  - Border: `border-slack-border`
  - Focus ring: `ring-slack-accent-blue`
  - Text: `text-slack-text-primary`
  - Placeholder: `placeholder:text-slack-text-secondary`
  - Error state: `border-slack-accent-red`
  - _Requirements: 2.3, 2.4, 2.6_

- [x] 7. **Redesign `Card.tsx` and `Modal.tsx` with Slack tokens**
  - Card: `bg-slack-surface-primary border border-slack-border`
  - Modal content: `bg-slack-surface-primary rounded-lg`
  - _Requirements: 5.1, 5.3_

- [x] 8. **Checkpoint — UI Components Verified**
  - Manual visual check of Button, Input, Card, Modal variants
  - Verify all color values resolve to Slack hex values
  - Verify dark mode swaps all tokens automatically
  - Ask user if questions arise before proceeding

### Phase 3: Auth Screens

- [x] 9. **Redesign `LoginForm.tsx` and login page**
  - Update page background to cream-lavender gradient in light mode (`from-[#F4EDE4] to-[#FFFFFF]`)
  - Remove purple radial glow overlay
  - Replace all `kiro-*` classes with `slack-*` equivalents
  - Replace all `text-red-*` with `text-slack-accent-red`
  - Replace all `text-emerald-*` with `text-slack-accent-green`
  - Update CTA button to `bg-slack-primary rounded-pill`
  - Update link styles to `text-slack-accent-blue`
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 10. **Redesign `RegisterForm.tsx` and register page**
  - Same gradient background and token replacement as login
  - _Requirements: 2.2, 2.3, 2.4, 2.6_

- [ ] 11. **Redesign `ForgotPasswordForm.tsx`, `ResetPasswordForm.tsx` and their pages**
  - Same token substitution pattern
  - _Requirements: 2.2, 2.3, 2.4_

- [ ] 12. **Redesign verify-email page**
  - Replace success/error colors with Slack accent green / accent red
  - Update buttons to `bg-slack-primary rounded-pill`
  - _Requirements: 2.2, 2.5_

- [ ] 13. **Checkpoint — Auth Flow Verified**
  - Walk through: login → register → verify → login → redirect
  - Verify every button, input, and link still works
  - Verify light and dark mode on every auth page
  - Compare visually with mobile auth screens for parity
  - Ask user if questions arise before proceeding

### Phase 4: Chat Interface

- [ ] 14. **Redesign `chat/layout.tsx` navigation**
  - Desktop sidebar: `bg-slack-surface-secondary`
  - Active nav icon: `text-slack-primary`
  - Inactive nav icon: `text-slack-text-secondary`
  - Active indicator bar: `bg-slack-primary`
  - Profile avatar gradient → `bg-slack-primary`
  - Connection status dots → Slack accent colors
  - Error banner → `bg-slack-accent-red/20 text-slack-accent-red`
  - Mobile bottom tab: same token system
  - _Requirements: 3.5_

- [ ] 15. **Redesign DM chat page and `RoomSelector.tsx`**
  - Container: `bg-slack-surface-secondary`
  - Room items: `border-b border-slack-border`, padding using Slack spacing scale
  - Avatars: `bg-slack-primary` with `text-slack-text-inverse` initial
  - Room name: `text-slack-text-primary` with Slack type scale
  - Latest message preview: `text-slack-text-secondary` with Slack type scale
  - Search input: pill-shaped with `bg-slack-surface-tertiary`
  - _Requirements: 3.1_

- [ ] 16. **Redesign `MessageList.tsx`**
  - Own bubble: `bg-slack-primary text-slack-text-inverse rounded-lg rounded-br-sm`
  - Other bubble: `bg-slack-surface-tertiary text-slack-text-primary rounded-lg rounded-bl-sm`
  - Timestamps: `text-slack-text-secondary text-caption`
  - Date separators: `text-slack-text-secondary bg-slack-surface-tertiary rounded-pill`
  - System messages (JOIN/LEAVE): italic `text-slack-text-secondary`
  - _Requirements: 3.2, 3.3_

- [ ] 17. **Redesign `MessageInput.tsx`**
  - Container: `bg-slack-surface-primary border-t border-slack-border`
  - Input field: `bg-slack-surface-tertiary rounded-pill`
  - Send button: `bg-slack-primary rounded-full`
  - Icon buttons: `text-slack-text-secondary`
  - _Requirements: 3.2, 3.3_

- [ ] 18. **Redesign channels page, contacts page, profile page**
  - Apply same patterns: Slack surface backgrounds, Slack text colors, Slack typography
  - `UserList.tsx`, `FriendsPanel.tsx`, `UserSearch.tsx`, `RoomCreateModal.tsx`
  - Friend status badges: accepted → `text-slack-accent-green`, pending → `text-slack-accent-yellow`
  - Logout/delete buttons → `bg-slack-accent-red rounded-pill`
  - _Requirements: 3.4, 3.5_

- [ ] 19. **Checkpoint — Chat Interface Verified**
  - Walk through: browse DMs → open room → send/receive messages
  - Walk through: browse channels → create channel → join → message
  - Walk through: contacts → search → add → accept → DM opens
  - Walk through: profile → view → logout
  - Verify light and dark mode on every chat page
  - Compare visually with mobile chat screens
  - Ask user if questions arise before proceeding

### Phase 5: Landing Page & Demo Pages

- [ ] 20. **Redesign `NavigationHeader.tsx`**
  - Background: `bg-slack-surface-primary` with backdrop blur
  - CTA buttons: `bg-slack-primary rounded-pill` / ghost variant
  - Nav links: `text-slack-text-primary`
  - Mobile hamburger menu: Slack surface colors
  - _Requirements: 4.1_

- [ ] 21. **Redesign `HeroSection.tsx`**
  - Remove purple gradient from heading text
  - Stat cards: `bg-slack-surface-secondary` with Slack border
  - CTA buttons: `bg-slack-primary rounded-pill`
  - Secondary text: `text-slack-text-secondary`
  - _Requirements: 4.1_

- [ ] 22. **Redesign `FeaturesSection.tsx` and `FooterSection.tsx`**
  - Section backgrounds: `bg-slack-surface-secondary`
  - Feature cards: `bg-slack-surface-primary border border-slack-border`
  - Footer: `bg-slack-surface-secondary` with `text-slack-text-secondary`
  - _Requirements: 4.2_

- [ ] 23. **Redesign demo pages** (auth-demo, chat-demo, ui-demo)
  - Update background, card, button, input classes to Slack tokens
  - _Requirements: 5.1, 5.2_

- [ ] 24. **Checkpoint — Landing & Demo Verified**
  - Walk through landing page — all sections render correctly in light and dark mode
  - Walk through all demo pages — visual consistency with main app
  - Ask user if questions arise before proceeding

### Phase 6: Final Validation

- [ ] 25. **Token audit — verify cross-platform parity**
  - Compare every hex value in web `globals.css` against mobile `constants/Colors.ts`
  - Compare typography values (font size, weight, line height) across platforms
  - Compare spacing, radius, shadow values
  - All values MUST be identical
  - _Requirements: 1.1, 1.2, 1.3, 1.5_

- [ ] 26. **Hardcoded color sweep — grep for remaining old tokens**
  - Run: `rg -n 'kiro-\|#[0-9a-fA-F]\{6\}\|rgba(\|rgb(' components/ app/ --include='*.tsx' --include='*.ts'`
  - Zero matches allowed (excluding design docs, README, etc.)
  - _Requirements: 5.3_

- [ ] 27. **Preservation check — verify all functionality works**
  - Walk through every critical path: register → verify → login → DM → channel → contacts → profile → logout
  - Verify every button, input, link, and navigation target works identically to before
  - _Requirements: 6.1, 6.2, 6.3_

- [ ] 28. **Checkpoint — Redesign Complete**
  - All web screens use Slack tokens in both light and dark mode
  - Cross-platform token values are identical between web and mobile
  - All pre-existing functionality is preserved
  - Run `npx tsc --noEmit` — confirm pre-existing errors only
  - Run `npm run build` — confirm no build errors
  - Ask user if questions arise before writing
