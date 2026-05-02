# Lesson: Implementing Social Discovery and Room Management

## Task Context

This lesson covers adding social discovery (user search, friend requests, friends list) and room management UX (create, search, delete) to the real-time chat system. The goal is to extend the existing authenticated REST API and Next.js frontend while preserving accessibility and authorization guarantees.

## Files Modified

- src/main/java/org/example/chat/entity/FriendRequestStatus.java (created)
- src/main/java/org/example/chat/entity/FriendRequest.java (created)
- src/main/java/org/example/chat/entity/Friendship.java (created)
- src/main/java/org/example/chat/repository/FriendRequestRepository.java (created)
- src/main/java/org/example/chat/repository/FriendshipRepository.java (created)
- src/main/java/org/example/chat/repository/UserRepository.java (modified)
- src/main/java/org/example/chat/exception/ConflictException.java (created)
- src/main/java/org/example/chat/exception/FriendRequestNotFoundException.java (created)
- src/main/java/org/example/chat/dto/PublicUserResponse.java (created)
- src/main/java/org/example/chat/dto/FriendRequestCreateRequest.java (created)
- src/main/java/org/example/chat/dto/FriendRequestResponse.java (created)
- src/main/java/org/example/chat/dto/FriendRequestListResponse.java (created)
- src/main/java/org/example/chat/dto/FriendshipResponse.java (created)
- src/main/java/org/example/chat/dto/RelationshipStatus.java (created)
- src/main/java/org/example/chat/dto/UserSearchResultResponse.java (created)
- src/main/java/org/example/chat/service/FriendService.java (created)
- src/main/java/org/example/chat/controller/FriendController.java (created)
- src/main/java/org/example/chat/controller/UserSearchController.java (created)
- src/main/java/org/example/chat/service/ChatRoomService.java (modified)
- src/main/java/org/example/chat/controller/ChatRoomController.java (modified)
- src/main/java/org/example/chat/dto/RoomDeleteResponse.java (deleted)
- src/main/java/org/example/chat/dto/FriendRequestActionResponse.java (deleted)
- frontend/types/domain.ts (modified)
- frontend/types/api.ts (modified)
- frontend/types/index.ts (modified)
- frontend/lib/api/friends.ts (created)
- frontend/lib/api/users.ts (created)
- frontend/lib/api/rooms.ts (modified)
- frontend/components/ui/Modal.tsx (created)
- frontend/components/ui/index.ts (modified)
- frontend/components/chat/RoomCreateModal.tsx (created)
- frontend/components/chat/UserSearch.tsx (created)
- frontend/components/chat/FriendsPanel.tsx (created)
- frontend/components/chat/RoomSelector.tsx (modified)
- frontend/components/chat/index.ts (modified)
- frontend/app/chat/page.tsx (modified)
- frontend/app/chat/[roomId]/page.tsx (modified)

## Step-by-Step Changes

### Step 1: Add friend request and friendship data models

We introduced new entities and repositories to represent friend requests and friendships. Friend requests are unique per requester-recipient pair and default to PENDING, while friendships store a canonical user pair so we can fetch friends regardless of direction.

### Step 2: Build social discovery services and controllers

A dedicated FriendService handles user search, request validation, accept/decline flows, and friends list lookup. We added FriendController and UserSearchController to expose new endpoints and return public profile data plus relationship status.

### Step 3: Extend room deletion with authorization checks

ChatRoomService now enforces OWNER or MODERATOR roles before deleting a room. ChatRoomController exposes DELETE /api/rooms/{id} and returns a 204 response when deletion succeeds.

### Step 4: Expand frontend types and API clients

We added public user, relationship status, friend request, and search result types, then added new API clients for friends and user search. The room API client now supports deletion.

### Step 5: Build friends and user search UI

FriendsPanel shows the friends list with online status, incoming and outgoing requests, and a debounced user search component. UserSearch renders relationship badges with action buttons for sending, accepting, or declining requests.

### Step 6: Add room creation, filtering, and deletion UX

The chat rooms page now includes a create-room modal, live search filtering, and per-room delete actions for owners. The room detail page also supports deletion with a confirmation dialog and redirects to the rooms list afterward.

## Why This Approach

- **Explicit social entities**: Separate FriendRequest and Friendship models make it easy to validate duplicates, handle pending states, and list friends efficiently.
- **Public user DTOs**: Dedicated PublicUserResponse ensures we only send safe profile fields for discovery and friend lists.
- **Shared modal primitives**: The Modal component handles focus and Escape behavior once, keeping dialogs accessible and consistent.
- **Client-side filtering**: Room search uses a local filter for instant feedback without extra API calls.

## Alternatives Considered

- **Single friendship join table with two rows per friendship**: This simplifies lookups but duplicates data. We used a single normalized row instead.
- **Returning full UserResponse in search results**: This would leak email addresses, so we used a public DTO.
- **Dedicated friends page**: We kept social discovery adjacent to rooms for faster navigation.

## Key Concepts

- **Relationship status mapping**: FRIENDS, PENDING_INCOMING, PENDING_OUTGOING, NONE determine available actions in search results.
- **Role-based authorization**: Room deletion checks membership roles instead of relying on client-side UI gating alone.
- **Accessible modals**: Focus restoration and Escape handling make dialogs keyboard-friendly.
- **Debounced search**: A short delay reduces API load while keeping the UI responsive.

## Potential Pitfalls

- **Missing role data in the UI**: Moderators can delete rooms via the API, but the UI currently shows delete only for owners.
- **Stale search results**: If another device changes friendships, a refresh is needed to reflect new statuses.
- **Soft-deleted requests**: We delete accepted or declined requests; if audit history is needed, keep them with status updates instead.

## What You Learned

- How to model friend requests and friendships with clear validation rules.
- How to provide public profile data without exposing sensitive fields.
- How to combine room management actions with accessible UI patterns.
- How to wire debounced search and relationship actions into a single panel.
