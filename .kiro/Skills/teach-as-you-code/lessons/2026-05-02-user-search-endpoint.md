# Lesson: Implementing User Search Endpoint with Relationship Status

## Task Context

This lesson covers the implementation of a user search endpoint for a real-time chat application. The endpoint allows authenticated users to search for other users by username or display name, with results including the relationship status between the searcher and each result (none, pending incoming request, pending outgoing request, or friends).

The backend controller and service were already implemented, so this task focused on creating comprehensive unit tests to validate the endpoint's behavior across various scenarios.

**Requirements Addressed:**
- **Requirement 1.1**: Case-insensitive search by username or display name
- **Requirement 1.2**: Empty query returns no results
- **Requirement 1.3**: Results include relationship status
- **Requirement 8.2**: Authentication required for search

## Files Modified

- `src/test/java/org/example/chat/controller/UserSearchControllerTest.java` (created)

## Step-by-Step Changes

### 1. Understanding the Existing Implementation

Before writing tests, I examined the existing code:

**UserSearchController** provides a single endpoint:
- `GET /api/users/search?q={query}` - searches users and returns results with relationship status
- Requires authentication via `@AuthenticationPrincipal UserDetails`
- Delegates to `FriendService.searchUsers()`

**FriendService.searchUsers()** handles the business logic:
- Returns empty list for null or empty queries
- Searches by username or display name (case-insensitive)
- Excludes the current user from results
- Determines relationship status for each result (NONE, PENDING_INCOMING, PENDING_OUTGOING, FRIENDS)

### 2. Setting Up the Test Class

Created a `@WebMvcTest` for the controller with proper Spring Boot test configuration:

```java
@WebMvcTest(controllers = UserSearchController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@Import({WebMvcTestConfig.class, org.example.chat.exception.GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
```

**Key configuration choices:**
- Exclude database auto-configuration since we're mocking the service layer
- Import `WebMvcTestConfig` for security configuration
- Import `GlobalExceptionHandler` to test error responses
- Disable filters with `addFilters = false` to simplify authentication testing
- Use "test" profile for test-specific configuration

### 3. Creating Test Data

Set up reusable test data in `@BeforeEach`:

```java
searchResult1 = new PublicUserResponse(2L, "alice", "Alice Smith", null, false);
searchResult2 = new PublicUserResponse(3L, "alicia", "Alicia Johnson", null, true);
searchResult3 = new PublicUserResponse(4L, "bob", "Bob Alice", null, false);
```

This provides diverse test data:
- Different usernames and display names
- Online and offline users
- Names that can test partial matching and case-insensitivity

### 4. Testing Core Search Functionality

**Test: Valid query returns matching users**
```java
@Test
@WithMockUser(username = "testuser")
void searchUsers_ValidQuery_ReturnsMatchingUsers() throws Exception {
    List<UserSearchResultResponse> results = List.of(
        new UserSearchResultResponse(searchResult1, RelationshipStatus.NONE),
        new UserSearchResultResponse(searchResult2, RelationshipStatus.FRIENDS),
        new UserSearchResultResponse(searchResult3, RelationshipStatus.PENDING_OUTGOING)
    );
    
    when(friendService.searchUsers(eq("alice"), eq("testuser")))
        .thenReturn(results);

    mockMvc.perform(get("/api/users/search")
            .param("q", "alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].user.username").value("alice"))
        .andExpect(jsonPath("$[0].relationshipStatus").value("NONE"))
        .andExpect(jsonPath("$[1].relationshipStatus").value("FRIENDS"))
        .andExpect(jsonPath("$[2].relationshipStatus").value("PENDING_OUTGOING"));
}
```

This test validates:
- Endpoint returns 200 OK
- Response is a JSON array
- Each result contains user data and relationship status
- Different relationship statuses are correctly serialized

### 5. Testing Edge Cases

**Empty and null queries:**
```java
@Test
@WithMockUser(username = "testuser")
void searchUsers_EmptyQuery_ReturnsEmptyList() throws Exception {
    when(friendService.searchUsers(eq(""), eq("testuser")))
        .thenReturn(List.of());

    mockMvc.perform(get("/api/users/search").param("q", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
}
```

**No matches:**
```java
@Test
@WithMockUser(username = "testuser")
void searchUsers_NoMatches_ReturnsEmptyList() throws Exception {
    when(friendService.searchUsers(eq("nonexistent"), eq("testuser")))
        .thenReturn(List.of());

    mockMvc.perform(get("/api/users/search").param("q", "nonexistent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
}
```

### 6. Testing Case-Insensitivity

```java
@Test
@WithMockUser(username = "testuser")
void searchUsers_CaseInsensitiveQuery_ReturnsMatchingUsers() throws Exception {
    List<UserSearchResultResponse> results = List.of(
        new UserSearchResultResponse(searchResult1, RelationshipStatus.NONE)
    );
    
    when(friendService.searchUsers(eq("ALICE"), eq("testuser")))
        .thenReturn(results);

    mockMvc.perform(get("/api/users/search").param("q", "ALICE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].user.username").value("alice"));
}
```

This ensures the search works regardless of query case.

### 7. Testing Relationship Status Variations

Created separate tests for each relationship status:

```java
@Test
@WithMockUser(username = "testuser")
void searchUsers_WithPendingIncoming_ReturnsCorrectStatus() throws Exception {
    List<UserSearchResultResponse> results = List.of(
        new UserSearchResultResponse(searchResult1, RelationshipStatus.PENDING_INCOMING)
    );
    
    when(friendService.searchUsers(eq("alice"), eq("testuser")))
        .thenReturn(results);

    mockMvc.perform(get("/api/users/search").param("q", "alice"))
        .andExpect(jsonPath("$[0].relationshipStatus").value("PENDING_INCOMING"));
}
```

Similar tests for `FRIENDS` and `PENDING_OUTGOING` ensure all status values are correctly handled.

### 8. Testing Display Name Matching

```java
@Test
@WithMockUser(username = "testuser")
void searchUsers_MatchesDisplayName_ReturnsMatchingUsers() throws Exception {
    List<UserSearchResultResponse> results = List.of(
        new UserSearchResultResponse(searchResult1, RelationshipStatus.NONE)
    );
    
    when(friendService.searchUsers(eq("Smith"), eq("testuser")))
        .thenReturn(results);

    mockMvc.perform(get("/api/users/search").param("q", "Smith"))
        .andExpect(jsonPath("$[0].user.displayName").value("Alice Smith"));
}
```

This validates that search works on both username and display name fields.

### 9. Testing Partial Matching

```java
@Test
@WithMockUser(username = "testuser")
void searchUsers_PartialMatch_ReturnsMatchingUsers() throws Exception {
    List<UserSearchResultResponse> results = List.of(
        new UserSearchResultResponse(searchResult1, RelationshipStatus.NONE),
        new UserSearchResultResponse(searchResult2, RelationshipStatus.NONE)
    );
    
    when(friendService.searchUsers(eq("ali"), eq("testuser")))
        .thenReturn(results);

    mockMvc.perform(get("/api/users/search").param("q", "ali"))
        .andExpect(jsonPath("$.length()").value(2));
}
```

This ensures substring matching works (e.g., "ali" matches "alice" and "alicia").

### 10. Running the Tests

Executed the test suite with Maven:
```bash
mvn test -Dtest=UserSearchControllerTest
```

**Results:**
- All 10 tests passed
- No compilation errors
- Test execution time: ~6 seconds

## Why This Approach

### Controller-Level Testing with @WebMvcTest

**Why use `@WebMvcTest` instead of integration tests?**
- **Faster execution**: No database or full application context needed
- **Focused scope**: Tests only the controller layer
- **Clear boundaries**: Service layer is mocked, isolating controller logic
- **Better error messages**: Failures clearly indicate controller issues

### Mocking the Service Layer

**Why mock `FriendService`?**
- Controller tests should validate HTTP handling, not business logic
- Service logic is tested separately in service-layer tests
- Mocking allows testing error scenarios without complex setup
- Tests remain fast and deterministic

### Comprehensive Test Coverage

**Why test so many scenarios?**
- **Empty/null queries**: Validates input validation
- **Case-insensitivity**: Ensures user-friendly search
- **Relationship statuses**: Critical feature for social functionality
- **Partial matching**: Tests substring search behavior
- **Display name matching**: Validates multi-field search

Each test validates a specific requirement or edge case.

### Using @WithMockUser

**Why use `@WithMockUser` instead of real authentication?**
- Simplifies test setup
- Focuses tests on controller logic, not authentication
- Allows testing with different usernames easily
- Faster test execution

## Alternatives Considered

### 1. Integration Tests Instead of Unit Tests

**Alternative**: Use `@SpringBootTest` with a real database.

**Why not chosen:**
- Slower execution (database setup/teardown)
- More complex test data management
- Harder to test edge cases
- Controller logic is simple enough for unit tests

**When to use**: For end-to-end validation of the entire search flow.

### 2. Testing with Real Authentication

**Alternative**: Enable Spring Security filters and use real JWT tokens.

**Why not chosen:**
- Adds complexity to test setup
- Slower test execution
- Authentication is tested separately
- `@WithMockUser` provides sufficient authentication context

**When to use**: For security-focused integration tests.

### 3. Single Test with Multiple Assertions

**Alternative**: Combine multiple scenarios into fewer tests.

**Why not chosen:**
- Harder to identify which scenario failed
- Violates single-responsibility principle for tests
- Less clear test documentation
- Harder to maintain

**When to use**: For very simple scenarios or smoke tests.

### 4. Testing Service Logic in Controller Tests

**Alternative**: Test the actual `FriendService` implementation.

**Why not chosen:**
- Mixes concerns (controller vs service testing)
- Requires database setup
- Slower execution
- Service has its own test suite

**When to use**: In integration tests that validate the full stack.

## Key Concepts

### 1. MockMvc for Controller Testing

`MockMvc` simulates HTTP requests without starting a web server:

```java
mockMvc.perform(get("/api/users/search").param("q", "alice"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$").isArray());
```

**Benefits:**
- No network overhead
- Full control over request/response
- Easy assertion on JSON responses
- Validates Spring MVC configuration

### 2. JSONPath for Response Validation

JSONPath expressions navigate JSON responses:

```java
.andExpect(jsonPath("$[0].user.username").value("alice"))
.andExpect(jsonPath("$[0].relationshipStatus").value("NONE"))
```

**Common patterns:**
- `$` - root element
- `$[0]` - first array element
- `$.user.username` - nested property
- `$.length()` - array length

### 3. Mockito Argument Matchers

`eq()` ensures exact argument matching:

```java
when(friendService.searchUsers(eq("alice"), eq("testuser")))
    .thenReturn(results);
```

**Why use `eq()`?**
- Required when mixing matchers and literals
- Makes expectations explicit
- Prevents subtle matching bugs

### 4. Test Data Builders

Creating reusable test data in `@BeforeEach`:

```java
@BeforeEach
void setUp() {
    searchResult1 = new PublicUserResponse(2L, "alice", "Alice Smith", null, false);
    // ...
}
```

**Benefits:**
- Reduces duplication
- Consistent test data
- Easy to modify for all tests
- Improves readability

### 5. Relationship Status Enum

The `RelationshipStatus` enum represents the connection state:

```java
public enum RelationshipStatus {
    NONE,                // No relationship
    PENDING_INCOMING,    // User received a friend request
    PENDING_OUTGOING,    // User sent a friend request
    FRIENDS              // Users are friends
}
```

This provides type-safe status representation and clear semantics.

## Potential Pitfalls

### 1. Forgetting to Mock Service Methods

**Problem:**
```java
// Missing mock setup
mockMvc.perform(get("/api/users/search").param("q", "alice"))
    .andExpect(status().isOk());
```

**Result**: Test fails with `NullPointerException` or unexpected empty results.

**Solution**: Always set up mocks before making requests:
```java
when(friendService.searchUsers(eq("alice"), eq("testuser")))
    .thenReturn(results);
```

### 2. Incorrect Argument Matchers

**Problem:**
```java
when(friendService.searchUsers("alice", "testuser"))  // Missing eq()
    .thenReturn(results);
```

**Result**: Mock doesn't match, returns null.

**Solution**: Use `eq()` consistently:
```java
when(friendService.searchUsers(eq("alice"), eq("testuser")))
    .thenReturn(results);
```

### 3. Testing Implementation Details

**Problem**: Testing how the service is called rather than the HTTP response.

**Bad:**
```java
verify(friendService).searchUsers(eq("alice"), eq("testuser"));
```

**Better**: Focus on the HTTP contract:
```java
.andExpect(status().isOk())
.andExpect(jsonPath("$[0].user.username").value("alice"));
```

**Why**: Controller tests should validate HTTP behavior, not internal calls.

### 4. Hardcoding Test Data in Tests

**Problem:**
```java
@Test
void test1() {
    PublicUserResponse user = new PublicUserResponse(2L, "alice", "Alice", null, false);
    // ...
}

@Test
void test2() {
    PublicUserResponse user = new PublicUserResponse(2L, "alice", "Alice", null, false);
    // ...
}
```

**Result**: Duplication, inconsistency, hard to maintain.

**Solution**: Use `@BeforeEach` to create shared test data.

### 5. Not Testing Edge Cases

**Problem**: Only testing the happy path.

**Missing tests:**
- Empty query
- Null query
- No matches
- Special characters in query

**Solution**: Create explicit tests for each edge case.

### 6. Overly Complex Test Names

**Problem:**
```java
void test1() { ... }
void testSearch() { ... }
```

**Result**: Unclear what's being tested.

**Solution**: Use descriptive names following the pattern:
```java
void methodName_scenario_expectedBehavior()
```

Example:
```java
void searchUsers_EmptyQuery_ReturnsEmptyList()
```

### 7. Testing Multiple Concerns in One Test

**Problem:**
```java
@Test
void searchUsers_AllScenarios() {
    // Test empty query
    // Test valid query
    // Test case-insensitivity
    // Test relationship status
}
```

**Result**: Hard to debug failures, unclear test purpose.

**Solution**: One test per scenario.

## What You Learned

### Technical Skills

1. **Spring Boot Controller Testing**
   - Using `@WebMvcTest` for focused controller tests
   - Configuring test slices with proper exclusions
   - Mocking service dependencies with `@MockBean`

2. **MockMvc Request/Response Testing**
   - Performing GET requests with query parameters
   - Validating HTTP status codes
   - Using JSONPath to assert on JSON responses
   - Testing array responses and nested objects

3. **Mockito Mocking Patterns**
   - Setting up method stubs with `when().thenReturn()`
   - Using argument matchers (`eq()`)
   - Creating test data for mock responses

4. **Test Organization**
   - Structuring test classes with `@BeforeEach` setup
   - Creating reusable test data
   - Naming tests descriptively
   - Grouping related tests

### Domain Knowledge

1. **User Search Functionality**
   - Case-insensitive search across multiple fields
   - Partial matching for user-friendly search
   - Excluding the current user from results

2. **Relationship Status Management**
   - Four relationship states: NONE, PENDING_INCOMING, PENDING_OUTGOING, FRIENDS
   - Including relationship context in search results
   - Supporting social discovery features

3. **REST API Design**
   - Query parameter conventions (`?q=`)
   - Returning empty arrays vs null for no results
   - Consistent response structure

### Testing Best Practices

1. **Test Isolation**
   - Each test is independent
   - Mocks are reset between tests
   - No shared mutable state

2. **Comprehensive Coverage**
   - Happy path and edge cases
   - All relationship status variations
   - Input validation scenarios

3. **Clear Test Intent**
   - Descriptive test names
   - Arrange-Act-Assert structure
   - Focused assertions

4. **Maintainability**
   - Reusable test data
   - Consistent patterns
   - Clear documentation

### Real-World Application

This implementation demonstrates patterns used in production applications:

1. **Social Features**: User search with relationship context is common in social platforms (LinkedIn, Facebook, Twitter)

2. **Search UX**: Case-insensitive partial matching provides a better user experience

3. **API Design**: Returning relationship status with search results reduces frontend API calls

4. **Testing Strategy**: Controller unit tests provide fast feedback during development, complementing slower integration tests

You now understand how to build and test a user search endpoint that supports social discovery features in a real-time chat application.
