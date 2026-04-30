# Lesson: Creating REST Controller for Chat Room Operations

## Task Context

This lesson covers the implementation of a REST API controller for chat room management in a Spring Boot application. The task was to create endpoints that allow authenticated users to:
- Create new chat rooms
- List all available chat rooms
- Get details of a specific chat room
- Retrieve members of a chat room

This controller is part of a real-time chat system and follows REST API best practices with proper authentication, validation, and error handling. It builds on top of the ChatRoomService created in a previous task and integrates with Spring Security for authentication.

## Files Modified

- `src/main/java/org/example/chat/controller/ChatRoomController.java` (created)
- `src/main/java/org/example/chat/dto/CreateRoomRequest.java` (created)
- `src/main/java/org/example/chat/dto/ChatRoomResponse.java` (created)
- `src/test/java/org/example/chat/controller/ChatRoomControllerTest.java` (created)

## Step-by-Step Changes

### Step 1: Create the Request DTO

First, we created `CreateRoomRequest.java` to encapsulate the data needed to create a new chat room:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    @NotBlank(message = "Room name is required")
    @Size(min = 1, max = 100, message = "Room name must be between 1 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
```

**Key points:**
- Uses Lombok's `@Data` annotation to generate getters, setters, equals, hashCode, and toString
- Applies Jakarta Bean Validation annotations (`@NotBlank`, `@Size`) to enforce constraints
- The `name` field is required, while `description` is optional
- Validation messages provide clear feedback to API consumers

### Step 2: Create the Response DTO

Next, we created `ChatRoomResponse.java` to represent chat room data in API responses:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private UserResponse createdBy;
    
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
            chatRoom.getId(),
            chatRoom.getName(),
            chatRoom.getDescription(),
            chatRoom.getCreatedAt(),
            UserResponse.from(chatRoom.getCreatedBy())
        );
    }
}
```

**Key points:**
- Includes a static factory method `from()` to convert entity to DTO
- Nests `UserResponse` to provide creator information without exposing sensitive data
- Separates the API representation from the database entity (important for API stability)

### Step 3: Create the REST Controller

We implemented `ChatRoomController.java` with four endpoints:

#### POST /api/rooms - Create Room
```java
@PostMapping
public ResponseEntity<ChatRoomResponse> createRoom(
        @Valid @RequestBody CreateRoomRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
    
    User currentUser = userRepository.findByUsername(userDetails.getUsername())
        .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    
    ChatRoom chatRoom = chatRoomService.createRoom(
        request.getName(),
        request.getDescription(),
        currentUser.getId()
    );

    ChatRoomResponse response = ChatRoomResponse.from(chatRoom);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

**Key points:**
- `@Valid` triggers validation on the request body
- `@AuthenticationPrincipal` injects the authenticated user from Spring Security
- Returns HTTP 201 (Created) status for successful creation
- Retrieves the full User entity from the database using the username from UserDetails

#### GET /api/rooms - List All Rooms
```java
@GetMapping
public ResponseEntity<List<ChatRoomResponse>> listRooms() {
    List<ChatRoom> rooms = chatRoomService.listRooms();
    List<ChatRoomResponse> response = rooms.stream()
        .map(ChatRoomResponse::from)
        .collect(Collectors.toList());
    return ResponseEntity.ok(response);
}
```

**Key points:**
- Uses Java Streams to transform entities to DTOs
- Returns HTTP 200 (OK) with the list of rooms

#### GET /api/rooms/{id} - Get Room Details
```java
@GetMapping("/{id}")
public ResponseEntity<ChatRoomResponse> getRoomById(@PathVariable Long id) {
    ChatRoom chatRoom = chatRoomService.getRoomById(id);
    ChatRoomResponse response = ChatRoomResponse.from(chatRoom);
    return ResponseEntity.ok(response);
}
```

**Key points:**
- `@PathVariable` extracts the room ID from the URL path
- Service layer throws exception if room not found (handled by global exception handler)

#### GET /api/rooms/{id}/members - Get Room Members
```java
@GetMapping("/{id}/members")
public ResponseEntity<List<UserResponse>> getRoomMembers(@PathVariable Long id) {
    List<User> members = chatRoomService.getRoomMembers(id);
    List<UserResponse> response = members.stream()
        .map(UserResponse::from)
        .collect(Collectors.toList());
    return ResponseEntity.ok(response);
}
```

**Key points:**
- Returns a list of users who are members of the specified room
- Uses existing `UserResponse` DTO to maintain consistency

### Step 4: Handle Authentication

The controller needed to work with Spring Security's authentication system. We discovered that `CustomUserDetailsService` returns a Spring Security `User` object, not our custom `User` entity. To solve this:

1. Injected `UserRepository` into the controller
2. Used `userDetails.getUsername()` to get the authenticated username
3. Queried the database to retrieve the full `User` entity
4. Extracted the user ID to pass to the service layer

This approach maintains separation of concerns while providing the necessary user information.

### Step 5: Create Comprehensive Tests

We created `ChatRoomControllerTest.java` with tests covering:

1. **Successful room creation** - Verifies HTTP 201 and correct response structure
2. **Invalid request validation** - Ensures validation errors return HTTP 400
3. **Duplicate room name** - Tests error handling for business rule violations
4. **List all rooms** - Verifies multiple rooms are returned correctly
5. **Get room by ID** - Tests successful retrieval of a specific room
6. **Non-existent room** - Ensures proper error handling for missing resources
7. **Get room members** - Verifies member list retrieval
8. **Unauthenticated access** - Confirms security is enforced (HTTP 401)

**Testing approach:**
- Used `@WebMvcTest` to test only the web layer
- Mocked `ChatRoomService` and `UserRepository` to isolate controller logic
- Used `@WithMockUser` to simulate authenticated requests
- Applied `.with(csrf())` to bypass CSRF protection in tests

## Why This Approach

### DTOs Instead of Entities
We created separate DTO classes rather than exposing entities directly because:
- **API Stability**: Changes to database schema don't break API contracts
- **Security**: Prevents accidental exposure of sensitive fields (like password hashes)
- **Flexibility**: API representation can differ from database structure
- **Validation**: DTOs can have different validation rules than entities

### Constructor Injection
The controller uses constructor injection for dependencies:
```java
public ChatRoomController(ChatRoomService chatRoomService, UserRepository userRepository) {
    this.chatRoomService = chatRoomService;
    this.userRepository = userRepository;
}
```
This is preferred over field injection because:
- Makes dependencies explicit and testable
- Enables immutability (fields can be final)
- Works without Spring (useful for unit tests)

### ResponseEntity Return Type
All endpoints return `ResponseEntity<T>` rather than just `T` because:
- Allows explicit control over HTTP status codes
- Enables setting custom headers if needed
- Makes the API contract clearer

### Static Factory Methods
The `from()` methods in DTOs follow the static factory method pattern:
- More readable than constructors: `ChatRoomResponse.from(room)` vs `new ChatRoomResponse(...)`
- Can have descriptive names for different conversion scenarios
- Encapsulates conversion logic in one place

## Alternatives Considered

### Alternative 1: Custom UserDetails Implementation
We could have created a custom `UserDetails` implementation that wraps our `User` entity:
```java
public class CustomUserDetails implements UserDetails {
    private final User user;
    // implement UserDetails methods
}
```
**Pros**: Direct access to User entity in controllers
**Cons**: More complex, tighter coupling between security and domain model
**Decision**: Kept it simple by querying the repository

### Alternative 2: ModelMapper for DTO Conversion
We could have used a library like ModelMapper to automatically convert entities to DTOs:
```java
ChatRoomResponse response = modelMapper.map(chatRoom, ChatRoomResponse.class);
```
**Pros**: Less boilerplate code
**Cons**: Magic behavior, harder to debug, performance overhead
**Decision**: Used explicit factory methods for clarity and control

### Alternative 3: Separate Endpoints for Public vs Member-Only Rooms
We could have created different endpoints for listing public rooms vs rooms the user is a member of:
```java
GET /api/rooms/public
GET /api/rooms/my-rooms
```
**Pros**: More explicit, easier to apply different security rules
**Cons**: More endpoints to maintain, current design is simpler
**Decision**: Single list endpoint for now, can add filtering later if needed

### Alternative 4: Pagination for List Endpoints
We could have added pagination to the list endpoints:
```java
@GetMapping
public ResponseEntity<Page<ChatRoomResponse>> listRooms(Pageable pageable)
```
**Pros**: Better performance with many rooms, standard Spring Data pattern
**Cons**: More complex for a learning project with 10-20 users
**Decision**: Simple list for now, pagination can be added when needed

## Key Concepts

### REST API Design
This controller follows REST principles:
- **Resources**: Chat rooms are the primary resource
- **HTTP Methods**: POST for creation, GET for retrieval
- **Status Codes**: 201 for creation, 200 for success, 400 for validation errors, 401 for unauthorized
- **Stateless**: Each request contains all necessary information (JWT token)

### Spring MVC Annotations
- `@RestController`: Combines `@Controller` and `@ResponseBody`, automatically serializes return values to JSON
- `@RequestMapping`: Defines the base path for all endpoints in the controller
- `@PostMapping`, `@GetMapping`: Shorthand for `@RequestMapping(method = RequestMethod.POST/GET)`
- `@RequestBody`: Deserializes JSON request body to Java object
- `@PathVariable`: Extracts values from URL path
- `@Valid`: Triggers Bean Validation on the annotated parameter

### Bean Validation
Jakarta Bean Validation provides declarative validation:
- `@NotBlank`: Field must not be null and must contain at least one non-whitespace character
- `@Size`: Constrains string length or collection size
- Validation happens automatically before the controller method executes
- Validation errors result in HTTP 400 with error details

### Spring Security Integration
- `@AuthenticationPrincipal`: Injects the authenticated user from the security context
- Security is configured globally in `SecurityConfig`
- All `/api/rooms` endpoints require authentication (except `/api/auth/*`)
- JWT token is validated by `JwtAuthenticationFilter` before reaching the controller

### Dependency Injection
Spring automatically creates and injects dependencies:
1. Spring creates a `ChatRoomService` bean
2. Spring creates a `UserRepository` bean
3. Spring creates a `ChatRoomController` bean, injecting the service and repository
4. Spring registers the controller to handle HTTP requests

### Exception Handling
The controller throws `IllegalArgumentException` for business rule violations:
- These are caught by `GlobalExceptionHandler` (created in a previous task)
- Converted to appropriate HTTP responses (400 Bad Request)
- Provides consistent error handling across the application

## Potential Pitfalls

### Pitfall 1: Forgetting @Valid Annotation
**Problem**: Without `@Valid`, validation annotations on DTOs are ignored
```java
// Wrong - validation doesn't happen
public ResponseEntity<ChatRoomResponse> createRoom(@RequestBody CreateRoomRequest request)

// Correct - validation is enforced
public ResponseEntity<ChatRoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request)
```

### Pitfall 2: Exposing Entities Directly
**Problem**: Returning entities can expose sensitive data or cause serialization issues
```java
// Risky - might expose password hashes, cause lazy loading issues
public ResponseEntity<ChatRoom> createRoom(...)

// Safe - controlled data exposure
public ResponseEntity<ChatRoomResponse> createRoom(...)
```

### Pitfall 3: Not Handling Optional.empty()
**Problem**: Calling `.get()` on an empty Optional throws NoSuchElementException
```java
// Dangerous
User user = userRepository.findByUsername(username).get();

// Safe
User user = userRepository.findByUsername(username)
    .orElseThrow(() -> new IllegalArgumentException("User not found"));
```

### Pitfall 4: Incorrect HTTP Status Codes
**Problem**: Using wrong status codes confuses API consumers
```java
// Wrong - should be 201 for resource creation
return ResponseEntity.ok(response);

// Correct
return ResponseEntity.status(HttpStatus.CREATED).body(response);
```

### Pitfall 5: Missing CSRF Token in Tests
**Problem**: POST requests fail in tests without CSRF token
```java
// Fails with 403 Forbidden
mockMvc.perform(post("/api/rooms").content(...))

// Works
mockMvc.perform(post("/api/rooms").with(csrf()).content(...))
```

### Pitfall 6: Circular References in DTOs
**Problem**: If DTOs reference each other circularly, JSON serialization fails
```java
// Potential issue if not careful
public class ChatRoomResponse {
    private List<UserResponse> members; // Each user has rooms, each room has users
}
```
**Solution**: Only include IDs or limit nesting depth

### Pitfall 7: N+1 Query Problem
**Problem**: Loading room members one by one causes many database queries
```java
// Can cause N+1 queries if not careful
List<User> members = chatRoomService.getRoomMembers(id);
```
**Solution**: Use JOIN FETCH in repository queries (already handled in service layer)

## What You Learned

In this lesson, you learned how to:

1. **Design REST APIs** following standard conventions for resource-based endpoints
2. **Create DTOs** to separate API contracts from database entities
3. **Implement validation** using Jakarta Bean Validation annotations
4. **Integrate with Spring Security** to access authenticated user information
5. **Use ResponseEntity** to control HTTP status codes and headers
6. **Apply dependency injection** to wire services and repositories into controllers
7. **Write controller tests** using MockMvc and Spring Security Test
8. **Handle authentication** by bridging Spring Security's UserDetails with your domain model
9. **Follow the factory method pattern** for DTO conversion
10. **Structure a Spring Boot application** with proper separation of concerns (controller, service, repository, DTO)

You now have a complete REST API for chat room management that:
- Enforces authentication and authorization
- Validates input data
- Returns appropriate HTTP status codes
- Provides clear error messages
- Is fully tested and ready for integration with a frontend application

This controller serves as a template for creating additional REST endpoints in your application. The patterns used here (DTOs, validation, authentication, testing) should be applied consistently across all your API endpoints.
