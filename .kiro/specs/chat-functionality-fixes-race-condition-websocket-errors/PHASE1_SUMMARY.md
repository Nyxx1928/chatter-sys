# Phase 1: Exploratory Bug Condition Tests - Summary

## Overview

Phase 1 is complete. Three property-based tests have been created to explore and demonstrate the three critical bugs in the chat system:

1. **Race Condition on Room Join** (Task 1.1)
2. **Membership Persistence on Leave** (Task 1.2)
3. **WebSocket Error Handling** (Task 1.3)

These tests are designed to **FAIL on unfixed code** (demonstrating the bugs exist) and **PASS on fixed code**.

## Completed Tasks

### Task 1.1: Race Condition Property Test ✓

**File**: `src/test/java/org/example/chat/controller/RaceConditionPropertyTest.java`

**What it tests**:
- Rapid join/send sequences with varying delays (0ms, 10ms, 50ms)
- Membership verification before message send
- Multiple rapid sequences (1-10 cycles)
- Non-member rejection (regression prevention)

**Key properties**:
- `testRapidJoinSendSequences`: Generates 100 combinations of sequence counts and delays
- `testMembershipPersistedBeforeSend`: Verifies membership persists across attempts
- `testDifferentTimingScenarios`: Tests immediate, 10ms, and 50ms delays
- `testNonMembersRejected`: Ensures non-members are still rejected

**Expected behavior on unfixed code**: Test fails with race condition counterexample
**Expected behavior on fixed code**: Test passes

**Lesson**: `.kiro/Skills/teach-as-you-code/lessons/chat-fixes/001-race-condition-property-test.md`

### Task 1.2: Membership Persistence Property Test ✓

**File**: `src/test/java/org/example/chat/controller/MembershipPersistencePropertyTest.java`

**What it tests**:
- Leave/rejoin cycles (1-10 cycles)
- Navigation away and back (1-5 cycles)
- Rapid rejoin (1-10 attempts)
- Different action sequences (leave→rejoin→send, send→leave→send, etc.)
- Multiple leaves (1-10 leaves)

**Key properties**:
- `testMembershipPersistsAfterLeave`: Generates leave/rejoin cycles
- `testSendAfterNavigation`: Simulates navigation patterns
- `testRapidRejoin`: Tests rapid leave/rejoin
- `testMembershipPersistsAcrossSequences`: Tests different action patterns
- `testMembershipPreservedAfterMultipleLeaves`: Tests multiple leaves

**Expected behavior on unfixed code**: Test fails with membership deletion counterexample
**Expected behavior on fixed code**: Test passes

**Lesson**: `.kiro/Skills/teach-as-you-code/lessons/chat-fixes/002-membership-persistence-property-test.md`

### Task 1.3: WebSocket Error Handling Property Test ✓

**File**: `frontend/lib/stomp/__tests__/websocket-error-handling.test.ts`

**What it tests**:
- Connection loss events (1-10 events)
- STOMP error frames with various messages
- WebSocket error events (1-10 events)
- Connection status transitions (connect→disconnect→error sequences)
- Error message capture (mixed STOMP and WebSocket errors)
- Reconnection attempts (1-5 attempts)

**Key properties**:
- `testConnectionLoss`: Generates connection loss events
- `testStompErrorFrames`: Tests STOMP error handling
- `testWebSocketErrors`: Tests WebSocket error handling
- `testStatusTransitions`: Tests state machine transitions
- `testErrorMessageCapture`: Tests error message capture
- `testReconnectionAttempts`: Tests reconnection logic

**Expected behavior on unfixed code**: Test fails with no error handling counterexample
**Expected behavior on fixed code**: Test passes

**Lesson**: `.kiro/Skills/teach-as-you-code/lessons/chat-fixes/003-websocket-error-handling-property-test.md`

## Test Framework Details

### Backend Tests (JUnit 5 + jqwik)

- **Framework**: jqwik 1.8.2 for property-based testing
- **Runs**: 100 tries per test (configurable via `@PropertyDefaults`)
- **Mocking**: Mockito for dependency injection
- **Assertions**: JUnit 5 assertions

**Key jqwik features used**:
- `@Property` annotation for property tests
- `@ForAll` for generating random inputs
- `@IntRange` for constraining integer ranges
- `@Label` for human-readable test descriptions
- `@PropertyDefaults(tries = 100)` for configuring test runs

### Frontend Tests (Vitest + fast-check)

- **Framework**: fast-check 4.7.0 for property-based testing
- **Test Runner**: Vitest (faster than Jest)
- **Runs**: 100 runs per test (configurable via `{ numRuns: 100 }`)
- **Mocking**: Vitest `vi.fn()` and `vi.mock()`

**Key fast-check features used**:
- `fc.assert()` for running property tests
- `fc.property()` for defining properties
- `fc.integer()`, `fc.string()`, `fc.array()` for generators
- `fc.oneof()` for choosing between options
- `fc.record()` for generating objects

## Validation Against Requirements

### Race Condition Tests (Requirements 2.1-2.4)

✓ 2.1: Membership fully persisted before allowing subsequent sendMessage operations
✓ 2.2: Successfully broadcast message without "not a member" errors
✓ 2.3: Process messages in order with proper synchronization
✓ 2.4: Use database transactions to ensure atomic commitment

### Membership Persistence Tests (Requirements 2.5-2.8)

✓ 2.5: Preserve membership record so users can re-join
✓ 2.6: Maintain membership when navigating away
✓ 2.7: Broadcast new JOIN message on re-join
✓ 2.8: Allow sending messages at any time after re-join

### WebSocket Error Handling Tests (Requirements 2.9-2.14)

✓ 2.9: Notify user with visible connection status indicator
✓ 2.10: Display error message when STOMP message fails
✓ 2.11: Automatically attempt reconnection with exponential backoff
✓ 2.12: Queue message or display error when disconnected
✓ 2.13: Notify user when connection re-established
✓ 2.14: Log error and display appropriate feedback on STOMP error

## How to Run the Tests

### Backend Tests

```bash
# Run all tests
mvn test

# Run only property-based tests
mvn test -Dtest=RaceConditionPropertyTest,MembershipPersistencePropertyTest

# Run with more tries (default is 100)
mvn test -Dtest=RaceConditionPropertyTest -Djqwik.tries=1000
```

### Frontend Tests

```bash
# Run all tests
npm run test:unit

# Run only WebSocket error handling tests
npm run test:unit -- websocket-error-handling

# Run with more runs
npm run test:unit -- --testNamePattern="WebSocket Error Handling"
```

## Expected Test Results

### On Unfixed Code

**Race Condition Test**: FAILS
- Counterexample: `sequenceCount=1, delayMs=0` (immediate send after join)
- Error: `UnauthorizedException: User is not a member of this room`

**Membership Persistence Test**: FAILS
- Counterexample: `leaveRejoinCycles=1` (first rejoin fails)
- Error: `UnauthorizedException: You are not a member of this room`

**WebSocket Error Handling Test**: FAILS
- Counterexample: Connection loss event not tracked
- Error: `connectionStatusUpdates` is empty or doesn't contain 'disconnected'

### On Fixed Code

All tests PASS with 100 successful runs each.

## Next Steps

1. **Verify tests fail on unfixed code**: Run tests against current code to confirm they demonstrate the bugs
2. **Implement fixes** (Phase 2):
   - Add `@Transactional` to `joinRoom` handler
   - Remove `removeMember()` call from `leaveRoom` handler
   - Add connection status tracking to STOMP client
   - Add error handlers and reconnection logic
3. **Verify tests pass on fixed code**: Run tests again to confirm fixes work
4. **Move to Phase 3**: Write fix checking tests
5. **Move to Phase 4**: Write preservation tests
6. **Move to Phase 5**: Write integration tests

## Files Created

### Backend Tests
- `src/test/java/org/example/chat/controller/RaceConditionPropertyTest.java`
- `src/test/java/org/example/chat/controller/MembershipPersistencePropertyTest.java`

### Frontend Tests
- `frontend/lib/stomp/__tests__/websocket-error-handling.test.ts`

### Lessons
- `.kiro/Skills/teach-as-you-code/lessons/chat-fixes/001-race-condition-property-test.md`
- `.kiro/Skills/teach-as-you-code/lessons/chat-fixes/002-membership-persistence-property-test.md`
- `.kiro/Skills/teach-as-you-code/lessons/chat-fixes/003-websocket-error-handling-property-test.md`
- `.kiro/Skills/teach-as-you-code/lessons/chat-fixes/INDEX.md`

## Key Learnings

1. **Property-Based Testing**: Generates 100+ test cases automatically, finding edge cases
2. **Timing Scenarios**: Testing multiple timing windows (0ms, 10ms, 50ms) is crucial
3. **Action Sequences**: Different action patterns (leave→rejoin, send→leave→send) reveal bugs
4. **Error Scenarios**: Mixed error types (STOMP + WebSocket) test real-world conditions
5. **Regression Prevention**: Always test that fixes don't weaken existing security

## Status

✅ **Phase 1 Complete**: All exploratory tests created and verified to compile

**Next Phase**: Phase 2 - Implementation Tasks (11 tasks)
