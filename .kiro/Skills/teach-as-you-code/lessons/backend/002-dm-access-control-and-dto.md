# Lesson: DM Access Control Guards, DTO Update, and Frontend DM Rendering (Tasks 6–12)

## Task Context

Tasks 6–12 of the direct-messaging spec complete the backend protection layer and the entire frontend rendering path for DM rooms. After tasks 1–5 created the `RoomType` enum and `DirectMessageService`, these tasks ensure DM rooms cannot be accidentally deleted or have members invited, expose `roomType` through the API, and update the frontend to display DM rooms with a person icon, the other participant's name, and no invite/delete controls.

## Files Modified

- `src/main/java/org/example/chat/service/ChatRoomService.java` (modified)
- `src/main/java/org/example/chat/controller/ChatRoomController.java` (modified)
- `src/main/java/org/example/chat/dto/ChatRoomResponse.java` (modified)
- `frontend/types/domain.ts` (modified)
- `frontend/components/chat/RoomSelector.tsx` (modified)
- `frontend/app/chat/page.tsx` (modified)
- `frontend/components/chat/FriendsPanel.tsx` (modified)
- `frontend/app/chat/contacts/page.tsx` (modified)
- `frontend/app/chat-demo/page.tsx` (modified)
- `.kiro/specs/direct-messaging/tasks.md` (modified)

## Step-by-Step Changes

### Step 1 — Guard `deleteRoom` in `ChatRoomService` (Task 6.1)

Before the existing role check, we added a guard that throws `UnauthorizedException` if the room is a `DIRECT` room:

```java
if (room.getRoomType() == RoomType.DIRECT) {
    throw new UnauthorizedException("Cannot delete a direct message room");
}
```

This runs before the membership/role check, so the error message is clear and specific.

### Step 2 — Guard `addMember` in `ChatRoomService` (Task 6.2)

Same pattern in `addMember`, right after loading the room:

```java
if (room.getRoomType() == RoomType.DIRECT) {
    throw new UnauthorizedException("Cannot invite users to a direct message room");
}
```

We also added `import org.example.chat.entity.RoomType;` to clean up the FQN references.

### Step 3 — Guard `inviteToRoom` in `ChatRoomController` (Task 6.3)

The controller's `inviteToRoom` endpoint already loaded the room to check membership. We added the DM guard right after that membership check:

```java
if (chatRoom.getRoomType() == RoomType.DIRECT) {
    throw new UnauthorizedException("Cannot invite users to a direct message room");
}
```

This gives defence-in-depth: the service layer also guards, but the controller rejects early with a clear 403.

### Step 4 — Add `roomType` to `ChatRoomResponse` DTO (Task 7)

Added a `RoomType roomType` field and updated the `from(ChatRoom)` factory to include it:

```java
return new ChatRoomResponse(
    chatRoom.getId(),
    chatRoom.getName(),
    chatRoom.getDescription(),
    chatRoom.getCreatedAt(),
    createdByResponse,
    chatRoom.getRoomType()   // ← new
);
```

The `@AllArgsConstructor` from Lombok picks this up automatically.

### Step 5 — Update TypeScript types (Task 9)

In `frontend/types/domain.ts`:

- Added `export type RoomType = 'GROUP' | 'DIRECT';`
- Added `roomType: RoomType` (required) to `ChatRoom`
- Added `otherParticipant?: PublicUser` (optional, client-side only) to `ChatRoom`
- Added `dmRoomId: number` to `Friendship`

The `otherParticipant` field is never sent by the server — it's derived on the client after loading room members.

### Step 6 — Update `RoomSelector` for DM rendering (Task 10)

Three changes in `renderRoom`:

1. **Label**: `roomLabel` is `otherParticipant.displayName` for DIRECT rooms, `room.name` for GROUP rooms.
2. **Avatar**: DIRECT rooms show a person SVG icon instead of the first letter.
3. **Delete button**: `showDelete` now also checks `!isDirect`, so the trash icon never appears on DM rooms.
4. **ARIA label**: Added `aria-label="Direct message with {name}"` on the button for DM rooms.

### Step 7 — Update `chat/page.tsx` for DM handling (Task 11)

**11.1 — Derive `otherParticipant`**: After `getRoomMembers` resolves, if `roomDetails.roomType === 'DIRECT'`, find the member whose `id !== user.id` and spread it as `otherParticipant` onto the room object before calling `setSelectedRoom`.

**11.2 — Chat header**: The room avatar now conditionally renders a person icon for DIRECT rooms. The `<h2>` shows `otherParticipant.displayName` for DIRECT rooms.

**11.3 — Suppress controls**:
- Invite button is wrapped in `{selectedRoom.roomType !== 'DIRECT' && (...)}`.
- Delete button condition updated to `user?.id === selectedRoom.createdBy?.id && selectedRoom.roomType !== 'DIRECT'`.
- `canDeleteRoom` prop on `RoomSelector` updated to `(room) => room.roomType !== 'DIRECT' && room.createdBy?.id === user?.id`.

### Step 8 — `FriendsPanel` callback (Task 12)

`FriendsPanel` now accepts an optional `onDmRoomCreated?: (dmRoomId: number) => void` prop. After `acceptFriendRequest` succeeds, it calls the callback with `friendship.dmRoomId`.

In `contacts/page.tsx`, the callback navigates to `/chat?room={dmRoomId}` using `useRouter`, so the new DM room is immediately selected.

## Why This Approach

- **Defence-in-depth**: Both the service layer and the controller guard against DM room mutations. If someone calls the service directly (e.g., in tests), they still get the right error.
- **Client-side `otherParticipant`**: Keeping this field off the server avoids a new API endpoint. The members list is already loaded on room selection, so deriving the other participant is free.
- **`roomType` in DTO**: Exposing `roomType` through `ChatRoomResponse` is the minimal change needed — the frontend can now branch on it without any extra API calls.
- **Callback pattern for `FriendsPanel`**: Using a prop callback keeps `FriendsPanel` decoupled from routing. The contacts page decides what to do when a DM room is created.

## Alternatives Considered

- **Separate DM endpoint**: Could have added `/api/dm/{userId}` to open a DM. Rejected — the existing room infrastructure already handles it, and adding a new endpoint would duplicate logic.
- **Store `otherParticipant` on the server**: Would require a new DTO field or a join query. Not worth it since the client already has the members list.
- **Redirect vs. `loadRooms()`**: Could have called `loadRooms()` in the contacts page after accepting a request. Chose `router.push` instead so the user is taken directly to the new DM room.

## Key Concepts

- **Guard clauses**: Checking a precondition early and throwing immediately keeps the happy path clean and the error message specific.
- **DTO evolution**: Adding a field to a response DTO is non-breaking for existing clients that ignore unknown fields (JSON deserialization is lenient by default).
- **TypeScript discriminated unions**: `roomType: RoomType` on `ChatRoom` lets TypeScript narrow the type in conditionals (`if (room.roomType === 'DIRECT')`), giving compile-time safety.
- **Client-side derived state**: Fields like `otherParticipant` that are computed from existing data don't need to be stored or fetched separately.

## Potential Pitfalls

- **`@AllArgsConstructor` field order**: Lombok generates the constructor in field declaration order. Adding `roomType` at the end of `ChatRoomResponse` means the `from()` factory must pass it last — which it does.
- **Demo/test mock data**: Any hardcoded `ChatRoom` objects in tests or demo pages need `roomType` added. We fixed `chat-demo/page.tsx`; check any other mock data if TypeScript errors appear.
- **`otherParticipant` on re-render**: `setSelectedRoom` is called with the derived room object. If the room is re-fetched (e.g., after a STOMP update), `otherParticipant` will be lost unless the derivation runs again. This is acceptable for MVP.

## What You Learned

- How to add guard clauses to a service layer to enforce business rules on entity types.
- How to evolve a DTO by adding a field and updating its factory method.
- How to extend TypeScript interfaces with discriminated union types and optional client-side fields.
- How to conditionally render UI elements based on a room type discriminator.
- How to use a callback prop to decouple a child component from routing decisions.
