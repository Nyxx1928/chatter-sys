# Lesson: Adding RoomType Enum and DirectMessageService (Tasks 1–5)

## Task Context

We're implementing the first phase of the Direct Messaging feature. The goal is to add a `RoomType` discriminator to `ChatRoom`, create a `DirectMessageService` that auto-creates DM rooms when friendships are accepted, and wire everything together so the `FriendshipResponse` DTO carries back the new DM room ID.

Tasks covered: 1, 2, 3, 4, and 5 (checkpoint).

## Files Modified

- `src/main/java/org/example/chat/entity/RoomType.java` (created)
- `src/main/java/org/example/chat/entity/ChatRoom.java` (modified)
- `src/main/java/org/example/chat/repository/ChatRoomRepository.java` (modified)
- `src/main/java/org/example/chat/service/DirectMessageService.java` (created)
- `src/main/java/org/example/chat/dto/FriendshipResponse.java` (modified)
- `src/main/java/org/example/chat/service/FriendService.java` (modified)
- `src/test/java/org/example/chat/service/FriendServiceTest.java` (modified)
- `src/test/java/org/example/chat/controller/FriendControllerTest.java` (modified)

## Step-by-Step Changes

### Step 1 — Create the `RoomType` enum

We need a way to tell GROUP rooms apart from DIRECT (DM) rooms at the database level. A Java enum stored as a `STRING` column is the cleanest approach.

```java
// src/main/java/org/example/chat/entity/RoomType.java
public enum RoomType {
    GROUP,
    DIRECT
}
```

Simple, two values. `GROUP` is the default for all existing rooms; `DIRECT` marks a private DM room.

### Step 2 — Add `roomType` field to `ChatRoom`

We add the field right after `name` so it's visually grouped with the room's identity fields:

```java
@Enumerated(EnumType.STRING)
@Column(name = "room_type", nullable = false, length = 20)
private RoomType roomType = RoomType.GROUP;
```

Key points:
- `@Enumerated(EnumType.STRING)` stores `"GROUP"` or `"DIRECT"` in the DB, not `0`/`1`. This makes the DB readable and safe to rename enum values later.
- `nullable = false` — every room must have a type.
- Default `RoomType.GROUP` means all existing rooms created via `createRoom()` automatically get the right type without any code change.
- Hibernate `ddl-auto: update` adds the `room_type` column automatically in dev. For production, a Flyway/Liquibase migration is needed.

### Step 3 — Add `findByNameAndRoomType` to `ChatRoomRepository`

```java
Optional<ChatRoom> findByNameAndRoomType(String name, RoomType roomType);
```

Spring Data JPA derives the SQL from the method name — no `@Query` needed. We scope the lookup to both name AND type so a GROUP room named `"dm__1__2"` (unlikely but possible) doesn't collide with a DIRECT room.

### Step 4 — Create `DirectMessageService`

This service owns the DM room lifecycle. Two public methods:

**`getOrCreateDmRoom(User userA, User userB)`**
- Builds a deterministic name: `"dm__" + min(id) + "__" + max(id)`.
- Looks up the room by name + type. If found, returns it immediately (idempotent).
- If not found, creates the room with `roomType = DIRECT`, then adds both users as `MEMBER`.
- The whole thing is `@Transactional` so the room + two memberships are committed atomically.

**`findDmRoomBetween(User userA, User userB)`**
- Read-only lookup, returns `Optional<ChatRoom>`.

**Why the deterministic name?**
`"dm__1__2"` is always the same regardless of whether you call `getOrCreateDmRoom(userA, userB)` or `getOrCreateDmRoom(userB, userA)`. The `Math.min`/`Math.max` trick ensures the lower ID always comes first.

### Step 5 — Update `FriendshipResponse` DTO

Added a `Long dmRoomId` field. The frontend needs this ID to navigate directly to the new DM room after accepting a friend request.

```java
private Long dmRoomId;
```

### Step 6 — Wire `DirectMessageService` into `FriendService`

Injected via constructor (Spring's preferred style). In `acceptFriendRequest`, after the friendship is saved:

```java
var dmRoom = directMessageService.getOrCreateDmRoom(currentUser, requester);
return new FriendshipResponse(PublicUserResponse.from(friend), friendship.getCreatedAt(), dmRoom.getId());
```

The DM room creation is inside the same `@Transactional` method, so if anything fails, the whole operation rolls back — no orphaned rooms.

### Step 7 — Fix existing tests

Two test files constructed `FriendshipResponse` or `FriendService` with the old signatures:
- `FriendControllerTest`: updated `new FriendshipResponse(...)` to pass `42L` as the `dmRoomId`.
- `FriendServiceTest`: added `@Mock DirectMessageService directMessageService`, passed it to the constructor, and stubbed `getOrCreateDmRoom` in the two `acceptFriendRequest` tests.

## Why This Approach

**Enum discriminator over separate table**: A `room_type` column is the simplest way to distinguish room kinds. A separate `DirectMessageRoom` entity would require polymorphism or joins everywhere. The enum keeps the model flat and queries simple.

**Deterministic name convention**: Using `"dm__minId__maxId"` as the room name gives us a free uniqueness constraint (the `name` column is already `UNIQUE`). No extra unique constraint needed.

**`@Transactional` on `getOrCreateDmRoom`**: Without it, two concurrent friend-accept calls could both see "no room exists" and try to create one, causing a unique constraint violation. The transaction + `findByNameAndRoomType` lookup handles this safely.

**Default `RoomType.GROUP`**: Existing code that creates rooms via `ChatRoomService.createRoom()` doesn't need to change — the default kicks in automatically.

## Alternatives Considered

- **Separate `DirectMessageRoom` entity**: More type-safe but adds complexity (polymorphism, extra joins, more DTOs). Overkill for two room types.
- **Boolean `isDirect` flag**: Works but doesn't scale if a third room type (e.g., `ANNOUNCEMENT`) is added later. An enum is more expressive.
- **Storing DM rooms by user pair in a separate table**: Cleaner conceptually but requires a new table and more queries. Reusing `ChatRoom` means all existing message/membership infrastructure works for free.

## Key Concepts

- **`@Enumerated(EnumType.STRING)`**: Stores the enum name as a string in the DB. Safer than `ORDINAL` (which breaks if you reorder enum values).
- **Spring Data derived queries**: Method names like `findByNameAndRoomType` are parsed by Spring Data to generate SQL automatically.
- **Idempotency**: `getOrCreateDmRoom` can be called multiple times safely — it always returns the same room. This is important because `acceptFriendRequest` could theoretically be retried.
- **Constructor injection**: Spring recommends constructor injection over `@Autowired` field injection for testability and immutability.
- **`@Transactional` propagation**: When `acceptFriendRequest` (already `@Transactional`) calls `getOrCreateDmRoom` (also `@Transactional`), the inner method joins the outer transaction by default (`REQUIRED` propagation).

## Potential Pitfalls

- **Missing `ddl-auto: update` in prod**: The `room_type` column won't exist in production until a migration script adds it. Don't forget to write a Flyway/Liquibase migration before deploying.
- **Null `createdBy` on DM rooms**: DM rooms intentionally have `createdBy = null` (no single owner). Any code that calls `chatRoom.getCreatedBy().getId()` without a null check will NPE on DM rooms. The `ChatRoomResponse.from()` factory already handles this with a null check.
- **Unstubbed mock in tests**: If a test calls `acceptFriendRequest` without stubbing `directMessageService.getOrCreateDmRoom`, Mockito returns `null` by default, causing an NPE when `.getId()` is called. Always stub the mock in tests that exercise this path.

## What You Learned

- How to add a discriminator column to an existing JPA entity without breaking existing data (using a default value).
- How to design an idempotent "get or create" service method using a deterministic key.
- How to propagate new data (the DM room ID) through the DTO layer back to the frontend.
- Why `@Transactional` on both the outer and inner service methods is safe and correct in Spring.
- How to update existing unit tests when a service's constructor signature changes.
