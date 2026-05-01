# Lesson: Creating Integration Tests for Spring Boot Backend

## Task Context

The backend had comprehensive unit tests but no integration tests. Unit tests use mocked dependencies (`@WebMvcTest`, `@MockBean`) which test components in isolation. Integration tests test the full application stack with real database connections, actual Spring Security configuration, and all components wired together.

This lesson covers creating a complete integration test suite for a Spring Boot chat application with:
- Real H2 database (configured in `application-test.yml`)
- Full Spring Security with JWT authentication
- All layers working together (Controller → Service → Repository → Database)

## Files Modified

- `src/test/java/org/example/chat/integration/BaseIntegrationTest.java` (created)
- `src/test/java/org/example/chat/integration/AuthenticationIntegrationTest.java` (created)
- `src/test/java/org/example/chat/integration/ChatRoomIntegrationTest.java` (created)
- `src/test/java/org/example/chat/integration/MessageIntegrationTest.java` (created)
- `src/test/java/org/example/chat/integration/UserIntegrationTest.java` (created)

## Step-by-Step Changes

### Step 1: Create Base Integration Test Class

Created `BaseIntegrationTest.java` as an abstract base class that all integration tests extend. This provides:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
    
    @Autowired
    protected ObjectMapper objectMapper;
}
```

**Key annotations:**
- `@SpringBootTest`: Loads the full application context (unlike `@WebMvcTest` which only loads web layer)
- `webEnvironment = RANDOM_PORT`: Starts embedded server on random port
- `@AutoConfigureMockMvc`: Provides `MockMvc` for making HTTP requests
- `@ActiveProfiles("test")`: Uses `application-test.yml` configuration (H2 database)
- `@Transactional`: Rolls back database changes after each test (keeps tests isolated)

### Step 2: Authentication Integration Tests

Created tests for the complete registration and login flow:

**Complete flow test:**
```java
@Test
void completeAuthenticationFlow_RegisterAndLogin_Success() throws Exception {
    // Step 1: Register
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(registerRequest)))
        .andExpect(status().isCreated());
    
    // Step 2: Verify in database
    User savedUser = userRepository.findByUsername("integrationuser").orElse(null);
    assertNotNull(savedUser);
    
    // Step 3: Login
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isString());
}
```

This tests:
- HTTP request/response
- Database persistence
- Password hashing
- JWT token generation
- All layers working together

### Step 3: Chat Room Integration Tests

Created tests for room creation and membership:

**Setup with real user and JWT:**
```java
@BeforeEach
void setUp() {
    // Create real user in database
    testUser = new User();
    testUser.setUsername("roomtestuser");
    testUser.setPasswordHash(passwordEncoder.encode("password123"));
    testUser = userRepository.save(testUser);
    
    // Generate real JWT token
    authToken = jwtUtil.generateToken(testUser.getUsername());
}
```

**Testing with authentication:**
```java
@Test
void createRoom_ValidRequest_Success() throws Exception {
    mockMvc.perform(post("/api/rooms")
            .header("Authorization", "Bearer " + authToken)  // Real JWT
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
        .andExpect(status().isCreated());
    
    // Verify in database
    List<ChatRoom> rooms = chatRoomRepository.findAll();
    assertEquals(1, rooms.size());
    
    // Verify membership was created
    List<RoomMembership> memberships = roomMembershipRepository.findByChatRoom(rooms.get(0));
    assertEquals(MemberRole.OWNER, memberships.get(0).getRole());
}
```

### Step 4: Message Integration Tests

Created tests for message history retrieval:

**Testing pagination:**
```java
@Test
void getMessageHistory_RoomWithMessages_ReturnsMessages() throws Exception {
    // Create messages in database
    Message message1 = new Message();
    message1.setChatRoom(testRoom);
    message1.setSender(testUser);
    message1.setContent("First message");
    message1.setMessageType(MessageType.TEXT);
    messageRepository.save(message1);
    
    // Test retrieval
    mockMvc.perform(get("/api/rooms/" + testRoom.getId() + "/messages")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)));  // Paginated response
}
```

**Testing authorization:**
```java
@Test
void getMessageHistory_UserNotMember_ReturnsForbidden() throws Exception {
    // Create another user
    User otherUser = userRepository.save(createUser("otheruser"));
    String otherToken = jwtUtil.generateToken(otherUser.getUsername());
    
    // Try to access room as non-member
    mockMvc.perform(get("/api/rooms/" + testRoom.getId() + "/messages")
            .header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isForbidden());
}
```

### Step 5: User Profile Integration Tests

Created tests for profile management:

**Testing update flow:**
```java
@Test
void completeUserFlow_GetAndUpdate_Success() throws Exception {
    // Get current profile
    mockMvc.perform(get("/api/users/me")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(jsonPath("$.displayName").value("Profile Test User"));
    
    // Update profile
    mockMvc.perform(put("/api/users/me")
            .header("Authorization", "Bearer " + authToken)
            .content(toJson(updateRequest)))
        .andExpect(status().isOk());
    
    // Verify changes persisted
    mockMvc.perform(get("/api/users/me")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(jsonPath("$.displayName").value("Updated Name"));
}
```

### Step 6: Fixing Entity Mismatches

During implementation, discovered entity field name differences:
- `Message.room` → `Message.chatRoom`
- `Message.type` → `Message.messageType`
- `MessageType.CHAT` → `MessageType.TEXT`
- `RoomMembership.room` → `RoomMembership.chatRoom`
- `CreateRoomRequest` requires both `name` and `description`

Fixed by reading actual entity classes and updating test code to match.

### Step 7: Understanding API Response Formats

The message history API returns paginated responses:
```json
{
  "content": [...],  // Actual messages
  "pageable": {...},
  "totalElements": 3,
  "totalPages": 1
}
```

Updated tests to check `$.content` instead of `$` for message arrays.

## Why This Approach

**Integration tests vs Unit tests:**
- **Unit tests**: Fast, isolated, test single components with mocks
- **Integration tests**: Slower, test full stack, catch integration issues

**When to use each:**
- Unit tests: Business logic, edge cases, error handling
- Integration tests: API contracts, database interactions, security, full workflows

**Benefits of this approach:**
1. **Catches real issues**: Tests actual database queries, transactions, security
2. **Documents API behavior**: Shows how endpoints actually work
3. **Regression protection**: Ensures changes don't break existing functionality
4. **Confidence in deployment**: If integration tests pass, the app works end-to-end

## Alternatives Considered

### Alternative 1: TestContainers with PostgreSQL
Instead of H2 in-memory database, use TestContainers to run real PostgreSQL:

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
```

**Pros:**
- Tests against production database
- Catches PostgreSQL-specific issues
- More realistic

**Cons:**
- Slower (Docker startup)
- Requires Docker installed
- More complex setup

**When to use:** For production-critical applications where database-specific behavior matters.

### Alternative 2: @DataJpaTest for Repository Tests
Test only the repository layer:

```java
@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;
}
```

**Pros:**
- Faster than full integration tests
- Focuses on data layer
- Auto-configured test database

**Cons:**
- Doesn't test controllers or services
- Doesn't test security
- Misses integration issues

**When to use:** When you need to test complex queries or repository methods in isolation.

### Alternative 3: @WebMvcTest with @MockBean
Keep using unit tests with mocks:

```java
@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest {
    @MockBean
    private ChatRoomService chatRoomService;
}
```

**Pros:**
- Very fast
- Isolated testing
- No database needed

**Cons:**
- Doesn't catch integration issues
- Mocks can diverge from real behavior
- Doesn't test security configuration

**When to use:** For testing controller logic, request/response mapping, validation.

## Key Concepts

### 1. Spring Boot Test Slices

Spring Boot provides different test annotations for different layers:

| Annotation | What it loads | Use case |
|------------|---------------|----------|
| `@SpringBootTest` | Full application context | Integration tests |
| `@WebMvcTest` | Web layer only | Controller unit tests |
| `@DataJpaTest` | JPA components only | Repository tests |
| `@JsonTest` | JSON serialization | DTO tests |

### 2. Test Database Configuration

The `@ActiveProfiles("test")` annotation loads `application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop  # Recreate schema for each test run
```

H2 is an in-memory database that:
- Starts fresh for each test run
- Is fast (no disk I/O)
- Supports most SQL features
- Is automatically cleaned up

### 3. Transaction Rollback

`@Transactional` on the test class means:
- Each test runs in a transaction
- Transaction is rolled back after test completes
- Database returns to clean state
- Tests don't affect each other

**Without `@Transactional`:**
```java
@Test
void test1() {
    userRepository.save(user);  // Persists to database
}

@Test
void test2() {
    // User from test1 still exists! Tests interfere with each other
}
```

**With `@Transactional`:**
```java
@Test
@Transactional
void test1() {
    userRepository.save(user);  // Persists to database
    // Transaction rolled back after test
}

@Test
@Transactional
void test2() {
    // Clean database, user from test1 doesn't exist
}
```

### 4. MockMvc vs RestTemplate vs WebTestClient

Three ways to test HTTP endpoints:

**MockMvc** (what we used):
```java
mockMvc.perform(get("/api/users/me"))
    .andExpect(status().isOk());
```
- Doesn't start real HTTP server
- Faster
- Can't test some server-specific behavior

**RestTemplate**:
```java
restTemplate.getForEntity("http://localhost:" + port + "/api/users/me", UserResponse.class);
```
- Starts real HTTP server
- Tests full HTTP stack
- Slower

**WebTestClient** (for reactive apps):
```java
webTestClient.get().uri("/api/users/me")
    .exchange()
    .expectStatus().isOk();
```
- For WebFlux/reactive applications
- Fluent API
- Supports streaming

### 5. JWT Authentication in Tests

Integration tests need real JWT tokens:

```java
// Generate real token
String token = jwtUtil.generateToken(username);

// Use in request
mockMvc.perform(get("/api/users/me")
        .header("Authorization", "Bearer " + token))
    .andExpect(status().isOk());
```

This tests:
- JWT generation
- Token parsing
- Security filter chain
- Authentication/authorization

## Potential Pitfalls

### Pitfall 1: Forgetting @Transactional

**Problem:**
```java
@Test
void test1() {
    userRepository.save(user);  // Persists
}

@Test
void test2() {
    // User from test1 still exists!
    // Test fails because of unexpected data
}
```

**Solution:** Add `@Transactional` to test class or individual tests.

### Pitfall 2: Testing Against Wrong Database

**Problem:**
```java
// Accidentally using production database!
spring.datasource.url=jdbc:postgresql://prod-db:5432/chat
```

**Solution:** 
- Always use `@ActiveProfiles("test")`
- Verify `application-test.yml` uses H2 or test database
- Never put production credentials in test config

### Pitfall 3: Flaky Tests Due to Timing

**Problem:**
```java
@Test
void testAsync() {
    service.doAsyncOperation();
    // Assertion runs before async operation completes!
    assertEquals(expected, actual);  // Fails randomly
}
```

**Solution:**
- Use `@Async` with proper waiting mechanisms
- Use `Awaitility` library for async testing
- Or avoid async operations in integration tests

### Pitfall 4: Not Cleaning Up Test Data

**Problem:**
```java
@Test
void test() {
    // Creates 1000 records
    for (int i = 0; i < 1000; i++) {
        repository.save(entity);
    }
    // No cleanup, slows down subsequent tests
}
```

**Solution:**
- Use `@Transactional` for automatic rollback
- Or manually clean up in `@AfterEach`
- Keep test data minimal

### Pitfall 5: Testing Implementation Details

**Problem:**
```java
@Test
void test() {
    // Testing internal method calls instead of behavior
    verify(repository, times(1)).save(any());
}
```

**Solution:** Integration tests should test behavior, not implementation:
```java
@Test
void test() {
    // Test the outcome
    User saved = userRepository.findByUsername("test").orElseThrow();
    assertEquals("test", saved.getUsername());
}
```

### Pitfall 6: Ignoring Response Format

**Problem:**
```java
// API returns paginated response
mockMvc.perform(get("/api/messages"))
    .andExpect(jsonPath("$", hasSize(3)));  // Fails!
```

**Solution:** Check actual API response format:
```java
// Correct: check content field
mockMvc.perform(get("/api/messages"))
    .andExpect(jsonPath("$.content", hasSize(3)));
```

### Pitfall 7: Hardcoding IDs

**Problem:**
```java
@Test
void test() {
    User user = new User();
    user.setId(1L);  // Hardcoded ID
    userRepository.save(user);
    // ID might be different in database!
}
```

**Solution:** Let database generate IDs:
```java
@Test
void test() {
    User user = new User();
    // Don't set ID
    User saved = userRepository.save(user);
    // Use generated ID
    assertNotNull(saved.getId());
}
```

## What You Learned

1. **Integration vs Unit Tests**: Integration tests verify the full stack works together, while unit tests verify individual components in isolation.

2. **Spring Boot Test Annotations**: `@SpringBootTest` loads the full application context, `@ActiveProfiles("test")` uses test configuration, `@Transactional` provides automatic rollback.

3. **Test Database Setup**: Use H2 in-memory database for fast, isolated tests that don't affect production data.

4. **Real Authentication**: Integration tests should use real JWT tokens and test the full security chain, not mocked authentication.

5. **API Response Formats**: Always check the actual API response structure (paginated responses have `content` field, not direct arrays).

6. **Test Isolation**: Use `@Transactional` to ensure tests don't interfere with each other by rolling back database changes.

7. **Complete Workflows**: Integration tests should test complete user workflows (register → login → create room → send message) to catch integration issues.

8. **Database Verification**: After API calls, verify data was correctly persisted to the database using repository methods.

9. **Error Cases**: Test not just happy paths, but also authentication failures, authorization denials, validation errors, and not-found cases.

10. **Test Organization**: Use a base test class to share common setup (MockMvc, ObjectMapper) and reduce duplication across test classes.
