# Lesson: Property-Based Testing for WebSocket Error Handling

## Task Context

This lesson covers writing a property-based test for WebSocket error handling. The test simulates connection loss and message send failures, verifying that users are notified and the system attempts to reconnect.

The test is designed to **FAIL on unfixed code** (no error handling) and **PASS on fixed code**.

## Files Modified

- `frontend/lib/stomp/__tests__/websocket-error-handling.test.ts` (created)

## Step-by-Step Changes

### 1. Understanding WebSocket Error Handling

WebSocket errors can occur in several ways:
1. **Connection Loss**: Network drops, server goes down
2. **STOMP Errors**: Server sends ERROR frame
3. **WebSocket Errors**: Low-level WebSocket protocol errors
4. **Message Send Failures**: Message fails to send while disconnected

The fix: Track connection status, display errors, and attempt reconnection.

### 2. Setting Up the Test File

Unlike the backend tests, frontend tests use Jest with fast-check:

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest';
import fc from 'fast-check';
import { Client, IFrame } from '@stomp/stompjs';
import { createStompClient } from '../client';

describe('WebSocket Error Handling - Property-Based Tests', () => {
  let mockOnConnect: ReturnType<typeof vi.fn>;
  let mockOnDisconnect: ReturnType<typeof vi.fn>;
  // ... other mocks
});
```

**Key differences from backend tests:**
- Uses `vitest` instead of Jest (faster, better TypeScript support)
- Uses `fast-check` for property generation
- Mocks browser APIs like SockJS

### 3. Connection Loss Test

This test generates multiple connection loss events:

```typescript
it('should handle connection loss with property-based testing', () => {
  fc.assert(
    fc.property(
      fc.integer({ min: 1, max: 10 }), // Number of connection loss events
      fc.integer({ min: 0, max: 100 }), // Delay between events (ms)
      (eventCount, delayMs) => {
        const connectionStatusUpdates: string[] = [];
        // Simulate connection loss events
        // Verify status is updated
      }
    ),
    { numRuns: 100 }
  );
});
```

**Why this works:**
- Generates 100 different combinations of event counts and delays
- Tests various connection loss scenarios
- Verifies status is updated for each event

### 4. STOMP Error Frame Test

This test generates STOMP error frames with different messages:

```typescript
it('should handle STOMP error frames with property-based testing', () => {
  fc.assert(
    fc.property(
      fc.array(fc.string({ minLength: 1, maxLength: 100 }), {
        minLength: 1,
        maxLength: 10,
      }), // Array of error messages
      (errorMessages) => {
        const capturedErrors: string[] = [];
        // Simulate STOMP error frames
        // Verify errors are captured
      }
    ),
    { numRuns: 100 }
  );
});
```

**Why this matters:**
- Tests that different error messages are handled
- Verifies error messages are captured for display
- Tests edge cases like very long error messages

### 5. WebSocket Error Test

This test generates WebSocket error events:

```typescript
it('should handle WebSocket errors with property-based testing', () => {
  fc.assert(
    fc.property(
      fc.integer({ min: 1, max: 10 }), // Number of WebSocket errors
      (errorCount) => {
        const errorEvents: Event[] = [];
        // Simulate WebSocket errors
        // Verify errors are captured
      }
    ),
    { numRuns: 100 }
  );
});
```

**Why this is important:**
- Tests low-level WebSocket protocol errors
- Verifies the system handles network-level failures
- Tests repeated error scenarios

### 6. Connection Status Transition Test

This test generates different connection state transitions:

```typescript
it('should transition connection status correctly with property-based testing', () => {
  fc.assert(
    fc.property(
      fc.array(
        fc.oneof(
          fc.constant('connect'),
          fc.constant('disconnect'),
          fc.constant('error')
        ),
        { minLength: 1, maxLength: 20 }
      ), // Sequence of events
      (eventSequence) => {
        const statusHistory: string[] = [];
        // Simulate event sequence
        // Verify status transitions are correct
      }
    ),
    { numRuns: 100 }
  );
});
```

**Why this approach:**
- Tests realistic sequences of connection events
- Verifies status transitions are correct
- Catches edge cases like error→connect→disconnect

### 7. Error Message Capture Test

This test verifies that error messages are captured:

```typescript
it('should capture error messages for display with property-based testing', () => {
  fc.assert(
    fc.property(
      fc.record({
        stompErrors: fc.array(fc.string(...), { minLength: 0, maxLength: 5 }),
        webSocketErrors: fc.integer({ min: 0, max: 5 }),
      }),
      ({ stompErrors, webSocketErrors }) => {
        const capturedMessages: string[] = [];
        // Simulate various errors
        // Verify all messages are captured
      }
    ),
    { numRuns: 100 }
  );
});
```

**Why this matters:**
- Tests that error messages are available for UI display
- Verifies both STOMP and WebSocket errors are captured
- Tests mixed error scenarios

### 8. Reconnection Attempt Test

This test verifies reconnection is attempted:

```typescript
it('should attempt reconnection after connection loss with property-based testing', () => {
  fc.assert(
    fc.property(
      fc.integer({ min: 1, max: 5 }), // Number of reconnection attempts
      (attemptCount) => {
        const reconnectionAttempts: number[] = [];
        // Simulate connection loss and reconnection
        // Verify reconnection attempts are made
      }
    ),
    { numRuns: 100 }
  );
});
```

**Why this is important:**
- Tests that the system attempts to reconnect
- Verifies reconnection logic is triggered
- Tests multiple reconnection attempts

## Why This Approach

### Property-Based Testing Benefits

1. **Comprehensive Coverage**: Tests many error scenarios automatically
2. **Edge Case Discovery**: Finds error combinations you might not think of
3. **Reproducible Failures**: Captures failing scenarios as counterexamples
4. **Regression Prevention**: Ensures error handling works in all scenarios

### Fast-Check Advantages

- **Powerful Generators**: Can generate complex data structures
- **Shrinking**: Automatically simplifies failing inputs
- **Reproducibility**: Can replay failing scenarios
- **Type Safety**: Works well with TypeScript

### Error Scenario Variety

- **Connection Loss**: Network drops
- **STOMP Errors**: Server-side errors
- **WebSocket Errors**: Protocol-level errors
- **Mixed Scenarios**: Multiple errors in sequence

## Alternatives Considered

### 1. Fixed Unit Tests
Instead of property-based testing:
```typescript
it('should handle connection loss', () => { /* ... */ });
it('should handle STOMP error', () => { /* ... */ });
it('should handle WebSocket error', () => { /* ... */ });
```

**Why we chose property-based instead:**
- Property-based generates 100+ scenarios automatically
- More maintainable: one test instead of many
- Finds edge cases we might miss

### 2. Integration Tests with Real WebSocket
We could test with a real WebSocket server:
```typescript
it('should handle real connection loss', async () => {
  const server = startTestServer();
  // Connect and simulate connection loss
  server.close();
  // Verify reconnection
});
```

**Why we chose unit tests with mocks instead:**
- Faster execution (no server startup)
- More deterministic (no network variability)
- Easier to debug (isolated to client logic)

### 3. E2E Tests with Playwright
We could test the full UI with Playwright:
```typescript
test('user sees connection status indicator', async ({ page }) => {
  await page.goto('/chat');
  // Simulate connection loss
  // Verify UI shows disconnected status
});
```

**Why we chose unit tests instead:**
- Faster execution (no browser startup)
- More focused (tests error handling logic, not UI)
- Easier to debug (isolated to STOMP client)

## Key Concepts

### Connection Status States
- **connected**: WebSocket is open and STOMP is authenticated
- **disconnected**: WebSocket is closed
- **reconnecting**: Attempting to reconnect
- **error**: An error occurred

### Error Types
- **STOMP Errors**: Server sends ERROR frame (e.g., authorization failure)
- **WebSocket Errors**: Low-level protocol errors (e.g., connection refused)
- **Network Errors**: Connection drops (e.g., network unavailable)

### Exponential Backoff
Reconnection attempts should use exponential backoff:
- 1st attempt: 1 second
- 2nd attempt: 2 seconds
- 3rd attempt: 4 seconds
- Max: 60 seconds

### Error Display
Error messages should be:
- Visible to the user
- Clear and actionable
- Automatically cleared on reconnection

## Potential Pitfalls

### 1. Not Testing Error Message Content
If we only test that errors are captured, we might miss that messages are wrong:
```typescript
// ❌ Wrong: Only checks that error exists
expect(capturedErrors.length).toBeGreaterThan(0);
```

**Solution**: Verify the actual error message content
```typescript
// ✓ Right: Checks message content
expect(capturedErrors).toContain(expectedMessage);
```

### 2. Not Testing Status Transitions
If we only test individual events, we might miss transition issues:
```typescript
// ❌ Wrong: Only tests connect event
client.onConnect?.();
expect(status).toBe('connected');
```

**Solution**: Test sequences of events
```typescript
// ✓ Right: Tests connect→disconnect→connect
client.onConnect?.();
client.onDisconnect?.();
client.onConnect?.();
```

### 3. Not Testing Mixed Error Scenarios
If we only test one error type at a time, we might miss interactions:
```typescript
// ❌ Wrong: Only tests STOMP errors
stompErrors.forEach(error => client.onStompError?.(error));
```

**Solution**: Test mixed scenarios
```typescript
// ✓ Right: Tests STOMP and WebSocket errors together
stompErrors.forEach(error => client.onStompError?.(error));
webSocketErrors.forEach(() => client.onWebSocketError?.(new Event('error')));
```

### 4. Not Testing Reconnection Attempts
If we don't test reconnection, we might miss that it's not happening:
```typescript
// ❌ Wrong: Only tests connection loss
client.onDisconnect?.();
```

**Solution**: Test that reconnection is attempted
```typescript
// ✓ Right: Tests reconnection after loss
client.onDisconnect?.();
// Verify reconnection logic is triggered
expect(reconnectionAttempts.length).toBeGreaterThan(0);
```

## What You Learned

1. **WebSocket Error Handling**: How to test error handling in WebSocket connections
2. **Property-Based Testing in Frontend**: How to use fast-check for frontend testing
3. **Error Scenarios**: How to generate and test various error scenarios
4. **Status Transitions**: How to test state machine transitions
5. **Error Message Capture**: How to verify error messages are captured for display

## Next Steps

1. Run this test against unfixed code to verify it fails with error handling gaps
2. Implement the WebSocket error handling fixes:
   - Add connection status tracking
   - Add error handlers
   - Add automatic reconnection
3. Run this test again to verify it passes
4. Move on to Phase 2 (Implementation tasks)
