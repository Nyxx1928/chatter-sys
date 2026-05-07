# Lesson: RBAC — Room Visibility by Membership (Invite-Only)

## Task Context

The app previously showed **every room to every user**, regardless of whether they had joined it.
A new user who just registered could see (and enter) all rooms created by other users.

The goal was to implement **invite-only RBAC** so that:
- `GET /api/rooms` returns **only rooms the user is a member of**
- `GET /api/rooms/{id}` returns **403** if the user is not a member (no auto-join)
- A new `POST /api/rooms/{id}/invite?inviteeId=X` endpoint lets **existing members invite others**
- The frontend shows an **Invite Member** modal (search users → click Invite)
- Users can only enter rooms they were **invited into** or **created themselves**

---

## Files Modified

- `src/main/java/org/example/chat/controller/ChatRoomController.java` (modified)
- `src/main/java/org/example/chat/service/ChatRoomService.java` (modified)
- `frontend/lib/api/rooms.ts` (modified)
- `frontend/app/chat/page.tsx` (modified)

---

## Step-by-Step Changes

### Step 1 — Add `listRoomsForUser` to the service

`ChatRoomService` already had a `findByMembersContaining(User)` query in the repository.
We just needed to expose it as a service method:

```java
public List<ChatRoom> listRoomsForUser(User user) {
    return chatRoomRepository.findByMembersContaining(user);
}
```

The repository query uses a JPQL JOIN:
```java
@Query("SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.memberships m WHERE m.user = :user")
List<ChatRoom> findByMembersContaining(@Param("user") User user);
```

This returns only rooms where a `RoomMembership` row exists for that user.

---

### Step 2 — Change `GET /api/rooms` to filter by membership

Before:
```java
@GetMapping
public ResponseEntity<List<ChatRoomResponse>> listRooms() {
    List<ChatRoom> rooms = chatRoomService.listRooms(); // returns ALL rooms
    ...
}
```

After:
```java
@GetMapping
public ResponseEntity<List<ChatRoomResponse>> listRooms(
        @AuthenticationPrincipal UserDetails userDetails) {
    User currentUser = userRepository.findByUsername(userDetails.getUsername())...;
    List<ChatRoom> rooms = chatRoomService.listRoomsForUser(currentUser); // only MY rooms
    ...
}
```

`@AuthenticationPrincipal` injects the currently logged-in user from the JWT token.
Spring Security already validated the token in `JwtAuthenticationFilter` before this runs.

---

### Step 3 — Add `POST /api/rooms/{id}/invite`

Any existing member can invite another user by their ID:

```java
@PostMapping("/{id}/invite")
public ResponseEntity<Void> inviteToRoom(
        @PathVariable Long id,
        @RequestParam Long inviteeId,
        @AuthenticationPrincipal UserDetails userDetails) {

    // 1. Verify the inviter is a member
    roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
        .orElseThrow(() -> new UnauthorizedException("You are not a member of this room"));

    // 2. Add the invitee as MEMBER
    chatRoomService.addMember(id, inviteeId, MemberRole.MEMBER);
    return ResponseEntity.ok().build();
}
```

Key design decisions:
- **Any member can invite** (not just owners). This is a common pattern in chat apps.
- The invitee ID comes as a `@RequestParam` — simple and RESTful for a single value.
- `addMember` is idempotent — inviting someone already in the room is a no-op.

---

### Step 4 — Enforce membership on `GET /api/rooms/{id}`

Before, accessing a room auto-joined the user. Now it enforces membership:

```java
// Before (auto-join):
roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
    .orElseGet(() -> chatRoomService.addMember(...));

// After (enforce):
roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
    .orElseThrow(() -> new UnauthorizedException("You are not a member of this room"));
```

`UnauthorizedException` maps to HTTP 403 via the global exception handler.

---

### Step 5 — Frontend API

Removed `discoverRooms` and `joinRoom` (self-join is no longer allowed).
Added `inviteToRoom`:

```ts
export const inviteToRoom = async (
  token: string,
  roomId: number,
  inviteeId: number
): Promise<void> =>
  apiCall<void>(`/api/rooms/${roomId}/invite?inviteeId=${inviteeId}`, {
    method: 'POST',
    token
  });
```

---

### Step 6 — Invite Member modal in the chat page

Added a 👤+ button in the room header that opens a modal:
1. User types a name/username (min 2 chars)
2. Results come from the existing `searchUsers` API
3. Users already in the room are filtered out client-side
4. Clicking "Invite" calls `inviteToRoom`, shows "Invited ✓" on success
5. If the invited room is currently open, the member list refreshes immediately

State added:
```ts
const [showInviteModal, setShowInviteModal] = useState(false);
const [inviteRoomTarget, setInviteRoomTarget] = useState<ChatRoom | null>(null);
const [inviteSearchQuery, setInviteSearchQuery] = useState('');
const [inviteSearchResults, setInviteSearchResults] = useState<UserSearchResult[]>([]);
const [inviteSearchLoading, setInviteSearchLoading] = useState(false);
const [invitingUserId, setInvitingUserId] = useState<number | null>(null);
const [inviteSuccess, setInviteSuccess] = useState<Record<number, boolean>>({});
```

---

## Why This Approach

**Invite-only** is the correct model when rooms should be private by default.
The previous "discover and self-join" model was open — anyone could browse and enter any room.

Keeping the invite endpoint simple (`POST /invite?inviteeId=X`) avoids over-engineering.
There's no separate "invite request" entity — the invite is immediate membership.

The `searchUsers` API already existed, so the frontend reuses it rather than adding a new endpoint.

---

## Alternatives Considered

| Approach | Why not chosen |
|---|---|
| Invite request with accept/decline | More complex; better for friend requests, not room invites |
| Only owners can invite | Too restrictive for a chat app; any member should be able to grow the room |
| Invite by username string (not ID) | Requires an extra lookup; ID is more reliable |
| Keep self-join via `/discover` | Contradicts the invite-only requirement |

---

## Key Concepts

**RBAC (Role-Based Access Control)** — restricting resource access based on a user's role or membership.
Here the "role" is simply being a member of a room.

**`@AuthenticationPrincipal`** — Spring Security annotation that injects the currently authenticated
user's `UserDetails` directly into a controller method parameter.

**Idempotent membership** — `addMember` returns the existing membership if the user is already in the room,
so double-inviting is safe and doesn't throw an error.

**Membership enforcement vs. auto-join** — auto-join is convenient but breaks access control.
Explicit invite-only membership makes the data model trustworthy.

**JPQL JOIN query** — `SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.memberships m WHERE m.user = :user`
traverses the `@OneToMany` relationship from `ChatRoom` → `RoomMembership` → `User`.

---

## Potential Pitfalls

- **Existing users lose access** — if users were auto-joined before this change, they still have
  `RoomMembership` rows, so they won't lose access. New users start with zero rooms.
- **Inviting by ID requires knowing the ID** — the frontend uses `searchUsers` to resolve
  username → ID before calling the invite endpoint.
- **`UserSearchResult` wraps `user: PublicUser`** — not a flat object. Access fields via `result.user.id`,
  not `result.id`. Easy to get wrong when first reading the type.
- **`GET /api/rooms/{id}` now returns 403** — any frontend code that relied on auto-join
  (e.g., navigating directly to a room URL) will now get a 403 instead of silently joining.

---

## What You Learned

- How to enforce invite-only room access at the API layer (not just the UI)
- How `@AuthenticationPrincipal` connects Spring Security's JWT filter to controller logic
- The difference between **authentication** (who are you?) and **authorization** (what can you access?)
- How to build an invite flow that reuses an existing user search API
- How idempotent membership operations prevent errors on duplicate invites
- How to keep invite state per-user in the frontend (`inviteSuccess` map keyed by user ID)

## Task Context

The app previously showed **every room to every user**, regardless of whether they had joined it.
A new user who just registered could see (and enter) all rooms created by other users.

The goal was to implement **Role-Based Access Control (RBAC)** so that:
- `GET /api/rooms` returns **only rooms the user is a member of**
- `GET /api/rooms/{id}` returns **403** if the user is not a member (no more auto-join)
- A new `GET /api/rooms/discover` endpoint returns rooms the user has **not** joined yet
- A new `POST /api/rooms/{id}/join` endpoint lets users **explicitly join** a room
- The frontend shows a **Discover Rooms** modal with a Join button

---

## Files Modified

- `src/main/java/org/example/chat/controller/ChatRoomController.java` (modified)
- `src/main/java/org/example/chat/service/ChatRoomService.java` (modified)
- `frontend/lib/api/rooms.ts` (modified)
- `frontend/app/chat/page.tsx` (modified)

---

## Step-by-Step Changes

### Step 1 — Add `listRoomsForUser` to the service

`ChatRoomService` already had a `findByMembersContaining(User)` query in the repository.
We just needed to expose it as a service method:

```java
public List<ChatRoom> listRoomsForUser(User user) {
    return chatRoomRepository.findByMembersContaining(user);
}
```

The repository query uses a JPQL JOIN:
```java
@Query("SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.memberships m WHERE m.user = :user")
List<ChatRoom> findByMembersContaining(@Param("user") User user);
```

This returns only rooms where a `RoomMembership` row exists for that user.

---

### Step 2 — Change `GET /api/rooms` to filter by membership

Before:
```java
@GetMapping
public ResponseEntity<List<ChatRoomResponse>> listRooms() {
    List<ChatRoom> rooms = chatRoomService.listRooms(); // returns ALL rooms
    ...
}
```

After:
```java
@GetMapping
public ResponseEntity<List<ChatRoomResponse>> listRooms(
        @AuthenticationPrincipal UserDetails userDetails) {
    User currentUser = userRepository.findByUsername(userDetails.getUsername())...;
    List<ChatRoom> rooms = chatRoomService.listRoomsForUser(currentUser); // only MY rooms
    ...
}
```

`@AuthenticationPrincipal` injects the currently logged-in user from the JWT token.
Spring Security already validated the token in `JwtAuthenticationFilter` before this runs.

---

### Step 3 — Add `GET /api/rooms/discover`

This endpoint returns rooms the user has **not** joined — the "browse" view:

```java
@GetMapping("/discover")
public ResponseEntity<List<ChatRoomResponse>> discoverRooms(...) {
    List<ChatRoom> allRooms = chatRoomService.listRooms();
    List<ChatRoom> memberRooms = chatRoomService.listRoomsForUser(currentUser);
    List<Long> memberRoomIds = memberRooms.stream().map(ChatRoom::getId).toList();

    List<ChatRoomResponse> response = allRooms.stream()
        .filter(r -> !memberRoomIds.contains(r.getId()))
        .map(ChatRoomResponse::from)
        .toList();
    ...
}
```

---

### Step 4 — Add `POST /api/rooms/{id}/join`

Users now explicitly opt in to a room:

```java
@PostMapping("/{id}/join")
public ResponseEntity<ChatRoomResponse> joinRoom(@PathVariable Long id, ...) {
    chatRoomService.addMember(id, currentUser.getId(), MemberRole.MEMBER);
    ChatRoom chatRoom = chatRoomService.getRoomById(id);
    return ResponseEntity.ok(ChatRoomResponse.from(chatRoom));
}
```

`addMember` already handles the idempotency case (returns existing membership if already joined).

---

### Step 5 — Enforce membership on `GET /api/rooms/{id}`

Before, accessing a room auto-joined the user. Now it enforces membership:

```java
// Before (auto-join):
roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
    .orElseGet(() -> chatRoomService.addMember(...));

// After (enforce):
roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
    .orElseThrow(() -> new UnauthorizedException("You are not a member of this room"));
```

`UnauthorizedException` maps to HTTP 403 via the global exception handler.

---

### Step 6 — Frontend API additions

Added two new functions to `frontend/lib/api/rooms.ts`:

```ts
export const discoverRooms = async (token: string): Promise<ChatRoom[]> =>
  apiCall<ChatRoom[]>('/api/rooms/discover', { method: 'GET', token });

export const joinRoom = async (token: string, roomId: number): Promise<ChatRoom> =>
  apiCall<ChatRoom>(`/api/rooms/${roomId}/join`, { method: 'POST', token });
```

---

### Step 7 — Discover Rooms modal in the chat page

Added a 🔍 button in the room list header that opens a modal showing all joinable rooms.
Each room has a "Join" button that calls `joinRoom()`, then refreshes the member room list.

State added:
```ts
const [showDiscoverModal, setShowDiscoverModal] = useState(false);
const [discoverableRooms, setDiscoverableRooms] = useState<ChatRoom[]>([]);
const [discoverLoading, setDiscoverLoading] = useState(false);
const [joiningRoomId, setJoiningRoomId] = useState<number | null>(null);
```

---

## Why This Approach

**Membership-first filtering at the API layer** is the right place to enforce this.
Doing it only on the frontend would be insecure — anyone could call `GET /api/rooms` directly.

The existing `RoomMembership` entity and `findByMembersContaining` query were already in place,
so the backend change was minimal — just wiring them up correctly.

The **explicit join** pattern (instead of auto-join) gives users control and makes the
permission model clear: you only see what you've opted into.

---

## Alternatives Considered

| Approach | Why not chosen |
|---|---|
| Keep auto-join, just hide rooms on frontend | Insecure — API still leaks all rooms |
| Add a `public` flag to rooms | More complex; not needed for this use case |
| Role-based Spring Security `@PreAuthorize` | Overkill here; membership check is domain logic, not role-based |
| Filter in `listRooms()` service method directly | Would break admin/discover use cases that need all rooms |

---

## Key Concepts

**RBAC (Role-Based Access Control)** — restricting resource access based on a user's role or membership.
In this case, the "role" is simply being a member of a room.

**`@AuthenticationPrincipal`** — Spring Security annotation that injects the currently authenticated
user's `UserDetails` directly into a controller method parameter.

**Membership enforcement vs. auto-join** — auto-join is convenient but breaks access control.
Explicit join gives users agency and makes the data model trustworthy.

**JPQL JOIN query** — `SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.memberships m WHERE m.user = :user`
traverses the `@OneToMany` relationship from `ChatRoom` → `RoomMembership` → `User`.

---

## Potential Pitfalls

- **Existing users lose access** — if users were auto-joined before this change, they still have
  `RoomMembership` rows, so they won't lose access. New users start with zero rooms.
- **`/discover` endpoint performance** — it loads all rooms then filters in memory. For large
  datasets, a dedicated query like `findRoomsNotJoinedByUser(user)` would be more efficient.
- **Race condition on join** — `addMember` handles the duplicate-join case with a
  `DataIntegrityViolationException` catch, so concurrent joins are safe.
- **`GET /api/rooms/{id}` now returns 403** — any frontend code that relied on auto-join
  (e.g., navigating directly to a room URL) will now get a 403 instead of silently joining.

---

## What You Learned

- How to filter API responses based on the authenticated user's data (membership)
- How `@AuthenticationPrincipal` connects Spring Security's JWT filter to controller logic
- The difference between **authentication** (who are you?) and **authorization** (what can you access?)
- How to add an explicit "join" flow as an alternative to silent auto-join
- How to build a discoverable room list that only shows rooms the user hasn't joined yet
- How to wire new backend endpoints to a React frontend with loading/joining state
