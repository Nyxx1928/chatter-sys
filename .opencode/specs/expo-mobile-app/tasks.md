# Implementation Plan: Expo Mobile App

## Overview

Eight implementation phases over ~20-28 days (one developer, full-time). Each phase builds on the previous, with explicit checkpoints to validate progress before continuing. Optional tasks are marked with `*`. Every task references the requirements it validates.

---

## Tasks

### Phase 1: Foundation — Project Setup + Shared Code Migration

**Goal:** Establish the Expo project, configure tooling, and migrate all reusable code from the web frontend so that types, stores, API client, and STOMP client compile without errors.

- [ ] **1.1** Create Expo project with tabs template
  - `npx create-expo-app@latest expo-chat-app --template tabs`
  - Install dependencies: `zustand`, `@stomp/stompjs`, `expo-secure-store`, `@react-native-async-storage/async-storage`, `@shopify/flash-list`, `expo-notifications`, `expo-device`, `expo-linking`, `@react-native-community/netinfo`
  - Configure `app.json` with app name "Chatter", scheme "chatter", SecureStore + Notifications + Linking plugins
  - Configure `tsconfig.json`, `eslint`, and `eas.json` for development/preview/production profiles
  - _Requirements: 12.1, 12.2, 12.3_

- [ ] **1.2** Copy and verify all TypeScript types from `frontend/types/`
  - Copy `domain.ts`, `api.ts`, `stomp.ts`, `index.ts` to `src/types/` with zero changes
  - Verify compilation: `npx tsc --noEmit`
  - _Requirements: All (types are cross-cutting)_

- [ ] **1.3** Adapt and copy API client from `frontend/lib/api/client.ts`
  - Create `src/api/client.ts`:
    - Replace `process.env.NEXT_PUBLIC_*` with `process.env.EXPO_PUBLIC_*`
    - Remove SockJS references
    - Keep `ApiError`, `NetworkError`, `apiCall<T>()`, CSRF injection, `getErrorMessage`
  - Verify with a test call to `GET /api/auth/health` (if exists)
  - _Requirements: All (client is cross-cutting)_

- [ ] **1.4** Copy and adapt API modules from `frontend/lib/api/`
  - Copy `auth.ts`, `rooms.ts`, `messages.ts`, `friends.ts`, `users.ts` to `src/api/` — zero content changes needed (they call `apiCall()`)
  - Create new `src/api/notifications.ts` with `registerPushToken()` and `unregisterPushToken()` (stubs for now, connected in Phase 7)
  - _Requirements: 2.x, 3.x, 5.x, 6.x, 9.x_

- [ ] **1.5** Copy and adapt Zustand stores from `frontend/lib/store/`
  - Copy `chatStore.ts`, `presenceStore.ts` verbatim — no platform dependencies
  - Adapt `authStore.ts`: replace `localStorage` calls with `expo-secure-store` equivalents (`getItemAsync`, `setItemAsync`, `deleteItemAsync`) and `AsyncStorage` for user cache
  - Adapt `connectionStore.ts`: replace SockJS-based STOMP client with raw WebSocket STOMP client
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 4.1_

- [ ] **1.6** Copy and adapt STOMP client from `frontend/lib/stomp/`
  - Copy `hooks.ts` verbatim (React hooks, no platform deps)
  - Adapt `client.ts`: replace `webSocketFactory: () => new SockJS(brokerUrl)` with `webSocketFactory: () => new WebSocket(brokerUrl)`. Keep all reconnect logic, heartbeat config, connect headers, and error handlers
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] **1.7** Write storage utility `src/utils/storage.ts`
  - `setSecureToken(token)`, `getSecureToken()`, `clearSecureToken()` using `expo-secure-store`
  - `setCachedUser(user)`, `getCachedUser()`, `clearCachedUser()` using `AsyncStorage`
  - `clearAll()` — clears both stores
  - _Requirements: 1.1, 1.5_

- [ ] **1.8** Create constants utility `src/utils/constants.ts`
  - `API_BASE_URL` from `EXPO_PUBLIC_API_BASE_URL` or default `http://localhost:8080`
  - `WS_URL` from `EXPO_PUBLIC_WS_URL` or default `ws://localhost:8080/ws`
  - _Requirements: None (infrastructure)_

- [ ] **1.9** Checkpoint — Verify Phase 1
  - All TypeScript compiles without errors (`npx tsc --noEmit`)
  - Expo dev server starts (`npx expo start`)
  - All 5 stores instantiate without runtime errors
  - API client can reach the backend (manual test with known-good endpoint)
  - **Ask the user if questions arise before proceeding**

---

### Phase 2: Auth Screens

**Goal:** Build the complete authentication flow — login, register, forgot password, reset password, email verification — with session persistence and proper error handling.

- [ ] **2.1** Build root layout (`app/_layout.tsx`) with auth gate
  - On mount: check `expo-secure-store` for JWT → `authStore.validateSession()`
  - While validating: show splash screen (logo + ActivityIndicator)
  - After validation: if authenticated → show `(tabs)`; if not → show `(auth)`
  - Register `expo-notifications` handler for notification tap → deep link (stub, connected in Phase 7)
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 12.4, 12.5, 12.6_

- [ ] **2.2** Build auth group layout (`app/(auth)/_layout.tsx`)
  - Stack navigator with no header
  - Routes: login, register, forgot-password, reset-password, verify-email
  - _Requirements: 12.1, 12.2_

- [ ] **2.3** Build login screen (`app/(auth)/login.tsx`)
  - Username + password inputs with validation (non-empty, min lengths)
  - Submit → `authStore.login()` → on success navigate to tabs
  - Error display: invalid credentials (401) → "Invalid username or password"
  - Links: "Create account" → register, "Forgot password?" → forgot-password
  - Handle `?registered=true` and `?emailSent=true` query params for post-registration/post-reset messages
  - Loading state: disable inputs + show spinner on submit button
  - _Requirements: 2.1, 2.2_

- [ ] **2.4** Build register screen (`app/(auth)/register.tsx`)
  - Username + email + password + display name inputs with validation
  - Submit → `authStore.register()` → on success navigate to login with `?registered=true`
  - Error: 409 Conflict → "Username or email already exists"
  - Loading state: disable form + spinner
  - _Requirements: 2.3_

- [ ] **2.5** Build forgot password screen (`app/(auth)/forgot-password.tsx`)
  - Email input with validation
  - Submit → `POST /api/auth/forgot-password` → show "Check your email for reset link"
  - _Requirements: 2.4_

- [ ] **2.6** Build reset password screen (`app/(auth)/reset-password.tsx`)
  - Read `?token=` from route params
  - New password + confirm password inputs
  - Submit → `POST /api/auth/reset-password` → on success navigate to login with `?emailSent=true`
  - _Requirements: 2.5_

- [ ] **2.7** Build email verification result screen (`app/(auth)/verify-email.tsx`)
  - Read `?status=success|error` and `?message=` from route params
  - Display success icon + message, or error icon + message
  - "Continue to Login" button → navigate to login
  - _Requirements: 2.6_

- [ ] **2.8** Checkpoint — Auth Flow Validation
  - Full flow: launch → splash → login → enter credentials → navigate to tabs
  - Kill app → relaunch → session restored → no login screen
  - Logout → token cleared → login screen shown
  - Register → success → login → success
  - Forgot password → reset → login with new password
  - **Ask the user if questions arise before proceeding**

---

### Phase 3: Chat Screen — Direct Messages

**Goal:** Build the core chat experience — room list, real-time messaging, message history, and presence indicators.

- [ ] **3.1** Build tab layout (`app/(tabs)/_layout.tsx`)
  - 4 bottom tabs: Chats, Channels, Contacts, Profile
  - Icons from `@expo/vector-icons` (Ionicons): chatbubbles, hash, people, person
  - Active tab highlighting with custom tint color
  - Badge on Contacts tab from `authStore.pendingRequestCount` (populated in Phase 5)
  - _Requirements: 12.1, 12.2, 12.3_

- [ ] **3.2** Build Chats stack layout (`app/(tabs)/chats/_layout.tsx`)
  - Stack navigator: index (room list) → `[roomId]` (chat view)
  - _Requirements: 12.3_

- [ ] **3.3** Build room list screen (`app/(tabs)/chats/index.tsx`)
  - On mount: `GET /api/rooms` → filter `roomType === 'DIRECT'` → render in `FlashList`
  - Each row: `RoomListItem` — other participant's display name, latest message preview, timestamp, presence dot
  - Text input at top for filtering rooms client-side by display name
  - Pull-to-refresh
  - Subscribe to STOMP `/topic/rooms` for real-time room list updates
  - Empty state: "No conversations yet. Add friends to start chatting."
  - Error state: inline error with retry button
  - Loading state: skeleton rows
  - _Requirements: 3.1, 3.6_

- [ ] **3.4** Build `RoomListItem` component
  - Avatar (initials fallback) + presence dot (green/gray)
  - Display name + last message preview (truncated to 1 line)
  - Relative timestamp ("2m ago", "Yesterday", "Jun 12")
  - _Requirements: 3.1_

- [ ] **3.5** Build `PresenceDot` component
  - Small circle: `online ? green (#22c55e) : gray (#6b7280)`
  - _Requirements: 7.1, 7.2_

- [ ] **3.6** Build chat view screen (`app/(tabs)/chats/[roomId].tsx`)
  - Header: back button + other participant name + info button
  - `MessageList` (FlashList) at center
  - `MessageInput` at bottom (keyboard-avoiding)
  - On mount:
    1. `GET /api/rooms/{roomId}/messages?page=0&size=50` → load initial messages
    2. Subscribe STOMP `/topic/room/{roomId}` → incoming messages
    3. Subscribe STOMP `/topic/presence/{roomId}` → presence updates
  - Auto-scroll to bottom on new messages (unless user scrolled up → show "New Messages" button)
  - _Requirements: 3.2, 3.4, 7.3_

- [ ] **3.7** Build `MessageList` component
  - `FlashList` with `estimatedItemSize={72}`
  - Sections with date separators ("Today", "Yesterday", date strings)
  - `onEndReached` (scrolled to top) → load next page of history → prepend older messages
  - Auto-scroll to bottom on initial load and new incoming messages
  - `MessageBubble` per item: inbound (left, gray bg) vs outbound (right, primary color bg)
  - Sending state: `status === 'sending'` → opacity 0.6
  - Failed state: `status === 'failed'` → red indicator + tap to retry
  - _Requirements: 3.2, 3.4, 3.5, 3.7_

- [ ] **3.8** Build `MessageBubble` component
  - Props: `message: Message, isOwn: boolean`
  - Outbound: right-aligned, primary color background, white text
  - Inbound: left-aligned, gray background, dark text
  - System messages (JOIN/LEAVE): centered, small, italic
  - Timestamp below bubble on long-press (future: configurable)
  - _Requirements: 3.3, 3.4, 3.7_

- [ ] **3.9** Build `MessageInput` component
  - Multi-line `TextInput` with auto-grow
  - Send button (icon, disabled when empty)
  - Wrapped in `KeyboardAvoidingView` (behavior: padding on iOS, height on Android)
  - On send: call `connectionStore.sendMessage('/app/chat.send/${roomId}', { content })`
  - Optimistic insert in chatStore BEFORE send
  - _Requirements: 3.3, 3.7_

- [ ] **3.10** Implement message history pagination
  - Track `currentPage` and `hasMore` flags per room
  - `FlashList.onEndReached` (at top) → `GET /api/rooms/{roomId}/messages?page=${page+1}&size=50`
  - Prepend fetched messages to existing array — maintain sort order by timestamp
  - Infer `hasMore` from `totalPages` in response
  - _Requirements: 3.5_

- [ ] **3.11** Implement connection banner
  - `ConnectionBanner` component: full-width bar at top of chat screen
  - Listens to `connectionStore.error` and `connectionStore.connected`
  - States: "No Connection" (red) when disconnected, "Reconnecting..." (yellow) during reconnect
  - Dismisses automatically on reconnection
  - _Requirements: 4.4, 4.5, 11.1, 11.2_

- [ ] **3.12** Checkpoint — Direct Messaging Validation
  - Room list loads with correct rooms
  - Tap room → messages load with pagination
  - Send message → appears optimistically → confirmed via STOMP
  - Receive message from another user in real-time
  - Kill Wi-Fi → banner appears → restore Wi-Fi → reconnects → banner dismissed
  - Scroll to top → older messages load
  - **Ask the user if questions arise before proceeding**

---

### Phase 4: Group Channels

**Goal:** Add group channel support — list, chat, create, invite members, and delete.

- [ ] **4.1** Build Channels stack layout (`app/(tabs)/channels/_layout.tsx`)
  - Stack navigator: index (channel list) → `[roomId]` (channel chat view)
  - _Requirements: 12.3_

- [ ] **4.2** Build channel list screen (`app/(tabs)/channels/index.tsx`)
  - On mount: `GET /api/rooms` → filter `roomType === 'GROUP'` → render in FlashList
  - Each row: channel name, member count, latest message preview
  - Header with "Channels" title + "+" create button (top-right)
  - Subscribe to STOMP `/topic/rooms` for real-time updates
  - Empty state: "No channels yet. Create one!"
  - _Requirements: 5.1_

- [ ] **4.3** Build create channel modal
  - Modal with name (required) + description (optional) + "Create" button
  - On submit: `POST /api/rooms` → on success navigate to new channel's chat view
  - Error: name already taken or validation → inline error
  - _Requirements: 5.2, 5.3_

- [ ] **4.4** Build channel chat view (`app/(tabs)/channels/[roomId].tsx`)
  - Reuses same `MessageList` + `MessageInput` from Phase 3 (extract shared components)
  - Header: channel name + member count + info (ⓘ) button
  - Info button → push member list screen (or bottom sheet)
  - _Requirements: 5.4_

- [ ] **4.5** Build member list screen/bottom sheet
  - `GET /api/rooms/{id}/members` → render users with presence indicators
  - Owner/moderator sees: "Invite" button + "Delete Channel" button
  - Invite → user search flow (same component from contacts, Phase 5)
  - Delete → confirmation dialog → `DELETE /api/rooms/{id}` → navigate back to channel list
  - _Requirements: 5.5, 5.6, 5.7, 5.8_

- [ ] **4.6** Checkpoint — Channel Validation
  - Channel list loads GROUP rooms only
  - Create channel → appears in list → can send messages
  - Invite user → user appears in member list
  - Delete channel (as owner) → removed from list
  - Non-owner cannot delete
  - **Ask the user if questions arise before proceeding**

---

### Phase 5: Contacts and Friends

**Goal:** Build the complete social layer — friend list, user search, friend requests, and accept/decline flow.

- [ ] **5.1** Build Contacts stack layout (`app/(tabs)/contacts/_layout.tsx`)
  - Stack navigator: index (friend list) → add (search users) → requests (pending requests)
  - _Requirements: 12.3_

- [ ] **5.2** Build friends list screen (`app/(tabs)/contacts/index.tsx`)
  - On mount: `GET /api/friends` → render in FlashList
  - Each row: friend avatar + display name + presence dot + "Chat" button
  - "Chat" → navigate to DM room (use `dmRoomId` from `Friendship` response)
  - Header with "Contacts" title + "+" (add) + badge with pending count + "Requests" link
  - Subscribe to STOMP `/topic/presence/*` for global presence updates
  - Empty state: "No friends yet. Search for users to add!"
  - _Requirements: 6.1, 6.2, 6.9_

- [ ] **5.3** Build add friend screen (`app/(tabs)/contacts/add.tsx`)
  - Search input with 300ms debounce
  - On query: `GET /api/users/search?q=` → render `UserSearchResult` items
  - Each result shows: avatar + display name + username + action button
  - Action button states:
    - `relationshipStatus === 'NONE'` → "Add" → `POST /api/friends/requests` → updates to "Pending"
    - `relationshipStatus === 'PENDING_OUTGOING'` → "Pending" (disabled)
    - `relationshipStatus === 'PENDING_INCOMING'` → "Accept" → navigates to requests
    - `relationshipStatus === 'FRIENDS'` → "Friend" (disabled)
  - Empty state when no query: "Search for users by name or username"
  - _Requirements: 6.3, 6.4_

- [ ] **5.4** Build friend requests screen (`app/(tabs)/contacts/requests.tsx`)
  - On mount: `GET /api/friends/requests` → separate into incoming/outgoing sections
  - Incoming: each row shows requester name + "Accept" + "Decline" buttons
    - Accept → `POST /api/friends/requests/{id}/accept` → move row to "Accepted" (briefly) → remove from list
    - Decline → `POST /api/friends/requests/{id}/decline` → remove row
  - Outgoing: each row shows recipient name + "Pending" badge (no actions)
  - Empty state (no requests): "No pending requests"
  - _Requirements: 6.5, 6.6, 6.7_

- [ ] **5.5** Implement remove friend (from contacts index or long-press)
  - Swipe-to-delete or long-press menu → "Remove Friend"
  - Confirmation dialog → `DELETE /api/friends/{friendId}` → update list
  - _Requirements: 6.8_

- [ ] **5.6** Connect user presence to contacts
  - `presenceStore` tracks `Map<userId, {online, lastSeen}>` from STOMP `/topic/presence/*`
  - `ContactCard` renders `PresenceDot` based on `presenceStore.presence.get(friend.id)?.online`
  - _Requirements: 7.1, 7.3_

- [ ] **5.7** Checkpoint — Contacts Validation
  - Friends list loads with presence indicators
  - Search users → results shown with correct relationship status
  - Send friend request → status changes to "Pending"
  - Other user accepts → friend appears in list with DM room
  - Decline request → removed from list
  - Remove friend → removed from list
  - Badge updates with pending count
  - **Ask the user if questions arise before proceeding**

---

### Phase 6: Profile Screen

**Goal:** Build the profile screen with view/edit, logout, and account deletion.

- [ ] **6.1** Build Profile stack layout (`app/(tabs)/profile/_layout.tsx`)
  - Stack navigator: index only (single screen)
  - _Requirements: 12.3_

- [ ] **6.2** Build profile screen (`app/(tabs)/profile/index.tsx`)
  - Display: avatar (initials), display name (editable), email (editable)
  - Edit fields: `TextInput` pre-filled with current values
  - "Save Changes" button → `PUT /api/users/me` → on success update local state
  - Divider
  - "Logout" button → confirmation dialog → clear stores + secure store → navigate to login
  - "Delete Account" button → two-step confirmation:
    1. "Are you sure? This cannot be undone." → "Cancel" / "Continue"
    2. "Type your username to confirm:" → text input → "Delete" (enabled when text matches)
  - App version from `app.json` displayed at bottom
  - Loading state: skeleton placeholders
  - Error state on save: inline error with retry
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] **6.3** Checkpoint — Profile Validation
  - Profile loads current user data
  - Edit display name → save → reflects in UI
  - Edit email → save → reflects in UI
  - Logout → token cleared → login screen
  - Delete account → two-step confirmation → account removed → login screen
  - **Ask the user if questions arise before proceeding**

---

### Phase 7: Push Notifications

**Goal:** Implement push notification infrastructure on both backend and mobile — token registration, offline detection, notification dispatch, and tap handling.

#### Backend Tasks

- [ ] **7.1** Create `PushToken` entity (`src/main/java/org/example/chat/entity/PushToken.java`)
  - Fields: `id` (Long, PK, auto), `user` (ManyToOne → User), `pushToken` (String, unique), `platform` (String), `createdAt`/`updatedAt` (LocalDateTime)
  - Use Lombok `@Data`, `@Entity`, `@Table(name = "push_tokens")`
  - _Requirements: 10.3_

- [ ] **7.2** Create `PushTokenRepository`
  - `findByUserId(Long userId): List<PushToken>`
  - `findByPushToken(String pushToken): Optional<PushToken>`
  - `deleteByPushToken(String pushToken): void`
  - _Requirements: 10.3_

- [ ] **7.3** Create `RegisterPushTokenRequest` DTO
  - Fields: `pushToken` (String, @NotBlank), `platform` (String, @NotBlank)
  - _Requirements: 10.1_

- [ ] **7.4** Create `UnregisterPushTokenRequest` DTO
  - Fields: `pushToken` (String, @NotBlank)
  - _Requirements: 10.2_

- [ ] **7.5** Create `PushNotificationController`
  - `POST /api/push/register` — authenticated — saves token linked to current user
  - `POST /api/push/unregister` — authenticated — removes token by pushToken
  - Validation: reject blank tokens, invalid platform values
  - Error: duplicate token → upsert (update user_id), not error
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] **7.6** Create `PushNotificationService`
  - `sendPushIfOffline(Message message, Long roomId)`:
    1. Get room members via `ChatRoomService.getRoomMembers(roomId)`
    2. For each member (excluding message sender):
       a. If `UserPresenceService.isOnline(memberId)` → skip
       b. Fetch push tokens from `PushTokenRepository.findByUserId(memberId)`
       c. For each token: build Expo push payload → send via `WebClient`
  - Expo push payload format:
    ```json
    {
      "to": "ExponentPushToken[...]",
      "title": "<sender display name>",
      "body": "<message content (first 120 chars)>",
      "data": { "roomId": <roomId>, "type": "new_message" },
      "sound": "default",
      "priority": "high"
    }
    ```
  - `WebClient` configuration: `POST https://exp.host/--/api/v2/push/send`, timeout 10s
  - Log push response for debugging (success/failure counts)
  - _Requirements: 9.4, 9.6_

- [ ] **7.7** Modify `ChatMessageService.sendMessage()`
  - After `messageRepository.save(savedMessage)` and `broadcastMessage(savedMessage, roomId)`:
    `pushNotificationService.sendPushIfOffline(savedMessage, roomId)`
  - Wrap in try-catch — push failure must NOT interrupt message delivery
  - _Requirements: 9.4, 9.6_

- [ ] **7.8** Update `SecurityConfig`
  - Add `POST /api/push/**` to authenticated endpoints permit list
  - _Requirements: 10.1, 10.2_

#### Frontend Tasks

- [ ] **7.9** Create `notificationStore` (`src/stores/notificationStore.ts`)
  - State: `pushToken: string | null`, `permissionGranted: boolean`, `notification: Notification | null`
  - Actions:
    - `requestPermissionsAndRegister()`:
      1. `Notifications.requestPermissionsAsync()` → if granted, set `permissionGranted`
      2. `Notifications.getExpoPushTokenAsync()` → store token
      3. `POST /api/push/register` with token + platform
    - `unregisterPushToken()`: `POST /api/push/unregister` → clear local token
    - `handleNotificationTap(notification)`: extract `data.roomId` → navigate to `chats/[roomId]`
  - _Requirements: 9.1, 9.2, 9.3, 9.5_

- [ ] **7.10** Create `useNotifications` hook (`src/hooks/useNotifications.ts`)
  - On app mount (if authenticated):
    1. Call `notificationStore.requestPermissionsAndRegister()`
    2. Set up `Notifications.addNotificationResponseReceivedListener` → `handleNotificationTap`
  - On logout: call `notificationStore.unregisterPushToken()`
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] **7.11** Integrate notification hook into root layout
  - In `app/_layout.tsx`, after auth is confirmed, call `useNotifications()` hook
  - Handle notification tap → navigate to correct room via Expo Router
  - _Requirements: 9.5_

- [ ] **7.12** Handle foreground notifications
  - Configure `Notifications.setNotificationHandler`:
    - `shouldShowAlert: true` — show banner even when app is foregrounded
    - `shouldPlaySound: true`
    - `shouldSetBadge: false`
  - _Requirements: 9.4_

- [ ] **7.13** Checkpoint — Push Notification Validation
  - Backend: `POST /api/push/register` stores token successfully
  - Backend: Send message to offline user → push delivered to device
  - Backend: Send message to online user → NO push delivered
  - Backend: `POST /api/push/unregister` removes token
  - Mobile: Permission prompt appears on first launch
  - Mobile: Notification appears when app is backgrounded
  - Mobile: Tap notification → opens correct chat room
  - Mobile: Logout → token unregistered
  - **Ask the user if questions arise before proceeding**

---

### Phase 8: Polish, Edge Cases, and Testing

**Goal:** Handle all edge cases, offline resilience, performance optimization, and visual polish.

- [ ] **8.1** Implement offline message queue
  - When `connectionStore.connected === false` and user sends a message:
    - Store message in local queue (in-memory + AsyncStorage backup)
    - Show message with "pending" status in chat
  - On STOMP reconnect: flush all queued messages in FIFO order
  - On flush failure: retry with exponential backoff (1s, 2s, 4s, 8s, max 30s)
  - _Requirements: 11.3_

- [ ] **8.2** Implement `useNetworkStatus` hook
  - Wraps `@react-native-community/netinfo`
  - Exposes `isConnected: boolean`, `connectionType: string`
  - On disconnect: set `connectionStore` disconnected → show banner
  - On reconnect: trigger STOMP reconnect + message queue flush
  - _Requirements: 11.1, 11.2_

- [ ] **8.3** Add empty states for all list screens
  - Chats: "No conversations yet. Add friends to start chatting."
  - Channels: "No channels yet. Create one!"
  - Contacts: "No friends yet. Search for users to add!"
  - Requests: "No pending requests"
  - Message list: "No messages yet. Say hello!"
  - Search: "No users found"
  - Each with relevant icon + optional action button
  - _Requirements: 3.6, 6.1_

- [ ] **8.4** Add loading states for all screens
  - Skeleton placeholders for: room list (3 rows), message list (5 bubbles), contacts (3 rows), channel list (3 rows), profile (3 fields)
  - ActivityIndicator for: auth actions, save, delete, create
  - _Requirements: None (quality)_

- [ ] **8.5** Add error states with retry for all data-fetching screens
  - Inline error message + "Retry" button for failed API calls
  - Distinguish network errors ("No internet connection") from server errors ("Something went wrong")
  - _Requirements: 11.4, 11.5, 11.6_

- [ ] **8.6** App icon and splash screen
  - Create app icon (1024x1024) using existing branding
  - Configure splash screen in `app.json` with dark background + logo
  - Test on both platforms
  - _Requirements: None (quality)_

- [ ] **8.7** Performance optimization
  - Verify `FlashList` `estimatedItemSize` is tuned (72px for messages, 64px for list items)
  - Add `keyExtractor` for all lists (use entity `id`)
  - Memoize expensive components with `React.memo`
  - Verify Zustand selectors don't cause unnecessary re-renders
  - Test with 1000+ messages in a single room
  - _Requirements: None (quality)_

- [ ] **8.8** Accessibility
  - All touch targets minimum 44x44pt
  - `accessibilityLabel` on all interactive elements (send button, back button, tab icons)
  - `accessibilityRole` on list items, buttons, inputs
  - Screen reader test on iOS (VoiceOver) and Android (TalkBack)
  - _Requirements: None (quality)_

- [ ] **8.9** Run complete manual QA regression
  - Execute full Manual QA Checklist:
    - Auth: login, register, forgot/reset password, email verify, session restore, logout, delete account
    - Chat: room list, message history pagination, send/receive real-time, optimistic UI, connection loss/reconnect
    - Channels: list, create, chat, invite, delete
    - Contacts: list, search, add friend, accept/decline requests, remove friend, presence
    - Profile: view, edit, logout, delete
    - Push: permission, receive while backgrounded, tap to open room, register on login, unregister on logout
    - Offline: banner on network loss, queued messages flush on reconnect, retry on failure
  - Fix any discovered issues
  - _Requirements: All_

- [ ] **8.10** Build for internal testing via EAS
  - `eas build --platform android --profile preview` → APK
  - `eas build --platform ios --profile preview` → IPA
  - Distribute QR code / install link to testers
  - _Requirements: None (deployment)_

- [ ] **8.11** Checkpoint — Final Validation
  - All QA checklist items pass
  - Builds succeed on both platforms
  - Testers can install and use the app without crashes
  - **Ask the user if questions arise before proceeding**

---

## Notes

- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation throughout development
- No tasks are marked optional — all are required for full feature parity with the web app
- Phase 7 requires both backend (Java) and frontend (Expo) changes — coordinate accordingly
