# Lesson: STOMP Real-Time Integration for Chat Rooms

## Task Context

This lesson covers completing the real-time integration for the chat system by implementing STOMP subscriptions for room messages, presence updates, message sending, and room join/leave functionality. The goal is to enable seamless real-time communication between users in chat rooms using the STOMP protocol over WebSocket.

**Task 31: Integrate STOMP subscriptions in chat pages**
- 31.1: Connect to STOMP on authentication ✓ (already implemented)
- 31.2: Subscribe to room messages ✓ (already implemented)
- 31.3: Subscribe to presence updates ✓ (already implemented)
- 31.4: Implement message sending ✓ (already implemented)
- 31.5: Implement room join/leave (completed in this lesson)

## Files Modified

- `frontend/app/chat/[roomId]/page.tsx` (modified) - Added room join/leave STOMP messages
- `frontend/lib/store/connectionStore.ts` (modified) - Added error queue subscription

## Step-by-Step Changes

### Step 1: Understanding the Current State

The frontend already had most of the real-time integration implemented:
- STOMP connection established in chat layout when user authenticates
- Subscription to `/topic/room/{roomId}` for receiving messages
- Subscription to `/topic/presence/{roomId}` for presence updates
- Message sending via `/app/chat.send/{roomId}`

What was missing:
- Sending join/leave messages when entering/exiting rooms
- Subscribing to the error queue for server-side error notifications

### Step 2: Adding Room Join/Leave Messages

We modified the `useEffect` hook in the chat room page to send STOMP messages when:
1. **Joining a room**: When the component mounts and the user is connected
2. **Leaving a room**: When the component unmounts (cleanup function)

```typescript
useEffect(() => {
  loadRoomData();
  
  // Send room join message when connected
  if (connected && user) {
    sendMessage(`/app/room.join/${roomId}`, {});
  }

  // Send room leave message on unmount
  return () => {
    if (connected && user) {
      sendMessage(`/app/room.leave/${roomId}`, {});
    }
  };
}, [roomId, token, connected, user, sendMessage]);
```

The backend handles these messages by:
- Adding the user to the room membership
- Broadcasting a JOIN/LEAVE system message to all room subscribers
- Other users see "User X joined the room" or "User X left the room"

### Step 3: Adding Error Queue Subscription

We enhanced the connection store to subscribe to the user-specific error queue when the STOMP connection is established:

```typescript
onConnect: () => {
  set({ connected: true, connecting: false, error: null });
  
  // Subscribe to user-specific error queue
  client.subscribe('/user/queue/errors', (message) => {
    try {
      const errorData = JSON.parse(message.body);
      set({ error: errorData.message || 'An error occurred' });
    } catch {
      set({ error: 'An error occurred' });
    }
  });
}
```

This allows the backend to send error messages directly to specific users when:
- Message validation fails
- User tries to send to a room they're not a member of
- Any other STOMP message processing error occurs

## Why This Approach

### Room Join/Leave in useEffect

We use React's `useEffect` hook with a cleanup function because:
1. **Automatic lifecycle management**: Join when entering, leave when exiting
2. **Dependency tracking**: Re-join if roomId changes (user switches rooms)
3. **Cleanup guarantee**: React ensures cleanup runs before unmount
4. **Conditional execution**: Only sends messages when connected and authenticated

### Error Queue Subscription

We subscribe to the error queue in the `onConnect` callback because:
1. **User-specific errors**: Each user has their own `/user/queue/errors` destination
2. **Centralized error handling**: All STOMP errors flow through one channel
3. **State integration**: Errors update the connection store state, triggering UI updates
4. **Graceful degradation**: If parsing fails, we still show a generic error message

## Alternatives Considered

### Alternative 1: Manual Join/Leave Buttons

We could have required users to explicitly click "Join Room" and "Leave Room" buttons.

**Pros:**
- More explicit user control
- Clearer user intent

**Cons:**
- Extra friction in the user experience
- Users might forget to leave rooms
- More UI clutter
- Not standard for modern chat applications

**Why we didn't choose this:** Modern chat apps (Slack, Discord, Teams) automatically handle room membership when you navigate to a room. This provides a smoother, more intuitive experience.

### Alternative 2: HTTP REST API for Join/Leave

We could have used REST endpoints instead of STOMP messages for join/leave operations.

**Pros:**
- Simpler error handling (HTTP status codes)
- Easier to test with standard HTTP tools
- No dependency on WebSocket connection

**Cons:**
- Requires two separate connections (HTTP + WebSocket)
- Less real-time (no immediate notification to other users)
- Inconsistent with the rest of the real-time architecture
- More complex state synchronization

**Why we didn't choose this:** Using STOMP for join/leave keeps all real-time operations on the same protocol, simplifies the architecture, and ensures immediate notification to all room members.

### Alternative 3: Polling for Errors

We could have polled a REST endpoint periodically to check for errors.

**Pros:**
- Works without WebSocket
- Simpler implementation

**Cons:**
- Delayed error notification (polling interval)
- Unnecessary server load (constant polling)
- Wastes bandwidth
- Not real-time

**Why we didn't choose this:** Since we already have a persistent STOMP connection, using it for error delivery is more efficient and provides instant feedback.

## Key Concepts

### 1. STOMP Protocol Destinations

STOMP uses different destination patterns for different purposes:

- **Application destinations** (`/app/*`): Client sends to server
  - `/app/chat.send/{roomId}` - Send a message
  - `/app/room.join/{roomId}` - Join a room
  - `/app/room.leave/{roomId}` - Leave a room

- **Topic destinations** (`/topic/*`): Server broadcasts to all subscribers
  - `/topic/room/{roomId}` - Room messages
  - `/topic/presence/{roomId}` - Presence updates

- **User queue destinations** (`/user/queue/*`): Server sends to specific user
  - `/user/queue/errors` - User-specific errors

### 2. React useEffect Cleanup

The cleanup function in `useEffect` is crucial for preventing memory leaks and ensuring proper resource cleanup:

```typescript
useEffect(() => {
  // Setup code runs when dependencies change
  
  return () => {
    // Cleanup code runs before next effect or unmount
  };
}, [dependencies]);
```

In our case, the cleanup function sends the leave message, ensuring users are properly removed from rooms even if they close the browser tab or navigate away.

### 3. Pub/Sub Pattern

The STOMP broker implements a publish/subscribe pattern:

1. **Subscribe**: Clients register interest in a topic
2. **Publish**: Server sends messages to a topic
3. **Broadcast**: All subscribers receive the message

This decouples senders from receivers - the sender doesn't need to know who's listening, and subscribers don't need to know who's sending.

### 4. System Messages

System messages (JOIN, LEAVE) are different from user messages:
- Generated by the server, not sent by users
- Have a special `messageType` field
- Provide context about room events
- Can be styled differently in the UI

## Potential Pitfalls

### Pitfall 1: Sending Messages Before Connection

**Problem:** If you try to send a join message before the STOMP connection is established, it will fail silently or throw an error.

**Solution:** Always check the `connected` state before sending messages:

```typescript
if (connected && user) {
  sendMessage(`/app/room.join/${roomId}`, {});
}
```

### Pitfall 2: Memory Leaks from Subscriptions

**Problem:** If you don't unsubscribe from STOMP topics, you'll create memory leaks and receive duplicate messages.

**Solution:** The `useStompSubscription` hook automatically handles unsubscription in its cleanup function. Always use this hook instead of subscribing manually.

### Pitfall 3: Race Conditions with Multiple Rooms

**Problem:** If a user quickly switches between rooms, join/leave messages might arrive out of order.

**Solution:** The `useEffect` dependency array includes `roomId`, so React will:
1. Run cleanup (send leave for old room)
2. Run setup (send join for new room)

This ensures proper ordering.

### Pitfall 4: Error Queue Not Subscribed

**Problem:** If you don't subscribe to `/user/queue/errors`, server-side errors will be lost and users won't know what went wrong.

**Solution:** Subscribe to the error queue immediately after connection in the `onConnect` callback, before any other operations.

### Pitfall 5: Stale Closures in useEffect

**Problem:** The cleanup function might capture stale values of `connected`, `user`, or `sendMessage` if dependencies aren't properly specified.

**Solution:** Include all used variables in the dependency array. In our case, we include `roomId`, `token`, `connected`, `user`, and `sendMessage`.

## What You Learned

1. **STOMP Lifecycle Management**: How to properly send join/leave messages when entering and exiting chat rooms using React's useEffect cleanup pattern.

2. **Error Handling in Real-Time Systems**: How to subscribe to user-specific error queues to receive server-side error notifications through the WebSocket connection.

3. **Pub/Sub Architecture**: How the STOMP broker routes messages between application destinations (client → server), topic destinations (server → all subscribers), and user queue destinations (server → specific user).

4. **React Effect Cleanup**: How to use useEffect cleanup functions to ensure resources are properly released when components unmount or dependencies change.

5. **System Messages**: How to distinguish between user-generated messages and system-generated events (JOIN, LEAVE) in a chat application.

6. **Conditional Message Sending**: How to guard STOMP operations with connection state checks to prevent errors when the WebSocket is not yet established.

7. **Real-Time State Synchronization**: How to keep the UI in sync with server state by subscribing to multiple STOMP topics (messages, presence, errors) simultaneously.

The real-time integration is now complete! Users can join rooms, send messages, see presence updates, and receive error notifications - all through the persistent STOMP connection.
