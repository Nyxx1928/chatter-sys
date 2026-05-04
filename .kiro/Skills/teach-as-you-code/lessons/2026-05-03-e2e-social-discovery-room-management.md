# Lesson: End-to-End Testing for Social Discovery and Room Management

## Task Context

This lesson covers the implementation of comprehensive end-to-end (E2E) integration tests for the social discovery and room management feature. The tests validate complete user workflows including:

1. **User Search**: Finding other users by username or display name
2. **Friend Request Lifecycle**: Sending, accepting, and declining friend requests
3. **Friends List**: Viewing and managing friendships
4. **Room Creation**: Creating chat rooms with proper authorization
5. **Room Deletion**: Deleting rooms with ownership validation

The tests ensure that all requirements (1.1, 2.1, 3.1, 4.1, 6.1) are properly implemented and work together as a cohesive system.

## Files Modified

- `src/test/java/org/example/chat/integration/SocialDiscoveryAndRoomManagementIT.java` (created)

## Step-by-Step Changes

### 1. Created the Test Class Structure

First, we created a new integration test class that extends `BaseIntegrationTest` to inherit common test infrastructure:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SocialDiscoveryAndRoomManagementIT extends BaseIntegrationTest
```

The `@Transactional` annotation ensures each test runs in its own transaction that rolls back after completion, keeping tests isolated.

### 2. Set Up Test Users

In the `@BeforeEach` method, we create two test users (alice and bob) with:
- Unique usernames and emails
- Encrypted passwords
- JWT tokens for authentication

This setup is reused across all tests, ensuring consistency.

### 3. Implemented the Complete Flow Test

The `completeFlow_SearchSendAcceptCreateDelete_Success()` test validates the entire user journey:

**Step 1: User Search**
- User1 searches for User2 by username
- Verifies search returns correct user with "NONE" relationship status

**Step 2: Send Friend Request**
- User1 sends a friend request to User2
- Verifies the request is created with correct requester/recipient
- Tests duplicate prevention (409 Conflict response)

**Step 3: View Pending Requests**
- User2 retrieves their pending requests
- Verifies the request appears in the "incoming" list

**Step 4: Accept Friend Request**
- User2 accepts the request
- Verifies friendship is created in the database

**Step 5: Verify Friends List**
- Both users retrieve their friends list
- Each sees the other user in their list

**Step 6: Create Chat Room**
- User1 creates a new room
- Verifies room appears in the room list
- Can retrieve room by ID

**Step 7: Delete Chat Room**
- User1 deletes the room they created
- Verifies room is removed from database

### 4. Implemented Focused Test Cases

We created 14 additional test methods covering specific scenarios:

**User Search Tests:**
- Various query types (username, display name, case-insensitive)
- Empty query handling
- Relationship status indicators (NONE, PENDING_INCOMING, PENDING_OUTGOING, FRIENDS)

**Friend Request Tests:**
- Send and accept flow
- Send and decline flow
- Self-request validation (400 Bad Request)
- Duplicate request prevention (409 Conflict)

**Friends List Tests:**
- Display after acceptance
- Empty state handling

**Room Creation Tests:**
- Valid data creates room successfully
- Authentication requirement (401 Unauthorized)

**Room Deletion Tests:**
- Owner can delete (204 No Content)
- Non-owner cannot delete (403 Forbidden)
- Nonexistent room returns 404

**Authentication Tests:**
- All endpoints require valid JWT tokens

### 5. Used MockMvc for HTTP Testing

All tests use Spring's `MockMvc` to simulate HTTP requests:

```java
mockMvc.perform(get("/api/users/search")
        .param("q", "bob")
        .header("Authorization", "Bearer " + user1Token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].user.username").value("bob"));
```

This approach:
- Tests the full Spring MVC stack
- Validates request/response serialization
- Checks HTTP status codes and JSON responses
- Doesn't require a running server

### 6. Verified Database State

Tests directly query repositories to verify data persistence:

```java
assertTrue(friendshipRepository.count() >= 1);
assertFalse(chatRoomRepository.findById(roomId).isPresent());
```

This ensures the API not only returns correct responses but also correctly modifies the database.

## Why This Approach

### Integration Tests Over Unit Tests

We chose integration tests because:
1. **Real Interactions**: Tests verify how components work together, not in isolation
2. **Database Validation**: Ensures data is correctly persisted and retrieved
3. **Full Stack**: Tests the entire request/response cycle including serialization, validation, and security
4. **Confidence**: Provides higher confidence that the feature works end-to-end

### Test Organization

We organized tests into:
1. **One comprehensive flow test**: Validates the happy path from start to finish
2. **Focused scenario tests**: Cover edge cases, error conditions, and specific requirements

This structure makes it easy to:
- Understand the main user journey
- Identify which specific scenario failed
- Add new test cases without duplication

### MockMvc vs RestTemplate

We used `MockMvc` instead of `RestTemplate` because:
- No need to start a real HTTP server
- Faster test execution
- Direct access to Spring MVC infrastructure
- Better error messages and debugging

## Alternatives Considered

### 1. Separate Test Classes

We could have created separate test classes for each feature area (UserSearchIT, FriendRequestIT, RoomManagementIT). 

**Pros:**
- Better organization for large test suites
- Easier to run specific feature tests

**Cons:**
- Harder to test cross-feature workflows
- More setup code duplication

**Decision:** Single class works well for this feature since the workflows are interconnected.

### 2. Test Data Builders

We could have used the Builder pattern for test data:

```java
User alice = UserBuilder.create()
    .withUsername("alice")
    .withEmail("alice@example.com")
    .build();
```

**Pros:**
- More readable test setup
- Easier to create variations

**Cons:**
- Additional code to maintain
- Overkill for simple test data

**Decision:** Direct entity creation is sufficient for our needs.

### 3. Testcontainers for Real Database

We could have used Testcontainers to run tests against a real PostgreSQL instance instead of H2.

**Pros:**
- Tests against production database
- Catches database-specific issues

**Cons:**
- Slower test execution
- Requires Docker
- More complex setup

**Decision:** H2 in-memory database is fast and sufficient for integration tests. Production database testing can be done in staging environments.

## Key Concepts

### 1. Integration Testing

Integration tests verify that multiple components work together correctly. Unlike unit tests that mock dependencies, integration tests use real implementations.

**Key characteristics:**
- Test multiple layers (controller → service → repository → database)
- Use real Spring context
- Verify actual database operations
- Test serialization/deserialization

### 2. Test Isolation with @Transactional

The `@Transactional` annotation on the test class ensures each test method:
1. Starts a new transaction
2. Executes the test
3. Rolls back the transaction

This keeps tests isolated—changes in one test don't affect others.

### 3. JWT Authentication in Tests

Tests authenticate by:
1. Creating test users in `@BeforeEach`
2. Generating JWT tokens using `JwtUtil`
3. Including tokens in request headers: `Authorization: Bearer <token>`

This tests the full authentication flow without mocking security.

### 4. JSONPath for Response Validation

JSONPath expressions query JSON responses:

```java
.andExpect(jsonPath("$[0].user.username").value("bob"))
.andExpect(jsonPath("$.incoming", hasSize(1)))
.andExpect(jsonPath("$[?(@.id == " + roomId + ")]").doesNotExist())
```

- `$` = root element
- `[0]` = first array element
- `.user.username` = nested property access
- `[?(@.id == X)]` = filter by condition

### 5. Test Naming Convention

Test names follow the pattern: `methodName_scenario_expectedResult`

Examples:
- `userSearch_VariousQueries_ReturnsMatchingUsers`
- `friendRequest_ToSelf_ReturnsBadRequest`
- `deleteRoom_ByOwner_Success`

This makes test failures immediately understandable.

## Potential Pitfalls

### 1. Test Data Pollution

**Problem:** Tests that don't clean up data can cause other tests to fail.

**Solution:** Use `@Transactional` to automatically roll back changes. Avoid static data or shared state.

### 2. Flaky Tests Due to Timing

**Problem:** Tests that depend on timing (async operations, eventual consistency) can fail intermittently.

**Solution:** Our tests are synchronous and transactional, avoiding timing issues. If async operations are needed, use `Awaitility` or similar libraries.

### 3. Over-Asserting

**Problem:** Tests that assert too many things become brittle and hard to maintain.

**Solution:** Each test focuses on one scenario. The comprehensive flow test validates the happy path, while focused tests cover specific cases.

### 4. Hardcoded IDs

**Problem:** Assuming specific database IDs can cause tests to fail when run in different orders.

**Solution:** Extract IDs from responses:

```java
Long roomId = objectMapper.readTree(response).get("id").asLong();
```

### 5. Ignoring HTTP Status Codes

**Problem:** Only checking response body without validating status codes misses important API contract details.

**Solution:** Always assert status codes first:

```java
.andExpect(status().isCreated())  // or isOk(), isBadRequest(), etc.
```

### 6. Not Testing Error Cases

**Problem:** Only testing happy paths leaves error handling untested.

**Solution:** We included tests for:
- 400 Bad Request (invalid data, self-requests)
- 401 Unauthorized (missing authentication)
- 403 Forbidden (insufficient permissions)
- 404 Not Found (nonexistent resources)
- 409 Conflict (duplicate requests)

## What You Learned

1. **End-to-End Integration Testing**: How to write tests that validate complete user workflows across multiple API endpoints

2. **Spring Boot Test Infrastructure**: Using `@SpringBootTest`, `MockMvc`, and `@Transactional` to create isolated, repeatable tests

3. **Authentication Testing**: How to test secured endpoints by generating and using JWT tokens

4. **Database Verification**: Combining HTTP assertions with direct repository queries to ensure data integrity

5. **Test Organization**: Structuring tests with one comprehensive flow test and multiple focused scenario tests

6. **Error Handling Validation**: Testing not just success cases but also various error conditions (400, 401, 403, 404, 409)

7. **JSONPath Assertions**: Using JSONPath expressions to validate complex JSON responses

8. **Test Isolation**: Ensuring tests don't interfere with each other using transactions and proper setup/teardown

9. **Requirement Traceability**: Documenting which requirements each test validates in comments

10. **Real-World Workflows**: Testing realistic user journeys that span multiple features (search → friend → room → delete)

This comprehensive test suite provides confidence that the social discovery and room management features work correctly both individually and as an integrated system.
