# Lesson: Property-Based Testing for Race Conditions

## Task Context

This lesson covers writing a property-based test that explores the race condition bug on room join. The test generates rapid join/send sequences with varying delays and verifies that membership is correctly verified before message send.

The test is designed to **FAIL on unfixed code** (demonstrating the race condition exists) and **PASS on fixed code**.

## Files Modified

- `src/test/java/org/example/chat/controller/RaceConditionPropertyTest.java` (created)

## Step-by-Step Changes

### 1. Understanding Property-Based Testing

Property-based testing is a testing approach where you define properties (invariants) that should hold true for all inputs, and the testing framework generates many random inputs to verify those properties.

**Key Concepts:**
- `@Property` annotation marks a test method as a property test
- `@ForAll` generates random values for test parameters
- `@IntRange` constrains the range of generated integers
- `@Label` provides a human-readable description of the property
- `@PropertyDefaults(tries = 100)` runs the test 100 times with different inputs

### 2. Setting Up the Test Class

The test class uses Mockito to mock dependencies and jqwik to generate test data:

```java
@PropertyDefaults(tries = 100)
class RaceConditionPropertyTest {
    @Mock private ChatMessageService chatMessageService;
    @Mock private ChatRoomService chatRoomService;
    // ... other mocks
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Initialize test data
    }
}
```

This setup runs each property test 100 times with different generated inputs, increasing the likelihood of finding edge cases.

### 3. First Property Test: Rapid Join/Send Sequences

The main property test generates:
- **sequenceCount**: 1-10 rapid join/send sequences
- **delayMs**: 0-50ms delay between join and send

```java
@Property
void testRapidJoinSendSequences(
        @ForAll @IntRange(min = 1, max = 10) int sequenceCount,
        @ForAll @IntRange(min = 0, max = 50) int delayMs) {
    // Setup mocks for successful join and send
    // Execute rapid join/send sequences
    // Assert no "not a member" errors occur
}
```

**Why this works:**
- Generates 100 different combinations of sequenceCount and delayMs
- Each combination tests a different timing scenario
- If a race condition exists, it will likely appear in at least one combination
- The framework captures the failing combination as a counterexample

### 4. Membership Persistence Test

This test verifies that membership persists across multiple join/send attempts:

```java
@Property
void testMembershipPersistedBeforeSend(
        @ForAll @IntRange(min = 1, max = 5) int attemptCount) {
    // Join and send multiple times
    // Verify membership lookup succeeds every time
}
```

**Why this matters:**
- If membership isn't persisted, the second join/send attempt will fail
- This catches bugs where membership is deleted or not committed to the database

### 5. Different Timing Scenarios Test

This test explicitly tests three timing scenarios:

```java
@Property
void testDifferentTimingScenarios(
        @ForAll @IntRange(min = 0, max = 2) int scenarioIndex) {
    int delayMs = switch (scenarioIndex) {
        case 0 -> 0;      // Immediate send
        case 1 -> 10;     // 10ms delay
        case 2 -> 50;     // 50ms delay
        default -> 0;
    };
    // Join, delay, then send
    // Assert send succeeds
}
```

**Why this approach:**
- Tests the three specific timing scenarios mentioned in requirements
- Immediate send (0ms) is the most likely to trigger race conditions
- Delays (10ms, 50ms) test if the fix handles various timing windows

### 6. Non-Member Rejection Test

This test verifies that the race condition fix doesn't accidentally allow non-members:

```java
@Property
void testNonMembersRejected(
        @ForAll @IntRange(min = 1, max = 5) int attemptCount) {
    // Configure mocks to simulate non-member
    // Attempt to join multiple times
    // Assert all attempts are rejected
}
```

**Why this is important:**
- Regression prevention: ensures the fix doesn't weaken authorization
- Verifies that only members can join/send

## Why This Approach

### Property-Based Testing Benefits

1. **Comprehensive Coverage**: Generates 100+ test cases automatically instead of writing them manually
2. **Edge Case Discovery**: Finds timing scenarios you might not think of
3. **Reproducible Failures**: Captures failing inputs as counterexamples for debugging
4. **Regression Prevention**: Ensures fixes don't break existing functionality

### Mocking Strategy

- Mocks all dependencies to isolate the controller logic
- Allows testing without a real database or STOMP broker
- Makes tests fast and deterministic

### Timing Scenarios

- **0ms (immediate)**: Most likely to trigger race conditions
- **10ms**: Tests if fix handles short delays
- **50ms**: Tests if fix handles longer delays

## Alternatives Considered

### 1. Unit Tests with Fixed Inputs
Instead of property-based testing, we could write fixed unit tests:
```java
@Test
void testImmediateJoinSend() { /* test with 0ms delay */ }
@Test
void test10msDelay() { /* test with 10ms delay */ }
@Test
void test50msDelay() { /* test with 50ms delay */ }
```

**Why we chose property-based instead:**
- Property-based generates 100+ combinations automatically
- Finds edge cases we might miss with fixed tests
- More maintainable: one test instead of many

### 2. Integration Tests with Real Database
We could test with a real database and STOMP broker:
```java
@SpringBootTest
void testRealDatabaseRaceCondition() { /* ... */ }
```

**Why we chose unit tests with mocks instead:**
- Faster execution (no database startup)
- More deterministic (no timing variability)
- Easier to debug (isolated to controller logic)
- Can be run in CI/CD without infrastructure

### 3. Concurrent Testing with Threads
We could use multiple threads to simulate concurrent STOMP messages:
```java
ExecutorService executor = Executors.newFixedThreadPool(10);
// Submit join and send tasks concurrently
```

**Why we chose sequential testing instead:**
- Simpler to understand and debug
- Property-based testing already covers timing variations
- Thread-based testing adds complexity without proportional benefit

## Key Concepts

### Race Condition
A race condition occurs when two operations (join and send) execute in an unexpected order, causing one to fail. In this case:
1. User calls `room.join` STOMP handler
2. User immediately calls `chat.send` STOMP handler
3. If send executes before join completes, membership check fails

### Property-Based Testing
A testing approach where you define properties (invariants) that should hold for all inputs, and the framework generates many random inputs to verify those properties.

### Counterexample
When a property test fails, jqwik captures the specific input values that caused the failure, making it easy to reproduce and debug.

### Transactional Consistency
Database transactions ensure that membership changes are atomically committed before the join handler returns, preventing race conditions.

## Potential Pitfalls

### 1. Flaky Tests
If the test sometimes passes and sometimes fails, it might be due to:
- Timing assumptions (e.g., assuming 10ms is always enough)
- Mock configuration issues
- **Solution**: Use property-based testing to explore many timing scenarios

### 2. False Positives
The test might pass even if the bug exists because:
- The mock setup doesn't accurately reflect the real behavior
- The timing scenarios don't trigger the race condition
- **Solution**: Run the test against unfixed code to verify it fails

### 3. Test Complexity
Property-based tests can be harder to understand than fixed tests:
- Multiple generated inputs make debugging harder
- Counterexamples help, but require interpretation
- **Solution**: Add clear labels and comments explaining what each property tests

### 4. Insufficient Tries
If `@PropertyDefaults(tries = 100)` isn't enough to trigger the bug:
- Increase the number of tries
- Adjust the range of generated values
- **Solution**: Start with 100 tries and increase if needed

## What You Learned

1. **Property-Based Testing**: How to use jqwik to generate test inputs and verify properties
2. **Race Condition Testing**: How to test for timing-dependent bugs using property-based testing
3. **Mocking Strategy**: How to mock dependencies to isolate controller logic
4. **Timing Scenarios**: Why testing multiple timing scenarios (0ms, 10ms, 50ms) is important
5. **Regression Prevention**: How to ensure fixes don't accidentally weaken authorization

## Next Steps

1. Run this test against unfixed code to verify it fails with a race condition counterexample
2. Implement the race condition fix (add `@Transactional` to `joinRoom` handler)
3. Run this test again to verify it passes
4. Move on to task 1.2 (membership persistence property test)
