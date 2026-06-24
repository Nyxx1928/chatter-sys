# Implementation Plan: Slack-Inspired Visual Redesign

## Overview

Implementation is organized into four phases: (1) design token installation and platform mapping, (2) authentication and navigation shell, (3) main chat interface and remaining screens, (4) cross-platform verification and dark mode. Each phase has a visual checkpoint.

## Tasks

### Phase 1: Design Token Foundation

- [ ] 1. **Install Slack DESIGN.md and create platform token files**
  - Run `npx getdesign@latest add slack` from project root
  - Read the installed `.opencode/design.md` and extract all token values
  - Create `expo-chat-app/constants/Colors.ts` with Slack's full palette (light + dark)
  - Create `expo-chat-app/constants/Typography.ts` with Slack's type scale
  - Create `expo-chat-app/constants/Spacing.ts` with Slack's spacing scale
  - Create `expo-chat-app/constants/Shadows.ts` with Slack's shadow tokens (if defined)
  - Create `expo-chat-app/constants/BorderRadius.ts` with Slack's radius tokens
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 2. **Map Slack tokens to Tailwind config (web frontend)**
  - Update `frontend/tailwind.config.js` to include Slack's color palette
  - Update fontFamily to use `'Noto Sans Display'` / `'Noto Sans'`
  - Update spacing, borderRadius, and boxShadow to match Slack's scale
  - _Requirements: 5.1, 5.2, 5.3_

- [ ] 3. **Add Noto Sans fonts to mobile app**
  - Install `@expo-google-fonts/noto-sans` and `@expo-google-fonts/noto-sans-display`
  - Configure font loading in `app/_layout.tsx` using `useFonts`
  - Verify fonts render correctly on both iOS and Android
  - _Requirements: 1.5_

- [ ] 4. **Checkpoint — Token Foundation Verified**
  - Every Slack token from DESIGN.md has a corresponding constant in mobile files
  - Tailwind config references Slack tokens, not hardcoded values
  - Fonts load without errors on mobile
  - Ask user if questions arise before proceeding

### Phase 2: Authentication Screens and Navigation Shell

- [ ] 5. **Redesign login screen**
  - **File**: `expo-chat-app/app/(auth)/login.tsx`
  - Replace blue `#2f95dc` button with aubergine (`#4A154B`) pill-shaped CTA
  - Update background to Slack's cream-lavender gradient (`#F4EDE4` → `#FFFFFF`)
  - Apply Slack typography scale to all text (title → `--type-display-xl`, body → `--type-body-md`)
  - Apply Slack spacing scale (`--space-xl` between elements)
  - Update link colors to Slack accent blue (`#36C5F0`)
  - Update error text to Slack accent red (`#E01E5A`)
  - Add back button using Slack's styling
  - _Requirements: 2.1, 2.4_

- [ ] 6. **Redesign register screen**
  - **File**: `expo-chat-app/app/(auth)/register.tsx`
  - Apply same token system as login screen
  - Ensure form spacing matches Slack density
  - _Requirements: 2.2_

- [ ] 7. **Redesign forgot-password and reset-password screens**
  - **Files**: `expo-chat-app/app/(auth)/forgot-password.tsx`, `reset-password.tsx`
  - Apply Slack tokens to match auth flow consistency
  - _Requirements: 2.3, 2.4_

- [ ] 8. **Redesign verify-email screen**
  - **File**: `expo-chat-app/app/(auth)/verify-email.tsx`
  - Apply Slack tokens
  - _Requirements: 2.3_

- [ ] 9. **Redesign auth layout shell**
  - **File**: `expo-chat-app/app/(auth)/_layout.tsx`
  - Add Slack-styled back navigation
  - Apply Slack surface colors to layout background
  - _Requirements: 2.1, 4.4_

- [ ] 10. **Checkpoint — Auth Flow Complete**
  - All 5 auth screens use Slack tokens
  - Auth flow works end-to-end: register → verify → login → redirect
  - Light and dark mode both render correctly on auth screens
  - Ask user if questions arise before proceeding

### Phase 3: Main Chat Interface and Internal Screens

- [ ] 11. **Redesign main tab bar**
  - **File**: `expo-chat-app/app/(tabs)/_layout.tsx`
  - Update active tab color to `--slack-primary`
  - Update inactive tab color to `--slack-text-secondary`
  - Update badge styling to Slack accent red pill
  - Update tab bar background to `--slack-surface-primary`
  - _Requirements: 4.1_

- [ ] 12. **Redesign chats list screen**
  - **File**: `expo-chat-app/app/(tabs)/chats/index.tsx`
  - Update container background to `--slack-surface-secondary`
  - Update search input to pill-shape with `--slack-surface-tertiary` background
  - Update room list items with `--slack-surface-primary` and `--slack-border` dividers
  - Apply Slack typography to room names, previews, and timestamps
  - Update online indicators to Slack accent green
  - Update empty state text to `--slack-text-secondary`
  - _Requirements: 3.1_

- [ ] 13. **Redesign chat room detail screen**
  - **File**: `expo-chat-app/app/(tabs)/chats/[roomId].tsx`
  - Update message bubbles to Slack rounded style with sender color coding
  - Update message input to pill-shaped with `--radius-pill`
  - Update timestamps to `--type-caption` / `--slack-text-secondary`
  - Update system messages (JOIN/LEAVE) to italic secondary style
  - Update presence indicators to accent green dot
  - _Requirements: 3.2, 3.3_

- [ ] 14. **Redesign channels list and detail screens**
  - **Files**: `expo-chat-app/app/(tabs)/channels/*.tsx`
  - Apply Slack tokens to match chats list consistency
  - Update channel member display
  - Update create-channel form
  - _Requirements: 3.4, 4.2_

- [ ] 15. **Redesign contacts screens**
  - **Files**: `expo-chat-app/app/(tabs)/contacts/*.tsx`
  - Apply Slack tokens to friend list, requests, and add-friend screens
  - Update relationship status badges (friends → accent green, pending → accent yellow)
  - _Requirements: 3.5_

- [ ] 16. **Redesign profile screen**
  - **File**: `expo-chat-app/app/(tabs)/profile/index.tsx`
  - Apply Slack's settings-like card grouping with surface colors
  - Update user info display with Slack typography
  - Update logout button to slack accent red pill
  - _Requirements: 4.3_

- [ ] 17. **Redesign shared components**
  - **Files**: `expo-chat-app/src/components/*.tsx`
  - Update `RoomListItem` with Slack tokens
  - Update `MessageBubble` with Slack styling
  - Update `MessageInput` with Slack pill styling
  - Update `PresenceDot` with Slack accent colors
  - Update `ConnectionBanner` with Slack accent colors
  - Update `MessageList` with Slack spacing
  - _Requirements: 3.1, 3.2, 3.3_

- [ ] 18. **Checkpoint — Main Interface Complete**
  - All main screens use Slack tokens
  - Chat flow works: browse rooms → open room → send/receive messages
  - Contacts flow works: search → add → accept → DM opens
  - Channels flow works: list → create → join → message
  - Ask user if questions arise before proceeding

### Phase 4: Dark Mode, Cross-Platform Parity, and Final Validation

- [ ] 19. **Verify and polish dark mode on all screens**
  - Walk through every screen in dark mode
  - Verify all tokens have dark-mode values (no light-mode fallbacks visible)
  - Fix any contrast issues or missing dark-mode colors
  - _Requirements: 1.3_

- [ ] 20. **Apply Slack tokens to web frontend** *(if web frontend has visual components)*
  - Update all CSS modules / styled components to use Slack tokens
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 21. **Token audit — verify cross-platform parity**
  - Compare mobile `Colors.ts` vs web Tailwind config for every Slack token
  - Confirm hex values are identical
  - Confirm typography values are identical (with platform-appropriate font loading)
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 22. **Preservation check — verify all functionality still works**
  - Run the app through all critical paths: auth, messaging, contacts, channels
  - Verify every button, input, link, and navigation target from before the redesign still exists and works
  - _Requirements: All_

- [ ] 23. **Checkpoint — Redesign Complete**
  - All screens use Slack tokens in both light and dark mode
  - Cross-platform token values are identical
  - All pre-existing functionality is preserved
  - Ask user if questions arise before writing
