# Lesson: Replacing IllegalArgumentException with RoomNotFoundException for Proper 404 Status Codes

## Task Context

This lesson covers fixing a bug where REST API endpoints were returning incorrect HTTP status codes when chat rooms were not found. The application was returning `400 Bad Request` instead of the semantically correct `404 Not Found` status code.

**The Problem:**
- When requesting `/api/rooms/{id}/messages` or `/api/rooms/{id}` for a nonexistent room
- The system threw `IllegalArgumentException` 
- The global exception handler mapped this to `400 Bad Request`
- HTTP semantics require `404 Not Found` for missing resources

**The Solution:**
Replace `IllegalArgumentException` with `RoomNotFoundException` in three key locations:
1. `ChatRoomService.getRoomById()` - The service layer method that retrieves rooms
2. `MessageHistoryController.getMessageHistory()` - The endpoint for fetching room messages
3. `ChatRoomController.getRoomById()` - The endpoint for fetching room details

## Files Modified

- `src/main/java/org/example/chat/service/ChatRoomService.java` (modified)
- `src/main/java/org/example/chat/controller/MessageHistoryController.java` (modified)
- `src/main/java/org/example/chat/controller/ChatRoomController.java` (modified)

## Step-by-Step Changes

### Step 1: Update ChatRoomService

**What we changed:**
- Added import for `RoomNotFoundException`
- Modified `getRoomById()` to throw `RoomNotFoundException` instead of `IllegalArgumentException`
- Updated the Javadoc to reflect the new exception type

**Before:**
```java
public ChatRoom getRoomById(Long roomId) {
    logger.debug("Retrieving chat room by ID: {}", roomId);
    return chatRoomRepository.findById(roomId)
        .orElseThrow(() -> {
            logger.warn("Chat room not found: {}", roomId);
            return new IllegalArgumentException("Chat room not found");
        });
}
```

**After:**
```java
public ChatRoom getRoomById(Long roomId) {
    logger.debug("Retrieving chat room by ID: {}", roomId);
    return chatRoomRepository.findById(roomId)
        .orElseThrow(() -> {
            logger.warn("Chat room not found: {}", roomId);
            return new RoomNotFoundException(roomId);
        });
}
```

**Why this matters:** The service layer is the source of truth for room retrieval. By fixing it here, any controller that calls `getRoomById()` automatically gets the correct exception.

### Step 2: Update MessageHistoryController

**What we changed:**
- Added import for `RoomNotFoundException`
- Modified the room lookup in `getMessageHistory()` to throw `RoomNotFoundException`

**Before:**
```java
ChatRoom chatRoom = chatRoomRepository.findById(roomId)
    .orElseThrow(() -> {
        logger.warn("Message history request failed: chat room not found: {}", roomId);
        return new IllegalArgumentException("Chat room not found");
    });
```

**After:**
```java
ChatRoom chatRoom = chatRoomRepository.findById(roomId)
    .orElseThrow(() -> {
        logger.warn("Message history request failed: chat room not found: {}", roomId);
        return new RoomNotFoundException(roomId);
    });
```

**Why this matters:** This controller directly queries the repository instead of using the service method. We need to ensure consistency across all room lookups.

### Step 3: Update ChatRoomController

**What we changed:**
- Added import for `RoomNotFoundException`
- Updated the catch block in `getRoomById()` to catch `RoomNotFoundException` instead of `IllegalArgumentException`

**Before:**
```java
try {
    ChatRoom chatRoom = chatRoomService.getRoomById(id);
    // ... rest of method
} catch (IllegalArgumentException e) {
    logger.warn("Chat room not found: {}", id);
    throw e;
}
```

**After:**
```java
try {
    ChatRoom chatRoom = chatRoomService.getRoomById(id);
    // ... rest of method
} catch (RoomNotFoundException e) {
    logger.warn("Chat room not found: {}", id);
    throw e;
}
```

**Why this matters:** Since `ChatRoomService.getRoomById()` now throws `RoomNotFoundException`, the controller needs to catch the correct exception type.

## Why This Approach

### HTTP Status Code Semantics

HTTP status codes have specific meanings defined by RFC 7231:

- **400 Bad Request**: The request is malformed or contains invalid syntax (e.g., invalid JSON, missing required fields)
- **404 Not Found**: The requested resource does not exist on the server

**Our bug:** We were using `IllegalArgumentException` which the global exception handler maps to 400. This tells the client "your request is malformed" when we really mean "the room you're looking for doesn't exist."

**The fix:** Use `RoomNotFoundException` which the global exception handler maps to 404. This correctly tells the client "the resource doesn't exist."

### Custom Exception Hierarchy

The application has a well-designed exception hierarchy:

```
ChatApplicationException (base class)
├── RoomNotFoundException (404 Not Found)
├── UserNotFoundException (404 Not Found)
├── UnauthorizedException (403 Forbidden)
└── ValidationException (400 Bad Request)
```

Each custom exception is mapped to the appropriate HTTP status code in the `GlobalExceptionHandler`. By using the right exception, we get the right status code automatically.

### Consistency Across Layers

We updated three locations to ensure consistency:

1. **Service Layer** (`ChatRoomService`): The core business logic layer
2. **Controller Layer** (`MessageHistoryController`, `ChatRoomController`): The HTTP interface layer

This ensures that no matter where a room lookup fails, the same exception is thrown and the same status code is returned.

## Alternatives Considered

### Alternative 1: Keep IllegalArgumentException and Update GlobalExceptionHandler

We could have kept throwing `IllegalArgumentException` and modified the `GlobalExceptionHandler` to inspect the exception message and return 404 for "not found" messages.

**Why we didn't do this:**
- Fragile: Relies on string matching in exception messages
- Ambiguous: `IllegalArgumentException` is too generic and could represent many different error conditions
- Poor separation of concerns: Exception handlers shouldn't need to parse messages to determine status codes

### Alternative 2: Return Optional from Service Methods

We could have changed `getRoomById()` to return `Optional<ChatRoom>` and let controllers handle the empty case.

**Why we didn't do this:**
- More boilerplate: Every controller would need to check for empty and throw the exception
- Inconsistent: Some methods might forget to check, leading to `NoSuchElementException`
- Less expressive: Exceptions clearly signal error conditions in the method signature

### Alternative 3: Create a Generic NotFoundException

We could have created a single `NotFoundException` instead of specific exceptions like `RoomNotFoundException` and `UserNotFoundException`.

**Why we didn't do this:**
- Less specific: Harder to debug when you don't know what type of resource is missing
- Less flexible: Can't add room-specific or user-specific error handling later
- The existing codebase already has specific exceptions, so we follow that pattern

## Key Concepts

### 1. Exception-Driven Error Handling in REST APIs

Modern REST APIs use exceptions to signal error conditions:
- Throw specific exceptions in business logic
- Let a global exception handler catch them
- Map exceptions to appropriate HTTP status codes

**Benefits:**
- Clean separation: Business logic doesn't know about HTTP
- Centralized: All status code mapping in one place
- Consistent: Same exception always produces same status code

### 2. Semantic HTTP Status Codes

Status codes communicate the outcome of a request:
- **2xx**: Success
- **4xx**: Client error (bad request, not found, unauthorized, etc.)
- **5xx**: Server error (internal error, service unavailable, etc.)

Using the correct status code helps clients:
- Understand what went wrong
- Implement proper retry logic (retry 5xx, don't retry 4xx)
- Display appropriate error messages to users

### 3. The Optional.orElseThrow Pattern

This is a common pattern in Java for handling missing data:

```java
return repository.findById(id)
    .orElseThrow(() -> new NotFoundException(id));
```

**How it works:**
- `findById()` returns `Optional<T>`
- If present, `orElseThrow()` returns the value
- If empty, `orElseThrow()` throws the exception

**Why it's useful:**
- Concise: One line instead of if/else
- Safe: Forces you to handle the empty case
- Expressive: Clearly shows the error condition

### 4. Constructor Overloading for Exceptions

Notice `RoomNotFoundException` has multiple constructors:

```java
public RoomNotFoundException(String message)
public RoomNotFoundException(Long roomId)
public RoomNotFoundException(String message, Throwable cause)
```

**Why this is useful:**
- Convenience: Can pass just the ID and get a formatted message
- Flexibility: Can provide custom messages when needed
- Chaining: Can wrap other exceptions with `cause`

We used the `RoomNotFoundException(Long roomId)` constructor which automatically formats the message as "Chat room not found with id: {roomId}".

## Potential Pitfalls

### Pitfall 1: Forgetting to Import the Exception

**Symptom:** Compilation error "cannot find symbol: class RoomNotFoundException"

**Solution:** Add the import statement:
```java
import org.example.chat.exception.RoomNotFoundException;
```

**Prevention:** Modern IDEs auto-import, but always check imports when adding new exception types.

### Pitfall 2: Catching the Wrong Exception Type

**Symptom:** Exception is not caught, propagates to global handler unexpectedly

**Example:**
```java
try {
    ChatRoom room = service.getRoomById(id);
} catch (IllegalArgumentException e) {  // Wrong! Service now throws RoomNotFoundException
    // This won't catch the exception
}
```

**Solution:** Update catch blocks when you change what exceptions are thrown:
```java
} catch (RoomNotFoundException e) {  // Correct
```

**Prevention:** Run tests after changing exception types to catch these issues.

### Pitfall 3: Inconsistent Exception Usage

**Symptom:** Some places throw `RoomNotFoundException`, others throw `IllegalArgumentException` for the same condition

**Example:**
```java
// In one controller
throw new RoomNotFoundException(roomId);

// In another controller
throw new IllegalArgumentException("Room not found");  // Inconsistent!
```

**Solution:** Search the codebase for all places that handle "room not found" and update them all.

**Prevention:** Use IDE "Find Usages" to locate all room lookups and ensure consistency.

### Pitfall 4: Not Updating Javadoc

**Symptom:** Documentation says method throws `IllegalArgumentException` but it actually throws `RoomNotFoundException`

**Solution:** Update the `@throws` tag in Javadoc:
```java
/**
 * @throws RoomNotFoundException if room is not found
 */
```

**Prevention:** Review Javadoc when changing method signatures or exception types.

### Pitfall 5: Breaking Existing Exception Handlers

**Symptom:** Code that was catching `IllegalArgumentException` for room lookups stops working

**Example:**
```java
// Somewhere else in the codebase
try {
    service.getRoomById(id);
} catch (IllegalArgumentException e) {
    // This won't catch RoomNotFoundException
}
```

**Solution:** Search for all catch blocks that might be affected and update them.

**Prevention:** Run the full test suite to catch any broken exception handling.

## What You Learned

### Core Takeaways

1. **HTTP status codes matter**: Use 404 for missing resources, not 400
2. **Custom exceptions are powerful**: They map business errors to HTTP semantics
3. **Consistency is key**: Update all locations that handle the same error condition
4. **Exception hierarchies help**: Specific exceptions are better than generic ones

### Practical Skills

- How to replace one exception type with another across multiple files
- How to use custom exceptions to control HTTP status codes
- How to maintain consistency across service and controller layers
- How to use the `Optional.orElseThrow()` pattern effectively

### Design Principles

- **Separation of concerns**: Business logic throws domain exceptions, not HTTP exceptions
- **Single responsibility**: Exception handlers map exceptions to status codes
- **Fail fast**: Throw exceptions immediately when errors occur
- **Be specific**: Use specific exception types for specific error conditions

### Next Steps

Now that you understand exception-driven error handling:
- Look for other places where `IllegalArgumentException` might be misused
- Consider creating custom exceptions for other domain errors
- Review the `GlobalExceptionHandler` to understand all exception mappings
- Think about how to test exception handling in your controllers

### Testing This Change

To verify this fix works:
1. Start the application
2. Make a request to `/api/rooms/99999` (nonexistent room)
3. Verify the response is `404 Not Found` instead of `400 Bad Request`
4. Check the response body contains the error message with the room ID

Integration tests should also verify:
- Existing rooms still return 200 OK
- Invalid room IDs return 404 Not Found
- The error response format is correct
