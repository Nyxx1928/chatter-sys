# Tasks: Chat Functionality Fixes - Race Conditions, WebSocket Error Handling, and Room Re-join

## Overview

This bugfix addresses three critical functionality issues:
1. **Race Condition on Room Join** - Users get "not a member" errors when sending messages immediately after joining
2. **Incomplete Membership Fix** - Users cannot re-join rooms after leaving
3. **No WebSocket Error Handling** - Users don't know when connection drops

The task structure follows the bugfix workflow: exploratory tests → implementation → fix verification → preservation verification → integration tests.

---

## Phase 1: Exploratory Bug Condition Tests

These tests are designed to FAIL on unfixed code and PASS on fixed code. They surface counterexamples that demonstrate each bug exists.

### 1.1 Write bug condition exploration property test for race condition on room join

**Description**: Write a property-based test that generates rapid join/send sequences and verifies that membership is correctly verified before message send. This test should fail on unfixed code (demonstrating the race condition exists) and pass on fixed code.

**Acceptance Criteria**:
- Test generates multiple rapid join/send sequences with varying delays
- Test verifies that no "not a member" errors occur
- Test verifies that membership is persisted before send handler executes
- Test includes at least 3 different timing scenarios (immediate send, 10ms delay, 50ms delay)
- Test uses property-based testing framework (JUnit 5 with jqwik)
- Test fails on unfixed code with counterexample showing race condition
- Test passes on fixed code

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

**Implementation Notes**:
- Use jqwik `@Property` annotation
- Generate userId, roomId, and timing variations
- Simulate STOMP message sequence
- Assert membership verification succeeds
- Capture failing counterexamples

### 1.2 Write bug condition exploration property test for membership removal on leave

**Description**: Write a property-based test that generates leave/rejoin sequences and verifies that membership persists across navigation and leave operations. This test should fail on unfixed code (demonstrating membership is deleted) and pass on fixed code.

**Acceptance Criteria**:
- Test generates multiple leave/rejoin sequences
- Test verifies that membership record persists after leave
- Test verifies that users can re-join without errors
- Test includes scenarios: explicit leave, navigation away, rapid rejoin
- Test uses property-based testing framework (JUnit 5 with jqwik)
- Test fails on unfixed code with counterexample showing membership deletion
- Test passes on fixed code

**Validates: Requirements 2.5, 2.6, 2.7, 2.8**

**Implementation Notes**:
- Use jqwik `@Property` annotation
- Generate userId, roomId, and action sequences
- Verify membership record exists in database
- Assert rejoin succeeds without "not a member" errors
- Capture failing counterexamples

### 1.3 Write bug condition exploration property test for WebSocket error handling

**Description**: Write a property-based test that simulates connection loss and message send failures, verifying that users are notified and the system attempts to reconnect. This test should fail on unfixed code (no error handling) and pass on fixed code.

**Acceptance Criteria**:
- Test generates connection loss events and message send attempts
- Test verifies that connection status is tracked and updated
- Test verifies that error messages are displayed to user
- Test verifies that automatic reconnection is attempted
- Test includes scenarios: connection drop, message send during disconnect, reconnection
- Test uses property-based testing framework (Jest with fast-check for frontend)
- Test fails on unfixed code with counterexample showing no error handling
- Test passes on fixed code

**Validates: Requirements 2.9, 2.10, 2.11, 2.12, 2.13, 2.14**

**Implementation Notes**:
- Use fast-check for property generation
- Simulate WebSocket connection events
- Mock STOMP client
- Verify connection status store updates
- Verify error display component renders
- Capture failing counterexamples

---

## Phase 2: Implementation Tasks

### 2.1 Implement race condition prevention - add transactional synchronization to room join handler

**Description**: Modify the `joinRoom` handler in `ChatMessageController` to use `@Transactional` annotation and ensure membership is fully persisted before handler returns. This prevents race conditions where sendMessage executes before join completes.

**Acceptance Criteria**:
- `joinRoom` method is annotated with `@Transactional`
- Membership addition is flushed to database before handler returns
- Database transaction is committed before handler completes
- Subsequent sendMessage handlers see committed membership
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Add `@Transactional` to `joinRoom` method
- Use `entityManager.flush()` after membership save
- Ensure transaction isolation level is appropriate
- Test with concurrent join/send operations

### 2.2 Implement race condition prevention - add message ordering guarantee

**Description**: Add sequence numbers or ordering mechanism to STOMP messages to ensure join completes before send for the same user/room. This provides an additional layer of synchronization beyond database transactions.

**Acceptance Criteria**:
- STOMP messages include sequence numbers or ordering metadata
- Messages for same user/room are processed in order
- Join handler completes before send handler for rapid sequences
- No changes to message content or format visible to users
- Existing tests continue to pass

**Implementation Notes**:
- Add sequence tracking to STOMP handler
- Use Spring's message ordering capabilities
- Consider using a queue or executor for ordered processing
- Test with concurrent messages

### 2.3 Implement membership persistence - modify leave handler to preserve membership

**Description**: Modify the `leaveRoom` handler to NOT delete the membership record. Only broadcast the LEAVE message. This allows users to re-join rooms after leaving.

**Acceptance Criteria**:
- `leaveRoom` handler broadcasts LEAVE message
- `leaveRoom` handler does NOT delete membership record
- Membership record persists in database after leave
- Users can send messages immediately after returning to room
- No changes to LEAVE message broadcast
- Existing tests continue to pass

**Implementation Notes**:
- Remove `removeMember()` call from `leaveRoom` handler
- Keep LEAVE message broadcast
- Verify membership record remains in database
- Test with leave/rejoin sequences

### 2.4 Implement membership persistence - add explicit leave endpoint for permanent removal

**Description**: Create a new REST endpoint `/api/rooms/{roomId}/leave` that allows users to explicitly remove their membership. This distinguishes between temporary leave (navigation) and permanent leave (explicit exit).

**Acceptance Criteria**:
- New endpoint `/api/rooms/{roomId}/leave` is created
- Endpoint requires authentication
- Endpoint removes membership record from database
- Endpoint broadcasts LEAVE message to room members
- Endpoint returns 200 OK on success
- Endpoint returns 404 if room not found
- Endpoint returns 403 if user not a member
- Existing tests continue to pass

**Implementation Notes**:
- Create new controller method
- Use `@PostMapping` or `@DeleteMapping`
- Verify user is authenticated
- Verify user is member of room
- Delete membership record
- Broadcast LEAVE message
- Return appropriate HTTP status

### 2.5 Implement membership persistence - update frontend to use persistent membership

**Description**: Modify the frontend to NOT automatically call the leave handler when navigating away from a room. Only call the explicit leave endpoint when user explicitly requests it.

**Acceptance Criteria**:
- Navigation away from room does NOT trigger leave handler
- Membership persists across page navigation
- Users can send messages immediately after returning to room
- Explicit "Leave Room" button calls new leave endpoint
- Users can re-join rooms they previously left
- Existing tests continue to pass

**Implementation Notes**:
- Remove automatic leave on navigation
- Add explicit "Leave Room" button to UI
- Call new `/api/rooms/{roomId}/leave` endpoint
- Update room selector to show re-join option
- Test with navigation patterns

### 2.6 Implement WebSocket error handling - add connection status tracking

**Description**: Modify `frontend/lib/stomp/client.ts` to track WebSocket connection state (connected, disconnected, reconnecting) and notify the UI of connection changes through the connection store.

**Acceptance Criteria**:
- Connection status is tracked in `connectionStore`
- Connection status includes: connected, disconnected, reconnecting, error
- Connection status is updated on connection events
- Connection status is updated on disconnection events
- Connection status is updated on reconnection attempts
- UI components can subscribe to connection status changes
- Existing tests continue to pass

**Implementation Notes**:
- Add connection state to `connectionStore`
- Add event listeners for connection/disconnection
- Update store on status changes
- Expose connection status through store selectors
- Test with connection lifecycle events

### 2.7 Implement WebSocket error handling - add error handlers for WebSocket events

**Description**: Add error handlers to the STOMP client to catch connection loss, STOMP ERROR frames, and message send failures. Log errors and update connection status.

**Acceptance Criteria**:
- Error handler for connection loss is implemented
- Error handler for STOMP ERROR frames is implemented
- Error handler for message send failures is implemented
- Errors are logged with appropriate context
- Connection status is updated on error
- Error details are stored for display to user
- Existing tests continue to pass

**Implementation Notes**:
- Add `onError` callback to STOMP client
- Add `onStompError` callback for STOMP errors
- Add error handler to message send operations
- Log errors with userId, roomId, error code
- Update connection status to "error"
- Store error message for UI display

### 2.8 Implement WebSocket error handling - add automatic reconnection with exponential backoff

**Description**: Add automatic reconnection logic to the STOMP client that attempts to reconnect when connection is lost, using exponential backoff to avoid overwhelming the server.

**Acceptance Criteria**:
- Automatic reconnection is attempted on connection loss
- Reconnection uses exponential backoff (1s, 2s, 4s, 8s, max 60s)
- Maximum 10 reconnection attempts before giving up
- Reconnection attempts are logged
- Connection status is updated to "reconnecting" during attempts
- Connection status is updated to "connected" on successful reconnection
- User is notified of reconnection attempts
- Existing tests continue to pass

**Implementation Notes**:
- Implement exponential backoff algorithm
- Use setTimeout for retry scheduling
- Track retry count and max attempts
- Update connection status during retries
- Log reconnection attempts
- Test with simulated connection loss

### 2.9 Implement WebSocket error handling - add connection status UI component

**Description**: Create a new UI component to display connection status to the user, showing indicators for connected, disconnected, reconnecting, and error states.

**Acceptance Criteria**:
- Component displays connection status indicator
- Component shows "Connected" when connected
- Component shows "Disconnected" when disconnected
- Component shows "Reconnecting..." when reconnecting
- Component shows error message when error occurs
- Component is visible in chat layout
- Component updates in real-time as status changes
- Component is accessible (ARIA labels, semantic HTML)
- Existing tests continue to pass

**Implementation Notes**:
- Create new component `ConnectionStatus.tsx`
- Subscribe to connection store
- Display appropriate icon/text for each status
- Use color coding (green=connected, red=disconnected, yellow=reconnecting)
- Add ARIA labels for accessibility
- Test with different connection states

### 2.10 Implement WebSocket error handling - add message send error handling

**Description**: Add error handling to message send operations to catch failures, display errors to user, and optionally queue messages for retry.

**Acceptance Criteria**:
- Message send failures are caught
- Error messages are displayed to user
- User is informed when message cannot be sent
- Connection status is checked before send
- Error includes reason (disconnected, authorization, etc.)
- User can retry sending message
- Existing tests continue to pass

**Implementation Notes**:
- Add try/catch to message send operation
- Check connection status before send
- Display error toast/notification
- Include error reason in message
- Add retry button to error notification
- Test with various failure scenarios

### 2.11 Implement WebSocket error handling - add server-side error response handler

**Description**: Modify the backend to send error responses to the user's error queue when STOMP operations fail, including error code and message.

**Acceptance Criteria**:
- Error responses are sent to user's error queue
- Error responses include error code and message
- Error responses include context (roomId, userId, operation)
- Errors are logged on server
- Error responses are formatted consistently
- Existing tests continue to pass

**Implementation Notes**:
- Add error response handler in `ChatMessageController`
- Send errors to `/user/queue/errors`
- Include error code, message, and context
- Log errors with appropriate level
- Test with various error scenarios

---

## Phase 3: Fix Checking Tests

These tests verify that the fixes work correctly for all inputs where the bug condition holds.

### 3.1 Write fix checking test for race condition prevention

**Description**: Write a unit test that verifies the race condition fix works correctly. Test that rapid join/send sequences succeed without "not a member" errors.

**Acceptance Criteria**:
- Test joins room and immediately sends message
- Test verifies message is broadcast successfully
- Test verifies no "not a member" error is thrown
- Test verifies membership is correctly verified
- Test includes multiple rapid sequences
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Call join handler, then immediately call send handler
- Assert message is persisted and broadcast
- Assert no exceptions are thrown
- Test with various timing scenarios

### 3.2 Write fix checking test for membership persistence

**Description**: Write a unit test that verifies membership persists after leave and users can re-join. Test that users can send messages immediately after returning to room.

**Acceptance Criteria**:
- Test leaves room and verifies membership persists
- Test re-joins room and verifies membership is active
- Test sends message after re-join and verifies success
- Test verifies no "not a member" error is thrown
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Call leave handler
- Verify membership record still exists
- Call send handler
- Assert message is persisted and broadcast
- Assert no exceptions are thrown

### 3.3 Write fix checking test for WebSocket error handling

**Description**: Write a unit test that verifies connection status is tracked, errors are displayed, and reconnection is attempted.

**Acceptance Criteria**:
- Test simulates connection loss
- Test verifies connection status is updated to "disconnected"
- Test verifies error message is stored
- Test verifies reconnection is attempted
- Test verifies connection status is updated to "reconnecting"
- Test verifies connection status is updated to "connected" on success
- Test uses Jest test framework
- Test passes with fixed code

**Implementation Notes**:
- Mock STOMP client
- Simulate connection loss event
- Verify connection store is updated
- Verify error handler is called
- Verify reconnection logic is triggered
- Test with various connection scenarios

### 3.4 Write fix checking test for message send error handling

**Description**: Write a unit test that verifies message send errors are caught and displayed to user.

**Acceptance Criteria**:
- Test attempts to send message while disconnected
- Test verifies error is caught
- Test verifies error message is displayed
- Test verifies user is informed of failure
- Test uses Jest test framework
- Test passes with fixed code

**Implementation Notes**:
- Mock STOMP client
- Simulate disconnected state
- Attempt to send message
- Verify error handler is called
- Verify error message is stored
- Verify UI is updated with error

---

## Phase 4: Preservation Checking Tests

These tests verify that existing functionality is preserved for inputs where the bug condition does NOT hold.

### 4.1 Write preservation test for normal message sending

**Description**: Write a unit test that verifies normal message sending from authorized room members continues to work correctly after fixes.

**Acceptance Criteria**:
- Test sends message from authorized room member
- Test verifies message is persisted
- Test verifies message is broadcast to all subscribers
- Test verifies message includes correct sender, timestamp, content
- Test verifies no errors are thrown
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Add user to room membership
- Send message
- Assert message is persisted with correct data
- Assert message is broadcast
- Assert no exceptions are thrown

### 4.2 Write preservation test for unauthorized message sending

**Description**: Write a unit test that verifies unauthorized users still cannot send messages to rooms they are not members of.

**Acceptance Criteria**:
- Test attempts to send message from non-member user
- Test verifies message is rejected
- Test verifies authorization error is thrown
- Test verifies message is NOT persisted
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Do NOT add user to room membership
- Attempt to send message
- Assert authorization error is thrown
- Assert message is NOT persisted

### 4.3 Write preservation test for JOIN/LEAVE system messages

**Description**: Write a unit test that verifies JOIN and LEAVE system messages continue to be broadcast correctly.

**Acceptance Criteria**:
- Test joins room and verifies JOIN message is broadcast
- Test leaves room and verifies LEAVE message is broadcast
- Test verifies system messages are visible to other room members
- Test verifies system messages include correct user and timestamp
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test users and room
- Join room and verify JOIN message
- Leave room and verify LEAVE message
- Assert messages are broadcast to subscribers
- Assert messages include correct data

### 4.4 Write preservation test for message history retrieval

**Description**: Write a unit test that verifies message history retrieval continues to work correctly for authorized users.

**Acceptance Criteria**:
- Test retrieves message history for authorized user
- Test verifies all messages are returned
- Test verifies messages are in correct order
- Test verifies pagination works correctly
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Add user to room membership
- Send multiple messages
- Retrieve message history
- Assert all messages are returned
- Assert messages are in correct order
- Assert pagination works

### 4.5 Write preservation test for real-time message delivery latency

**Description**: Write a performance test that verifies message delivery latency remains under 100ms for authorized users.

**Acceptance Criteria**:
- Test measures message delivery latency
- Test verifies latency is under 100ms for 95th percentile
- Test sends multiple messages and measures timing
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Send message and measure delivery time
- Repeat multiple times
- Calculate 95th percentile latency
- Assert latency is under 100ms

### 4.6 Write preservation test for WebSocket connection establishment

**Description**: Write a unit test that verifies WebSocket connection establishment continues to work correctly for authenticated users.

**Acceptance Criteria**:
- Test establishes WebSocket connection
- Test verifies connection is successful
- Test verifies heartbeat is working
- Test verifies connection status is "connected"
- Test uses Jest test framework
- Test passes with fixed code

**Implementation Notes**:
- Mock STOMP client
- Simulate connection establishment
- Verify connection status is updated
- Verify heartbeat is configured
- Verify no errors are thrown

---

## Phase 5: Integration Tests

These tests verify that all fixes work together correctly in realistic scenarios.

### 5.1 Write integration test for race condition prevention with multiple users

**Description**: Write an integration test that simulates multiple users joining a room and sending messages rapidly, verifying that all messages are broadcast without errors.

**Acceptance Criteria**:
- Test creates multiple test users
- Test has all users join same room
- Test has all users send messages rapidly
- Test verifies all messages are broadcast
- Test verifies no "not a member" errors occur
- Test verifies message order is preserved
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create 5-10 test users
- Have all users join room
- Have all users send messages in rapid succession
- Verify all messages are persisted and broadcast
- Verify no exceptions are thrown
- Verify message order is correct

### 5.2 Write integration test for membership persistence with navigation

**Description**: Write an integration test that simulates user navigation patterns, verifying that membership persists and users can send messages after returning to room.

**Acceptance Criteria**:
- Test user joins room and sends message
- Test user navigates away from room
- Test user returns to room
- Test user sends message again
- Test verifies both messages are broadcast
- Test verifies no "not a member" errors occur
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Join room and send message
- Simulate navigation away (no leave call)
- Return to room
- Send message again
- Verify both messages are persisted and broadcast

### 5.3 Write integration test for WebSocket error handling with reconnection

**Description**: Write an integration test that simulates connection loss and reconnection, verifying that user is notified and messages resume delivery.

**Acceptance Criteria**:
- Test simulates connection loss
- Test verifies user is notified of disconnection
- Test verifies reconnection is attempted
- Test simulates successful reconnection
- Test verifies user is notified of reconnection
- Test verifies messages resume delivery
- Test uses Jest test framework
- Test passes with fixed code

**Implementation Notes**:
- Mock STOMP client
- Simulate connection loss
- Verify connection status is updated
- Verify error notification is displayed
- Simulate reconnection
- Verify connection status is updated
- Verify messages can be sent again

### 5.4 Write integration test for end-to-end chat flow with all fixes

**Description**: Write an end-to-end integration test that simulates a complete chat flow with all fixes: join, send, leave, rejoin, connection loss, reconnection.

**Acceptance Criteria**:
- Test user joins room
- Test user sends message
- Test user leaves room
- Test user re-joins room
- Test user sends message again
- Test connection loss occurs
- Test user is notified
- Test reconnection occurs
- Test user sends message after reconnection
- Test all messages are broadcast correctly
- Test uses Spring Boot test framework with embedded database
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Execute complete flow
- Verify all operations succeed
- Verify all messages are persisted and broadcast
- Verify connection status is updated correctly
- Verify no exceptions are thrown

---

## Summary

**Total Tasks**: 24

**Phase Breakdown**:
- Phase 1 (Exploratory Tests): 3 tasks
- Phase 2 (Implementation): 11 tasks
- Phase 3 (Fix Checking): 4 tasks
- Phase 4 (Preservation): 6 tasks
- Phase 5 (Integration): 4 tasks

**Key Files Modified**:
- Backend: `src/main/java/org/example/chat/controller/ChatMessageController.java`
- Backend: `src/main/java/org/example/chat/service/ChatRoomService.java` (new endpoint)
- Frontend: `frontend/lib/stomp/client.ts`
- Frontend: `frontend/lib/store/connectionStore.ts`
- Frontend: `frontend/components/chat/ConnectionStatus.tsx` (new component)

**Testing Frameworks**:
- Backend: JUnit 5 with jqwik for property-based tests
- Frontend: Jest with fast-check for property-based tests
- Integration: Spring Boot test framework with embedded database
