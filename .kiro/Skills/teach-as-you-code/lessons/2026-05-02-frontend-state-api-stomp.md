# Lesson: Frontend State, API, and STOMP Setup

## Task Context

The frontend needed state management, a typed API layer, and a reusable STOMP WebSocket client. This lesson covers tasks 21-26: Zustand stores, HTTP API client helpers, and STOMP utilities to support the real-time chat UI.

## Files Modified

- frontend/types/api.ts (modified)
- frontend/types/index.ts (modified)
- frontend/utils/storage.ts (created)
- frontend/lib/api/client.ts (created)
- frontend/lib/api/auth.ts (created)
- frontend/lib/api/rooms.ts (created)
- frontend/lib/api/messages.ts (created)
- frontend/lib/store/authStore.ts (created)
- frontend/lib/store/chatStore.ts (created)
- frontend/lib/store/connectionStore.ts (created)
- frontend/lib/stomp/client.ts (created)
- frontend/lib/stomp/hooks.ts (created)

## Step-by-Step Changes

### Step 1: Add a Typed Update Profile Request

The backend exposes `PUT /api/users/me` with an optional email and display name. We added a matching request type so the frontend has compile-time safety:

```ts
export interface UpdateProfileRequest {
  email?: string;
  displayName?: string;
}
```

We also exported it from the central types index to keep imports consistent across the app.

### Step 2: Create Local Storage Helpers

Auth state needs persistence across reloads, but localStorage is only available in the browser. We created a small helper that guards access to `window` and returns safe defaults.

Key helpers:

- `getStoredAuth()` returns `{ token, user }` with null-safe fallbacks
- `setStoredAuth()` writes token and user to localStorage
- `clearStoredAuth()` removes persisted auth data

This avoids server-side rendering crashes and keeps token handling consistent.

### Step 3: Build a Base API Client

We created a `apiCall` helper that standardizes JSON handling and error shaping:

```ts
export const apiCall = async <T>(
  path: string,
  options: ApiCallOptions = {},
) => {
  const response = await fetch(`${API_BASE_URL}${path}`, { ...options });
  if (!response.ok) {
    const details = await parseResponseBody(response);
    throw new ApiError(message, response.status, details);
  }
  return (await parseResponseBody(response)) as T;
};
```

Core features:

- Adds `Authorization` header when a token is supplied
- Sets JSON headers when a body is present
- Parses JSON when possible, falls back to text
- Throws `ApiError` for HTTP failures and `NetworkError` for fetch failures

### Step 4: Add Typed API Modules

With the base client in place, we added typed API modules to keep calls consistent:

- `auth.ts`: login, register, getCurrentUser, updateProfile
- `rooms.ts`: createRoom, listRooms, getRoomDetails, getRoomMembers
- `messages.ts`: getMessageHistory with pagination support

This keeps API usage simple inside components and stores:

```ts
export const getRoomMembers = async (token: string, roomId: number) =>
  apiCall<User[]>(`/api/rooms/${roomId}/members`, { method: "GET", token });
```

### Step 5: Create Zustand Stores

We implemented three stores to separate responsibilities:

**Auth store (`authStore.ts`):**

- Holds `user`, `token`, `isAuthenticated`
- `login()` calls the API and stores auth in localStorage
- `register()` saves the user (no token yet)
- `logout()` clears localStorage and resets state

**Chat store (`chatStore.ts`):**

- Holds `rooms`, `currentRoom`, and `messages` per room
- Uses a `Map<string, Message[]>` to cache messages by room ID
- `addMessage()` clones the Map to keep updates immutable

**Connection store (`connectionStore.ts`):**

- Tracks `client`, `connected`, `connecting`, and `error`
- `connect()` builds a STOMP client and activates it
- `disconnect()` deactivates and resets state
- `sendMessage()` publishes JSON payloads to destinations

### Step 6: Build STOMP Client Utilities

We created a STOMP client factory and a reusable subscription hook:

**Client factory (`client.ts`):**

- Uses SockJS with a configurable `NEXT_PUBLIC_WS_URL`
- Adds `Authorization` header when a token exists
- Sets heartbeat and reconnect values
- Plumbs connection and error callbacks into the store

**Subscription hook (`hooks.ts`):**

- Subscribes only when connected and destination is defined
- Parses JSON payloads with a generic type
- Automatically unsubscribes on cleanup

This keeps STOMP usage clean in React components and supports typed payloads.

## Why This Approach

- Centralized API errors let UI components focus on UX instead of HTTP mechanics.
- A small storage helper prevents localStorage access during server rendering.
- Zustand stores keep state logic isolated and easy to test later.
- Reusable STOMP utilities keep connection logic consistent and support reconnection.

## Alternatives Considered

- Zustand `persist` middleware instead of a custom storage helper.
- Axios or a full-featured client like `ky` instead of `fetch`.
- Handling STOMP subscriptions directly in components instead of a hook.

## Key Concepts

- Typed API boundaries between frontend and backend DTOs.
- Persisted auth state vs in-memory UI state.
- Immutable updates when storing messages by room.
- STOMP lifecycle management: connect, subscribe, publish, disconnect.

## Potential Pitfalls

- Calling localStorage in server components without guarding for `window`.
- Mutating a `Map` in place and missing React updates.
- Publishing messages before the STOMP client is connected.
- Ignoring error bodies that are not JSON.

## What You Learned

You now have a reusable, typed foundation for frontend state management, API calls, and WebSocket messaging that matches the backend endpoints and supports the real-time chat UI.
