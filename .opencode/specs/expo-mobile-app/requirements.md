# Requirements Document: Expo Mobile App

## Introduction

Build a cross-platform mobile application for the Real-Time Chat System using Expo (React Native) that achieves full feature parity with the existing Next.js web frontend. The mobile app will reuse the existing Java Spring Boot backend (REST API + WebSocket/STOMP), adding push notification support so users receive messages when the app is backgrounded. Initial distribution targets internal testing via EAS Build.

## Glossary

- **DM**: Direct Message — a private chat room between two users (RoomType: DIRECT)
- **Channel**: A group chat room with multiple members (RoomType: GROUP)
- **Expo**: An open-source framework for building native iOS/Android apps with React Native
- **EAS**: Expo Application Services — cloud build and submission service
- **Push Notification**: An alert delivered to a device when the app is not in the foreground
- **Expo Push Token**: A device-specific token (`ExponentPushToken[...]`) that identifies the device for push notifications
- **STOMP**: Simple/Streaming Text Oriented Messaging Protocol — protocol used over WebSocket for real-time messaging
- **Presence**: Online/offline status indicator for users in a chat room
- **Splash Screen**: The initial loading screen displayed while the app restores session state
- **Optimistic UI**: A pattern where the UI updates immediately before receiving server confirmation, then reconciles

## Requirements

### Requirement 1: Session Management

**User Story:** As a user, I want my authentication session to persist across app restarts, so that I don't need to log in every time I open the app.

#### Acceptance Criteria

1. WHEN the app launches, THE system SHALL check `expo-secure-store` for a stored JWT token
2. WHEN a stored JWT token exists, THE system SHALL validate it via `GET /api/users/me` with an 8-second timeout
3. WHEN the token is valid, THE system SHALL navigate directly to the main chat screen
4. WHEN the token is expired or invalid, THE system SHALL clear the token and navigate to the login screen
5. WHEN the user logs out, THE system SHALL clear the token from secure storage and navigate to the login screen

### Requirement 2: Authentication

**User Story:** As a user, I want to log in, register, and recover my password from the mobile app.

#### Acceptance Criteria

1. WHEN the user submits valid credentials on the login screen, THE system SHALL call `POST /api/auth/login`, store the JWT in `expo-secure-store`, and navigate to the main chat screen
2. WHEN the user submits invalid credentials, THE system SHALL display an appropriate error message
3. WHEN the user completes the registration form, THE system SHALL call `POST /api/auth/register`, show a success message, and navigate to the login screen
4. WHEN the user submits an email on the forgot-password screen, THE system SHALL call `POST /api/auth/forgot-password` and show a confirmation message
5. WHEN the user submits a new password with a valid reset token, THE system SHALL call `POST /api/auth/reset-password` and navigate to the login screen
6. WHEN the email verification result page loads with query parameters, THE system SHALL display the corresponding success or error message

### Requirement 3: Direct Messages

**User Story:** As a user, I want to see my direct message conversations and send/receive messages in real-time.

#### Acceptance Criteria

1. WHEN the user navigates to the Chats tab, THE system SHALL display a list of DIRECT rooms from `GET /api/rooms`, filtered by `roomType === 'DIRECT'`
2. WHEN the user taps a DM room, THE system SHALL display paginated message history via `GET /api/rooms/{roomId}/messages`
3. WHEN the user sends a message, THE system SHALL publish it via STOMP to `/app/chat.send/{roomId}` and display it optimistically in the message list
4. WHEN a new message is broadcast via STOMP `/topic/room/{roomId}`, THE system SHALL append it to the message list in real-time
5. WHEN the user scrolls to the top of the message list, THE system SHALL load older messages (infinite scroll pagination)
6. WHEN the room is empty, THE system SHALL show an appropriate empty state ("No messages yet")
7. WHEN a network error occurs during sending, THE system SHALL mark the message as failed with a retry option

### Requirement 4: Real-Time Messaging

**User Story:** As a user, I want to receive and send messages in real-time without polling.

#### Acceptance Criteria

1. WHEN the app is in the foreground and authenticated, THE system SHALL maintain an active STOMP WebSocket connection to `/ws`
2. WHEN the user sends a message, THE system SHALL use STOMP destination `/app/chat.send/{roomId}`
3. WHEN a message is broadcast to `/topic/room/{roomId}`, THE system SHALL update the UI in real-time
4. WHEN the WebSocket disconnects, THE system SHALL display a connection status banner and auto-reconnect every 5 seconds
5. WHEN the WebSocket reconnects, THE system SHALL re-subscribe to all active topics and dismiss the banner
6. WHEN the app goes to the background, THE system SHALL disconnect the STOMP connection
7. WHEN the app returns to the foreground, THE system SHALL re-establish the STOMP connection

### Requirement 5: Group Channels

**User Story:** As a user, I want to create, join, and participate in group chat channels.

#### Acceptance Criteria

1. WHEN the user navigates to the Channels tab, THE system SHALL display a list of GROUP rooms from `GET /api/rooms`
2. WHEN the user taps the create button, THE system SHALL present a form with name and description fields
3. WHEN the user submits the create form, THE system SHALL call `POST /api/rooms` and navigate to the new channel
4. WHEN the user taps a channel, THE system SHALL display its chat view (same behavior as DM chat with Requirement 3)
5. WHEN the user taps the channel info button, THE system SHALL display the member list from `GET /api/rooms/{id}/members`
6. WHEN the user is an owner or moderator, THE system SHALL show invite and delete options
7. WHEN the user taps invite, THE system SHALL present a user search and call `POST /api/rooms/{id}/invite?inviteeId=`
8. WHEN the owner/moderator confirms deletion, THE system SHALL call `DELETE /api/rooms/{id}` and navigate back to the channel list

### Requirement 6: Contacts and Friends

**User Story:** As a user, I want to manage my friends list and send/accept friend requests.

#### Acceptance Criteria

1. WHEN the user navigates to the Contacts tab, THE system SHALL display the user's friends from `GET /api/friends`
2. WHEN the user taps a friend, THE system SHALL navigate to their DM room
3. WHEN the user taps the search button, THE system SHALL display a search screen with debounced user search via `GET /api/users/search?q=`
4. WHEN the user taps "Add" on a search result with `relationshipStatus === 'NONE'`, THE system SHALL call `POST /api/friends/requests` and update the button to "Pending"
5. WHEN the user navigates to the requests screen, THE system SHALL display incoming and outgoing requests from `GET /api/friends/requests`
6. WHEN the user accepts a request, THE system SHALL call `POST /api/friends/requests/{id}/accept` and move the user to the friends list
7. WHEN the user declines a request, THE system SHALL call `POST /api/friends/requests/{id}/decline` and remove it from the list
8. WHEN the user removes a friend, THE system SHALL call `DELETE /api/friends/{friendId}` and update the list
9. THE system SHALL show a badge on the Contacts tab indicating the count of pending incoming requests

### Requirement 7: User Presence

**User Story:** As a user, I want to see which of my contacts and room members are currently online.

#### Acceptance Criteria

1. WHEN the user views the friend list, THE system SHALL display an online/offline indicator for each friend
2. WHEN the user views a room's member list, THE system SHALL display presence indicators for each member
3. WHEN a user's presence changes, THE system SHALL update in real-time via STOMP `/topic/presence/{roomId}`
4. WHEN the user is online, their presence SHALL be reflected to other users in shared rooms
5. WHEN the user disconnects, their presence SHALL be updated to offline

### Requirement 8: Profile Management

**User Story:** As a user, I want to view and edit my profile, log out, and delete my account.

#### Acceptance Criteria

1. WHEN the user navigates to the Profile tab, THE system SHALL display their display name, email, and other profile information from `GET /api/users/me`
2. WHEN the user edits their display name or email and taps save, THE system SHALL call `PUT /api/users/me` and update the display
3. WHEN the user taps Logout, THE system SHALL clear all stored state and navigate to the login screen
4. WHEN the user taps Delete Account, THE system SHALL display a two-step confirmation dialog
5. WHEN the user confirms deletion, THE system SHALL call `DELETE /api/users/me` and navigate to the login screen

### Requirement 9: Push Notifications

**User Story:** As a user, I want to receive a notification when someone sends me a message while the app is closed or in the background, and tapping the notification should open the relevant chat.

#### Acceptance Criteria

1. WHEN the app launches for the first time, THE system SHALL request notification permission from the OS
2. WHEN permission is granted, THE system SHALL obtain an Expo Push Token and register it with the backend via `POST /api/push/register`
3. WHEN the user logs out, THE system SHALL unregister the push token via `POST /api/push/unregister`
4. WHEN a message is sent to an offline user, THE backend SHALL send a push notification via the Expo Push API
5. WHEN the user taps an incoming push notification, THE system SHALL navigate to the relevant chat room
6. WHEN the user is online (STOMP connected), THE backend SHALL NOT send a push notification
7. WHEN permission is denied, THE system SHALL gracefully handle the absence of push notifications

### Requirement 10: Push Token Registration (Backend)

**Functionality:** The backend shall allow mobile clients to register and unregister push notification tokens.

#### Acceptance Criteria

1. THE system SHALL expose `POST /api/push/register` accepting `{pushToken: string, platform: string}`
2. THE system SHALL expose `POST /api/push/unregister` accepting `{pushToken: string}`
3. THE system SHALL persist push tokens in a `push_tokens` table linked to the user
4. THE system SHALL validate that the authenticated user owns the token
5. THE system SHALL remove the token record on unregister

### Requirement 11: Error Handling and Offline Resilience

**User Story:** As a user, I want the app to handle network errors gracefully and not lose my messages.

#### Acceptance Criteria

1. WHEN the device loses network connectivity, THE system SHALL show a "No Connection" banner
2. WHEN the STOMP connection is lost, THE system SHALL show a "Reconnecting..." banner
3. WHEN the user sends a message while offline, THE system SHALL queue the message and send it when connectivity resumes
4. WHEN an API call fails due to network error, THE system SHALL display an inline error with a retry option
5. WHEN an API call fails due to server error (5xx), THE system SHALL display a user-friendly message
6. WHEN an API call fails due to client error (4xx), THE system SHALL display the server's error message

### Requirement 12: Navigation and Layout

**User Story:** As a user, I want to navigate between app sections using a standard mobile tab bar.

#### Acceptance Criteria

1. THE system SHALL provide a bottom tab bar with four tabs: Chats, Channels, Contacts, Profile
2. THE system SHALL highlight the active tab
3. THE system SHALL provide stack navigation within each tab (list → detail screens)
4. THE system SHALL show a loading/splash screen during auth session validation
5. THE system SHALL redirect unauthenticated users to the login screen
6. THE system SHALL prevent navigation to authenticated screens when not logged in

## Scope

### In-Scope

- iOS and Android support via Expo managed workflow
- Full auth flow (login, register, forgot/reset password, email verification)
- Direct message conversations with real-time updates
- Group channels with create, invite, delete, and member list
- Friends list management with search, requests, accept/decline
- User presence indicators (online/offline)
- Profile viewing and editing, logout, account deletion
- Push notifications for offline message delivery
- Backend push token registration and notification sending
- Optimistic UI for message sending
- Offline resilience with queued messages
- Internal distribution via EAS Build

### Out-of-Scope

- File/image upload and sharing (backend lacks this feature; no change to backend scope)
- Voice/video calling
- Typing indicators (can be added post-MVP)
- Read receipts
- Message reactions
- Admin panel functionality
- Web version changes (existing Next.js frontend remains unchanged)
- App store publishing (internal distribution only for v1)
