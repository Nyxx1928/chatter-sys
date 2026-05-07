# Design Document: Mobile-First Chat Redesign

## Overview

This document describes the technical design for the mobile-first responsive redesign of the Chatter frontend. The goal is to make the chat application fully usable on viewports as narrow as 320 px, wire up the four bottom-tab-bar navigation items to dedicated pages, and fix layout issues in the existing chat sub-components.

The project is a **Next.js 16 / React 19** application using **Tailwind CSS v4** for styling and **Zustand** for client-side state. All new pages live under `frontend/app/chat/` and share the existing `ChatLayout` wrapper at `frontend/app/chat/layout.tsx`.

### Key Design Decisions

1. **No new routing library** — Next.js App Router handles all navigation via `<Link>` and `useRouter`. The four new routes (`/chat/channels`, `/chat/contacts`, `/chat/profile`) are plain Next.js page files.
2. **Reuse existing components** — `RoomSelector`, `FriendsPanel`, `MessageList`, `MessageInput`, and `UserList` are updated in-place rather than replaced. This minimises regression risk.
3. **CSS-only responsive switching** — The bottom tab bar / desktop sidebar visibility toggle is handled entirely with Tailwind responsive prefixes (`md:hidden`, `hidden md:flex`). No JavaScript media-query listeners are needed.
4. **`100dvh` root height** — The chat layout already uses `h-[100dvh]`; this is preserved and extended to all new pages.
5. **Playwright for integration tests** — The project has no unit-test runner (no Vitest/Jest config). All automated tests are Playwright e2e tests. Property-based testing is not applicable here (see Correctness Properties section).

---

## Architecture

```
frontend/app/chat/
├── layout.tsx          ← ChatLayout (updated: nav links wired to real routes)
├── page.tsx            ← Chats page (updated: mobile room→chat flow)
├── channels/
│   └── page.tsx        ← NEW: Channels page
├── contacts/
│   └── page.tsx        ← NEW: Contacts page
├── profile/
│   └── page.tsx        ← NEW: Profile page
└── [roomId]/
    └── page.tsx        ← Existing redirect (unchanged)

frontend/components/chat/
├── RoomSelector.tsx    ← Updated: mobile-safe widths, touch targets
├── MessageList.tsx     ← Updated: 85% max-width on mobile, break-words, smart scroll
├── MessageInput.tsx    ← Updated: hide non-essential icons on narrow viewports
└── UserList.tsx        ← No changes needed (already uses min-w-0, truncate)
```

### Data Flow

```
ChatLayout (auth guard, STOMP connection, nav chrome)
  └── {children}  (one of the four pages below)
        ├── /chat          → ChatRoomsPage  (room list + inline chat)
        ├── /chat/channels → ChannelsPage   (room browser + create)
        ├── /chat/contacts → ContactsPage   (FriendsPanel full-screen)
        └── /chat/profile  → ProfilePage    (user info + logout)
```

The `ChatLayout` already establishes the STOMP WebSocket connection and handles the auth redirect. All child pages inherit this context. No new global state is required; the existing `authStore`, `connectionStore`, `presenceStore`, and `uiStore` are sufficient.

---

## Components and Interfaces

### 1. ChatLayout (`frontend/app/chat/layout.tsx`) — Updated

**Changes:**
- Bottom tab bar: replace `MobileTabButton` (toggle) for Channels and Contacts with `MobileTab` (`<Link>`) pointing to `/chat/channels` and `/chat/contacts` respectively.
- Bottom tab bar: replace `MobileTabButton` for Profile with `MobileTab` pointing to `/chat/profile`.
- Desktop sidebar: add `NavIcon` links for Channels (`/chat/channels`), Contacts (`/chat/contacts`), and Profile (`/chat/profile`). Remove the non-navigating Search, Messages, and Files icons (or repurpose them).
- Active state: use `usePathname()` to derive active state for all four nav items using `pathname.startsWith(route)` logic.
- Touch targets: ensure each `MobileTab` has `min-h-[44px]` and `min-w-[44px]` via padding.
- `aria-current="page"` on the active tab.

**Active-state logic:**
```ts
const isChatsActive    = pathname === '/chat';
const isChannelsActive = pathname.startsWith('/chat/channels');
const isContactsActive = pathname.startsWith('/chat/contacts');
const isProfileActive  = pathname.startsWith('/chat/profile');
```

### 2. ChatRoomsPage (`frontend/app/chat/page.tsx`) — Updated

**Changes (Requirement 10):**
- Mobile: when no room is selected, show `RoomSelector` full-screen (`flex w-full`); hide chat area.
- Mobile: when a room is selected, show chat area full-screen; hide `RoomSelector`.
- The back button in the chat header already exists (`lg:hidden`); change to `md:hidden` to match the 768 px breakpoint.
- Desktop (`md+`): show both panels side by side (existing behaviour).
- `UserList` sidebar: show only at `xl` breakpoint (existing behaviour, no change needed).
- Remove the `FriendsPanel` overlay from this page — it now lives at `/chat/contacts`.

**Mobile panel switching:**
```tsx
// Room list panel
className={`flex flex-col bg-[#16162a] border-r border-white/5
  ${mobileShowChat ? 'hidden md:flex' : 'flex w-full'}
  md:w-80 xl:w-72 md:shrink-0`}

// Chat column
className={`flex-1 flex flex-col min-w-0 bg-[#13131f]
  ${mobileShowChat ? 'flex' : 'hidden md:flex'}`}
```

### 3. ChannelsPage (`frontend/app/chat/channels/page.tsx`) — New

A focused room-browser page. Reuses the room-list logic from `ChatRoomsPage` but without the inline chat panel.

```tsx
'use client';
// Props: none (reads from authStore)
// State: rooms[], loading, error, searchQuery, showCreateModal
// On room select: router.push('/chat') — the Chats page handles room pre-selection
//   via URL search param: router.push(`/chat?room=${room.id}`)
```

**Layout:**
```
┌─────────────────────────────┐
│  Header: "Channels"  [+]    │
│  Search input               │
├─────────────────────────────┤
│  RoomSelector (full height) │
│  (scrollable)               │
└─────────────────────────────┘
```

**Room selection navigation:** When a user taps a room, navigate to `/chat?room={id}`. The `ChatRoomsPage` reads the `room` search param on mount and auto-selects that room.

### 4. ContactsPage (`frontend/app/chat/contacts/page.tsx`) — New

Renders `FriendsPanel` as the primary full-screen content. No modal overlay needed.

```tsx
'use client';
// Layout: full-height scrollable container with FriendsPanel inside
```

**Layout:**
```
┌─────────────────────────────┐
│  Header: "Contacts"         │
├─────────────────────────────┤
│  FriendsPanel               │
│  (scrollable, full height)  │
└─────────────────────────────┘
```

### 5. ProfilePage (`frontend/app/chat/profile/page.tsx`) — New

Displays user info and a logout button.

```tsx
'use client';
// Reads: useAuthStore() → user, logout
// On logout: logout() then router.push('/')
```

**Layout:**
```
┌─────────────────────────────┐
│  Avatar (initial, large)    │
│  Display name               │
│  @username                  │
│                             │
│  [Log Out]                  │
└─────────────────────────────┘
```

### 6. RoomSelector — Updated

**Changes (Requirement 7):**
- Remove any fixed pixel widths; use `w-full` on the root container.
- Room name `<h3>`: already has `truncate`; add `min-w-0` to the flex parent.
- Room item button: ensure `min-h-[44px]` (currently `py-2.5` on an `h-11` avatar row — already ≥44 px; verify and add explicit `min-h-[44px]` if needed).
- List container: already uses `overflow-y-auto`; no change needed.

### 7. MessageList — Updated

**Changes (Requirement 8):**
- Message bubble `max-w`: change from `max-w-[70%]` to `max-w-[85%] md:max-w-[70%]` (mobile gets 85%, desktop keeps 70%).
- Bubble text: add `break-words` class to the `<p>` inside the bubble (already has `whitespace-pre-wrap`; add `overflow-wrap-anywhere` via `[overflow-wrap:anywhere]` Tailwind arbitrary value or `break-words`).
- Container: already uses `h-full overflow-y-auto`; no change needed.
- Auto-scroll: update `useEffect` to check scroll position before scrolling — only auto-scroll if user is within 100 px of the bottom.

**Smart auto-scroll logic:**
```ts
useEffect(() => {
  const container = containerRef.current;
  if (!container) return;
  const isNearBottom =
    container.scrollHeight - container.scrollTop - container.clientHeight < 100;
  if (messages.length > prevMessageCountRef.current && isNearBottom) {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }
  prevMessageCountRef.current = messages.length;
}, [messages]);
```

### 8. MessageInput — Updated

**Changes (Requirement 9):**
- Hide Attach, GIF, Format icon buttons on viewports narrower than 480 px: wrap each in `<span className="hidden xs:inline-flex">` (or use `sm:` if 480 px maps to `sm`). Since Tailwind's default `sm` is 640 px, add a custom `xs: '480px'` breakpoint to `tailwind.config.ts`, or use an inline media query via `[@media(max-width:479px)]:hidden`.
- Send button: already has `shrink-0`; no change needed.
- Container: already uses `w-full`; no change needed.
- Textarea `min-h`: change `min-h-[24px]` to `min-h-[44px]` to meet touch target requirement.

**Icon visibility classes:**
```tsx
// Attach, GIF, Format — hidden below 480px
<span className="[@media(max-width:479px)]:hidden contents">
  <InputIconBtn label="Attach file" ...>...</InputIconBtn>
</span>
```

---

## Data Models

No new data models are introduced. The redesign reuses existing types from `frontend/types/domain.ts`:

- `ChatRoom` — used by ChannelsPage and ChatRoomsPage
- `User` — used by ProfilePage (via `authStore.user`)
- `Message` — used by MessageList
- `FriendRequestList`, `PublicUser` — used by FriendsPanel / ContactsPage

### URL Search Param: Room Pre-selection

The `ChannelsPage` navigates to `/chat?room={id}` when a room is selected. The `ChatRoomsPage` reads this on mount:

```ts
// In ChatRoomsPage, after rooms are loaded:
const searchParams = useSearchParams();
const preselectedRoomId = searchParams.get('room');
useEffect(() => {
  if (preselectedRoomId && rooms.length > 0) {
    const room = rooms.find((r) => r.id === Number(preselectedRoomId));
    if (room) void handleRoomSelect(room);
  }
}, [preselectedRoomId, rooms]);
```

---

## Correctness Properties

This feature is a **UI/layout redesign** involving:
- Responsive CSS class changes (Tailwind breakpoints)
- New Next.js page files (thin wrappers around existing components)
- Navigation wiring (`<Link>` href updates)
- Minor component prop/class adjustments

Property-based testing is **not applicable** here because:
1. There are no pure functions with meaningful input/output variation to test.
2. The correctness of responsive layout cannot be verified by running 100 iterations of a function — it requires visual/DOM inspection at specific viewport widths.
3. Navigation correctness (does clicking a tab go to the right URL?) is a deterministic, single-example assertion, not a universal property.
4. The project has no unit-test runner (no Vitest/Jest); the only automated test infrastructure is Playwright e2e.

The appropriate testing strategy is **Playwright snapshot and interaction tests** at defined viewport sizes, as described in the Testing Strategy section.

---

## Error Handling

### Authentication Guard
All new pages (`/chat/channels`, `/chat/contacts`, `/chat/profile`) are children of `ChatLayout`, which already redirects unauthenticated users to `/auth/login`. No additional auth guard is needed in the page files.

### Network Errors on ChannelsPage
- Room list fetch failure: display an inline error message with a "Try Again" button (same pattern as `ChatRoomsPage`).
- Room creation failure: surface the error inside `RoomCreateModal` (existing behaviour).

### Navigation Edge Cases
- If `/chat?room={id}` is loaded but the room ID does not exist in the fetched list (deleted room, permission change), silently ignore the pre-selection and show the empty-selection state.
- If the user navigates directly to `/chat/channels` while unauthenticated, `ChatLayout` handles the redirect before the page renders.

### Safe Area / Viewport
- `env(safe-area-inset-bottom)` is already applied to the bottom tab bar in `ChatLayout`. If the browser does not support CSS environment variables, the padding falls back to `0` gracefully.
- `100dvh` falls back to `100vh` in browsers that do not support dynamic viewport units; this is acceptable.

---

## Testing Strategy

Since the project uses **Playwright** for all automated testing (no unit-test runner), the testing strategy is entirely e2e-based.

### Existing Test Infrastructure
- `frontend/tests/e2e/chat-layout.spec.ts` — existing layout tests
- `frontend/tests/e2e/auth.spec.ts` — existing auth tests
- Playwright is configured with desktop and mobile viewports (see `playwright.config.ts`)

### New Tests to Add

**File: `frontend/tests/e2e/mobile-nav.spec.ts`**

1. **Bottom tab bar visibility**
   - At 375 px width: bottom tab bar is visible, desktop sidebar is hidden.
   - At 1024 px width: desktop sidebar is visible, bottom tab bar is hidden.

2. **Tab navigation**
   - Tapping "Channels" tab navigates to `/chat/channels`.
   - Tapping "Contacts" tab navigates to `/chat/contacts`.
   - Tapping "Profile" tab navigates to `/chat/profile`.
   - Tapping "Chats" tab navigates to `/chat`.

3. **Active tab highlighting**
   - When on `/chat/channels`, the Channels tab has `aria-current="page"`.
   - When on `/chat/contacts`, the Contacts tab has `aria-current="page"`.

4. **Mobile room-to-chat flow**
   - At 375 px: room list is visible, chat area is hidden.
   - After selecting a room: chat area is visible, room list is hidden.
   - Tapping back button: room list is visible again.

5. **Desktop sidebar navigation**
   - At 1024 px: clicking Channels icon navigates to `/chat/channels`.
   - At 1024 px: clicking Contacts icon navigates to `/chat/contacts`.
   - At 1024 px: clicking Profile icon navigates to `/chat/profile`.

6. **New pages render without errors**
   - `/chat/channels` renders the room list.
   - `/chat/contacts` renders the FriendsPanel.
   - `/chat/profile` renders the user's display name and a Log Out button.

7. **Profile logout**
   - Clicking "Log Out" on the Profile page redirects to `/`.

### Component-Level Checks (within Playwright)

- **MessageInput**: at 375 px viewport, Attach/GIF/Format buttons are not visible; Emoji and Send are visible.
- **MessageList**: message bubbles do not overflow the viewport horizontally at 320 px.
- **RoomSelector**: room items are at least 44 px tall (measured via `getBoundingClientRect`).

### Accessibility Checks
- Each new page is checked with Playwright's `page.accessibility.snapshot()` to verify ARIA roles and labels are present.
- Active tab has `aria-current="page"`.
- Focus order is verified by tabbing through the bottom tab bar.
