# Lesson: Integrating Real-Time Presence with Friends List

## Task Context

This lesson covers Task 12 from the social-discovery-and-room-management spec: "Integrate presence with friends list - Show online status where available."

**Requirement 3.2:** "WHEN a friend is online, THE system SHALL show an online indicator."

The challenge was to display real-time online/offline status for friends in the FriendsPanel component. The system already had:
- A backend presence system that broadcasts user online/offline status to room-specific STOMP topics (`/topic/presence/{roomId}`)
- A FriendsPanel that displays friends with static online status from the API
- STOMP WebSocket infrastructure for real-time updates

The goal was to make the online indicators update in real-time as friends come online or go offline, without requiring a manual refresh.

## Files Modified

- `frontend/lib/store/presenceStore.ts` (created)
- `frontend/lib/stomp/usePresenceSync.ts` (created)
- `frontend/components/chat/FriendsPanel.tsx` (modified)
- `frontend/app/chat/[roomId]/page.tsx` (modified)

## Step-by-Step Changes

### Step 1: Created a Global Presence Store

**File:** `frontend/lib/store/presenceStore.ts`

We created a Zustand store to track the online/offline status of all users globally. This store maintains a map of user IDs to their online status.

**Key features:**
- `presenceMap`: A record mapping userId to boolean (online status)
- `updatePresence`: Updates a single user's status
- `batchUpdatePresence`: Updates multiple users at once (useful for API responses)
- `isOnline`: Retrieves a user's online status
- `clearPresence`: Clears all presence data

**Why a global store?** The presence data needs to be shared across multiple components (FriendsPanel, chat rooms, etc.) and updated from multiple sources (API responses, STOMP messages).

### Step 2: Created Presence Sync Hooks

**File:** `frontend/lib/stomp/usePresenceSync.ts`

We created custom React hooks to subscribe to STOMP presence updates and sync them to the global presence store.

**Two hooks provided:**
1. `usePresenceSync(roomId)`: Subscribes to presence updates for a single room
2. `useMultiRoomPresenceSync(roomIds)`: Subscribes to presence updates for multiple rooms

These hooks bridge the gap between STOMP subscriptions and the global presence store.

### Step 3: Updated FriendsPanel to Use Presence Store

**File:** `frontend/components/chat/FriendsPanel.tsx`

We integrated the presence store into the FriendsPanel component:

1. **Import the presence store:**
   ```typescript
   const { presenceMap, batchUpdatePresence } = usePresenceStore();
   ```

2. **Sync API data to presence store:**
   When friends are loaded from the API, we batch update the presence store with their initial online status.

3. **Create a computed friends list:**
   ```typescript
   const friendsWithPresence = useMemo(() => {
     return friends.map((friend) => {
       const onlineStatus = presenceMap[friend.id] ?? friend.online;
       return { ...friend, online: onlineStatus };
     });
   }, [friends, presenceMap]);
   ```
   This merges the static friends list with real-time presence data. If presence data is available in the store, it takes precedence; otherwise, we fall back to the API data.

4. **Added accessibility labels:**
   ```typescript
   aria-label={friend.online ? `${friend.displayName} is online` : `${friend.displayName} is offline`}
   ```
   Screen readers now announce the online status of each friend.

5. **Updated the UI to use `friendsWithPresence`:**
   All references to `friends` in the JSX were updated to use `friendsWithPresence`.

### Step 4: Updated Chat Room Page to Sync Presence

**File:** `frontend/app/chat/[roomId]/page.tsx`

We modified the chat room page to sync presence updates to the global store:

1. **Import the presence store:**
   ```typescript
   const { updatePresence } = usePresenceStore();
   ```

2. **Update the presence handler:**
   ```typescript
   const handlePresenceUpdate = useCallback((update: { userId: number; online: boolean }) => {
     // Update local members list
     setMembers((prev) =>
       prev.map((member) =>
         member.id === update.userId
           ? { ...member, online: update.online }
           : member
       )
     );
     
     // Sync to global presence store for friends list
     updatePresence(update.userId, update.online);
   }, [updatePresence]);
   ```

Now, when a user's presence changes in any chat room, it's automatically synced to the global presence store, which updates the FriendsPanel in real-time.

## Why This Approach

### 1. Global State Management with Zustand

We chose Zustand for the presence store because:
- **Lightweight:** Minimal boilerplate compared to Redux
- **Simple API:** Easy to understand and use
- **React-friendly:** Integrates seamlessly with React hooks
- **Already in use:** The project already uses Zustand for auth and connection stores

### 2. Separation of Concerns

We separated the presence logic into three layers:
1. **Store layer** (`presenceStore.ts`): Manages the presence state
2. **Hook layer** (`usePresenceSync.ts`): Handles STOMP subscriptions
3. **Component layer** (`FriendsPanel.tsx`): Consumes and displays the data

This makes the code modular, testable, and easier to maintain.

### 3. Fallback to API Data

The computed `friendsWithPresence` list uses a fallback pattern:
```typescript
const onlineStatus = presenceMap[friend.id] ?? friend.online;
```

This ensures that:
- If real-time data is available, it's used
- If not (e.g., user hasn't joined any rooms yet), the API data is used
- The UI always shows the best available information

### 4. Syncing from Chat Rooms

By updating the global presence store from the chat room page, we ensure that:
- Presence updates are captured from all rooms the user is in
- The FriendsPanel updates automatically without needing its own STOMP subscriptions
- We avoid duplicate subscriptions and reduce WebSocket overhead

## Alternatives Considered

### Alternative 1: Direct STOMP Subscription in FriendsPanel

We could have subscribed to presence topics directly in the FriendsPanel.

**Pros:**
- Simpler, no global store needed
- Direct connection between data source and UI

**Cons:**
- Would need to subscribe to presence topics for all rooms where friends are members
- Complex subscription management (subscribing/unsubscribing as friends change)
- Duplicate subscriptions if the user is also viewing those rooms
- Doesn't scale well with many friends in many rooms

**Why we didn't choose this:** Too complex and inefficient for WebSocket connections.

### Alternative 2: Global Presence Topic

We could have asked the backend to create a global `/topic/presence` that broadcasts all presence changes.

**Pros:**
- Single subscription for all presence updates
- Simpler frontend logic

**Cons:**
- Requires backend changes
- Broadcasts presence to all connected users (privacy concern)
- Higher bandwidth usage
- Not aligned with the existing room-based presence architecture

**Why we didn't choose this:** Would require backend changes and doesn't align with the existing architecture.

### Alternative 3: Polling the Friends API

We could have periodically polled the `/api/friends` endpoint to get updated online status.

**Pros:**
- No need for WebSocket subscriptions
- Simple to implement

**Cons:**
- Not real-time (delay between status change and UI update)
- Higher server load (repeated API calls)
- Wastes bandwidth (fetching entire friends list repeatedly)
- Poor user experience (delayed updates)

**Why we didn't choose this:** Doesn't meet the real-time requirement and is inefficient.

## Key Concepts

### 1. Global State Management

Global state is shared across multiple components and persists across re-renders. In React, we use state management libraries like Zustand, Redux, or Context API to manage global state.

**When to use global state:**
- Data needs to be shared across multiple components
- Data needs to persist across navigation
- Data is updated from multiple sources

### 2. Computed Values with useMemo

`useMemo` is a React hook that memoizes (caches) the result of a computation. It only recalculates when dependencies change.

```typescript
const friendsWithPresence = useMemo(() => {
  return friends.map((friend) => {
    const onlineStatus = presenceMap[friend.id] ?? friend.online;
    return { ...friend, online: onlineStatus };
  });
}, [friends, presenceMap]);
```

**Why use useMemo here?**
- Avoids recalculating the merged list on every render
- Only recalculates when `friends` or `presenceMap` changes
- Improves performance, especially with large friends lists

### 3. Fallback Pattern with Nullish Coalescing

The `??` operator is the nullish coalescing operator. It returns the right-hand value if the left-hand value is `null` or `undefined`.

```typescript
const onlineStatus = presenceMap[friend.id] ?? friend.online;
```

This is different from `||` (logical OR), which also treats `false`, `0`, and `""` as falsy.

**Example:**
```typescript
const status1 = presenceMap[123] ?? true;  // If presenceMap[123] is false, returns false
const status2 = presenceMap[123] || true;  // If presenceMap[123] is false, returns true
```

### 4. ARIA Labels for Accessibility

ARIA (Accessible Rich Internet Applications) labels provide additional context for screen readers.

```typescript
aria-label={friend.online ? `${friend.displayName} is online` : `${friend.displayName} is offline`}
```

**Why this matters:**
- Screen readers announce the online status when focusing on a friend
- Users with visual impairments can understand the status without seeing the visual indicator
- Meets WCAG 2.1 AA accessibility standards

We also use `aria-hidden="true"` on the visual dot indicator because it's purely decorative and redundant with the text label.

### 5. Batch Updates for Performance

When loading friends from the API, we batch update the presence store:

```typescript
batchUpdatePresence(
  friendsList.map((friend) => ({
    userId: friend.id,
    online: friend.online
  }))
);
```

**Why batch?**
- Single state update instead of multiple updates
- Triggers only one re-render
- More efficient than calling `updatePresence` in a loop

## Potential Pitfalls

### 1. Stale Presence Data

**Problem:** If a user doesn't join any chat rooms, their friends' presence data won't update in real-time.

**Solution:** We use a fallback pattern (`presenceMap[friend.id] ?? friend.online`) to show API data when real-time data isn't available. The user can also click "Refresh" to get the latest data from the API.

### 2. Memory Leaks from Subscriptions

**Problem:** STOMP subscriptions that aren't cleaned up can cause memory leaks.

**Solution:** The `useStompSubscription` hook automatically unsubscribes when the component unmounts or when the destination changes. This is handled in the `useEffect` cleanup function.

### 3. Race Conditions

**Problem:** API responses and STOMP messages might arrive in different orders, causing inconsistent state.

**Solution:** We prioritize real-time data over API data using the fallback pattern. The most recent presence update (whether from API or STOMP) is always used.

### 4. Presence Store Growing Unbounded

**Problem:** The presence store could grow indefinitely as users come and go.

**Solution:** Currently, we don't implement cleanup because:
- The store only tracks users the current user has interacted with (friends, room members)
- The number of entries is bounded by the user's social graph
- The store is cleared when the user logs out (via `clearPresence`)

If this becomes an issue in the future, we could implement:
- Time-based expiration (remove entries after X minutes of inactivity)
- Size-based limits (keep only the N most recently updated entries)
- Cleanup on navigation (clear entries for users no longer visible)

### 5. Accessibility Announcement Spam

**Problem:** If presence updates rapidly, screen readers might announce too many status changes.

**Solution:** We use `aria-label` on the status badge, which only announces when the user focuses on that element. We don't use `aria-live` regions, which would announce every change automatically. This gives users control over when they hear status updates.

## What You Learned

In this lesson, you learned how to:

1. **Create a global state store with Zustand** to manage shared data across components
2. **Integrate real-time WebSocket data** with static API data using a fallback pattern
3. **Use `useMemo` to compute derived state** efficiently and avoid unnecessary recalculations
4. **Sync data from multiple sources** (API responses and STOMP messages) to a single store
5. **Add accessibility labels** to make dynamic status indicators screen-reader friendly
6. **Design a scalable presence system** that works with room-based WebSocket subscriptions
7. **Handle edge cases** like missing data, race conditions, and subscription cleanup

**Key takeaway:** When integrating real-time features, think about:
- Where the data comes from (API, WebSocket, both?)
- Where the data needs to go (which components need it?)
- How to handle missing or stale data (fallbacks, defaults)
- How to keep the UI accessible (ARIA labels, focus management)
- How to avoid performance issues (memoization, batch updates)

This pattern can be applied to other real-time features like typing indicators, read receipts, or live notifications.
