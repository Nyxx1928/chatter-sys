# Lesson: Implementing User Presence Tracking in a Real-Time Chat System

## Task Context

In this lesson, we implemented user presence tracking for a real-time chat system built with Spring Boot and WebSocket/STOMP. The goal was to track when users connect and disconnect from the chat system, update their online/offline status in the database, and broadcast these presence changes to all chat rooms where the user is a member.

This feature is essential for real-time chat applications because it allows users to see who is currently available to chat, improving the user experience and enabling better communication.

## Files Modified

- `src/main/java/org/example/chat/service/UserPresenceService.java` (created)
- `src/main/java/org/example/chat/listener/WebSocketEventListener.java` (modified)
- `src/test/java/org/example/chat/listener/WebSocketEventListenerTest.java` (modified)

## Step-by-Step Changes

### Step 1: Created UserPresenceService

We created a new service class `UserPresenceService` that encapsulates all presence tracking logic. This service is responsible for:

1. **Marking users online**: When a user connects, we set their `online` field to `true` and update their `lastSeen` timestamp
2. **Marking users offline**: When a user disconnects, we set their `online` field to `false` and update their `lastSeen` timestamp
3. **Publishing presence updates**: Broadcasting presence changes to all chat rooms where the user is a member via STOMP topics
4. **Querying online users**: Retrieving a list of online users in a specific chat room

The service uses:
- `UserRepository` to update user status in the database
- `RoomMembershipRepository` to find all rooms where a user is a member
- `SimpMessagingTemplate` to broadcast presence updates via STOMP

### Step 2: Refactored WebSocketEventListener

We refactored the existing `WebSocketEventListener` to delegate presence tracking to the new `UserPresenceService`. This follows the Single Responsibility Principle - the listener now only handles WebSocket lifecycle events and delegates business logic to the service layer.

**Before**: The listener directly updated user status and published presence updates
**After**: The listener calls `userPresenceService.markUserOnline()` and `userPresenceService.markUserOffline()`

This refactoring provides several benefits:
- **Separation of concerns**: Event handling is separate from business logic
- **Testability**: The service can be tested independently
- **Reusability**: The presence service can be used from other parts of the application
- **Maintainability**: Changes to presence logic only affect the service class

### Step 3: Updated Tests

We updated the `WebSocketEventListenerTest` to reflect the new architecture:

1. Replaced `RoomMembershipRepository` and `SimpMessagingTemplate` mocks with `UserPresenceService` mock
2. Updated test assertions to verify that the service methods are called correctly
3. Simplified tests since we no longer need to verify internal implementation details (like STOMP message publishing)

The tests now focus on verifying that:
- The listener calls `markUserOnline()` when a user connects
- The listener calls `markUserOffline()` when a user disconnects
- The listener handles edge cases (null principal, non-existent users)

## Why This Approach

### Service Layer Pattern

We chose to create a dedicated service class rather than keeping all logic in the event listener for several reasons:

1. **Single Responsibility**: The listener should only handle WebSocket events, not business logic
2. **Testability**: Services are easier to unit test than event listeners
3. **Reusability**: The presence service can be called from REST endpoints, scheduled tasks, or other components
4. **Transaction Management**: The `@Transactional` annotation on service methods ensures database consistency

### STOMP Topic Broadcasting

We broadcast presence updates to `/topic/presence/{roomId}` for each room where the user is a member. This approach:

1. **Scales efficiently**: Only users subscribed to a room receive updates for that room
2. **Reduces network traffic**: Users don't receive presence updates for rooms they're not in
3. **Follows STOMP conventions**: Using topic destinations for pub/sub messaging

### Presence Payload Structure

The presence payload includes:
- `userId`: Unique identifier for the user
- `username`: User's username
- `displayName`: User's display name
- `online`: Boolean indicating online/offline status
- `lastSeen`: Timestamp of last activity
- `roomId`: The room where this presence update applies

This structure provides all information needed by the frontend to update the UI without additional API calls.

## Alternatives Considered

### Alternative 1: Keep Logic in Event Listener

We could have kept all presence tracking logic in the `WebSocketEventListener` without creating a separate service.

**Pros**:
- Fewer classes to maintain
- Simpler architecture for small applications

**Cons**:
- Violates Single Responsibility Principle
- Harder to test
- Cannot reuse presence logic from other components
- Mixing infrastructure concerns (WebSocket) with business logic

**Why we didn't choose this**: As the application grows, this approach becomes harder to maintain and test.

### Alternative 2: Use Spring Events

We could have used Spring's event publishing mechanism to decouple the listener from the service.

**Pros**:
- Even more decoupled architecture
- Multiple listeners could react to presence changes
- Follows event-driven architecture patterns

**Cons**:
- More complex for this use case
- Adds indirection that may not be necessary
- Harder to debug event flow

**Why we didn't choose this**: For this learning project, direct service calls are simpler and easier to understand. Event-driven architecture would be beneficial in larger, more complex systems.

### Alternative 3: Use Redis for Presence Tracking

We could have used Redis to track online users instead of the PostgreSQL database.

**Pros**:
- Faster reads/writes for presence data
- Built-in expiration for automatic cleanup
- Better performance at scale

**Cons**:
- Adds another infrastructure dependency
- More complex deployment
- Overkill for 10-20 concurrent users

**Why we didn't choose this**: For a learning project with 10-20 users, PostgreSQL is sufficient. Redis would be a good choice for production systems with thousands of concurrent users.

## Key Concepts

### 1. Service Layer Pattern

The service layer sits between controllers/listeners and repositories, containing business logic. Benefits include:
- Separation of concerns
- Transaction management
- Reusability
- Testability

### 2. STOMP Topic Broadcasting

STOMP (Simple Text Oriented Messaging Protocol) provides pub/sub messaging over WebSocket:
- **Topics** (`/topic/*`): Broadcast messages to all subscribers
- **Queues** (`/queue/*`): Point-to-point messaging
- **User destinations** (`/user/*`): Messages to specific users

### 3. WebSocket Lifecycle Events

Spring WebSocket provides lifecycle events:
- `SessionConnectEvent`: Fired when a STOMP CONNECT frame is received
- `SessionDisconnectEvent`: Fired when a WebSocket session closes
- `SessionSubscribeEvent`: Fired when a client subscribes to a destination
- `SessionUnsubscribeEvent`: Fired when a client unsubscribes

### 4. Transactional Boundaries

The `@Transactional` annotation ensures that database operations are atomic:
- All operations succeed or all fail
- Changes are rolled back on exceptions
- Prevents partial updates that could leave data inconsistent

### 5. Dependency Injection

Spring's dependency injection allows us to:
- Inject services into listeners
- Mock dependencies in tests
- Swap implementations without changing code

## Potential Pitfalls

### 1. Race Conditions

**Problem**: Multiple WebSocket connections from the same user could cause race conditions when updating presence status.

**Solution**: Use database-level locking or optimistic locking with version fields. For this learning project, we accept the risk since users typically have one connection.

### 2. Stale Presence Data

**Problem**: If a WebSocket connection drops without a proper disconnect event, the user may appear online when they're actually offline.

**Solution**: Implement heartbeat/ping-pong mechanisms and timeout-based cleanup. STOMP provides built-in heartbeat support that we configured in `WebSocketConfig`.

### 3. Broadcasting to Too Many Rooms

**Problem**: If a user is a member of many rooms, broadcasting presence updates to all rooms could be slow.

**Solution**: 
- Use asynchronous messaging for broadcasts
- Batch presence updates
- Limit the number of rooms a user can join

For this project with 10-20 users, this isn't a concern.

### 4. Database Performance

**Problem**: Frequent presence updates could create database load.

**Solution**:
- Use database connection pooling (HikariCP, already configured)
- Consider caching online user lists
- Use Redis for high-traffic scenarios

### 5. Test Isolation

**Problem**: Tests that depend on database state can be flaky.

**Solution**: We use mocks in unit tests to avoid database dependencies. Integration tests would use test containers or in-memory databases.

## What You Learned

1. **Service Layer Pattern**: How to separate business logic from infrastructure concerns using service classes

2. **WebSocket Event Handling**: How to respond to WebSocket lifecycle events in Spring Boot

3. **STOMP Broadcasting**: How to broadcast messages to multiple subscribers using STOMP topics

4. **Refactoring for Testability**: How to refactor code to make it easier to test by extracting logic into services

5. **Presence Tracking**: How to implement a common real-time feature (user presence) in a chat application

6. **Transaction Management**: How to use `@Transactional` to ensure database consistency

7. **Dependency Injection**: How Spring's DI container manages service dependencies

8. **Test Refactoring**: How to update tests when refactoring implementation code

This implementation provides a solid foundation for presence tracking that can be extended with features like:
- Typing indicators
- Last seen timestamps
- Custom status messages
- Away/busy/do-not-disturb states
