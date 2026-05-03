# Lesson: Connecting to STOMP at Layout Level for Early Connection Establishment

## Task Context

In a real-time chat application, establishing a WebSocket connection takes time. If we wait until the user enters a specific chat room to connect, they'll experience a delay before they can send or receive messages. 

**Task 31.1** requires us to move the STOMP connection logic from the individual chat room page to the chat layout component. This ensures that:

1. The connection is established as soon as the user enters the chat area (after authentication)
2. The connection is ready before the user navigates to any specific room
3. The connection status is visible across all chat pages
4. Connection errors can be handled centrally with a retry mechanism

This is a common pattern in real-time applications: establish the transport layer early, then use it throughout the application.

## Files Modified

- `frontend/app/chat/layout.tsx` (modified) - Added STOMP connection logic, connection status indicator, and error handling
- `frontend/app/chat/[roomId]/page.tsx` (modified) - Removed duplicate connection logic and status indicator

## Step-by-Step Changes

### Step 1: Import Connection Store in Layout

First, we imported the `useConnectionStore` hook in the chat layout component:

```typescript
import { useConnectionStore } from '@/lib/store/connectionStore';
```

This gives us access to:
- `connected` - boolean indicating if STOMP is connected
- `connecting` - boolean indicating if connection is in progress
- `error` - string with error message if connection failed
- `connect(token)` - function to establish connection
- `disconnect()` - function to close connection

### Step 2: Add Connection Effect Hook

We added a `useEffect` hook that runs when the user is authenticated:

```typescript
useEffect(() => {
  if (isAuthenticated && token && !connected && !connecting) {
    connect(token);
  }

  return () => {
    if (connected) {
      disconnect();
    }
  };
}, [isAuthenticated, token, connected, connecting, connect, disconnect]);
```

**What this does:**
- **Condition check**: Only connects if user is authenticated, has a token, and isn't already connected/connecting
- **Connect call**: Passes the JWT token to the STOMP client for authentication
- **Cleanup function**: Disconnects when the user leaves the chat area (navigates away from `/chat/*` routes)

**Why the dependencies?**
- `isAuthenticated` and `token` - trigger connection when user logs in
- `connected` and `connecting` - prevent duplicate connection attempts
- `connect` and `disconnect` - stable references from Zustand store

### Step 3: Add Connection Status Indicator

We added a visual indicator in the header showing the connection state:

```typescript
<div
  className={`flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium ${
    connected
      ? 'bg-green-100 text-green-800'
      : connecting
      ? 'bg-yellow-100 text-yellow-800'
      : 'bg-red-100 text-red-800'
  }`}
  role="status"
  aria-live="polite"
>
  <div
    className={`w-2 h-2 rounded-full ${
      connected
        ? 'bg-green-500'
        : connecting
        ? 'bg-yellow-500 animate-pulse'
        : 'bg-red-500'
    }`}
    aria-hidden="true"
  />
  <span>
    {connected ? 'Connected' : connecting ? 'Connecting...' : 'Disconnected'}
  </span>
</div>
```

**Visual states:**
- **Green** - Connected successfully
- **Yellow (pulsing)** - Connection in progress
- **Red** - Disconnected or failed

**Accessibility:**
- `role="status"` - Identifies this as a status indicator
- `aria-live="polite"` - Screen readers announce changes when idle
- `aria-hidden="true"` on the dot - Hides decorative element from screen readers

### Step 4: Add Error Banner with Retry

We added an error banner that appears when connection fails:

```typescript
{error && (
  <div className="bg-red-50 border-l-4 border-red-500 p-4 mx-4 mt-4" role="alert">
    <div className="flex items-center">
      <svg className="w-5 h-5 text-red-500 mr-3 shrink-0">
        {/* Error icon */}
      </svg>
      <div className="flex-1">
        <p className="text-sm font-medium text-red-800">Connection Error</p>
        <p className="text-sm text-red-700 mt-1">{error}</p>
      </div>
      <button
        onClick={() => connect(token)}
        className="ml-4 px-3 py-1 text-sm font-medium text-red-800 hover:text-red-900 hover:bg-red-100 rounded-lg"
      >
        Retry
      </button>
    </div>
  </div>
)}
```

**Features:**
- Only shows when `error` is not null
- Displays the error message from the connection store
- Provides a "Retry" button to attempt reconnection
- Uses `role="alert"` for accessibility (screen readers announce immediately)

### Step 5: Remove Duplicate Logic from Room Page

We removed the connection logic from the chat room page since it's now handled at the layout level:

**Removed:**
```typescript
// This is no longer needed
useEffect(() => {
  if (!connected && !connecting && token) {
    connect(token);
  }
}, [connected, connecting, token, connect]);
```

**Also removed:**
- The `connecting` state variable (no longer used)
- The `connect` function import (no longer called)
- The duplicate connection status indicator in the room header

**Kept:**
- The `connected` state (still needed to check if we can send messages)
- The `sendMessage` function (still needed to send messages)

## Why This Approach

### 1. **Better User Experience**

By connecting at the layout level:
- Connection happens once when entering the chat area
- Users don't wait for connection when switching rooms
- Connection is ready before they need it

### 2. **Centralized Connection Management**

Having connection logic in one place:
- Easier to debug connection issues
- Single source of truth for connection state
- Consistent error handling across all chat pages

### 3. **Proper Cleanup**

The cleanup function in `useEffect`:
- Prevents memory leaks
- Closes WebSocket when user leaves chat
- Ensures clean state when user logs out

### 4. **Separation of Concerns**

- **Layout**: Handles connection lifecycle
- **Room page**: Handles room-specific logic (messages, subscriptions)
- Each component has a clear responsibility

## Alternatives Considered

### Alternative 1: Connect on App Mount

We could connect as soon as the app loads:

```typescript
// In root layout
useEffect(() => {
  if (token) {
    connect(token);
  }
}, [token]);
```

**Why we didn't choose this:**
- Wastes resources if user doesn't visit chat
- Connection might timeout before user needs it
- Harder to manage connection lifecycle

### Alternative 2: Keep Connection in Room Page

We could leave the connection logic in each room page:

**Why we didn't choose this:**
- Reconnects when switching rooms (bad UX)
- Duplicate connection logic across pages
- Harder to show global connection status

### Alternative 3: Use a Global Connection Provider

We could create a React Context provider:

```typescript
<ConnectionProvider>
  <ChatLayout>{children}</ChatLayout>
</ConnectionProvider>
```

**Why we didn't choose this:**
- Zustand already provides global state
- Adds unnecessary complexity
- Context re-renders can be expensive

## Key Concepts

### 1. **Effect Cleanup Functions**

The return function in `useEffect` runs when:
- Component unmounts
- Dependencies change (before next effect runs)

```typescript
useEffect(() => {
  // Setup
  connect(token);
  
  return () => {
    // Cleanup
    disconnect();
  };
}, [token]);
```

### 2. **Conditional Rendering with Error States**

Using `&&` for conditional rendering:

```typescript
{error && <ErrorBanner />}
```

This only renders `ErrorBanner` when `error` is truthy (not null, undefined, or empty string).

### 3. **ARIA Live Regions**

`aria-live="polite"` tells screen readers to announce changes:
- **polite** - Announce when user is idle
- **assertive** - Announce immediately (use for critical alerts)
- **off** - Don't announce (default)

### 4. **Layout Components in Next.js**

Layout components:
- Wrap all pages in their directory
- Persist across navigation (don't remount)
- Perfect for shared state like connections

### 5. **WebSocket Connection Lifecycle**

1. **Connecting** - Establishing TCP connection, STOMP handshake
2. **Connected** - Ready to send/receive messages
3. **Disconnecting** - Graceful shutdown
4. **Disconnected** - Connection closed

## Potential Pitfalls

### Pitfall 1: Infinite Reconnection Loop

**Problem:**
```typescript
useEffect(() => {
  connect(token); // Runs every render!
}, []); // Missing dependencies
```

**Solution:**
Include all dependencies and add guards:
```typescript
useEffect(() => {
  if (isAuthenticated && token && !connected && !connecting) {
    connect(token);
  }
}, [isAuthenticated, token, connected, connecting, connect]);
```

### Pitfall 2: Memory Leaks from Missing Cleanup

**Problem:**
```typescript
useEffect(() => {
  connect(token);
  // No cleanup!
}, [token]);
```

**Solution:**
Always clean up side effects:
```typescript
useEffect(() => {
  connect(token);
  return () => disconnect();
}, [token]);
```

### Pitfall 3: Stale Token in Reconnection

**Problem:**
```typescript
// Token might be outdated when retry is clicked
<button onClick={() => connect(token)}>Retry</button>
```

**Solution:**
Get fresh token from store:
```typescript
<button onClick={() => connect(useAuthStore.getState().token)}>
  Retry
</button>
```

Or use the token from the component scope (which is already fresh from the hook).

### Pitfall 4: Race Conditions

**Problem:**
Multiple components trying to connect simultaneously:
```typescript
// Layout connects
connect(token);

// Room page also connects
connect(token);
```

**Solution:**
Guard against duplicate connections:
```typescript
if (!connected && !connecting) {
  connect(token);
}
```

### Pitfall 5: Not Handling Reconnection

**Problem:**
Connection drops, but we don't attempt to reconnect.

**Solution:**
The STOMP client has built-in reconnection:
```typescript
reconnectDelay: 5000, // Retry every 5 seconds
```

But we also provide manual retry via the error banner.

## What You Learned

1. **Early Connection Pattern**: Establish transport layers early in the component tree for better UX
2. **Layout-Level State**: Use layout components for shared state that persists across page navigation
3. **Effect Cleanup**: Always clean up side effects (connections, timers, subscriptions) in useEffect
4. **Connection State Management**: Track multiple states (connecting, connected, error) for proper UI feedback
5. **Accessibility**: Use ARIA attributes (role, aria-live) to make dynamic content accessible
6. **Error Recovery**: Provide manual retry mechanisms when automatic reconnection isn't enough
7. **Separation of Concerns**: Keep connection logic separate from business logic (messages, rooms)
8. **Conditional Rendering**: Use guards to prevent duplicate operations and render errors conditionally

This pattern is applicable to any real-time application: WebSockets, Server-Sent Events, polling, etc. The key is to establish the connection early and manage its lifecycle properly.
