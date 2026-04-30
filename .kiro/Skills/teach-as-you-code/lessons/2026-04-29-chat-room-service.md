# Lesson: Creating ChatRoomService for Room Management

## Task Context

This lesson covers the implementation of the `ChatRoomService` class for the real-time chat system. This service is a critical component that manages chat room operations including:

- Creating new chat rooms with automatic owner assignment
- Retrieving room information by ID
- Listing all available rooms
- Managing room memberships (adding and removing members)
- Querying room members

The service acts as the business logic layer between the REST/WebSocket controllers and the data access layer (repositories). It enforces business rules, validates input, handles transactions, and provides a clean API for room management operations.

**Requirements Addressed:**
- Requirement 5.2: Chat room creation and management
- Requirement 5.4: Room membership tracking
- Requirement 6.4: Membership validation for message access
- Requirement 8.3: Database persistence of room membership

## Files Modified

- `src/main/java/org/example/chat/service/ChatRoomService.java` (created)
- `src/test/java/org/example/chat/service/ChatRoomServiceTest.java` (created)
- `.kiro/Skills/teach-as-you-code/lessons/INDEX.md` (modified)
- `.kiro/Skills/teach-as-you-code/lessons/2025-01-22-chat-room-service.md` (created)

## Step-by-Step Changes

### Step 1: Service Class Structure

Created the `ChatRoomService` class with three repository dependencies:
- `ChatRoomRepository`: For room CRUD operations
- `RoomMembershipRepository`: For membership management
- `UserRepository`: For user validation

The service uses constructor injection (Spring's recommended approach) and includes SLF4J logging for observability.

### Step 2: Create Room Method

Implemented `createRoom(String name, String description, Long creatorId)`:

1. **Validation**: Checks that room name is not empty and doesn't exceed 100 characters
2. **Uniqueness Check**: Verifies the room name doesn't already exist
3. **Creator Validation**: Ensures the creator user exists in the database
4. **Room Creation**: Creates and persists the ChatRoom entity with metadata
5. **Owner Assignment**: Automatically adds the creator as an OWNER member

The method is marked `@Transactional` to ensure atomicity - if adding the owner fails, the room creation is rolled back.

### Step 3: Retrieval Methods

Implemented three retrieval methods:

**`getRoomById(Long roomId)`**:
- Simple lookup that throws `IllegalArgumentException` if room not found
- Used internally by other methods for validation

**`listRooms()`**:
- Returns all available chat rooms
- Simple delegation to repository's `findAll()` method

**`getRoomMembers(Long roomId)`**:
- First validates the room exists
- Retrieves all memberships for the room
- Extracts and returns the User entities from memberships
- Uses Java Streams for clean transformation

### Step 4: Membership Management

Implemented two membership methods:

**`addMember(Long roomId, Long userId, MemberRole role)`**:
1. Validates both room and user exist
2. Checks user isn't already a member (prevents duplicates)
3. Creates RoomMembership with specified role (defaults to MEMBER if null)
4. Persists the membership
5. Marked `@Transactional` for data consistency

**`removeMember(Long roomId, Long userId)`**:
1. Validates both room and user exist
2. Checks user is actually a member (can't remove non-members)
3. Deletes the membership using repository's custom delete method
4. Marked `@Transactional` for data consistency

### Step 5: Comprehensive Unit Tests

Created `ChatRoomServiceTest` with 20 test cases covering:

**Happy Path Tests**:
- Creating rooms with valid data
- Retrieving existing rooms
- Listing all rooms
- Getting room members
- Adding new members
- Removing existing members

**Error Handling Tests**:
- Empty or too-long room names
- Duplicate room names
- Non-existent users or rooms
- Adding already-existing members
- Removing non-members
- Default role assignment when null

The tests use Mockito to mock repository dependencies, allowing isolated testing of business logic without database dependencies.

## Why This Approach

### Service Layer Pattern

The service layer pattern separates business logic from data access and presentation:

**Benefits**:
- **Single Responsibility**: Each layer has a clear purpose
- **Testability**: Business logic can be tested without database or web layer
- **Reusability**: Service methods can be called from REST controllers, WebSocket handlers, or scheduled tasks
- **Transaction Management**: `@Transactional` annotations ensure data consistency

### Constructor Injection

Using constructor injection instead of field injection:

**Advantages**:
- **Immutability**: Dependencies are final and cannot be changed
- **Testability**: Easy to create service instances in tests with mock dependencies
- **Explicit Dependencies**: Constructor signature shows all required dependencies
- **Null Safety**: Spring ensures all dependencies are provided at construction time

### Validation Strategy

Input validation happens at the service layer, not in entities or controllers:

**Rationale**:
- **Business Rules**: Validation often involves business logic (e.g., checking uniqueness)
- **Consistent Error Messages**: All validation errors come from one place
- **Early Failure**: Invalid requests fail before database operations
- **Clear Contracts**: Method signatures and exceptions document requirements

### Transaction Boundaries

Methods that modify data are marked `@Transactional`:

**Purpose**:
- **Atomicity**: Multiple database operations succeed or fail together
- **Consistency**: Database constraints are enforced
- **Isolation**: Concurrent operations don't interfere
- **Durability**: Committed changes are permanent

Example: In `createRoom`, if adding the owner membership fails, the room creation is rolled back automatically.

### Exception Handling

Using `IllegalArgumentException` for business rule violations:

**Reasoning**:
- **Standard Exception**: Part of Java standard library
- **Unchecked**: Doesn't force callers to handle (appropriate for programming errors)
- **Clear Intent**: Indicates invalid input or state
- **Future Enhancement**: Can be replaced with custom exceptions later (e.g., `RoomNotFoundException`)

## Alternatives Considered

### Alternative 1: Separate Owner Assignment

**Option**: Have `createRoom` only create the room, require separate call to add owner.

**Rejected Because**:
- **Atomicity**: Room without owner is invalid state
- **Convenience**: Every caller would need two method calls
- **Error Prone**: Easy to forget owner assignment
- **Transaction Complexity**: Harder to ensure consistency

### Alternative 2: Return DTOs Instead of Entities

**Option**: Create Data Transfer Objects (DTOs) and return those instead of entities.

**Deferred Because**:
- **Simplicity**: For a learning project, entities are sufficient
- **Future Enhancement**: Can add DTOs later when needed (e.g., for API responses)
- **Lombok**: Entity classes already have getters/setters via Lombok
- **JPA Lazy Loading**: Need to be careful about lazy-loaded collections

**When to Add DTOs**:
- When you need different views of data for different endpoints
- When you want to hide entity relationships from API consumers
- When you need to aggregate data from multiple entities

### Alternative 3: Soft Delete for Memberships

**Option**: Add `deleted` flag instead of actually deleting memberships.

**Not Implemented Because**:
- **Simplicity**: Hard delete is simpler for learning project
- **Requirements**: No requirement for membership history
- **Complexity**: Soft delete requires filtering in all queries

**When to Use Soft Delete**:
- When you need audit trails
- When you need to restore deleted data
- When deletion affects many related records

### Alternative 4: Role-Based Access Control in Service

**Option**: Check user permissions before allowing operations (e.g., only owners can remove members).

**Deferred Because**:
- **Scope**: Task 8.1 focuses on basic CRUD operations
- **Future Task**: Authorization will be added in later tasks
- **Separation**: Authorization logic should be separate from basic operations

**Future Enhancement**:
- Add permission checking methods
- Validate user roles before operations
- Throw `UnauthorizedException` for permission violations

## Key Concepts

### 1. Service Layer in Spring Boot

The service layer sits between controllers and repositories:

```
Controller → Service → Repository → Database
```

**Responsibilities**:
- Business logic and validation
- Transaction management
- Orchestrating multiple repository calls
- Converting between domain models and DTOs

### 2. Spring Transactions

`@Transactional` annotation provides:

**ACID Properties**:
- **Atomicity**: All operations succeed or all fail
- **Consistency**: Database constraints are maintained
- **Isolation**: Concurrent transactions don't interfere
- **Durability**: Committed changes survive system failures

**Default Behavior**:
- Starts transaction when method is called
- Commits on successful completion
- Rolls back on unchecked exceptions
- Uses database connection from connection pool

### 3. Repository Pattern

Repositories abstract data access:

**Benefits**:
- **Abstraction**: Service doesn't know about SQL or JPA
- **Testability**: Can mock repositories in tests
- **Flexibility**: Can swap database implementations
- **Query Methods**: Spring Data JPA generates implementations

### 4. Dependency Injection

Spring manages object creation and wiring:

**How It Works**:
1. Spring scans for `@Service`, `@Repository`, `@Controller` annotations
2. Creates singleton instances (by default)
3. Injects dependencies via constructor, setter, or field
4. Manages lifecycle (initialization, destruction)

**Constructor Injection** (used here):
```java
public ChatRoomService(ChatRoomRepository chatRoomRepository,
                      RoomMembershipRepository roomMembershipRepository,
                      UserRepository userRepository) {
    this.chatRoomRepository = chatRoomRepository;
    this.roomMembershipRepository = roomMembershipRepository;
    this.userRepository = userRepository;
}
```

### 5. Java Streams for Data Transformation

Used in `getRoomMembers` to extract users from memberships:

```java
List<User> members = memberships.stream()
    .map(RoomMembership::getUser)
    .collect(Collectors.toList());
```

**Stream Operations**:
- `stream()`: Creates a stream from collection
- `map()`: Transforms each element
- `collect()`: Gathers results into collection

**Benefits**:
- Declarative style (what, not how)
- Readable and concise
- Can be parallelized easily
- Lazy evaluation (only processes what's needed)

### 6. Mockito for Unit Testing

Mockito allows testing without real dependencies:

**Key Annotations**:
- `@Mock`: Creates mock object
- `@InjectMocks`: Creates object with mocked dependencies injected
- `@ExtendWith(MockitoExtension.class)`: Enables Mockito in JUnit 5

**Common Methods**:
- `when(...).thenReturn(...)`: Stub method behavior
- `verify(...)`: Assert method was called
- `any(...)`: Match any argument
- `assertThrows(...)`: Assert exception is thrown

## Potential Pitfalls

### Pitfall 1: Forgetting @Transactional

**Problem**: Multiple database operations without transaction can leave data inconsistent.

**Example**:
```java
// BAD: No @Transactional
public ChatRoom createRoom(...) {
    ChatRoom room = chatRoomRepository.save(room);
    addMember(room.getId(), creatorId, MemberRole.OWNER); // If this fails, room exists without owner
}
```

**Solution**: Always use `@Transactional` for methods that modify data.

### Pitfall 2: N+1 Query Problem

**Problem**: Loading memberships and then accessing users causes multiple queries.

**Example**:
```java
// Can cause N+1 queries if User is lazy-loaded
List<RoomMembership> memberships = repository.findByChatRoom(room);
for (RoomMembership m : memberships) {
    User user = m.getUser(); // Separate query for each user!
}
```

**Solution**: Use `@EntityGraph` or JOIN FETCH in repository queries to load users eagerly.

### Pitfall 3: Exposing Entities Directly

**Problem**: Returning JPA entities can cause:
- Lazy loading exceptions when accessing relationships
- Circular references in JSON serialization
- Exposing internal structure to API consumers

**Example**:
```java
// Can cause issues if ChatRoom has lazy-loaded messages
public ChatRoom getRoomById(Long id) {
    return chatRoomRepository.findById(id).orElseThrow();
}
```

**Solution**: Consider using DTOs for API responses (can be added later).

### Pitfall 4: Not Validating User Permissions

**Problem**: Current implementation doesn't check if user has permission to perform operations.

**Example**:
```java
// Anyone can remove anyone from any room!
public void removeMember(Long roomId, Long userId) {
    // No check if caller is owner/moderator
}
```

**Solution**: Add authorization checks in future tasks (check user role before operations).

### Pitfall 5: Hardcoded Error Messages

**Problem**: Error messages are hardcoded strings, making internationalization difficult.

**Example**:
```java
throw new IllegalArgumentException("Room name already exists");
```

**Better Approach** (for production):
- Use message bundles for i18n
- Use error codes for client handling
- Create custom exception classes with structured error information

### Pitfall 6: Testing with Real Database

**Problem**: Tests that use real database are:
- Slow (database I/O)
- Fragile (depend on database state)
- Hard to set up (need test database)

**Solution**: Use mocks for unit tests (as we did), save integration tests for critical paths.

### Pitfall 7: Ignoring Concurrent Access

**Problem**: Multiple users might try to create rooms with same name simultaneously.

**Example**:
```java
// Race condition: Both threads pass uniqueness check
if (chatRoomRepository.findByName(name).isEmpty()) {
    // Thread 1 and Thread 2 both reach here
    chatRoomRepository.save(room); // One will fail with constraint violation
}
```

**Solution**: Database unique constraint catches this, but consider optimistic locking for complex scenarios.

## What You Learned

### Technical Skills

1. **Service Layer Design**: How to structure business logic in Spring Boot applications
2. **Transaction Management**: Using `@Transactional` for data consistency
3. **Dependency Injection**: Constructor injection pattern for testable code
4. **Repository Pattern**: Abstracting data access from business logic
5. **Input Validation**: Validating business rules at the service layer
6. **Unit Testing**: Using Mockito to test services in isolation
7. **Java Streams**: Transforming collections with functional programming
8. **Exception Handling**: Using exceptions to signal business rule violations

### Design Patterns

1. **Service Layer Pattern**: Separating business logic from data access and presentation
2. **Repository Pattern**: Abstracting data persistence
3. **Dependency Injection**: Inverting control for flexibility and testability
4. **Transaction Script**: Organizing business logic as procedures (service methods)

### Best Practices

1. **Constructor Injection**: Prefer constructor injection over field injection
2. **Transactional Boundaries**: Mark data-modifying methods as transactional
3. **Fail Fast**: Validate input early and throw clear exceptions
4. **Logging**: Add logging for observability and debugging
5. **Test Coverage**: Write comprehensive unit tests for all scenarios
6. **Immutable Dependencies**: Use final fields for injected dependencies
7. **Single Responsibility**: Each method does one thing well

### Spring Boot Concepts

1. **@Service Annotation**: Marks class as Spring-managed service bean
2. **@Transactional Annotation**: Enables declarative transaction management
3. **Component Scanning**: How Spring discovers and manages beans
4. **Dependency Injection**: How Spring wires dependencies automatically
5. **JPA Repositories**: How Spring Data JPA simplifies data access

### Testing Concepts

1. **Unit Testing**: Testing components in isolation
2. **Mocking**: Using Mockito to simulate dependencies
3. **Test Organization**: Arrange-Act-Assert pattern
4. **Test Coverage**: Testing both happy paths and error cases
5. **Test Naming**: Descriptive test names that explain what's being tested

### Next Steps

With the `ChatRoomService` complete, you can now:

1. **Create REST Controller**: Build endpoints that use this service (Task 8.2)
2. **Add Authorization**: Implement role-based access control
3. **Create DTOs**: Add data transfer objects for API responses
4. **Integration Tests**: Test service with real database using Testcontainers
5. **WebSocket Integration**: Use service in STOMP message handlers

The service provides a solid foundation for room management that can be extended with additional features like:
- Room search and filtering
- Room privacy settings (public/private)
- Member invitation system
- Room archiving
- Member role management (promote/demote)
