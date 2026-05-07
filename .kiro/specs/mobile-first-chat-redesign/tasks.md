# Implementation Plan: Mobile-First Chat Redesign

## Overview

This plan breaks the mobile-first chat redesign into focused, incremental tasks. The work is grouped into six phases: layout foundation, navigation wiring, new pages, component responsive fixes, chat-page mobile flow, and testing/polish.

All changes are in `frontend/`. The project uses Next.js 16, React 19, Tailwind CSS v4, and Playwright for e2e tests.

## Tasks

- [x] 1. Update ChatLayout navigation wiring
  - [x] 1.1 Wire bottom tab bar to real routes
    - In `frontend/app/chat/layout.tsx`, replace the `MobileTabButton` for Channels with a `MobileTab` (`<Link>`) pointing to `/chat/channels`
    - Replace the `MobileTabButton` for Contacts with a `MobileTab` (`<Link>`) pointing to `/chat/contacts`
    - Replace the `MobileTabButton` for Profile with a `MobileTab` (`<Link>`) pointing to `/chat/profile`
    - Update active-state logic: derive `isChannelsActive`, `isContactsActive`, `isProfileActive` from `usePathname()` using `pathname.startsWith(...)` checks
    - Add `aria-current="page"` to the active tab link
    - Ensure each tab has `min-h-[44px]` padding to meet touch-target requirement
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 12.2_

  - [x] 1.2 Wire desktop sidebar to real routes
    - Replace the non-navigating Search, Messages, Files `NavIcon` entries with proper icons for Channels (`/chat/channels`), Contacts (`/chat/contacts`), and Profile (`/chat/profile`)
    - Keep the Home icon pointing to `/chat`
    - Update active-state logic for all four sidebar icons using `pathname.startsWith(...)` checks
    - Ensure each icon uses `<Link>` (not `<button>`) for keyboard and browser-history support
    - Remove the `NavButton` for People (FriendsPanel toggle) — Contacts is now a dedicated page
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [x] 1.3 Verify layout breakpoints and safe-area padding
    - Confirm the bottom tab bar has `md:hidden` and the desktop sidebar has `hidden md:flex`
    - Confirm `style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}` is on the bottom tab bar
    - Confirm the root container uses `h-[100dvh]` and `overflow-hidden`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [x] 2. Create Channels page
  - [x] 2.1 Create the Channels page file
    - Create `frontend/app/chat/channels/page.tsx`
    - Mark as `'use client'`
    - Import `useAuthStore`, `useRouter`, `listRooms`, `RoomSelector`, `RoomCreateModal`, `Button`, `Input`
    - Add state: `rooms`, `loading`, `error`, `searchQuery`, `showCreateModal`, `createError`, `isCreating`
    - Fetch rooms on mount using the same pattern as `ChatRoomsPage`
    - _Requirements: 4.1, 4.2_

  - [x] 2.2 Implement room selection navigation
    - On room select, call `router.push('/chat?room=' + room.id)` so the Chats page auto-selects the room
    - _Requirements: 4.3_

  - [x] 2.3 Add Create Room action
    - Render a `+` / "Create Room" button in the page header that opens `RoomCreateModal`
    - On successful creation, navigate to `/chat?room={newRoom.id}`
    - _Requirements: 4.4_

  - [x] 2.4 Apply mobile-safe layout
    - Root container: `flex flex-col h-full bg-[#13131f] min-w-0`
    - Header: `px-4 py-4 border-b border-white/5 shrink-0`
    - `RoomSelector` fills remaining height: `flex-1 overflow-hidden`
    - No fixed pixel widths anywhere
    - _Requirements: 4.5_

- [x] 3. Create Contacts page
  - [x] 3.1 Create the Contacts page file
    - Create `frontend/app/chat/contacts/page.tsx`
    - Mark as `'use client'`
    - Import `FriendsPanel`
    - Render a page header ("Contacts") and `FriendsPanel` as the primary full-height content
    - Root container: `flex flex-col h-full bg-[#13131f] min-w-0`
    - `FriendsPanel` wrapper: `flex-1 overflow-y-auto px-4 py-4`
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 4. Create Profile page
  - [x] 4.1 Create the Profile page file
    - Create `frontend/app/chat/profile/page.tsx`
    - Mark as `'use client'`
    - Import `useAuthStore`, `useRouter`
    - Read `user` from `useAuthStore()`
    - Display: large avatar circle with initial, display name, `@username`
    - Render a "Log Out" button that calls `logout()` then `router.push('/')`
    - Root container: `flex flex-col h-full bg-[#13131f] min-w-0`
    - Center content vertically and horizontally on mobile; left-align on `md+`
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 5. Update ChatRoomsPage for mobile room-to-chat flow
  - [x] 5.1 Fix mobile panel switching breakpoint
    - Change `lg:flex` / `hidden lg:flex` to `md:flex` / `hidden md:flex` on the room-list panel and chat column so the split happens at 768 px (matching the design spec)
    - Change the back button from `lg:hidden` to `md:hidden`
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 5.2 Add room pre-selection from URL search param
    - Import `useSearchParams` from `next/navigation`
    - After rooms are loaded, read `searchParams.get('room')` and auto-select the matching room via `handleRoomSelect`
    - Wrap in a `useEffect` that depends on `[preselectedRoomId, rooms]`
    - _Requirements: 4.3_

  - [x] 5.3 Remove FriendsPanel overlay from ChatRoomsPage
    - Remove the mobile FriendsPanel overlay (`showFriendsPanel && md:hidden` block) from `ChatRoomsPage` — it is now handled by the dedicated Contacts page
    - Remove the desktop FriendsPanel aside panels from `ChatRoomsPage` — the Contacts page handles this
    - Remove `showFriendsPanel` / `toggleFriendsPanel` usage from `ChatRoomsPage`
    - _Requirements: 5.4_

  - [x] 5.4 Hide UserList sidebar below xl breakpoint
    - Confirm `UserList` aside uses `hidden xl:flex` (already present; verify no regression)
    - _Requirements: 10.5_

- [x] 6. Fix RoomSelector for mobile
  - [x] 6.1 Remove fixed widths and ensure full-width rendering
    - Audit `RoomSelector.tsx` for any fixed pixel widths; replace with `w-full` or `min-w-0`
    - Add `min-w-0` to the flex parent of the room name `<h3>` if not already present
    - _Requirements: 7.1, 7.2_

  - [x] 6.2 Ensure touch targets meet 44 px minimum
    - Add `min-h-[44px]` to the room item `<button>` element
    - _Requirements: 7.3, 11.1, 11.2_

- [x] 7. Fix MessageList for mobile
  - [x] 7.1 Update message bubble max-width for mobile
    - Change `max-w-[70%]` to `max-w-[85%] md:max-w-[70%]` on the bubble wrapper `<div>`
    - _Requirements: 8.1_

  - [x] 7.2 Add word-break to bubble text
    - Add `break-words` (and `[overflow-wrap:anywhere]` via Tailwind arbitrary value) to the `<p>` inside each message bubble
    - _Requirements: 8.2_

  - [x] 7.3 Implement smart auto-scroll (near-bottom only)
    - Update the `useEffect` in `MessageList.tsx` to check `container.scrollHeight - container.scrollTop - container.clientHeight < 100` before calling `scrollIntoView`
    - Only auto-scroll when the user is within 100 px of the bottom
    - _Requirements: 8.4_

- [x] 8. Fix MessageInput for mobile
  - [x] 8.1 Hide non-essential icon buttons below 480 px
    - Wrap the Attach, GIF, and Format `InputIconBtn` elements in a `<span className="[@media(max-width:479px)]:hidden contents">` so they are hidden on narrow viewports
    - Keep Emoji and Send always visible
    - _Requirements: 9.1_

  - [x] 8.2 Update textarea minimum height
    - Change `min-h-[24px]` to `min-h-[44px]` on the `<textarea>` to meet touch-target requirements
    - _Requirements: 9.4, 11.1_

- [x] 9. Accessibility preservation
  - [x] 9.1 Verify ARIA attributes on updated components
    - Confirm `aria-current="page"` is set on the active bottom tab bar item
    - Confirm `aria-label` is present on all new page headers and interactive elements
    - Confirm `aria-label="Main navigation"` is preserved on both nav elements in `ChatLayout`
    - _Requirements: 12.1, 12.2, 12.3_

  - [x] 9.2 Verify focus order on mobile layout
    - Manually verify (or via Playwright keyboard navigation test) that Tab order on mobile is: content area → bottom tab bar items, left to right
    - _Requirements: 12.3_

- [x] 10. Write Playwright e2e tests
  - [x] 10.1 Create mobile navigation test file
    - Create `frontend/tests/e2e/mobile-nav.spec.ts`
    - Add viewport fixtures for mobile (375 × 812) and desktop (1280 × 800)
    - _Requirements: 1.5, 1.6, 2.1_

  - [x] 10.2 Test bottom tab bar visibility and navigation
    - At 375 px: assert bottom tab bar is visible, desktop sidebar is hidden
    - At 1280 px: assert desktop sidebar is visible, bottom tab bar is hidden
    - Click each tab and assert the URL changes to the correct route
    - Assert `aria-current="page"` is on the active tab
    - _Requirements: 1.5, 1.6, 2.2, 2.3, 2.4, 2.5, 2.6, 12.2_

  - [x] 10.3 Test desktop sidebar navigation
    - At 1280 px: click Channels, Contacts, Profile icons and assert URL
    - Assert active icon has the correct active styling class
    - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.6_

  - [x] 10.4 Test mobile room-to-chat flow
    - At 375 px: assert room list is visible and chat area is hidden
    - Select a room: assert chat area is visible and room list is hidden
    - Click back button: assert room list is visible again
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 10.5 Test new pages render correctly
    - Navigate to `/chat/channels`: assert room list is rendered
    - Navigate to `/chat/contacts`: assert FriendsPanel content is rendered
    - Navigate to `/chat/profile`: assert display name and Log Out button are visible
    - _Requirements: 4.1, 4.2, 5.1, 5.2, 6.1, 6.2_

  - [x] 10.6 Test Profile page logout
    - Navigate to `/chat/profile`, click Log Out, assert redirect to `/`
    - _Requirements: 6.3_

  - [x] 10.7 Test no horizontal overflow at 320 px
    - Load `/chat` at 320 px viewport width
    - Assert `document.documentElement.scrollWidth <= document.documentElement.clientWidth`
    - _Requirements: 1.1_

  - [x] 10.8 Test MessageInput icon visibility on mobile
    - At 375 px: assert Attach, GIF, Format buttons are not visible; Send button is visible
    - _Requirements: 9.1_

## Notes

- All new pages are children of `ChatLayout` and inherit its auth guard — no additional auth logic is needed in page files.
- The `uiStore.showFriendsPanel` toggle is no longer needed in the main chat flow once Contacts is a dedicated page; the store can be left in place for now and cleaned up in a follow-up.
- Tailwind CSS v4 is in use; arbitrary value syntax `[@media(...)]` is supported.
- The project has no unit-test runner (no Vitest/Jest config). All automated tests are Playwright e2e.
- Property-based testing is not applicable to this feature (UI/layout/navigation changes with no pure functions to test).
