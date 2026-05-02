# Lesson: Fixing Authorization Status Codes with UnauthorizedException

## Task Context

This task addresses a critical bug in the chat application's REST API where unauthorized access to chat rooms was returning incorrect HTTP status codes (400 Bad Request or 500 Internal Server Error) instead of the proper 403 Forbidden status code.

The bug manifested in two endpoints:
- `GET /api/rooms/{id}/messages` - When a user tried to access message history for a room they weren't a member of
- `GET /api/rooms/{id}` - When a user tried to view details of a room they weren't a member of

The root cause was that the controllers were throwing `IllegalArgumentException` for membership validation failures, which the global exception handler mapped to 400 Bad Request. The correct approach is to throw `UnauthorizedException`, which maps to 403 Forbidden.

## Files Modified

- `src/main/java/org/example/chat/controller/MessageHistoryController.java` (modified)
- `src/main/java/org/example/chat/controller/ChatRoomController.java` (modified)

## Step-by-Step Changes

### Step 1: Update MessageHistoryController Exception Type

**What we changed:**
In `MessageHistoryController.java`, we replaced the `IllegalArgumentException` with `UnauthorizedException` when membership validation fails.

**Before:**
```java
RoomMembership membership = roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
    .orElseThrow(() -> {
        logger.warn("Message history request denied: user {} is not a member of room {}", 
                   currentUser.getId(), roomId);
        return new IllegalArgumentException("User is not a member of this chat room");
    });
```

**After:**
```java
RoomMembership membership = roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
    .orElseThrow(() -> {
        logger.warn("Message history request denied: user {} is not a member of room {}", 
                   currentUser.getId(), roomId);
        return new UnauthorizedException("User is not a member of this chat room");
    });
```

We also added the import statement:
```java
import org.example.chat.exception.UnauthorizedException;
```

### Step 2: Add Membership Validation to ChatRoomController

**What we changed:**
The `getRoomById()` method in `ChatRoomController.java` previously had no membership validation at all. We added:

1. Changed the method signature to accept `@AuthenticationPrincipal UserDetails userDetails`
2. Retrieved the authenticated user from the database
3. Added membership validation using `RoomMembershipRepository`
4. Added proper exception handling for `UnauthorizedException`

**Before:**
```java
@GetMapping("/{id}")
public ResponseEntity<ChatRoomResponse> getRoomById(@PathVariable Long id) {
    logger.debug("Retrieving chat room with ID: {}", id);

    try {
        ChatRoom chatRoom = chatRoomService.getRoomById(id);
        ChatRoomResponse response = ChatRoomResponse.from(chatRoom);

        logger.debug("Retrieved chat room: {}", chatRoom.getName());
        return ResponseEntity.ok(response);
    } catch (RoomNotFoundException e) {
        logger.warn("Chat room not found: {}", id);
        throw e;
    }
}
```

**After:**
```java
@GetMapping("/{id}")
public ResponseEntity<ChatRoomResponse> getRoomById(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails) {
    
    logger.debug("Retrieving chat room with ID: {}", id);

    try {
        // Get the authenticated user
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        // Get the chat room
        ChatRoom chatRoom = chatRoomService.getRoomById(id);
        
        // Validate that the user is a member of the room
        roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
            .orElseThrow(() -> new UnauthorizedException("User is not a member of this chat room"));

        ChatRoomResponse response = ChatRoomResponse.from(chatRoom);
        logger.debug("Retrieved chat room: {}", chatRoom.getName());
        return ResponseEntity.ok(response);
    } catch (RoomNotFoundException e) {
        logger.warn("Chat room not found: {}", id);
        throw e;
    } catch (UnauthorizedException e) {
        logger.warn("Unauthorized access to chat room: {}", id);
        throw e;
    }
}
```

### Step 3: Add Required Dependencies to ChatRoomController

**What we changed:**
We added the `RoomMembershipRepository` dependency to the controller:

1. Added the import statement for `UnauthorizedException` and `RoomMembershipRepository`
2. Added a private field for `roomMembershipRepository`
3. Updated the constructor to inject `RoomMembershipRepository`

**Constructor before:**
```java
public ChatRoomController(ChatRoomService chatRoomService, UserRepository userRepository) {
    this.chatRoomService = chatRoomService;
    this.userRepository = userRepository;
}
```

**Constructor after:**
```java
public ChatRoomController(ChatRoomService chatRoomService, 
                        UserRepository userRepository,
                        RoomMembershipRepository roomMembershipRepository) {
    this.chatRoomService = chatRoomService;
    this.userRepository = userRepository;
    this.roomMembershipRepository = roomMembershipRepository;
}
```

## Why This Approach

### HTTP Status Code Semantics

HTTP status codes have specific meanings defined by the HTTP specification:

- **400 Bad Request**: The request was malformed or contains invalid data (e.g., invalid JSON, missing required fields)
- **403 Forbidden**: The server understood the request, but the authenticated user doesn't have permission to access the resource
- **404 Not Found**: The requested resource doesn't exist

In our case, when a user tries to access a chat room they're not a member of:
- The request is well-formed (not a 400)
- The room exists (not a 404)
- The user is authenticated but lacks permission (this is a 403)

### Exception Type Mapping

The application uses a global exception handler that maps exception types to HTTP status codes:

- `IllegalArgumentException` → 400 Bad Request
- `UnauthorizedException` → 403 Forbidden
- `RoomNotFoundException` → 404 Not Found

By throwing the correct exception type, we ensure the correct HTTP status code is returned to the client.

### Security Best Practices

Adding membership validation to `getRoomById()` follows the principle of **authorization at every endpoint**. Even though the endpoint only returns metadata about a room, we should still verify that the user has permission to view that information. This prevents information leakage and ensures consistent security across the API.

## Alternatives Considered

### Alternative 1: Custom Exception Handler for IllegalArgumentException

We could have modified the global exception handler to inspect the exception message and return 403 for membership-related errors:

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    if (e.getMessage().contains("not a member")) {
        return ResponseEntity.status(403).body(new ErrorResponse("FORBIDDEN", e.getMessage()));
    }
    return ResponseEntity.status(400).body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
}
```

**Why we didn't choose this:**
- Fragile: Relies on string matching in exception messages
- Violates separation of concerns: Exception handler shouldn't contain business logic
- Hard to maintain: Every new authorization case would require updating the handler

### Alternative 2: Return 404 Instead of 403

Some APIs return 404 Not Found for resources the user doesn't have access to, hiding the fact that the resource exists:

```java
roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
    .orElseThrow(() -> new RoomNotFoundException(roomId));
```

**Why we didn't choose this:**
- Less transparent: Doesn't distinguish between "doesn't exist" and "you can't access it"
- Harder to debug: Clients can't tell if they have a permission issue or a wrong ID
- Not aligned with the test requirements: The tests explicitly expect 403

### Alternative 3: Service-Layer Authorization

We could have moved the membership validation into the service layer:

```java
public ChatRoom getRoomByIdForUser(Long roomId, Long userId) {
    ChatRoom room = getRoomById(roomId);
    validateMembership(userId, room);
    return room;
}
```

**Why we didn't choose this:**
- The current architecture keeps authorization in the controller layer
- Would require refactoring multiple service methods
- The fix specification explicitly targets controller changes

## Key Concepts

### 1. Exception-Driven HTTP Status Codes

In Spring Boot, you can control HTTP status codes by throwing specific exception types that are mapped by a global exception handler. This approach:
- Centralizes status code logic
- Makes controllers cleaner (no manual ResponseEntity status setting)
- Ensures consistency across the API

### 2. Authorization vs Authentication

- **Authentication**: Verifying who the user is (handled by Spring Security)
- **Authorization**: Verifying what the user can do (handled by our membership checks)

Both are necessary for secure APIs. Authentication happens first (via JWT tokens), then authorization checks verify specific permissions.

### 3. Repository Query Methods

Spring Data JPA allows you to define query methods by naming convention:

```java
Optional<RoomMembership> findByUserAndChatRoom(User user, ChatRoom room);
```

This method automatically generates a query like:
```sql
SELECT * FROM room_membership WHERE user_id = ? AND chat_room_id = ?
```

The `Optional` return type allows us to use `.orElseThrow()` for clean error handling.

### 4. Dependency Injection in Spring

When we added `RoomMembershipRepository` to the constructor, Spring automatically injects it at runtime. This is called **constructor injection** and is the recommended approach because:
- Makes dependencies explicit
- Enables immutable fields (final)
- Easier to test (can pass mock repositories)

## Potential Pitfalls

### Pitfall 1: Forgetting to Add Imports

When you change exception types, you must add the corresponding import statement. Without it, you'll get a compilation error:

```
error: cannot find symbol
  symbol:   class UnauthorizedException
```

**Solution**: Always add the import when referencing a new class:
```java
import org.example.chat.exception.UnauthorizedException;
```

### Pitfall 2: Not Catching the New Exception Type

If you add a new exception type but don't catch it in the try-catch block, it will propagate up and might be handled incorrectly:

```java
try {
    // ... code that throws UnauthorizedException
} catch (RoomNotFoundException e) {
    // UnauthorizedException won't be caught here!
    throw e;
}
```

**Solution**: Add a catch block for the new exception type:
```java
} catch (UnauthorizedException e) {
    logger.warn("Unauthorized access to chat room: {}", id);
    throw e;
}
```

### Pitfall 3: Inconsistent Authorization Checks

If you add authorization to one endpoint but forget others, you create security holes. For example, if we fixed `getRoomById()` but not `getRoomMembers()`, users could still access member lists for rooms they're not in.

**Solution**: Audit all endpoints that access room data and ensure consistent authorization checks.

### Pitfall 4: Order of Validation Matters

In `getRoomById()`, we validate in this order:
1. User exists
2. Room exists
3. User is a member

This order is important! If we checked membership before checking if the room exists, we might throw 403 for a non-existent room, leaking information about which room IDs exist.

**Best practice**: Check existence before authorization.

### Pitfall 5: Breaking Existing Tests

When you change exception types, existing tests that expect `IllegalArgumentException` will fail. You need to update tests to expect `UnauthorizedException` instead.

**Solution**: After making code changes, run the test suite and update any tests that verify exception types.

## What You Learned

1. **HTTP status codes have specific meanings**: Use 403 for authorization failures, not 400
2. **Exception types control HTTP responses**: Throw the right exception to get the right status code
3. **Authorization should be explicit**: Add membership checks to every endpoint that accesses protected resources
4. **Dependency injection is powerful**: Spring automatically wires up dependencies through constructor parameters
5. **Security is layered**: Authentication (who you are) + Authorization (what you can do) = secure API
6. **Code changes require test updates**: When you change exception types, update tests accordingly
7. **Order of validation matters**: Check existence before authorization to avoid information leakage

This fix ensures that the chat application returns proper HTTP status codes for authorization failures, making the API more standards-compliant and easier for clients to handle correctly.
