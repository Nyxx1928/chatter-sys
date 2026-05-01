# Lesson: Creating Custom Exception Classes for Comprehensive Error Handling

## Task Context

In a real-time chat system, errors can occur at multiple layers: authentication failures, resource not found errors, validation issues, and WebSocket connection problems. To handle these errors consistently and provide meaningful feedback to clients, we need a well-designed exception hierarchy.

This lesson covers the implementation of a custom exception hierarchy for the real-time chat system, including:
- A base `ChatApplicationException` class with error codes and HTTP status
- Specific exception subclasses for different error scenarios
- How these exceptions integrate with Spring Boot's error handling

**Requirements Addressed:**
- Requirement 9.1: Comprehensive error logging with context
- Requirement 9.2: Appropriate error responses for different failure scenarios
- Requirement 9.3: Graceful error handling for WebSocket/STOMP operations

## Files Modified

- `src/main/java/org/example/chat/exception/ChatApplicationException.java` (created)
- `src/main/java/org/example/chat/exception/UserNotFoundException.java` (created)
- `src/main/java/org/example/chat/exception/RoomNotFoundException.java` (created)
- `src/main/java/org/example/chat/exception/UnauthorizedException.java` (created)
- `src/main/java/org/example/chat/exception/ValidationException.java` (created)
- `src/main/java/org/example/chat/exception/WebSocketException.java` (created)

## Step-by-Step Changes

### Step 1: Create the Base Exception Class

The `ChatApplicationException` serves as the foundation for all application-specific exceptions. It extends `RuntimeException` (unchecked exception) and adds two critical fields:

1. **errorCode**: A machine-readable string identifier (e.g., "USER_NOT_FOUND")
2. **httpStatus**: The appropriate HTTP status code for REST API responses

```java
public class ChatApplicationException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    public ChatApplicationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public ChatApplicationException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
```

**Key Design Decisions:**
- Extends `RuntimeException` so exceptions don't need to be declared in method signatures
- Immutable fields (`final`) ensure error codes and status can't be changed after creation
- Two constructors: one for simple exceptions, one for wrapping underlying causes
- Uses Spring's `HttpStatus` enum for type-safe HTTP status codes

### Step 2: Create UserNotFoundException

This exception is thrown when a user lookup fails (by ID, username, or email).

```java
public class UserNotFoundException extends ChatApplicationException {
    public UserNotFoundException(String message) {
        super(message, "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public UserNotFoundException(Long userId) {
        super("User not found with id: " + userId, "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, "USER_NOT_FOUND", HttpStatus.NOT_FOUND, cause);
    }
}
```

**Usage Examples:**
```java
// In a service method
User user = userRepository.findById(userId)
    .orElseThrow(() -> new UserNotFoundException(userId));

// Or with custom message
throw new UserNotFoundException("User with username 'john' not found");
```

### Step 3: Create RoomNotFoundException

Similar to `UserNotFoundException`, but for chat rooms.

```java
public class RoomNotFoundException extends ChatApplicationException {
    public RoomNotFoundException(String message) {
        super(message, "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public RoomNotFoundException(Long roomId) {
        super("Chat room not found with id: " + roomId, "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public RoomNotFoundException(String message, Throwable cause) {
        super(message, "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, cause);
    }
}
```

**Usage Examples:**
```java
ChatRoom room = chatRoomRepository.findById(roomId)
    .orElseThrow(() -> new RoomNotFoundException(roomId));
```

### Step 4: Create UnauthorizedException

This exception is thrown when a user attempts an action they don't have permission to perform.

```java
public class UnauthorizedException extends ChatApplicationException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, "UNAUTHORIZED", HttpStatus.FORBIDDEN, cause);
    }
}
```

**Important Note:** Uses `HttpStatus.FORBIDDEN` (403) rather than `HttpStatus.UNAUTHORIZED` (401). In HTTP semantics:
- **401 Unauthorized**: Authentication is required (user needs to log in)
- **403 Forbidden**: User is authenticated but lacks permission

**Usage Examples:**
```java
// Check if user is a member of the room
if (!isMember(user, room)) {
    throw new UnauthorizedException("You are not a member of this chat room");
}

// Check if user has moderator role
if (membership.getRole() != MemberRole.MODERATOR) {
    throw new UnauthorizedException("Only moderators can perform this action");
}
```

### Step 5: Create ValidationException

This exception is thrown when input validation fails (invalid data format, missing required fields, constraint violations).

```java
public class ValidationException extends ChatApplicationException {
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
}
```

**Usage Examples:**
```java
// Validate message content
if (content == null || content.trim().isEmpty()) {
    throw new ValidationException("Message content cannot be empty");
}

// Validate username format
if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
    throw new ValidationException("Username must be 3-20 alphanumeric characters");
}

// Validate room name uniqueness
if (chatRoomRepository.existsByName(roomName)) {
    throw new ValidationException("A room with this name already exists");
}
```

### Step 6: Create WebSocketException

This exception is thrown when WebSocket or STOMP operations fail.

```java
public class WebSocketException extends ChatApplicationException {
    public WebSocketException(String message) {
        super(message, "WEBSOCKET_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public WebSocketException(String message, Throwable cause) {
        super(message, "WEBSOCKET_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
```

**Usage Examples:**
```java
// In WebSocket event listener
try {
    messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
} catch (Exception e) {
    throw new WebSocketException("Failed to broadcast message", e);
}

// In STOMP message handler
if (stompSession == null || !stompSession.isConnected()) {
    throw new WebSocketException("STOMP session is not connected");
}
```

## Why This Approach

### 1. Consistent Error Handling

All exceptions share the same structure (message, errorCode, httpStatus), making it easy to handle them uniformly in a global exception handler:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ChatApplicationException.class)
    public ResponseEntity<ErrorResponse> handleChatException(ChatApplicationException ex) {
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }
}
```

### 2. Type-Safe Error Codes

Using specific exception classes instead of generic exceptions with string error codes provides:
- **Compile-time safety**: Can't misspell exception types
- **IDE support**: Auto-completion and refactoring tools work correctly
- **Clear intent**: `throw new UserNotFoundException(userId)` is more readable than `throw new GenericException("USER_NOT_FOUND", userId)`

### 3. Appropriate HTTP Status Codes

Each exception maps to the correct HTTP status:
- `UserNotFoundException`, `RoomNotFoundException` → 404 Not Found
- `UnauthorizedException` → 403 Forbidden
- `ValidationException` → 400 Bad Request
- `WebSocketException` → 500 Internal Server Error

This ensures REST API responses follow HTTP semantics correctly.

### 4. Convenience Constructors

Multiple constructors make exceptions easy to use in different scenarios:
```java
// Quick throw with ID
throw new UserNotFoundException(userId);

// Custom message
throw new UserNotFoundException("User not found with username: " + username);

// Wrap underlying exception
throw new WebSocketException("Connection failed", ioException);
```

### 5. Unchecked Exceptions

Extending `RuntimeException` means:
- No need to declare exceptions in method signatures
- Exceptions can propagate up the call stack automatically
- Spring's `@ControllerAdvice` can catch them globally

This is appropriate for application-level errors that should be handled centrally rather than at each call site.

## Alternatives Considered

### Alternative 1: Single Generic Exception with Error Codes

```java
// Not chosen
throw new ChatException("USER_NOT_FOUND", "User not found: " + userId);
```

**Pros:**
- Fewer classes to maintain
- Flexible error codes

**Cons:**
- No compile-time safety (can misspell error codes)
- Harder to catch specific exception types
- Less discoverable (need to look up error code strings)

### Alternative 2: Checked Exceptions

```java
// Not chosen
public class UserNotFoundException extends Exception { ... }
```

**Pros:**
- Forces explicit error handling
- Documents possible failures in method signatures

**Cons:**
- Verbose (every method must declare or catch)
- Doesn't work well with Spring's global exception handling
- Clutters code with try-catch blocks

### Alternative 3: Spring's Built-in Exceptions

```java
// Not chosen
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
```

**Pros:**
- No custom classes needed
- Built into Spring

**Cons:**
- No error codes for client-side handling
- Less semantic (all exceptions look the same)
- Harder to add custom fields or behavior

### Alternative 4: Error Code Enum

```java
// Not chosen
public enum ErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND),
    ROOM_NOT_FOUND("ROOM_NOT_FOUND", HttpStatus.NOT_FOUND);
    // ...
}

throw new ChatException(ErrorCode.USER_NOT_FOUND, "User not found");
```

**Pros:**
- Centralized error code definitions
- Type-safe error codes

**Cons:**
- Still need exception classes for type-specific catching
- More complex setup
- Harder to add exception-specific constructors

## Key Concepts

### 1. Exception Hierarchy

```
Throwable
  └─ Exception
      └─ RuntimeException
          └─ ChatApplicationException (base)
              ├─ UserNotFoundException
              ├─ RoomNotFoundException
              ├─ UnauthorizedException
              ├─ ValidationException
              └─ WebSocketException
```

**Benefits of hierarchy:**
- Can catch all chat exceptions with `catch (ChatApplicationException e)`
- Can catch specific exceptions when needed
- Shared behavior in base class (errorCode, httpStatus)

### 2. HTTP Status Code Semantics

| Status Code | Meaning | When to Use |
|-------------|---------|-------------|
| 400 Bad Request | Client sent invalid data | Validation failures |
| 403 Forbidden | Authenticated but not authorized | Permission denied |
| 404 Not Found | Resource doesn't exist | User/room not found |
| 500 Internal Server Error | Server-side failure | WebSocket errors, unexpected failures |

### 3. Error Codes for Client-Side Handling

Error codes allow frontend applications to handle errors programmatically:

```typescript
// Frontend TypeScript
try {
  await api.getUser(userId);
} catch (error) {
  if (error.code === 'USER_NOT_FOUND') {
    // Show "user not found" message
  } else if (error.code === 'UNAUTHORIZED') {
    // Redirect to login
  }
}
```

### 4. Exception Chaining

The `Throwable cause` parameter preserves the original exception:

```java
try {
  // Database operation
} catch (DataAccessException e) {
  throw new WebSocketException("Failed to load user data", e);
}
```

This maintains the full stack trace for debugging while providing a user-friendly message.

### 5. Immutability

Fields are `final` to prevent modification after creation:

```java
private final String errorCode;
private final HttpStatus httpStatus;
```

This ensures exceptions remain consistent and thread-safe.

## Potential Pitfalls

### Pitfall 1: Using 401 Instead of 403

**Wrong:**
```java
public class UnauthorizedException extends ChatApplicationException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED); // Wrong!
    }
}
```

**Correct:**
```java
public class UnauthorizedException extends ChatApplicationException {
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.FORBIDDEN); // Correct!
    }
}
```

**Why:** HTTP 401 means "authentication required" (not logged in), while 403 means "authenticated but forbidden" (logged in but lacks permission).

### Pitfall 2: Exposing Sensitive Information

**Wrong:**
```java
throw new ValidationException("Password hash mismatch: " + passwordHash);
```

**Correct:**
```java
throw new ValidationException("Invalid credentials");
```

**Why:** Exception messages may be logged or sent to clients. Never include sensitive data like passwords, tokens, or internal system details.

### Pitfall 3: Catching and Ignoring Exceptions

**Wrong:**
```java
try {
    userRepository.save(user);
} catch (ChatApplicationException e) {
    // Silently ignore
}
```

**Correct:**
```java
try {
    userRepository.save(user);
} catch (ChatApplicationException e) {
    logger.error("Failed to save user", e);
    throw e; // Re-throw or handle appropriately
}
```

**Why:** Silently catching exceptions hides errors and makes debugging difficult.

### Pitfall 4: Creating Too Many Exception Classes

**Wrong:**
```java
public class UserNotFoundByIdException extends ChatApplicationException { }
public class UserNotFoundByUsernameException extends ChatApplicationException { }
public class UserNotFoundByEmailException extends ChatApplicationException { }
```

**Correct:**
```java
public class UserNotFoundException extends ChatApplicationException {
    public UserNotFoundException(Long userId) { ... }
    public UserNotFoundException(String username) { ... }
}
```

**Why:** Too many exception classes create unnecessary complexity. Use constructor overloading instead.

### Pitfall 5: Not Providing Context in Exception Messages

**Wrong:**
```java
throw new RoomNotFoundException("Not found");
```

**Correct:**
```java
throw new RoomNotFoundException("Chat room not found with id: " + roomId);
```

**Why:** Detailed messages help with debugging and provide better user feedback.

### Pitfall 6: Using Exceptions for Control Flow

**Wrong:**
```java
try {
    User user = userRepository.findById(userId).orElseThrow();
    return user;
} catch (NoSuchElementException e) {
    return createDefaultUser();
}
```

**Correct:**
```java
return userRepository.findById(userId)
    .orElseGet(() -> createDefaultUser());
```

**Why:** Exceptions should represent exceptional conditions, not normal program flow. Use Optional methods instead.

## What You Learned

### Core Takeaways

1. **Exception Hierarchy Design**: Creating a base exception class with common fields (errorCode, httpStatus) provides consistency across all application exceptions.

2. **HTTP Status Code Mapping**: Each exception type maps to an appropriate HTTP status code, ensuring REST API responses follow HTTP semantics.

3. **Type Safety**: Using specific exception classes instead of generic exceptions with string codes provides compile-time safety and better IDE support.

4. **Convenience Constructors**: Multiple constructors make exceptions easy to use in different scenarios (by ID, custom message, with cause).

5. **Unchecked Exceptions**: Extending `RuntimeException` allows exceptions to propagate automatically and be handled globally by Spring's `@ControllerAdvice`.

### Integration with Spring Boot

These custom exceptions integrate seamlessly with Spring Boot's error handling:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ChatApplicationException.class)
    public ResponseEntity<ErrorResponse> handleChatException(ChatApplicationException ex) {
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }
}
```

This single handler catches all custom exceptions and returns consistent error responses.

### Best Practices Applied

1. **Immutability**: Fields are `final` to prevent modification
2. **Exception Chaining**: Support for `Throwable cause` preserves stack traces
3. **Semantic Naming**: Exception names clearly indicate the error condition
4. **Appropriate Granularity**: Not too many, not too few exception classes
5. **Documentation**: Javadoc comments explain when each exception is thrown

### Real-World Usage

These exceptions are used throughout the application:

**In Services:**
```java
public User getUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
}
```

**In Controllers:**
```java
public void validateRoomAccess(User user, ChatRoom room) {
    if (!isMember(user, room)) {
        throw new UnauthorizedException("You are not a member of this room");
    }
}
```

**In WebSocket Handlers:**
```java
public void broadcastMessage(Long roomId, Message message) {
    try {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    } catch (Exception e) {
        throw new WebSocketException("Failed to broadcast message", e);
    }
}
```

### Next Steps

With these exception classes in place, you can:
1. Use them throughout the application for consistent error handling
2. Implement a global exception handler to catch and format errors
3. Add logging to track when exceptions occur
4. Create error response DTOs for client-friendly error messages
5. Write tests to verify exceptions are thrown in the correct scenarios

This exception hierarchy provides a solid foundation for comprehensive error handling in the real-time chat system.
