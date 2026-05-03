# Implementation Plan: Social Discovery and Room Management

## Overview

Add backend endpoints for user search, friend requests, and room deletion, then integrate new frontend UI for user search, friends list, room creation/search, and room deletion.

## Tasks

### Phase 1: Backend APIs

- [x] 1. Create friend request data model and persistence
  - Add FriendRequest entity, repository, and DTOs
  - Add Friendship entity or join table for accepted relationships
  - _Requirements: 2.1, 2.3, 8.1_

- [x] 2. Implement friend request endpoints
  - Send request, list pending, accept, decline
  - Validation for self-request and duplicates
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 8.1_

- [x] 3. Implement user search endpoint
  - Query by username/display name, return relationship status
  - _Requirements: 1.1, 1.3, 8.2_

- [x] 4. Implement room deletion endpoint
  - Add DELETE /api/rooms/{id} with owner/moderator authorization
  - Ensure memberships and messages are deleted or cascaded
  - _Requirements: 6.1, 6.3, 6.4, 8.3_

- [x] 5. Checkpoint - Backend APIs ready
  - Run unit and integration tests
  - _Requirements: 1.1, 2.1, 6.1, 8.1_

### Phase 2: Frontend UI

- [x] 6. Add API client functions for friends, search, and room deletion
  - Add types for FriendRequest, Friendship, UserSearchResult
  - Add HTTP functions in lib/api
  - _Requirements: 1.1, 2.1, 3.1, 6.1_

- [x] 7. Build UserSearch and FriendsPanel components
  - Search input with debounce
  - Relationship status badges and action buttons
  - _Requirements: 1.1, 1.3, 2.1, 3.1, 7.1_

- [x] 8. Add room creation UI
  - Modal or inline form for name/description
  - Use createRoom API and refresh list
  - _Requirements: 4.1, 4.2, 4.3_

- [x] 9. Add room search/filter UI
  - Client-side filter in room list view
  - Empty state for no matches
  - _Requirements: 5.1, 5.2, 5.3_

- [x] 10. Add room deletion UI
  - Delete action with confirmation dialog
  - Redirect users out of deleted room
  - _Requirements: 6.1, 6.2, 7.1_

- [x] 11. Checkpoint - Frontend UX ready
  - Run accessibility checks and unit tests
  - _Requirements: 7.1, 7.2, 7.3_

### Phase 3: Integration and Validation

- [ ] 12. Integrate presence with friends list
  - Show online status where available
  - _Requirements: 3.2_

- [~] 13. End-to-end test flows
  - Search user, send request, accept, create room, delete room
  - _Requirements: 1.1, 2.1, 3.1, 4.1, 6.1_

- [~] 14. Checkpoint - Release readiness
  - Verify security behavior and error handling
  - _Requirements: 8.1, 8.2, 8.3_
