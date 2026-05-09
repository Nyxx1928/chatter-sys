# Implementation Plan: Direct Messaging

## Overview

Add private one-on-one DM rooms to the existing chat system. The implementation adds a `RoomType` discriminator to `ChatRoom`, creates a `DirectMessageService` that auto-creates DM rooms when friendships are accepted, enforces access control guards on existing services, and updates the frontend to render DM rooms with type-specific display logic. All changes build on existing infrastructure — no new STOMP topics or message-handling paths are needed.

## Tasks

- [x] 1. Add `RoomType` enum and `roomType` field to the `ChatRoom` entity
  - Create `src/main/java/org/example/chat/entity/RoomType.java` with `GROUP` and `DIRECT` values
  - Add `@Enumerated(EnumType.STRING)` `roomType` field to `ChatRoom.java` with `NOT NULL` column `room_type`, default `GROUP`
  - Hibernate `ddl-auto: update` will apply the column addition automatically in dev/prod
  - _Requirements: 6.1_

- [x] 2. Implement `DirectMessageService`
  - [x] 2.1 Create `DirectMessageService.java` in `src/main/java/org/example/chat/service/`
    - Inject `ChatRoomRepository` and `RoomMembershipRepository`
    - Implement `getOrCreateDmRoom(User userA, User userB)`: generate name `"dm__" + min(id) + "__" + max(id)`, query `findByNameAndRoomType`, create room + two memberships in a single `@Transactional` call if not found, return existing room if found
    - Implement `findDmRoomBetween(User userA, User userB)`: returns `Optional<ChatRoom>` using the same name convention
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x]* 2.2 Write property test for DM room creation idempotency (Property 2)
    - **Property 2: DM Room Creation Idempotency**
    - **Validates: Requirements 1.4**
    - Use jqwik `@Property` — call `getOrCreateDmRoom(A, B)` twice, assert same room ID and exactly one room in DB
    - Tag: `// Feature: direct-messaging, Property 2: DM room creation is idempotent`

  - [x]* 2.3 Write property test for DM room name determinism (Property 3)
    - **Property 3: DM Room Name Determinism**
    - **Validates: Requirements 1.5**
    - Use jqwik `@Property` — assert `getOrCreateDmRoom(A, B).getName()` equals `getOrCreateDmRoom(B, A).getName()`
    - Tag: `// Feature: direct-messaging, Property 3: DM room name is deterministic regardless of argument order`

- [x] 3. Add `findByNameAndRoomType` query to `ChatRoomRepository`
  - Add `Optional<ChatRoom> findByNameAndRoomType(String name, RoomType roomType)` to `ChatRoomRepository.java`
  - _Requirements: 1.4, 1.5_

- [x] 4. Extend `FriendService.acceptFriendRequest` to create the DM room
  - Inject `DirectMessageService` into `FriendService`
  - After `friendshipRepository.save(friendship)`, call `directMessageService.getOrCreateDmRoom(currentUser, requester)`
  - Update `FriendshipResponse` DTO: add `Long dmRoomId` field and update the constructor call in `acceptFriendRequest` to include `dmRoom.getId()`
  - _Requirements: 1.1, 1.2, 1.3, 2.5_

- [x] 5. Checkpoint — DM room creation backend complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Add access control guards to `ChatRoomService` and `ChatRoomController`
  - [x] 6.1 Guard `deleteRoom` in `ChatRoomService`
    - Before the existing role check, throw `UnauthorizedException("Cannot delete a direct message room")` if `room.getRoomType() == RoomType.DIRECT`
    - _Requirements: 4.5_

  - [x] 6.2 Guard `addMember` in `ChatRoomService`
    - After loading the room, throw `UnauthorizedException("Cannot invite users to a direct message room")` if `room.getRoomType() == RoomType.DIRECT`
    - _Requirements: 4.4_

  - [x] 6.3 Guard `inviteToRoom` in `ChatRoomController`
    - Load the room and return `403 Forbidden` (throw `UnauthorizedException`) if `roomType == DIRECT`
    - _Requirements: 4.4_

  - [x]* 6.4 Write property test for non-participant access rejection (Property 5)
    - **Property 5: Non-Participant Access Rejection**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**
    - Use jqwik `@Property` — for a generated outsider user, assert that `sendMessage`, `getMessageHistory`, `getRoomById`, `addMember`, and `deleteRoom` all throw `UnauthorizedException`
    - Tag: `// Feature: direct-messaging, Property 5: non-participant access is rejected for all DM room operations`

- [x] 7. Update `ChatRoomResponse` DTO to include `roomType`
  - Add `RoomType roomType` field to `ChatRoomResponse.java`
  - Update the `from(ChatRoom)` factory method to include `chatRoom.getRoomType()`
  - _Requirements: 6.1, 6.2_

- [x] 8. Checkpoint — Access control and DTO changes complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Update frontend TypeScript types
  - [x] 9.1 Add `RoomType` and update `ChatRoom` interface in `frontend/types/domain.ts`
    - Add `export type RoomType = 'GROUP' | 'DIRECT';`
    - Add `roomType: RoomType` field to the `ChatRoom` interface
    - Add optional `otherParticipant?: PublicUser` field (client-side only, derived from members)
    - _Requirements: 6.3_

  - [x] 9.2 Update `Friendship` interface in `frontend/types/domain.ts`
    - Add `dmRoomId: number` field to the `Friendship` interface
    - _Requirements: 2.5_

- [x] 10. Update `RoomSelector` component for DM room rendering
  - In `renderRoom`, when `room.roomType === 'DIRECT'`:
    - Display `room.otherParticipant?.displayName ?? room.name` as the room label (instead of `room.name`)
    - Render a person SVG icon instead of the first-letter avatar
    - Set `aria-label` on the button to `"Direct message with {otherParticipant.displayName}"`
  - Suppress the delete button for DM rooms: update the `showDelete` condition to also check `room.roomType !== 'DIRECT'`
  - _Requirements: 2.3, 2.4, 6.4, 7.1, 7.3_

- [x] 11. Update `chat/page.tsx` for DM room handling
  - [x] 11.1 Derive `otherParticipant` after room selection
  - [x] 11.2 Update chat header for DM rooms
  - [x] 11.3 Suppress invite and delete controls for DM rooms

- [x] 12. Update `FriendsPanel` to refresh room list after friend acceptance
  - Accept an `onDmRoomCreated?: (dmRoomId: number) => void` callback prop in `FriendsPanel`
  - In `handleAcceptRequest`, after `acceptFriendRequest` succeeds, call `onDmRoomCreated(friendship.dmRoomId)` if the callback is provided
  - In `chat/page.tsx` (or the contacts page where `FriendsPanel` is rendered), pass `onDmRoomCreated` that calls `loadRooms()` so the new DM room appears without a page reload
  - _Requirements: 2.5_

- [x] 13. Checkpoint — Frontend DM rendering complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 14. Write frontend unit tests for DM room rendering
  - [x]* 14.1 Write property test for DM room label display (Property 7)
  - [x]* 14.2 Write property test for DM room ARIA labels (Property 9)
  - [x]* 14.3 Write unit test for DM room suppresses invite and delete controls (Property 8)

- [x] 15. Write backend integration test for DM room creation round-trip (Property 1)
  - [x]* 15.1 Write `@SpringBootTest` integration test for DM room creation
  - [x]* 15.2 Write integration test for room list completeness (Property 4)
  - [x]* 15.3 Write integration test for DM message history ordering (Property 6)

- [x] 16. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation throughout development
- The `roomType` column uses Hibernate `ddl-auto: update` — no manual migration needed in dev; a Flyway/Liquibase script should be added before deploying to production
- `otherParticipant` is a client-side-only field derived from the members list; it is never stored on the server
- Property tests use jqwik (backend) and fast-check (frontend), both already present in the project
- The existing STOMP topic structure (`/topic/room/{roomId}`) is reused unchanged — DM rooms are just rooms with `roomType=DIRECT`
