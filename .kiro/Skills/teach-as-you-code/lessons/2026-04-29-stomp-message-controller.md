# Lesson: Building a STOMP Message Controller for Real-Time Chat

## Task Context

In this lesson, we implemented a Spring WebSocket controller that handles STOMP (Simple Text Oriented Messaging Protocol) messages for a real-time chat system. Unlike traditional REST controllers that handle HTTP requests, this controller processes WebSocket messages using the STOMP protocol for bidirectional, real-time communication.

The controller needed to:
1. Handle incoming chat messages and delegate to the service layer
2. Process room join requests and broadcast JOIN system messages
3. Process room leave requests and broadcast LEAVE system messages
4. Catch and handle errors, sending them to user-specific error queues

This is part of a larger real-time chat system built with Spring Boot (backend) and Next.js (frontend), designed to support 10-20 concurrent users as a learning project.

## Files Modified

- `src/main/java/org/example/chat/controller/ChatMessageController.java` (created)

## Step-by-Step Changes

### 1. Created the Controller Class Structure

We started by creating a `@Controller` class (not `@RestController`) because STOMP message handlers don't return HTTP responses. The controller uses:
- `@MessageMapping` annotations instead of `@GetMapping`/`@PostMapping`
- `Principal` parameter to access the authenticated user
- `@DestinationVariable` to extract path variables from STOMP destinations

### 2. Implemented Message Sending Handler

The `sendMessage()` method handles messages sent to `/app/chat.send/{roomId}`:
- Extracts the authenticated user from the `Principal` object
- Receives the message payload as a `Message` entity
- Delegates to `ChatMessageService` which validates, persists, and broadcasts
- The service layer handles the actual broadcasting to `/topic/room/{roomId}`

Key insight: The Principal's name contains the username (set during authentication), which we use to look up the full User entity.

### 3. Implemented Room Join Handler

The `joinRoom()` method handles join requests to `/app/room.join/{roomId}`:
- Adds the user to the room using `ChatRoomService.addMember()`
- Creates a JOIN system message with `MessageType.JOIN`
- Manually broadcasts the JOIN message to `/topic/room/{roomId}` using `SimpMessagingTemplate`
- Sets the message content to a user-friendly notification

Why manual broadcasting? JOIN/LEAVE are system messages that don't go through the normal message service flow.

### 4. Implemented Room Leave Handler

The `leaveRoom()` method mirrors the join handler:
- Removes the user from the room using `ChatRoomService.removeMember()`
- Creates a LEAVE system message with `MessageType.LEAVE`
- Broadcasts to the room topic so remaining users see the departure

### 5. Added Exception Handling

The `handleException()` method uses `@MessageExceptionHandler`:
- Catches any exception thrown during STOMP message processing
- Logs the error with user context
- Returns an `ErrorResponse` to the user's personal error queue at `/user/queue/errors`
- Uses `@SendToUser` to send the error only to the user who triggered it

This prevents errors from one user affecting others and provides targeted error feedback.

## Why This Approach

### STOMP Protocol Over Raw WebSocket

We used STOMP instead of raw WebSocket because:
- **Structured messaging**: STOMP provides frames with commands, headers, and body
- **Built-in routing**: Spring's STOMP broker handles pub/sub patterns automatically
- **Destination patterns**: Clear separation between application destinations (`/app/*`) and topics (`/topic/*`)
- **User-specific messaging**: Easy to send messages to specific users with `/user/queue/*`

### Separation of Concerns

We delegated business logic to services:
- `ChatMessageService` handles message validation, persistence, and broadcasting
- `ChatRoomService` handles membership management
- Controller focuses only on STOMP protocol concerns and user extraction

This makes the code testable and maintainable.

### System Messages for JOIN/LEAVE

We created separate system messages for JOIN/LEAVE events:
- Uses `MessageType` enum to distinguish from regular TEXT messages
- Allows frontend to render these differently (e.g., gray italics)
- Provides room awareness without cluttering the message service

### User-Specific Error Queues

Errors go to `/user/queue/errors` instead of broadcasting to everyone:
- Privacy: Other users don't see your errors
- Relevance: Only the affected user needs to know
- Security: Prevents information leakage

## Alternatives Considered

### Alternative 1: Return Values Instead of Manual Broadcasting

We could have used `@SendTo("/topic/room/{roomId}")` on the handler methods to automatically send return values to topics. We chose manual broadcasting with `SimpMessagingTemplate` because:
- More explicit control over what gets sent
- Easier to send to multiple destinations if needed
- Clearer separation between service broadcasting (for messages) and controller broadcasting (for system events)

### Alternative 2: Service Layer Handles JOIN/LEAVE Broadcasting

We could have moved JOIN/LEAVE message creation to the service layer. We kept it in the controller because:
- These are protocol-level concerns (STOMP system messages)
- Service layer focuses on business logic (membership management)
- Controller is the right place for STOMP-specific message construction

### Alternative 3: Separate Controllers for Each Concern

We could have split this into `ChatMessageController`, `RoomJoinController`, and `RoomLeaveController`. We kept them together because:
- All handle STOMP messages for the same domain (chat)
- Small controller with clear responsibilities
- Easier to understand the complete STOMP API in one place

### Alternative 4: Custom Exception Types

We could have created specific exception types like `MessageSendException`, `RoomJoinException`, etc. We used generic exception handling because:
- This is a learning project with simple error handling needs
- The error message provides sufficient context
- Can be enhanced later if needed

## Key Concepts

### 1. STOMP Protocol Basics

STOMP is a simple text-based protocol for messaging:
- **CONNECT**: Client connects to server
- **SUBSCRIBE**: Client subscribes to a destination (topic or queue)
- **SEND**: Client sends a message to a destination
- **MESSAGE**: Server sends a message to subscribed clients
- **DISCONNECT**: Client disconnects

### 2. Spring STOMP Destination Patterns

Spring uses prefixes to route messages:
- `/app/*`: Application destinations (handled by `@MessageMapping` methods)
- `/topic/*`: Pub/sub topics (many subscribers receive the same message)
- `/queue/*`: Point-to-point queues (one subscriber receives each message)
- `/user/*`: User-specific destinations (Spring adds username automatically)

### 3. Message Flow

When a client sends a message:
1. Client sends STOMP SEND frame to `/app/chat.send/123`
2. Spring routes to `@MessageMapping("/chat.send/{roomId}")` method
3. Controller extracts user, validates, calls service
4. Service persists message and broadcasts to `/topic/room/123`
5. All clients subscribed to `/topic/room/123` receive the message

### 4. Principal and Authentication

The `Principal` parameter is automatically injected by Spring:
- Contains the authenticated user's identity
- Set during WebSocket connection handshake
- `principal.getName()` returns the username
- Available in all `@MessageMapping` methods

### 5. SimpMessagingTemplate

This is Spring's template for sending STOMP messages:
- `convertAndSend(destination, payload)`: Send to a topic/queue
- `convertAndSendToUser(username, destination, payload)`: Send to specific user
- Handles message conversion (Java object → JSON)
- Thread-safe for concurrent use

### 6. Exception Handling in STOMP

`@MessageExceptionHandler` catches exceptions in message handlers:
- Similar to `@ExceptionHandler` in REST controllers
- Can return a value to send as error message
- Use `@SendToUser` to send errors to the user who caused them
- Prevents one user's errors from affecting others

## Potential Pitfalls

### 1. Forgetting to Extract User from Principal

**Pitfall**: Using `principal.getName()` directly as a user ID.

**Why it's wrong**: `principal.getName()` returns the username (String), not the user ID (Long).

**Solution**: Always look up the User entity from the repository using the username.

```java
// Wrong
chatMessageService.sendMessage(Long.parseLong(principal.getName()), ...);

// Right
User user = userRepository.findByUsername(principal.getName())
    .orElseThrow(() -> new IllegalArgumentException("User not found"));
chatMessageService.sendMessage(user.getId(), ...);
```

### 2. Not Setting MessageType for System Messages

**Pitfall**: Creating JOIN/LEAVE messages without setting `messageType`.

**Why it's wrong**: Frontend can't distinguish system messages from user messages.

**Solution**: Always set `messageType` to `JOIN`, `LEAVE`, or `SYSTEM` for non-text messages.

```java
// Wrong
Message joinMessage = new Message();
joinMessage.setContent("User joined");
// messageType defaults to TEXT

// Right
Message joinMessage = new Message();
joinMessage.setContent("User joined");
joinMessage.setMessageType(MessageType.JOIN);
```

### 3. Broadcasting Before Persisting

**Pitfall**: Broadcasting a message before it's saved to the database.

**Why it's wrong**: If the database save fails, clients receive a message that doesn't exist in history.

**Solution**: The service layer persists first, then broadcasts. Never broadcast in the controller before calling the service.

### 4. Using @SendTo with Dynamic Destinations

**Pitfall**: Trying to use `@SendTo("/topic/room/{roomId}")` with path variables.

**Why it's wrong**: `@SendTo` doesn't support dynamic destinations with variables.

**Solution**: Use `SimpMessagingTemplate.convertAndSend()` for dynamic destinations.

```java
// Wrong - doesn't work
@MessageMapping("/room.join/{roomId}")
@SendTo("/topic/room/{roomId}")
public Message joinRoom(@DestinationVariable Long roomId) { ... }

// Right
@MessageMapping("/room.join/{roomId}")
public void joinRoom(@DestinationVariable Long roomId) {
    // ...
    messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
}
```

### 5. Not Handling Null Principal

**Pitfall**: Assuming `principal` is never null.

**Why it's wrong**: If authentication fails or is bypassed, `principal` could be null.

**Solution**: Check for null in the exception handler (we did this with the ternary operator).

```java
@MessageExceptionHandler
public ErrorResponse handleException(Exception exception, Principal principal) {
    logger.error("Error for user: {}", 
                principal != null ? principal.getName() : "unknown", exception);
    // ...
}
```

### 6. Forgetting to Set Timestamp

**Pitfall**: Creating system messages without setting the timestamp.

**Why it's wrong**: Messages appear out of order or with incorrect times.

**Solution**: Always set `timestamp` to `LocalDateTime.now()` for manually created messages.

```java
// Wrong
Message joinMessage = new Message();
joinMessage.setContent("User joined");
// timestamp is null

// Right
Message joinMessage = new Message();
joinMessage.setContent("User joined");
joinMessage.setTimestamp(LocalDateTime.now());
```

### 7. Broadcasting to Wrong Destination

**Pitfall**: Broadcasting to `/app/room/{roomId}` instead of `/topic/room/{roomId}`.

**Why it's wrong**: `/app/*` destinations are for client-to-server messages, not server-to-client broadcasts.

**Solution**: Always broadcast to `/topic/*` or `/queue/*` destinations.

```java
// Wrong - clients can't subscribe to /app/*
messagingTemplate.convertAndSend("/app/room/" + roomId, message);

// Right - clients subscribe to /topic/*
messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
```

## What You Learned

In this lesson, you learned how to:

1. **Build a STOMP message controller** using `@MessageMapping` annotations to handle WebSocket messages instead of HTTP requests

2. **Extract authenticated users** from the `Principal` parameter and look them up in the database

3. **Delegate to service layers** for business logic while keeping the controller focused on protocol concerns

4. **Create and broadcast system messages** for JOIN/LEAVE events using `MessageType` enum and `SimpMessagingTemplate`

5. **Handle STOMP exceptions** with `@MessageExceptionHandler` and send errors to user-specific queues

6. **Understand STOMP destination patterns**: `/app/*` for client-to-server, `/topic/*` for pub/sub, `/user/queue/*` for user-specific messages

7. **Avoid common pitfalls** like using wrong destinations, forgetting to set message types, and not handling null principals

This controller is a key component of the real-time chat system, bridging the gap between STOMP protocol messages and the business logic in the service layer. It demonstrates how Spring WebSocket integrates with Spring Security (Principal) and Spring Messaging (STOMP) to create a robust real-time communication system.

Next steps could include:
- Writing integration tests with STOMP test clients
- Adding message validation in the controller
- Implementing typing indicators with additional STOMP endpoints
- Adding rate limiting to prevent message spam
