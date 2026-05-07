# Bug Fixes: Membership Error & Hydration Error

## Issues Fixed

### 1. "User is not a member of this room" Connection Error

**Root Cause:**
The `room.leave` STOMP handler was calling `chatRoomService.removeMember()`, which permanently deleted the user's room membership when they navigated away from a room. When they returned and tried to send a message, there was a race condition where `sendMessage` could fire before the `room.join` STOMP message completed, resulting in the "User is not a member of this chat room" error.

**Solution:**
Modified `ChatMessageController.leaveRoom()` to only broadcast the LEAVE system message without removing membership. Room membership now persists across navigation, preventing the race condition.

**Changed File:**
- `src/main/java/org/example/chat/controller/ChatMessageController.java`

**Rationale:**
- Navigating away from a room is not the same as explicitly leaving/exiting a room
- Users should remain members of rooms they've joined until they explicitly leave
- This matches the behavior of modern chat applications (Discord, Slack, etc.)
- Prevents race conditions between STOMP message handlers

### 2. React Hydration Error: Nested Buttons

**Root Cause:**
In `RoomSelector.tsx`, the delete button was nested inside the room select button:
```tsx
<button onClick={onRoomSelect}>
  {/* room content */}
  <button onClick={onRoomDelete}> {/* ❌ Invalid nesting */}
    Delete
  </button>
</button>
```

HTML spec forbids `<button>` elements from being descendants of other `<button>` elements, causing a hydration mismatch between server and client rendering.

**Solution:**
Restructured the room item to use absolute positioning for the delete button:
- Wrapped the `<li>` with `position: relative`
- Made the room select button full-width
- Positioned the delete button absolutely on top (with `z-index: 10`)
- Added a spacer div inside the select button to prevent content overlap

**Changed File:**
- `frontend/components/chat/RoomSelector.tsx`

### 3. Button Text: Removed "New" Label

**Issue:**
The create room button showed "+ New" which was redundant.

**Solution:**
Changed button text to just "+" with an `aria-label="Create new room"` for accessibility.

**Changed File:**
- `frontend/app/chat/page.tsx`

## Testing Recommendations

1. **Membership Persistence:**
   - Navigate to a room
   - Send a message (should work)
   - Navigate away and back
   - Send another message immediately (should work without error)

2. **Hydration:**
   - Check browser console for hydration warnings (should be none)
   - Verify delete button works correctly
   - Verify room selection works correctly

3. **STOMP Messages:**
   - Verify JOIN messages still appear when entering a room
   - Verify LEAVE messages still appear when leaving a room
   - Verify membership persists across navigation

## Related Tests to Update

The following test may need updating since it expects membership removal:
- `ChatMessageControllerTest.leaveRoom_ValidUser_RemovesFromRoomAndBroadcasts`

This test should be renamed and updated to verify that:
1. The LEAVE message is broadcast
2. Membership is **not** removed
