# Requirements Document

## Introduction

The Social Discovery and Room Management feature adds user search, friend requests, room creation, room search, and room deletion to the existing real-time chat system. It enables users to find people, build a friend list, and manage rooms faster while preserving existing authentication, WebSocket messaging, and accessibility standards.

## Glossary

- **Friend Request**: A pending invitation from one user to another to become friends.
- **Friendship**: A mutual relationship between two users after request acceptance.
- **User Search**: A query-based lookup of user profiles by username or display name.
- **Room Search**: A query-based filter of chat rooms by name or description.
- **Room Deletion**: Removing a chat room and its memberships and messages.

## Requirements

### Requirement 1: User Search

**User Story:** As a user, I want to search for other users, so that I can find people to add as friends.

#### Acceptance Criteria

1. WHEN a user provides a search query, THE system SHALL return users whose username or display name matches the query (case-insensitive).
2. WHEN the search query is empty, THE system SHALL return no results and prompt for input.
3. WHEN a user is already friends or has a pending request, THE system SHALL indicate that status in the search results.
4. IF the user is not authenticated, THEN THE system SHALL reject user search requests.

### Requirement 2: Friend Requests

**User Story:** As a user, I want to send and manage friend requests, so that I can build a friend list.

#### Acceptance Criteria

1. WHEN a user sends a friend request, THE system SHALL create a pending request and prevent duplicates.
2. WHEN a user receives a request, THE system SHALL show it in a pending list with accept and decline actions.
3. WHEN a user accepts a request, THE system SHALL create a friendship and remove the pending request.
4. WHEN a user declines a request, THE system SHALL remove the pending request without creating a friendship.
5. IF a user tries to send a request to themselves, THEN THE system SHALL reject the request with a validation error.

### Requirement 3: Friend List

**User Story:** As a user, I want to view my friends, so that I can quickly start conversations or find them online.

#### Acceptance Criteria

1. WHEN the user opens the friends view, THE system SHALL display their friends list.
2. WHEN a friend is online, THE system SHALL show an online indicator.
3. IF the friends list is empty, THEN THE system SHALL display an empty state with guidance.

### Requirement 4: Create Chat Room (UI + API Integration)

**User Story:** As a user, I want to create chat rooms, so that I can start new conversations.

#### Acceptance Criteria

1. WHEN a user submits a valid room name and optional description, THE system SHALL create a room and add the creator as a member.
2. WHEN room creation succeeds, THE system SHALL refresh or update the room list and navigate to the new room.
3. IF room creation fails, THEN THE system SHALL display a user-friendly error message.

### Requirement 5: Room Search and Filtering

**User Story:** As a user, I want to search or filter rooms, so that I can find relevant rooms quickly.

#### Acceptance Criteria

1. WHEN a user enters a room search query, THE system SHALL filter rooms by name or description in real time.
2. WHEN the query is cleared, THE system SHALL restore the full room list.
3. IF no rooms match, THEN THE system SHALL display an empty search state.

### Requirement 6: Delete Chat Room

**User Story:** As a room owner or moderator, I want to delete a room, so that I can remove unused or inappropriate rooms.

#### Acceptance Criteria

1. WHEN an authorized user confirms deletion, THE system SHALL delete the room and its memberships and messages.
2. WHEN deletion succeeds, THE system SHALL remove the room from the list and redirect users currently in that room to the rooms page.
3. IF a user is not authorized to delete the room, THEN THE system SHALL reject the request with a 403 response.
4. IF the room does not exist, THEN THE system SHALL return a 404 response.

### Requirement 7: Accessibility and UX

**User Story:** As a user with accessibility needs, I want the new UI to remain fully accessible, so that I can use it effectively.

#### Acceptance Criteria

1. WHEN new interactive elements are added, THE system SHALL provide proper ARIA labels and roles.
2. WHEN users navigate via keyboard, THE system SHALL support logical tab order and visible focus states.
3. IF a modal or panel is used, THEN THE system SHALL manage focus and allow dismissal with Escape.

### Requirement 8: Security and Privacy

**User Story:** As a user, I want my data protected, so that my profile and friend data are secure.

#### Acceptance Criteria

1. WHEN any friend or search endpoint is called, THE system SHALL require valid authentication.
2. WHEN returning user data, THE system SHALL limit fields to public profile information.
3. IF a user is not authorized, THEN THE system SHALL return an appropriate error response.
