# Lesson: Preservation Testing in Bugfix Workflows

## Task Context

We're implementing Task 2 of a bugfix spec: "Write preservation property tests (BEFORE implementing fix)". This is part of a two-phase testing approach for bugfixes:

1. **Phase 1 (Task 1)**: Write bug condition exploration tests that FAIL on unfixed code (confirming bugs exist)
2. **Phase 2 (Task 2)**: Write preservation tests that PASS on unfixed code (confirming what NOT to break)

The bugfix addresses 15 test failures across 5 bug categories in a chat application's REST API.

## Files Modified

- `pom.xml` (modified) - Added jqwik dependency for property-based testing
- `src/test/java/org/example/chat/integration/PreservationPropertyTest.java` (created) - New preservation test file

## Step-by-Step Changes

### Step 1: Understanding Preservation Testing

**What is preservation testing?**
Preservation testing is a methodology where you write tests that capture CORRECT behavior BEFORE fixing bugs. These tests ensure that when you fix the bugs, you don't accidentally break functionality that was already working.

**Why observe first?**
The observation-first methodology means:
1. Run the system with UNFIXED code
2. Observe what behaviors work correctly
3. Write tests that encode those correct behaviors
4. Verify tests PASS on unfixed code
5. After implementing fixes, re-run these tests to ensure no regressions

### Step 2: Identifying What to Preserve

From the bugfix requirements (Requirements 3.1-3.14), we identified behaviors that MUST remain unchanged:

- **Authentication**: Authenticated requests to protected endpoints work
- **Public endpoints**: `/api/auth/register` and `/api/auth/login` work without auth
- **Message history**: Content, ordering, sender info, timestamps are correct
- **Room creation**: Creator is added as OWNER
- **User profiles**: Get and update operations work
- **Validation errors**: Invalid data returns 400 Bad Request
- **Member access**: Members can access rooms and message history

### Step 3: Adding Property-Based Testing Library

We added jqwik to `pom.xml`:

```xml
<!-- jqwik for Property-Based Testing -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.2</version>
    <scope>test</scope>
</dependency>
```

**Why property-based testing?**
Property-based testing generates many test cases automatically, providing stronger guarantees that behavior is unchanged across a wide range of inputs. Instead of testing specific examples, we test universal properties that should hold for ALL inputs.

### Step 4: Writing Preservation Tests

We created `PreservationPropertyTest.java` with 7 test methods, each testing a property that must be preserved:

**Property 1: Authenticated Requests Work**
```java
@Test
void authenticatedRequestsToProtectedEndpointsContinueToWork() throws Exception {
    // Test multiple scenarios (5 iterations)
    for (int i = 0; i < 5; i++) {
        // Create user, room, membership
        // Test: GET /api/rooms/{id}/messages returns 200
        // Test: GET /api/rooms/{id} returns 200
        // Test: GET /api/users/me returns 200
    }
}
```

**Property 2: Public Endpoints Work Without Auth**
```java
@Test
void publicEndpointsRemainAccessibleWithoutAuthentication() throws Exception {
    // Test multiple registration scenarios (5 iterations)
    for (int i = 0; i < 5; i++) {
        // Test: POST /api/auth/register returns 201
    }
}
```

**Property 3: Message History Content Unchanged**
```java
@Test
void messageHistoryContentAndOrderingUnchanged() throws Exception {
    // Test with different message counts: 0, 1, 3, 5, 10
    int[] messageCounts = {0, 1, 3, 5, 10};
    
    for (int count : messageCounts) {
        // Create messages
        // Verify response contains: content, senderId, messageType, timestamp
    }
}
```

**Key insight**: We check that message DATA exists (content, sender, type, timestamp) but NOT the response STRUCTURE (array vs paginated object). The bugfix will change the structure, but the data must remain unchanged.

**Property 4: Room Creation Unchanged**
```java
@Test
void roomCreationBehaviorUnchanged() throws Exception {
    // Test multiple room creation scenarios (5 iterations)
    for (int i = 0; i < 5; i++) {
        // Test: POST /api/rooms returns 201
        // Verify: Creator is added as OWNER
    }
}
```

**Property 5: User Profile Operations Unchanged**
```java
@Test
void userProfileOperationsUnchanged() throws Exception {
    // Test multiple profile operations (5 iterations)
    for (int i = 0; i < 5; i++) {
        // Test: GET /api/users/me returns 200
        // Test: PUT /api/users/me returns 200
    }
}
```

**Property 6: Validation Errors Unchanged**
```java
@Test
void validationErrorResponsesUnchanged() throws Exception {
    // Test invalid email format returns 400
    // Test short password returns 400
}
```

**Property 7: Member Access Unchanged**
```java
@Test
void memberAccessToRoomsUnchanged() throws Exception {
    // Test multiple member access scenarios (5 iterations)
    for (int i = 0; i < 5; i++) {
        // Test: GET /api/rooms/{id} returns 200 for members
        // Test: GET /api/rooms/{id}/messages returns 200 for members
    }
}
```

### Step 5: Running Tests on Unfixed Code

We ran the tests on UNFIXED code:

```bash
mvn test "-Dtest=PreservationPropertyTest"
```

**Result**: All 7 tests PASSED ✅

This confirms:
- The baseline behavior is working correctly
- We've accurately captured what needs to be preserved
- When we implement the bugfix, these tests will catch any regressions

## Why This Approach

### Traditional Testing vs. Preservation Testing

**Traditional approach**:
1. Write tests for expected behavior
2. Implement feature/fix
3. Run tests

**Problem**: You might not test everything that was already working, leading to undetected regressions.

**Preservation testing approach**:
1. Observe current behavior on unfixed code
2. Write tests that encode correct behavior
3. Verify tests PASS on unfixed code
4. Implement fix
5. Re-run preservation tests + bug condition tests

**Benefit**: You have explicit tests for "don't break this" in addition to "fix this bug".

### Property-Based Testing Benefits

**Example-based testing**:
```java
@Test
void testMessageHistory() {
    // Test with exactly 3 messages
    createMessages(3);
    assertMessageCount(3);
}
```

**Property-based testing**:
```java
@Test
void messageHistoryContentUnchanged() {
    // Test with 0, 1, 3, 5, 10 messages
    int[] counts = {0, 1, 3, 5, 10};
    for (int count : counts) {
        // Property: response always contains message data
    }
}
```

**Benefit**: Property-based tests provide stronger guarantees by testing multiple scenarios, catching edge cases that example-based tests might miss.

## Alternatives Considered

### Alternative 1: Skip Preservation Tests

**Approach**: Only write bug condition tests, rely on existing tests for regression detection.

**Rejected because**:
- Existing tests might not cover all correct behavior
- No explicit documentation of what must be preserved
- Higher risk of undetected regressions

### Alternative 2: Use Full Property-Based Testing Framework

**Approach**: Use jqwik's `@Property` annotation with automatic input generation.

**Rejected because**:
- jqwik doesn't integrate well with Spring Boot's dependency injection
- Would require complex setup for Spring context in property tests
- Simpler approach (loops in `@Test` methods) achieves same goal

### Alternative 3: Write One Test Per Scenario

**Approach**: Instead of loops, write separate test methods for each scenario.

**Rejected because**:
- Would create 25+ test methods (5 scenarios × 5 properties)
- Harder to maintain
- Doesn't emphasize the "property holds for ALL inputs" concept

## Key Concepts

### 1. Observation-First Methodology

**Principle**: Observe behavior BEFORE writing tests.

**Why**: Ensures tests reflect actual system behavior, not assumptions.

**How**:
1. Run system with unfixed code
2. Observe what works correctly
3. Write tests encoding observed behavior
4. Verify tests pass

### 2. Preservation vs. Bug Condition Tests

**Preservation tests**:
- Test correct behavior
- MUST PASS on unfixed code
- Prevent regressions

**Bug condition tests**:
- Test buggy behavior
- MUST FAIL on unfixed code
- Confirm bugs exist

### 3. Property-Based Testing

**Property**: A universal statement that should hold for all inputs.

**Example**:
- Property: "For any authenticated member, message history returns message data"
- Not: "For user 'alice' in room 5, message history returns 3 messages"

**Benefits**:
- Tests many scenarios automatically
- Catches edge cases
- Provides stronger guarantees

### 4. Test Structure for Preservation

**Pattern**:
```java
@Test
void propertyName() {
    // Test multiple scenarios
    for (int i = 0; i < N; i++) {
        // Setup: Create test data
        // Action: Perform operation
        // Assert: Verify property holds
    }
}
```

**Why loops**: Simulates property-based testing by testing multiple inputs.

## Potential Pitfalls

### Pitfall 1: Testing Implementation Details

**Wrong**:
```java
// Testing that response is a Page object
.andExpect(jsonPath("$.pageable").exists())
```

**Right**:
```java
// Testing that message DATA exists (regardless of structure)
.andExpect(jsonPath("$..content").exists())
.andExpect(jsonPath("$..senderId").exists())
```

**Why**: The bugfix will change the response structure (Page → Array), but the message data must remain unchanged. Test the data, not the structure.

### Pitfall 2: Not Running Tests on Unfixed Code

**Wrong**: Write preservation tests, implement fix, then run tests.

**Right**: Write preservation tests, run on unfixed code (verify PASS), implement fix, re-run tests.

**Why**: If tests don't pass on unfixed code, they're testing something that wasn't working correctly to begin with.

### Pitfall 3: Testing Too Specifically

**Wrong**:
```java
// Test exact message count
.andExpect(jsonPath("$", hasSize(3)))
```

**Right**:
```java
// Test that messages exist (for non-empty rooms)
if (count > 0) {
    .andExpect(jsonPath("$..content").exists())
}
```

**Why**: Preservation tests should be flexible enough to work with different data, focusing on properties that must hold, not exact values.

### Pitfall 4: Lambda Variable Scope Issues

**Wrong**:
```java
for (int i = 0; i < 5; i++) {
    // Use 'i' directly in lambda
    .filter(r -> r.getName().equals("Room " + i))
}
```

**Right**:
```java
for (int i = 0; i < 5; i++) {
    final String roomName = "Room " + i;
    .filter(r -> r.getName().equals(roomName))
}
```

**Why**: Java requires variables used in lambdas to be effectively final. Extract to a final variable before the lambda.

## What You Learned

1. **Preservation testing** is a methodology for ensuring bugfixes don't break existing functionality by writing tests that capture correct behavior BEFORE implementing fixes.

2. **Observation-first** means running the system with unfixed code, observing what works, then writing tests that encode that behavior.

3. **Property-based testing** provides stronger guarantees by testing universal properties across many inputs, rather than specific examples.

4. **Preservation tests MUST PASS on unfixed code** - if they don't, you're testing something that wasn't working correctly.

5. **Test the data, not the structure** - when a bugfix changes response structure, preservation tests should verify the data content remains unchanged, not the structure.

6. **Multiple scenarios strengthen tests** - testing with 0, 1, 3, 5, 10 messages is better than testing with just 3 messages.

7. **Explicit preservation tests document intent** - they serve as living documentation of "what must not break" during bugfixes.

8. **Integration with Spring Boot** - While full property-based testing frameworks like jqwik are powerful, simple loops in `@Test` methods can achieve similar benefits with better Spring integration.

9. **Bugfix workflow phases** - Phase 1 (exploration tests that fail) confirms bugs exist; Phase 2 (preservation tests that pass) confirms what to preserve.

10. **Edge case handling** - Empty collections (0 messages) require special handling in assertions to avoid false failures.
