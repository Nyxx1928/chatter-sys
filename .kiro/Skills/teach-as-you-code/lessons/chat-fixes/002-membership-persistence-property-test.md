# Lesson: Property-Based Testing for Membership Persistence

## Task Context

This lesson covers writing a property-based test that explores the membership persistence bug. The test generates leave/rejoin sequences and verifies that membership persists across navigation and leave operations.

The test is designed to **FAIL on unfixed code** (demonstrating membership is deleted) and **PASS on fixed code**.

## Files Modified

- `src/test/java/org/example/chat/controller/MembershipPersistencePropertyTest.java` (created)

## Step-by-Step Changes

### 1. Understanding the Membership Persistence Bug

The bug occurs when:
1. User joins a room (membership is created)
2. User navigates away from the room
3. The `leaveRoom` handler is called and **deletes** the membership
4. User returns to the room
5. User tries to send a message but gets "not a member" error

The fix: Don't delete membership on leave. Only broadcast the LEAVE message.

### 2. Setting Up the Test Class

Similar to the race condition test, we use Mockito and jqwik:

```java
@PropertyDefaults(tries = 100)
class MembershipPersistencePropertyTest {
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

The key difference: we're testing that membership **persists** after leave, not that it's deleted.

### 3. Main Property Test: Leave/Rejoin Cycles

This test generates multiple leave/rejoin cycles:

```java
@Property
void testMembershipPersistsAfterLeave(
        @ForAll @IntRange(min = 1, max = 10) int leaveRejoinCycles) {
    // Setup mocks for successful leave and rejoin
    // Execute multiple leave/rejoin cycles
    // Assert all cycles succeed
}
```

**Why this works:**
- Generates 100 different cycle counts (1-10)
- Each cycle: leave → rejoin → send
- If membership is deleted on leave, rejoin will fail
- The test catches this failure as a counterexample

### 4. Navigation Simulation Test

This test simulates the real-world scenario of navigating away and back:

```java
@Property
void testSendAfterNavigation(
        @ForAll @IntRange(min = 1, max = 5) int navigationCycles) {
    // Send message before navigation
    // Simulate navigation away (no explicit leave call)
    // Return to room and send message
    // Assert both sends succeed
}
```

**Why this matters:**
- In real apps, users navigate away without calling leave
- The frontend might call `leaveRoom` STOMP handler on navigation
- If membership is deleted, the user can't send messages after returning

### 5. Rapid Rejoin Test

This test verifies that users can rejoin immediately after leaving:

```java
@Property
void testRapidRejoin(
        @ForAll @IntRange(min = 1, max = 10) int rejoinAttempts) {
    // Leave and rejoin rapidly
    // Assert all rejoin attempts succeed
}
```

**Why this is important:**
- Tests the specific scenario mentioned in requirements
- Verifies that membership persists even with rapid leave/rejoin

### 6. Different Action Sequences Test

This test generates different sequences of actions:

```java
@Property
void testMembershipPersistsAcrossSequences(
        @ForAll @IntRange(min = 0, max = 3) int actionSequence) {
    switch (actionSequence) {
        case 0: // leave, rejoin, send
        case 1: // send, leave, send
        case 2: // leave, send (without explicit rejoin)
        case 3: // multiple leaves and sends
    }
}
```

**Why this approach:**
- Tests different real-world action patterns
- Verifies membership persists regardless of action order
- Catches edge cases like sending after leave without explicit rejoin

### 7. Multiple Leaves Test

This test verifies that multiple leaves don't delete membership:

```java
@Property
void testMembershipPreservedAfterMultipleLeaves(
        @ForAll @IntRange(min = 1, max = 10) int leaveCount) {
    // Leave multiple times
    // Assert rejoin still succeeds
}
```

**Why this matters:**
- Tests an edge case: what if leave is called multiple times?
- Verifies that membership isn't deleted on the first leave

## Why This Approach

### Property-Based Testing Benefits

1. **Comprehensive Coverage**: Tests many different leave/rejoin patterns automatically
2. **Edge Case Discovery**: Finds action sequences you might not think of
3. **Reproducible Failures**: Captures failing sequences as counterexamples
4. **Regression Prevention**: Ensures membership persistence works in all scenarios

### Action Sequence Variety

- **leave, rejoin, send**: Standard pattern
- **send, leave, send**: Tests sending after leave without explicit rejoin
- **leave, send**: Tests sending immediately after leave
- **multiple leaves and sends**: Tests repeated patterns

### Timing Considerations

Unlike the race condition test, this test doesn't focus on timing. Instead, it focuses on:
- Action order (which action happens first)
- Repetition (how many times actions are repeated)
- State persistence (does membership survive the actions)

## Alternatives Considered

### 1. Fixed Unit Tests
Instead of property-based testing:
```java
@Test
void testLeaveRejoinOnce() { /* ... */ }
@Test
void testLeaveRejoinTwice() { /* ... */ }
@Test
void testLeaveRejoinTenTimes() { /* ... */ }
```

**Why we chose property-based instead:**
- Property-based generates 100+ combinations automatically
- More maintainable: one test instead of many
- Finds edge cases we might miss

### 2. Integration Tests with Real Database
We could test with a real database:
```java
@SpringBootTest
void testMembershipPersistenceWithRealDB() { /* ... */ }
```

**Why we chose unit tests with mocks instead:**
- Faster execution (no database startup)
- More deterministic (no database state issues)
- Easier to debug (isolated to controller logic)

### 3. Concurrent Testing with Multiple Users
We could test with multiple users leaving/rejoining simultaneously:
```java
ExecutorService executor = Executors.newFixedThreadPool(5);
// Submit leave/rejoin tasks for different users
```

**Why we chose sequential testing instead:**
- Simpler to understand and debug
- Property-based testing already covers many scenarios
- Thread-based testing adds complexity without proportional benefit

## Key Concepts

### Membership Persistence
The property that membership should survive a leave operation. Users should remain members of a room until they explicitly request permanent removal.

### Navigation vs. Explicit Leave
- **Navigation**: User navigates away from the room (membership should persist)
- **Explicit Leave**: User clicks "Leave Room" button (membership might be deleted)

### State Preservation
The test verifies that the membership state is preserved across multiple operations, not just one.

### Idempotency
The test verifies that operations are idempotent: calling leave multiple times should have the same effect as calling it once.

## Potential Pitfalls

### 1. Mock Configuration Issues
If the mock always returns the same membership, the test won't catch deletion:
```java
// ❌ Wrong: Always returns membership
when(roomMembershipRepository.findByUserAndChatRoom(...))
    .thenReturn(Optional.of(testMembership));
```

**Solution**: In real code, the repository would return empty if membership is deleted. The mock should reflect this.

### 2. Insufficient Action Sequences
If we only test one action sequence, we might miss bugs:
```java
// ❌ Wrong: Only tests one sequence
controller.leaveRoom(1L, principal);
controller.joinRoom(1L, principal);
```

**Solution**: Test multiple sequences (leave→rejoin, send→leave→send, etc.)

### 3. Not Testing Rapid Operations
If we don't test rapid leave/rejoin, we might miss timing issues:
```java
// ❌ Wrong: Only tests one leave/rejoin
controller.leaveRoom(1L, principal);
controller.joinRoom(1L, principal);
```

**Solution**: Use property-based testing to generate many rapid cycles

### 4. Forgetting to Test Send After Leave
If we only test rejoin, we might miss that send fails:
```java
// ❌ Wrong: Only tests rejoin, not send
controller.leaveRoom(1L, principal);
controller.joinRoom(1L, principal);
// Missing: controller.sendMessage(...)
```

**Solution**: Always test the full action sequence (leave → rejoin → send)

## What You Learned

1. **Membership Persistence**: How to test that membership survives leave operations
2. **Action Sequences**: How to test different patterns of user actions
3. **State Preservation**: How to verify that state persists across operations
4. **Property-Based Testing**: How to generate many test scenarios automatically
5. **Edge Cases**: How to find and test edge cases like multiple leaves

## Next Steps

1. Run this test against unfixed code to verify it fails with a membership deletion counterexample
2. Implement the membership persistence fix (remove `removeMember()` call from `leaveRoom`)
3. Run this test again to verify it passes
4. Move on to task 1.3 (WebSocket error handling property test)
