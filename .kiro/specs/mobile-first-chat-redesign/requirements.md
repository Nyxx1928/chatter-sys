# Requirements Document

## Introduction

This feature delivers a mobile-first responsive redesign of the Chatter frontend chat application. The current UI has several mobile usability gaps: the chat layout does not adapt gracefully to small screens, the bottom navigation bar items (Chats, Channels, Contacts, Profile) do not navigate to dedicated pages, and several chat sub-components (RoomSelector, MessageList, MessageInput, FriendsPanel, UserList) have layout issues on narrow viewports. The redesign applies a mobile-first Tailwind CSS approach throughout, creates dedicated pages for each of the four navbar items, and ensures the full chat experience is usable on devices as narrow as 320 px.

## Glossary

- **App**: The Next.js frontend application located in `frontend/`.
- **Chat_Layout**: The layout component at `frontend/app/chat/layout.tsx` that wraps all chat routes and renders the desktop left-nav and mobile bottom tab bar.
- **Chat_Page**: The main chat page at `frontend/app/chat/page.tsx` that renders the room list and inline message view.
- **Bottom_Tab_Bar**: The mobile navigation bar rendered at the bottom of the screen inside Chat_Layout, containing four tab items.
- **Desktop_Sidebar**: The slim icon-only left navigation column rendered on `md` and wider viewports inside Chat_Layout.
- **Navbar_Item**: One of the four navigation destinations: Chats, Channels, Contacts, Profile.
- **RoomSelector**: The component at `frontend/components/chat/RoomSelector.tsx` that lists available chat rooms.
- **MessageList**: The component at `frontend/components/chat/MessageList.tsx` that renders the scrollable message history.
- **MessageInput**: The component at `frontend/components/chat/MessageInput.tsx` that renders the message composition bar.
- **FriendsPanel**: The component at `frontend/components/chat/FriendsPanel.tsx` that renders the friends list, pending requests, and user search.
- **UserList**: The component at `frontend/components/chat/UserList.tsx` that renders room members with presence indicators.
- **Channels_Page**: A new dedicated page at `frontend/app/chat/channels/page.tsx` for the Channels navbar item.
- **Contacts_Page**: A new dedicated page at `frontend/app/chat/contacts/page.tsx` for the Contacts navbar item.
- **Profile_Page**: A new dedicated page at `frontend/app/chat/profile/page.tsx` for the Profile navbar item.
- **Safe_Area**: The device-specific inset area (e.g., iPhone notch/home indicator) that must not be obscured by UI elements.
- **Viewport**: The visible area of the browser window.
- **Touch_Target**: An interactive element that must be large enough to tap reliably on a touchscreen (minimum 44 × 44 CSS px per WCAG 2.5.5).

---

## Requirements

### Requirement 1: Mobile-First Base Layout

**User Story:** As a mobile user, I want the chat application to fill my screen correctly without horizontal scrolling or clipped content, so that I can use the app comfortably on my phone.

#### Acceptance Criteria

1. THE App SHALL render without horizontal overflow on viewports as narrow as 320 px.
2. THE Chat_Layout SHALL use `100dvh` (dynamic viewport height) as the root container height so that the layout accounts for mobile browser chrome (address bar, bottom bar).
3. WHEN the device has a Safe_Area inset, THE Bottom_Tab_Bar SHALL apply `padding-bottom: env(safe-area-inset-bottom)` so that tab labels are not obscured.
4. THE Chat_Layout SHALL prevent the page body from scrolling; only designated scrollable sub-regions (message list, room list, friends list) SHALL scroll independently.
5. WHILE the viewport width is less than 768 px, THE Desktop_Sidebar SHALL be hidden and THE Bottom_Tab_Bar SHALL be visible.
6. WHILE the viewport width is 768 px or greater, THE Bottom_Tab_Bar SHALL be hidden and THE Desktop_Sidebar SHALL be visible.

---

### Requirement 2: Functional Bottom Tab Bar Navigation

**User Story:** As a mobile user, I want each tab in the bottom navigation bar to navigate to its own dedicated page, so that I can access Chats, Channels, Contacts, and my Profile from anywhere in the app.

#### Acceptance Criteria

1. THE Bottom_Tab_Bar SHALL contain exactly four Navbar_Items: Chats, Channels, Contacts, and Profile, in that order.
2. WHEN the user taps the Chats tab, THE App SHALL navigate to `/chat`.
3. WHEN the user taps the Channels tab, THE App SHALL navigate to `/chat/channels`.
4. WHEN the user taps the Contacts tab, THE App SHALL navigate to `/chat/contacts`.
5. WHEN the user taps the Profile tab, THE App SHALL navigate to `/chat/profile`.
6. WHEN the current route matches a Navbar_Item's path, THE Bottom_Tab_Bar SHALL render that tab in the active (highlighted) state using the `kiro-purple-400` color.
7. THE Bottom_Tab_Bar SHALL render each tab as a `<Link>` element (not a `<button>`) so that the browser's native navigation and back-button behavior is preserved.
8. EACH Navbar_Item tab SHALL have a Touch_Target of at least 44 × 44 CSS px.

---

### Requirement 3: Functional Desktop Sidebar Navigation

**User Story:** As a desktop user, I want the left sidebar icons to navigate to dedicated pages, so that I can switch between Chats, Channels, Contacts, and Profile without relying on the mobile tab bar.

#### Acceptance Criteria

1. THE Desktop_Sidebar SHALL contain navigation icons for: Home (Chats), Channels, Contacts (People), and Profile (Settings/User), plus a logout action.
2. WHEN the user clicks the Home icon, THE App SHALL navigate to `/chat`.
3. WHEN the user clicks the Channels icon, THE App SHALL navigate to `/chat/channels`.
4. WHEN the user clicks the Contacts/People icon, THE App SHALL navigate to `/chat/contacts`.
5. WHEN the user clicks the Profile/Settings icon, THE App SHALL navigate to `/chat/profile`.
6. WHEN the current route matches a Desktop_Sidebar icon's path, THE Desktop_Sidebar SHALL render that icon in the active state with the `kiro-purple-600/30` background and `kiro-purple-400` icon color.
7. THE Desktop_Sidebar SHALL render each navigable icon as a `<Link>` element so that keyboard navigation and browser history work correctly.

---

### Requirement 4: Dedicated Channels Page

**User Story:** As a user, I want a dedicated Channels page that shows all available chat rooms in a browsable list, so that I can discover and join rooms from a focused view.

#### Acceptance Criteria

1. THE Channels_Page SHALL be accessible at the route `/chat/channels` and SHALL be protected by the same authentication guard as the rest of the chat section.
2. THE Channels_Page SHALL display a list of all available chat rooms using the existing RoomSelector component or an equivalent room-list UI.
3. WHEN a user selects a room on the Channels_Page, THE App SHALL navigate to `/chat` with that room pre-selected, OR navigate to a room-specific route, so that the user can begin chatting.
4. THE Channels_Page SHALL display a "Create Room" action that opens the room creation modal.
5. WHILE the viewport width is less than 768 px, THE Channels_Page SHALL render in a single-column full-screen layout with no horizontal overflow.

---

### Requirement 5: Dedicated Contacts Page

**User Story:** As a user, I want a dedicated Contacts page that shows my friends list, pending requests, and user search, so that I can manage my social connections from a focused full-screen view.

#### Acceptance Criteria

1. THE Contacts_Page SHALL be accessible at the route `/chat/contacts` and SHALL be protected by the same authentication guard as the rest of the chat section.
2. THE Contacts_Page SHALL render the FriendsPanel component (or equivalent) as its primary content, displaying the friends list, pending requests, and user search.
3. WHILE the viewport width is less than 768 px, THE Contacts_Page SHALL render in a single-column full-screen layout with no horizontal overflow.
4. THE Contacts_Page SHALL NOT require the FriendsPanel to be opened as a modal overlay; it SHALL be the primary content of the page.

---

### Requirement 6: Dedicated Profile Page

**User Story:** As a user, I want a dedicated Profile page where I can view my account information and log out, so that I have a clear place to manage my identity in the app.

#### Acceptance Criteria

1. THE Profile_Page SHALL be accessible at the route `/chat/profile` and SHALL be protected by the same authentication guard as the rest of the chat section.
2. THE Profile_Page SHALL display the authenticated user's display name, username, and avatar initial.
3. THE Profile_Page SHALL provide a "Log Out" button that, WHEN clicked, logs the user out and navigates to `/`.
4. WHILE the viewport width is less than 768 px, THE Profile_Page SHALL render in a single-column full-screen layout with no horizontal overflow.

---

### Requirement 7: Responsive RoomSelector Component

**User Story:** As a mobile user, I want the room list sidebar to be fully usable on a small screen, so that I can browse and select rooms without layout issues.

#### Acceptance Criteria

1. WHILE the viewport width is less than 768 px, THE RoomSelector SHALL render as a full-width, full-height panel with no fixed pixel widths that cause overflow.
2. THE RoomSelector SHALL use `min-w-0` and `truncate` on text elements so that long room names do not overflow their containers.
3. EACH room item in THE RoomSelector SHALL have a Touch_Target of at least 44 × 44 CSS px.
4. THE RoomSelector SHALL remain scrollable when the room list exceeds the available viewport height, using `overflow-y-auto` on the list container.

---

### Requirement 8: Responsive MessageList Component

**User Story:** As a mobile user, I want the message list to display correctly on a small screen, so that I can read conversations without messages being clipped or overflowing.

#### Acceptance Criteria

1. WHILE the viewport width is less than 768 px, message bubbles in THE MessageList SHALL have a maximum width of 85% of the container width (instead of the current 70%) to make better use of the available space.
2. THE MessageList SHALL use `break-words` and `overflow-wrap: break-word` on message bubble text so that long unbroken strings do not cause horizontal overflow.
3. THE MessageList SHALL fill the available height of its parent container using `h-full` and `overflow-y-auto` so that it scrolls independently without causing the page to scroll.
4. WHEN new messages arrive, THE MessageList SHALL auto-scroll to the bottom only if the user was already at or near the bottom of the list (within 100 px), so that reading older messages is not interrupted.

---

### Requirement 9: Responsive MessageInput Component

**User Story:** As a mobile user, I want the message input bar to be fully usable on a small screen, so that I can compose and send messages without the input being clipped or the keyboard obscuring the send button.

#### Acceptance Criteria

1. WHILE the viewport width is less than 480 px, THE MessageInput SHALL hide the non-essential icon buttons (Attach, GIF, Format) and show only the Emoji icon and the Send button, so that the textarea has maximum available width.
2. THE MessageInput SHALL use `flex-shrink-0` on the send button so that it is never squeezed out of view.
3. THE MessageInput container SHALL use `w-full` and avoid fixed pixel widths so that it adapts to any container width.
4. THE MessageInput textarea SHALL have a minimum height of 44 px to meet Touch_Target requirements.

---

### Requirement 10: Responsive Chat Page Layout (Mobile Room-to-Chat Flow)

**User Story:** As a mobile user, I want to tap a room in the list and see the full-screen chat view, then be able to go back to the room list, so that the navigation feels native and intuitive.

#### Acceptance Criteria

1. WHILE the viewport width is less than 768 px and no room is selected, THE Chat_Page SHALL display the RoomSelector panel full-screen and hide the chat message area.
2. WHEN a user selects a room on a mobile viewport, THE Chat_Page SHALL transition to showing the chat message area full-screen and hide the RoomSelector panel.
3. THE Chat_Page SHALL render a back button in the chat header on mobile viewports that, WHEN tapped, returns the user to the full-screen RoomSelector panel.
4. WHILE the viewport width is 768 px or greater, THE Chat_Page SHALL display the RoomSelector panel and the chat message area side by side simultaneously.
5. WHILE the viewport width is less than 1280 px, THE Chat_Page SHALL hide the UserList members sidebar; WHEN the viewport width is 1280 px or greater, THE Chat_Page SHALL show the UserList members sidebar.

---

### Requirement 11: Touch-Friendly Interactive Elements

**User Story:** As a mobile user, I want all buttons and interactive elements to be large enough to tap accurately, so that I don't accidentally trigger the wrong action.

#### Acceptance Criteria

1. THE App SHALL ensure all interactive elements (buttons, links, tab items) within the chat interface have a minimum Touch_Target size of 44 × 44 CSS px.
2. WHEN an interactive element's visible size is smaller than 44 × 44 CSS px, THE App SHALL use padding or a transparent hit-area extension to meet the Touch_Target requirement without altering the visual design.
3. THE App SHALL ensure a minimum spacing of 8 px between adjacent Touch_Targets to prevent accidental activation of neighboring elements.

---

### Requirement 12: Accessibility Preservation

**User Story:** As a user relying on assistive technology, I want the redesigned interface to maintain all existing accessibility features, so that the mobile-first changes do not regress screen reader or keyboard navigation support.

#### Acceptance Criteria

1. THE App SHALL preserve all existing ARIA roles, labels, and live regions on chat components after the responsive redesign.
2. THE Bottom_Tab_Bar SHALL mark the active tab with `aria-current="page"` so that screen readers announce the current location.
3. THE App SHALL maintain a logical focus order on both mobile and desktop layouts so that keyboard-only users can navigate all interactive elements in a predictable sequence.
4. WHEN a mobile overlay (members drawer, friends panel overlay) is open, THE App SHALL trap keyboard focus within the overlay and restore focus to the triggering element when the overlay is closed.
