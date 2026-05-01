# Lesson: Building a Comprehensive Global Exception Handler with Custom Exception Hierarchy

## Task Context

In this lesson, we implemented task 14.2 from the real-time chat system spec: creating a comprehensive global exception handler. The task required us to:

1. Create a custom exception hierarchy with `ChatApplicationException` as the base class
2. Implement specific exception types (UserNotFoundException, RoomNotFoundException, UnauthorizedException, ValidationException, WebSocketException)
3. Enhance the existing GlobalExceptionHandler to handle these custom exceptions
4. Update ErrorResponse to include error codes
5. Ensure all exceptions return appropriate HTTP status codes and structured error responses

This builds on task 14.1, which created the basic GlobalExceptionHandler structure. We're now adding the custom exception hierarchy mentioned in the design document and making the error handling more robust and consistent.

## Files Modified

- `src/main/java/org/example/chat/exception/ChatApplicationException.java` (created)
- `src/main/java/org/example/chat/exception/UserNotFoundException.java` (created)
- `src/main/java/org/example/chat/exception/RoomNotFoundException.java` (created)
- `src/main/java/org/example/chat/exception/UnauthorizedException.java` (created)
- `src/main/java/org/example/chat/exception/ValidationException.java` (created)
- `src/main/java/org/example/chat/exception/WebSocketException.java` (created)
- `src/main/java/org/example/chat/exception/ErrorResponse.java` (modified)
- `src/main/java/org/example/chat/exception/GlobalExceptionHandler.java` (modified)
- `src/test/java/org/example/chat/exception/GlobalExceptionHandlerTest.java` (created)

## Step-by-Step Changes

### Step 1: Create the Base Exception Class

We started by creating `ChatApplicationException`, which serves as the base class for all application-specific exceptions. This class:

- Extends `RuntimeException` (unchecked exception)
- Contains an `errorCode` field for machine-readable error identification
- Contains an `httpStatus` field to specify the HTTP response status
- Provides constructors with and without cause for exception chaining

**Why RuntimeException?** We use unchecked exceptions because most of our exceptions represent programming errors or business rule violations that shouldn't be caught and handled at every level. Spring's transaction management also works better with unchecked exceptions.

### Step 2: Create Specific Exception Types

We created five specific exception classes that extend `ChatApplicationException`:

1. **UserNotFoundException** - For when a user cannot be found (404 Not Found)
2. **RoomNotFoundException** - For when a chat room cannot be found (404 Not Found)
3. **UnauthorizedException** - For authorization failures (403 Forbidden)
4. **ValidationException** - For input validation errors (400 Bad Request)
5. **WebSocketException** - For WebSocket/STOMP errors (500 Internal Server Error)

Each exception class:
- Has a predefined error code (e.g., "USER_NOT_FOUND")
- Has a predefined HTTP status
- Provides convenient constructors for common use cases
- Can accept a custom message or generate one automatically

### Step 3: Update ErrorResponse

We enhanced the `ErrorResponse` class to include an `errorCode` field. This allows clients to:
- Programmatically identify error types without parsing messages
- Implement error-specific handling logic
- Support internationalization (error codes can map to localized messages on the client)

We also added a new constructor that accepts an error code, making it easier to create error responses with codes.

### Step 4: Enhance GlobalExceptionHandler

We added a new `@ExceptionHandler` method specifically for `ChatApplicationException`:

```java
@ExceptionHandler(ChatApplicationException.class)
public ResponseEntity<ErrorResponse> handleChatApplicationException(ChatApplicationException ex)
```

This handler:
- Extracts the HTTP status from the exception
- Logs errors differently based on severity (ERROR for 5xx, WARN for 4xx)
- Creates an ErrorResponse with the error code
- Returns the appropriate HTTP status

We also updated the existing handlers to include error codes in their responses:
- `MethodArgumentNotValidException` → "VALIDATION_ERROR"
- `IllegalArgumentException` → "INVALID_ARGUMENT"
- `IllegalStateException` → "INVALID_STATE"
- Generic `Exception` → "INTERNAL_ERROR"

### Step 5: Write Comprehensive Tests

We created `GlobalExceptionHandlerTest` with tests for:
- Each custom exception type
- Validation exception with field errors
- Standard Java exceptions (IllegalArgumentException, IllegalStateException)
- Generic exception handling

The tests verify:
- Correct HTTP status codes
- Proper error codes in responses
- Correct error messages
- Field-level validation errors are properly mapped

**Testing Challenge:** We encountered an issue testing `MethodArgumentNotValidException` because it requires a valid `MethodParameter`. We solved this by using reflection to create a real method parameter from a dummy test method.

## Why This Approach

### Exception Hierarchy Benefits

1. **Type Safety**: Service layer can throw specific exceptions that controllers can catch by type
2. **Consistent Structure**: All exceptions carry error codes and HTTP status
3. **Centralized Mapping**: HTTP status codes are defined once in the exception class
4. **Easy Extension**: New exception types can be added without modifying the handler

### Global Exception Handler Benefits

1. **DRY Principle**: Error handling logic is centralized, not duplicated in every controller
2. **Consistent API**: All errors follow the same response structure
3. **Separation of Concerns**: Controllers focus on business logic, not error formatting
4. **Logging**: All errors are logged in one place with consistent formatting

### Error Code Benefits

1. **Client-Friendly**: Clients can handle errors programmatically
2. **Internationalization**: Error codes can map to localized messages
3. **Debugging**: Error codes make it easier to search logs and track issues
4. **API Stability**: Error codes remain stable even if messages change

## Alternatives Considered

### Alternative 1: Checked Exceptions

We could have used checked exceptions (extending `Exception` instead of `RuntimeException`). 

**Pros:**
- Compiler enforces exception handling
- Makes error cases explicit in method signatures

**Cons:**
- Verbose code with try-catch blocks everywhere
- Doesn't work well with Spring's transaction management
- Functional programming (streams, lambdas) becomes difficult

**Decision:** We chose unchecked exceptions for cleaner code and better Spring integration.

### Alternative 2: Error Codes in Enum

We could have defined all error codes in a central enum:

```java
public enum ErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND),
    ROOM_NOT_FOUND("ROOM_NOT_FOUND", HttpStatus.NOT_FOUND),
    // ...
}
```

**Pros:**
- All error codes in one place
- Prevents typos in error codes
- Easy to see all possible errors

**Cons:**
- Tight coupling between exceptions and error codes
- Harder to extend in separate modules
- More boilerplate when creating exceptions

**Decision:** We embedded error codes in exception classes for better encapsulation and easier extension.

### Alternative 3: Problem Details (RFC 7807)

We could have implemented the RFC 7807 "Problem Details" standard:

```json
{
  "type": "https://example.com/errors/user-not-found",
  "title": "User Not Found",
  "status": 404,
  "detail": "User not found with id: 1",
  "instance": "/api/users/1"
}
```

**Pros:**
- Industry standard format
- More detailed error information
- Better for public APIs

**Cons:**
- More complex implementation
- Overkill for internal/learning project
- Requires more client-side parsing

**Decision:** We used a simpler custom format suitable for this learning project. For production APIs, RFC 7807 would be worth considering.

## Key Concepts

### 1. Exception Hierarchy

Java's exception hierarchy allows us to catch exceptions at different levels of specificity:

```java
try {
    // code
} catch (UserNotFoundException e) {
    // Handle user not found specifically
} catch (ChatApplicationException e) {
    // Handle any chat exception
} catch (Exception e) {
    // Handle any exception
}
```

### 2. @ControllerAdvice

Spring's `@ControllerAdvice` annotation creates a global exception handler that applies to all controllers. It's like an interceptor that catches exceptions before they reach the client.

### 3. @ExceptionHandler

The `@ExceptionHandler` annotation marks methods that handle specific exception types. Spring automatically routes exceptions to the appropriate handler method based on the exception type.

### 4. Exception Handler Precedence

When multiple handlers could handle an exception, Spring chooses the most specific one:
1. Exact type match (e.g., `UserNotFoundException`)
2. Superclass match (e.g., `ChatApplicationException`)
3. Generic match (e.g., `Exception`)

### 5. HTTP Status Codes

Understanding HTTP status codes is crucial for REST APIs:
- **2xx Success**: Request succeeded
- **4xx Client Error**: Client made a mistake (bad request, unauthorized, not found)
- **5xx Server Error**: Server encountered an error

Our exceptions map to appropriate status codes:
- 400 Bad Request: Validation errors, invalid arguments
- 403 Forbidden: Authorization failures
- 404 Not Found: Resource not found
- 500 Internal Server Error: Unexpected errors, WebSocket failures

### 6. Logging Levels

We use different logging levels based on error severity:
- **ERROR**: Server-side errors (5xx) that need investigation
- **WARN**: Client-side errors (4xx) that are expected but should be monitored
- **INFO**: Normal operations (not used for errors)
- **DEBUG**: Detailed information for debugging

## Potential Pitfalls

### Pitfall 1: Forgetting to Include Error Codes

**Problem:** If you forget to set the error code in ErrorResponse, clients can't programmatically identify errors.

**Solution:** Always use the constructor that accepts an error code, or ensure the error code is set in the exception.

### Pitfall 2: Wrong HTTP Status Codes

**Problem:** Using 500 Internal Server Error for client errors (like validation failures) confuses clients and makes debugging harder.

**Solution:** Follow HTTP status code conventions:
- 4xx for client errors (bad input, unauthorized, not found)
- 5xx for server errors (bugs, infrastructure failures)

### Pitfall 3: Exposing Sensitive Information

**Problem:** Including stack traces or internal details in error responses can expose security vulnerabilities.

**Solution:** 
- Log full details (including stack traces) server-side
- Return only safe, user-friendly messages to clients
- Never expose database queries, file paths, or internal architecture

### Pitfall 4: Not Testing Exception Handlers

**Problem:** Exception handlers are often forgotten in testing, leading to unexpected error responses in production.

**Solution:** Write comprehensive tests for all exception handlers, including edge cases and validation errors.

### Pitfall 5: Catching Too Broadly

**Problem:** A generic `Exception` handler might catch exceptions that should be handled differently (like Spring Security exceptions).

**Solution:** 
- Order handlers from most specific to most generic
- Let Spring Security exceptions propagate to Spring Security's handlers
- Only catch exceptions you actually want to handle

### Pitfall 6: Inconsistent Error Responses

**Problem:** Different controllers returning different error formats makes client integration difficult.

**Solution:** Use `@ControllerAdvice` to ensure all controllers return the same error format.

## What You Learned

In this lesson, you learned how to:

1. **Design an exception hierarchy** that encapsulates error codes and HTTP status codes
2. **Create custom exception classes** for different error scenarios in your application
3. **Implement a global exception handler** using Spring's `@ControllerAdvice` and `@ExceptionHandler`
4. **Structure error responses** with consistent fields (message, timestamp, status, errorCode, errors)
5. **Map exceptions to HTTP status codes** appropriately (4xx for client errors, 5xx for server errors)
6. **Log exceptions** with appropriate severity levels based on error type
7. **Test exception handlers** including complex cases like validation errors
8. **Handle validation errors** from Spring's `@Valid` annotation with field-level error details

You also learned important concepts about:
- The difference between checked and unchecked exceptions
- How Spring's exception handling mechanism works
- HTTP status code conventions for REST APIs
- The importance of error codes for client-side error handling
- Logging best practices for different error types

This exception handling infrastructure provides a solid foundation for the rest of the application. Services can now throw specific exceptions without worrying about HTTP status codes or error formatting—the global handler takes care of all that automatically.
