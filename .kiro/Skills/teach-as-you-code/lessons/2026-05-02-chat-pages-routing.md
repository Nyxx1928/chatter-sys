# Lesson: Creating Chat Pages with Authentication Protection

## Task Context

This lesson covers Task 29.3 from the realtime-chat-system spec: creating the chat page structure for a Next.js application. The task involves building three key pages:

1. **Chat Layout** (`app/chat/layout.tsx`) - Provides authentication protection and common UI for all chat pages
2. **Room List Page** (`app/chat/page.tsx`) - Displays available chat rooms
3. **Chat Room Page** (`app/chat/[roomId]/page.tsx`) - The main chat interface with messages, input, and user list

This task integrates all the previously built chat components (MessageList, MessageInput, UserList, RoomSelector) into a complete, protected chat experience.

**Requirements addressed:**
- 5.1: Chat room management with STOMP subscriptions
- 14.2: Frontend STOMP client integration
- 15.1-15.4: Frontend user interface components

## Files Modified

- `frontend/app/chat/layout.tsx` (created)
- `frontend/app/chat/page.tsx` (created)
- `frontend/app/chat/[roomId]/page.tsx` (created)

## Step-by-Step Changes

### Step 1: Create Chat Layout with Authentication Protection

The chat layout serves as a wrapper for all chat pages and implements authentication protection. This is a critical security feature that ensures only authenticated users can access chat functionality.

**Key features implemented:**
- **Authentication check**: Uses `useAuthStore` to check if user is authenticated
- **Automatic redirect**: Redirects unauthenticated users to `/auth/login`
- **Loading state**: Shows a spinner while checking authentication
- **Common header**: Provides user info and logout button across all chat pages
- **Layout structure**: Sets up the full-height layout for chat pages

**Why use a layout for authentication?**
In Next.js App Router, layouts wrap all child pages. By placing authentication logic in the layout, we ensure:
- All chat pages are automatically protected
- No need to duplicate authentication checks in each page
- Consistent UI structure across all chat pages
- Single source of truth for authentication state

### Step 2: Create Room List Page

The room list page displays all available chat rooms and allows users to navigate to specific rooms.

**Key features implemented:**
- **Data fetching**: Loads rooms from the API using `listRooms(token)`
- **Loading state**: Shows spinner while fetching data
- **Error handling**: Displays error message with retry button
- **Room display**: Uses the `RoomSelector` component to show rooms
- **Navigation**: Clicking a room navigates to `/chat/[roomId]`
- **Refresh functionality**: Allows users to manually refresh the room list

**Data flow:**
1. Component mounts → triggers `loadRooms()`
2. `loadRooms()` calls API with authentication token
3. API returns list of `ChatRoom` objects
4. State updates → component re-renders with rooms
5. User clicks room → navigates to room page

### Step 3: Create Individual Chat Room Page

This is the most complex page, integrating multiple components and real-time functionality.

**Key features implemented:**

**A. Data Loading:**
- Loads room details, message history, and members in parallel using `Promise.all`
- Uses pagination for message history (50 messages initially)
- Handles loading and error states

**B. STOMP Connection:**
- Automatically connects to STOMP server on mount
- Uses `useConnectionStore` to manage connection state
- Shows connection status indicator (connected/connecting/disconnected)

**C. Real-Time Subscriptions:**
- **Message subscription**: Subscribes to `/topic/room/{roomId}` to receive new messages
- **Presence subscription**: Subscribes to `/topic/presence/{roomId}` to receive user online/offline updates
- Uses `useStompSubscription` hook for type-safe subscriptions
- Automatically unsubscribes when component unmounts

**D. Message Sending:**
- Captures user input from `MessageInput` component
- Sends message via STOMP to `/app/chat.send/{roomId}`
- Includes sender info and timestamp in message payload
- Disables input when disconnected

**E. Responsive Layout:**
- **Desktop (lg+)**: Three-column layout with messages and fixed user list sidebar
- **Mobile/Tablet**: Full-width messages with modal user list
- Toggle button to show/hide user list on smaller screens
- Back button to return to room list on mobile

**F. UI Components Integration:**
- `MessageList`: Displays messages with auto-scroll
- `MessageInput`: Handles message composition
- `UserList`: Shows online/offline members
- All components receive appropriate props and callbacks

## Why This Approach

### 1. Layout-Based Authentication Protection

**Why use a layout instead of protecting each page individually?**

Using a layout for authentication provides several benefits:
- **DRY principle**: Authentication logic written once, applied to all child pages
- **Consistency**: All chat pages have the same authentication behavior
- **Maintainability**: Changes to authentication logic only need to be made in one place
- **Performance**: Authentication check happens at the layout level before rendering child pages

**Alternative considered:** Client-side middleware or protecting each page individually would require more code duplication and increase the risk of forgetting to protect a page.

### 2. Parallel Data Loading

**Why use `Promise.all` to load room data?**

```typescript
const [roomDetails, messageHistory, roomMembers] = await Promise.all([
  getRoomDetails(token, parseInt(roomId)),
  getMessageHistory(token, parseInt(roomId), { page: 0, size: 50 }),
  getRoomMembers(token, parseInt(roomId))
]);
```

This approach:
- **Faster loading**: All three API calls happen simultaneously instead of sequentially
- **Better UX**: User sees content sooner
- **Simpler code**: Single loading state for all data

**Sequential loading would take:** Time(A) + Time(B) + Time(C)
**Parallel loading takes:** Max(Time(A), Time(B), Time(C))

### 3. Automatic STOMP Connection

**Why connect to STOMP in the room page instead of the layout?**

The connection is established in the room page because:
- **Lazy connection**: Only connect when user actually enters a chat room
- **Resource efficiency**: Don't maintain WebSocket connection on room list page
- **Clear lifecycle**: Connection tied to room page lifecycle

The connection store prevents duplicate connections with the `connecting` flag.

### 4. Responsive User List

**Why use a modal on mobile instead of always showing the user list?**

Mobile screens have limited space. The modal approach:
- **Maximizes message area**: Full width for messages on mobile
- **On-demand access**: Users can view members when needed
- **Native feel**: Modal pattern is familiar on mobile devices
- **Desktop advantage**: Fixed sidebar on desktop for constant visibility

### 5. Type-Safe Subscriptions

**Why use the `useStompSubscription` hook?**

The custom hook provides:
- **Type safety**: Generic type parameter ensures correct message types
- **Automatic cleanup**: Unsubscribes when component unmounts or dependencies change
- **Reusability**: Same hook for all subscription types
- **Error handling**: Gracefully handles malformed messages

## Alternatives Considered

### Alternative 1: Server-Side Authentication Check

**Considered:** Using Next.js middleware or server components for authentication.

**Why not chosen:**
- Chat pages need client-side interactivity (STOMP, real-time updates)
- Client components can't use server-side authentication directly
- Would require additional API calls to verify authentication
- Current approach is simpler and more direct for client-heavy pages

**When to use server-side auth:** For pages that can be fully server-rendered and don't need real-time features.

### Alternative 2: Single Chat Page with Room Selector

**Considered:** One page with room selector sidebar instead of separate room list page.

**Why not chosen:**
- Mobile experience would be cramped with both room list and messages
- Harder to implement responsive design
- Less clear navigation flow
- Current approach follows common chat app patterns (Slack, Discord)

**When to use single page:** For desktop-only applications or when room switching needs to be instant.

### Alternative 3: Optimistic UI Updates for Messages

**Considered:** Immediately showing sent messages before server confirmation.

**Why not chosen:**
- Adds complexity for handling send failures
- Need to track pending messages and reconcile with server
- Current approach is simpler and sufficient for learning project
- Real-time broadcast is fast enough (< 100ms requirement)

**When to use optimistic updates:** For production apps where perceived performance is critical, or when network latency is high.

### Alternative 4: Infinite Scroll for Message History

**Considered:** Loading more messages as user scrolls up.

**Why not chosen:**
- Adds complexity to message list component
- Initial 50 messages sufficient for learning project
- Would need to handle scroll position preservation
- Can be added later as enhancement

**When to use infinite scroll:** For production apps with long message histories, or when initial load time is a concern.

## Key Concepts

### 1. Next.js App Router Layouts

Layouts in Next.js App Router wrap all child pages and persist across navigation:

```typescript
// layout.tsx wraps all pages in the same directory
export default function ChatLayout({ children }) {
  return (
    <div>
      <Header />
      {children}  {/* Child pages render here */}
      <Footer />
    </div>
  );
}
```

**Key characteristics:**
- Layouts don't re-render when navigating between child pages
- Perfect for shared UI and authentication checks
- Can be nested (root layout → chat layout → room layout)

### 2. Dynamic Routes in Next.js

The `[roomId]` folder creates a dynamic route:

```
app/chat/[roomId]/page.tsx → /chat/1, /chat/2, /chat/123
```

Access the parameter with `useParams()`:

```typescript
const params = useParams();
const roomId = params.roomId; // "1", "2", "123", etc.
```

### 3. STOMP Pub/Sub Pattern

STOMP uses a publish/subscribe pattern for real-time messaging:

**Publishing (sending):**
```typescript
client.publish({
  destination: '/app/chat.send/1',  // Application destination
  body: JSON.stringify(message)
});
```

**Subscribing (receiving):**
```typescript
client.subscribe('/topic/room/1', (message) => {
  // Handle received message
});
```

**Topic structure:**
- `/app/*` - Client sends to server (application destinations)
- `/topic/*` - Server broadcasts to all subscribers (pub/sub)
- `/user/*` - Server sends to specific user (point-to-point)

### 4. React State Management for Real-Time Data

Managing real-time data requires careful state updates:

```typescript
// Adding new messages (append)
setMessages((prev) => [...prev, newMessage]);

// Updating user presence (map and replace)
setMembers((prev) =>
  prev.map((member) =>
    member.id === update.userId
      ? { ...member, online: update.online }
      : member
  )
);
```

**Why use functional updates?**
- Ensures you're working with the latest state
- Prevents race conditions with rapid updates
- Required when new state depends on previous state

### 5. Authentication Flow in Client Components

Client components can't directly access server-side auth, so we use:

1. **Token storage**: JWT token stored in localStorage
2. **State management**: Zustand store provides token to components
3. **API calls**: Token passed as header in all API requests
4. **STOMP connection**: Token passed in STOMP connect headers

This creates a complete authentication flow from login to real-time messaging.

## Potential Pitfalls

### Pitfall 1: Forgetting to Pass Token to API Calls

**Problem:**
```typescript
const rooms = await listRooms();  // ❌ Missing token!
```

**Solution:**
```typescript
const { token } = useAuthStore();
const rooms = await listRooms(token);  // ✅ Token included
```

**Why it matters:** API calls without tokens will fail with 401 Unauthorized.

### Pitfall 2: Not Handling Disconnected State

**Problem:** Allowing users to send messages when disconnected leads to confusion.

**Solution:**
```typescript
<MessageInput
  onSend={handleSendMessage}
  disabled={!connected}  // ✅ Disable when disconnected
  placeholder={connected ? 'Type a message...' : 'Connecting...'}
/>
```

**Why it matters:** Users need clear feedback about connection status.

### Pitfall 3: Memory Leaks from Subscriptions

**Problem:** Not unsubscribing from STOMP topics when component unmounts.

**Solution:** The `useStompSubscription` hook automatically handles cleanup:

```typescript
useEffect(() => {
  const subscription = client.subscribe(destination, handler);
  return () => subscription.unsubscribe();  // ✅ Cleanup
}, [client, destination]);
```

**Why it matters:** Unclean subscriptions can cause duplicate messages and memory leaks.

### Pitfall 4: Race Conditions in Parallel Loading

**Problem:** State updates from parallel API calls can arrive in any order.

**Solution:** Use `Promise.all` and update all state together:

```typescript
const [roomDetails, messages, members] = await Promise.all([...]);
// All data loaded, now update state together
setRoom(roomDetails);
setMessages(messages.content);
setMembers(members);
```

**Why it matters:** Prevents partial UI updates and inconsistent state.

### Pitfall 5: Not Converting roomId to Number

**Problem:**
```typescript
const roomId = params.roomId;  // This is a string!
await getRoomDetails(token, roomId);  // ❌ Type error
```

**Solution:**
```typescript
const roomId = params.roomId;
await getRoomDetails(token, parseInt(roomId));  // ✅ Convert to number
```

**Why it matters:** API expects number, route params are always strings.

### Pitfall 6: Infinite Re-render Loops

**Problem:** Not memoizing callback functions passed to subscriptions:

```typescript
// ❌ Creates new function on every render
useStompSubscription('/topic/room/1', (msg) => {
  setMessages((prev) => [...prev, msg]);
});
```

**Solution:**
```typescript
// ✅ Memoized callback
const handleNewMessage = useCallback((msg) => {
  setMessages((prev) => [...prev, msg]);
}, []);

useStompSubscription('/topic/room/1', handleNewMessage);
```

**Why it matters:** New function reference causes subscription to re-subscribe on every render.

### Pitfall 7: Showing Stale Data After Navigation

**Problem:** Navigating between rooms without clearing previous room's data.

**Solution:** Load fresh data in `useEffect` that depends on `roomId`:

```typescript
useEffect(() => {
  loadRoomData();  // Loads fresh data for current roomId
}, [roomId, token]);
```

**Why it matters:** Users would see messages from previous room briefly.

## What You Learned

### Core Concepts

1. **Next.js App Router Layouts**: How to use layouts for shared UI and authentication protection
2. **Dynamic Routes**: Creating and using parameterized routes with `[roomId]`
3. **Authentication Protection**: Client-side authentication checks with automatic redirects
4. **STOMP Integration**: Subscribing to topics and sending messages in React components
5. **Responsive Design**: Building layouts that adapt from mobile to desktop

### React Patterns

1. **Parallel Data Loading**: Using `Promise.all` for faster initial load
2. **Real-Time State Updates**: Managing state that updates from WebSocket subscriptions
3. **Conditional Rendering**: Showing different UI based on loading, error, and success states
4. **Callback Memoization**: Using `useCallback` to prevent unnecessary re-renders
5. **Effect Dependencies**: Properly managing `useEffect` dependencies for data loading

### Architecture Decisions

1. **Layout-based protection** is more maintainable than per-page protection
2. **Parallel loading** improves perceived performance significantly
3. **Responsive modals** provide better mobile UX than always-visible sidebars
4. **Type-safe subscriptions** prevent runtime errors in real-time features
5. **Connection state management** provides clear feedback to users

### Real-World Applications

This pattern of protected, real-time pages applies to:
- **Chat applications**: Slack, Discord, Teams
- **Collaborative tools**: Google Docs, Figma, Miro
- **Live dashboards**: Analytics, monitoring, trading platforms
- **Gaming**: Multiplayer lobbies, live scoreboards
- **Social features**: Live comments, notifications, activity feeds

The authentication + real-time combination is fundamental to modern web applications.

### Next Steps

To extend this implementation, you could:
1. Add infinite scroll for message history
2. Implement typing indicators
3. Add message reactions or threading
4. Support file uploads and media messages
5. Add room creation UI
6. Implement user search and direct messages
7. Add notification sounds for new messages
8. Support message editing and deletion
