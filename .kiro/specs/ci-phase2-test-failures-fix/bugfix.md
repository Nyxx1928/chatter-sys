# Bugfix Requirements Document

## Introduction

The CI pipeline failed in phase 2 with 15 test failures across integration tests. These failures reveal five distinct categories of bugs in the chat application's REST API implementation:

1. **Authentication Status Code Mismatches**: Endpoints return 403 Forbidden instead of 401 Unauthorized when no authentication is provided
2. **Pagination Response Structure Mismatches**: Message history endpoints return paginated objects instead of simple arrays
3. **NullPointerException in ChatRoom Responses**: ChatRoomResponse.from() fails when createdBy user is null
4. **Resource Not Found Status Code Mismatches**: Endpoints return 400 Bad Request instead of 404 Not Found for nonexistent resources
5. **Authorization Check Failures**: Endpoints return incorrect status codes (400/500) instead of 403 Forbidden for unauthorized access

These bugs affect the authentication, message history, and chat room management features, causing integration tests to fail and potentially impacting API consumers who expect standard HTTP semantics.

## Bug Analysis

### Current Behavior (Defect)

#### 1. Authentication Status Code Issues

1.1 WHEN a request is made to `/api/rooms/{id}/messages` without authentication THEN the system returns 403 Forbidden instead of 401 Unauthorized

1.2 WHEN a request is made to `/api/rooms` (POST) without authentication THEN the system returns 403 Forbidden instead of 401 Unauthorized

1.3 WHEN a request is made to `/api/users/me` (GET) without authentication THEN the system returns 403 Forbidden instead of 401 Unauthorized

1.4 WHEN a request is made to `/api/users/me` (PUT) without authentication THEN the system returns 403 Forbidden instead of 401 Unauthorized

#### 2. Pagination Response Structure Issues

1.5 WHEN a request is made to `/api/rooms/{id}/messages` for an empty room THEN the system returns a paginated response object `{content: [], pageable: {...}, totalElements: 0, ...}` instead of an empty array `[]`

1.6 WHEN a request is made to `/api/rooms/{id}/messages` for a room with messages THEN the system returns a paginated response object with structure `{content: [...], pageable: {...}, totalElements: N, ...}` instead of a simple array of messages

1.7 WHEN a request is made to `/api/rooms/{id}/messages?limit=5` THEN the system returns a paginated response object instead of a simple array limited to 5 messages

1.8 WHEN a request is made to `/api/rooms/{id}/messages` for a room with multiple message types THEN the system returns a paginated response object instead of a simple array

1.9 WHEN a request is made to `/api/rooms/{id}/messages` for a room with messages from multiple users THEN the system returns a paginated response object instead of a simple array

#### 3. NullPointerException in ChatRoom Responses

1.10 WHEN a request is made to `/api/rooms` (GET) for rooms where createdBy is null THEN the system throws NullPointerException in ChatRoomResponse.from() with error "Cannot invoke org.example.chat.entity.User.getId() because user is null"

1.11 WHEN a request is made to `/api/rooms/{id}` (GET) for a room where createdBy is null THEN the system throws NullPointerException in ChatRoomResponse.from()

1.12 WHEN a request is made to `/api/rooms/{id}` (GET) by a non-member for a room where createdBy is null THEN the system throws NullPointerException instead of returning 403 Forbidden

#### 4. Resource Not Found Status Code Issues

1.13 WHEN a request is made to `/api/rooms/99999/messages` for a nonexistent room THEN the system returns 400 Bad Request instead of 404 Not Found

1.14 WHEN a request is made to `/api/rooms/99999` (GET) for a nonexistent room THEN the system returns 400 Bad Request instead of 404 Not Found

#### 5. Authorization Check Issues

1.15 WHEN a request is made to `/api/rooms/{id}/messages` by a user who is not a member THEN the system returns 400 Bad Request instead of 403 Forbidden

1.16 WHEN a request is made to `/api/rooms/{id}` (GET) by a user who is not a member THEN the system returns 400 Bad Request or 500 Internal Server Error instead of 403 Forbidden

### Expected Behavior (Correct)

#### 1. Authentication Status Code Fixes

2.1 WHEN a request is made to `/api/rooms/{id}/messages` without authentication THEN the system SHALL return 401 Unauthorized

2.2 WHEN a request is made to `/api/rooms` (POST) without authentication THEN the system SHALL return 401 Unauthorized

2.3 WHEN a request is made to `/api/users/me` (GET) without authentication THEN the system SHALL return 401 Unauthorized

2.4 WHEN a request is made to `/api/users/me` (PUT) without authentication THEN the system SHALL return 401 Unauthorized

#### 2. Pagination Response Structure Fixes

2.5 WHEN a request is made to `/api/rooms/{id}/messages` for an empty room THEN the system SHALL return an empty array `[]`

2.6 WHEN a request is made to `/api/rooms/{id}/messages` for a room with messages THEN the system SHALL return a simple array of message objects without pagination metadata

2.7 WHEN a request is made to `/api/rooms/{id}/messages?limit=5` THEN the system SHALL return a simple array containing at most 5 message objects

2.8 WHEN a request is made to `/api/rooms/{id}/messages` for a room with multiple message types THEN the system SHALL return a simple array of all message objects

2.9 WHEN a request is made to `/api/rooms/{id}/messages` for a room with messages from multiple users THEN the system SHALL return a simple array of all message objects

#### 3. NullPointerException Fixes

2.10 WHEN a request is made to `/api/rooms` (GET) for rooms where createdBy is null THEN the system SHALL return room data with createdBy as null or a default value without throwing an exception

2.11 WHEN a request is made to `/api/rooms/{id}` (GET) for a room where createdBy is null THEN the system SHALL return room data with createdBy as null or a default value without throwing an exception

2.12 WHEN a request is made to `/api/rooms/{id}` (GET) by a non-member for a room where createdBy is null THEN the system SHALL return 403 Forbidden without throwing an exception

#### 4. Resource Not Found Status Code Fixes

2.13 WHEN a request is made to `/api/rooms/99999/messages` for a nonexistent room THEN the system SHALL return 404 Not Found

2.14 WHEN a request is made to `/api/rooms/99999` (GET) for a nonexistent room THEN the system SHALL return 404 Not Found

#### 5. Authorization Check Fixes

2.15 WHEN a request is made to `/api/rooms/{id}/messages` by a user who is not a member THEN the system SHALL return 403 Forbidden

2.16 WHEN a request is made to `/api/rooms/{id}` (GET) by a user who is not a member THEN the system SHALL return 403 Forbidden

### Unchanged Behavior (Regression Prevention)

#### Authentication and Authorization

3.1 WHEN a request is made with valid authentication to any protected endpoint THEN the system SHALL CONTINUE TO process the request normally

3.2 WHEN a request is made to `/api/auth/register` or `/api/auth/login` without authentication THEN the system SHALL CONTINUE TO allow access without requiring authentication

#### Message History Functionality

3.3 WHEN a request is made to `/api/rooms/{id}/messages` by an authenticated member THEN the system SHALL CONTINUE TO return messages in chronological order

3.4 WHEN a request is made to `/api/rooms/{id}/messages` with pagination parameters THEN the system SHALL CONTINUE TO respect the pagination parameters (page, size, sort)

3.5 WHEN messages are retrieved from a room THEN the system SHALL CONTINUE TO include sender information, content, message type, and timestamp for each message

#### Chat Room Functionality

3.6 WHEN a request is made to `/api/rooms` (POST) with valid authentication and data THEN the system SHALL CONTINUE TO create a new chat room and add the creator as OWNER

3.7 WHEN a request is made to `/api/rooms` (GET) with valid authentication THEN the system SHALL CONTINUE TO return all available rooms

3.8 WHEN a request is made to `/api/rooms/{id}` (GET) by an authenticated member THEN the system SHALL CONTINUE TO return the room details

3.9 WHEN a request is made to `/api/rooms/{id}/members` THEN the system SHALL CONTINUE TO return the list of room members

#### User Profile Functionality

3.10 WHEN a request is made to `/api/users/me` (GET) with valid authentication THEN the system SHALL CONTINUE TO return the authenticated user's profile

3.11 WHEN a request is made to `/api/users/me` (PUT) with valid authentication and data THEN the system SHALL CONTINUE TO update the user's profile

#### Error Handling

3.12 WHEN a request is made with invalid data (e.g., invalid email format, empty required fields) THEN the system SHALL CONTINUE TO return 400 Bad Request with appropriate error messages

3.13 WHEN a request is made to create a room with a duplicate name THEN the system SHALL CONTINUE TO handle it according to existing business rules

3.14 WHEN database operations fail THEN the system SHALL CONTINUE TO return appropriate 500 Internal Server Error responses
