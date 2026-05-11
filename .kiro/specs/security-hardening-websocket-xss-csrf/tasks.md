# Tasks: Security Hardening - WebSocket Authorization, XSS Protection, and CSRF Defense

## Overview

This bugfix addresses three critical security vulnerabilities in the real-time chat system:

1. **Missing WebSocket Authorization** - Any authenticated user can send messages to ANY room without permission checks
2. **XSS (Cross-Site Scripting) Vulnerability** - Messages aren't sanitized, allowing script injection attacks
3. **CSRF (Cross-Site Request Forgery) Protection** - Application lacks CSRF tokens, allowing attackers to perform actions on behalf of users

The task structure follows the bugfix workflow: exploratory tests → implementation → fix verification → preservation verification → integration tests.

---

## Phase 1: Exploratory Bug Condition Tests

These tests are designed to FAIL on unfixed code and PASS on fixed code. They surface counterexamples that demonstrate each vulnerability exists.

### 1.1 Write bug condition exploration property test for WebSocket authorization bypass

**Description**: Write a property-based test that generates WebSocket messages from users attempting to send to rooms they are NOT members of. This test should fail on unfixed code (demonstrating the authorization bypass exists) and pass on fixed code.

**Acceptance Criteria**:
- Test generates random users and rooms where user is NOT a member
- Test attempts to send STOMP message to room via `/app/chat.send/{roomId}`
- Test verifies that message is broadcast to room subscribers (BUG on unfixed code)
- Test verifies that no authorization error is thrown (BUG on unfixed code)
- Test includes at least 3 scenarios: user never joined, user left room, user joined different room
- Test uses property-based testing framework (JUnit 5 with jqwik)
- Test fails on unfixed code with counterexample showing unauthorized message broadcast
- Test passes on fixed code

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

**Implementation Notes**:
- Use jqwik `@Property` annotation
- Generate userId, roomId pairs where user is not a member
- Simulate STOMP message via `StompHeaderAccessor` and `ChatMessageRequest`
- Assert that `messagingTemplate.convertAndSend()` is called (BUG on unfixed code)
- Assert that no `UnauthorizedException` is thrown (BUG on unfixed code)
- Capture failing counterexamples showing unauthorized broadcasts

### 1.2 Write bug condition exploration property test for XSS vulnerability

**Description**: Write a property-based test that generates messages with malicious HTML/JavaScript payloads and verifies they are stored and displayed without sanitization. This test should fail on unfixed code (demonstrating XSS vulnerability exists) and pass on fixed code.

**Acceptance Criteria**:
- Test generates random messages with dangerous patterns: `<script>`, `<img onerror=`, `<svg onload=`, `javascript:`, event handlers
- Test sends message via STOMP and verifies it is persisted
- Test retrieves message from database and verifies content is NOT escaped (BUG on unfixed code)
- Test verifies frontend receives raw content without escaping (BUG on unfixed code)
- Test includes at least 5 different XSS payload types
- Test uses property-based testing framework (JUnit 5 with jqwik)
- Test fails on unfixed code with counterexample showing unescaped malicious content
- Test passes on fixed code

**Validates: Requirements 2.5, 2.6, 2.7, 2.8**

**Implementation Notes**:
- Use jqwik `@Property` annotation with custom generators for XSS payloads
- Generate payloads: `<script>alert('xss')</script>`, `<img src=x onerror="alert('xss')">`, `<svg onload="fetch('http://attacker.com')">`, etc.
- Send message via `chatMessageService.sendMessage()`
- Retrieve message from `messageRepository`
- Assert that `message.getContent()` contains raw HTML (BUG on unfixed code)
- Assert that no HTML entity escaping occurred (BUG on unfixed code)
- Capture failing counterexamples showing unescaped payloads

### 1.3 Write bug condition exploration property test for CSRF vulnerability

**Description**: Write a property-based test that generates state-changing HTTP requests (POST, PUT, DELETE) without CSRF tokens and verifies they are processed. This test should fail on unfixed code (demonstrating CSRF vulnerability exists) and pass on fixed code.

**Acceptance Criteria**:
- Test generates random state-changing requests: POST to `/api/rooms`, PUT to `/api/rooms/{id}`, DELETE to `/api/rooms/{id}`
- Test sends requests WITHOUT CSRF token in headers (BUG on unfixed code)
- Test verifies requests are processed successfully (BUG on unfixed code)
- Test verifies HTTP 200/201 response is returned (BUG on unfixed code)
- Test verifies state is actually changed in database (BUG on unfixed code)
- Test includes at least 3 different endpoint types
- Test uses property-based testing framework (JUnit 5 with jqwik)
- Test fails on unfixed code with counterexample showing request processed without token
- Test passes on fixed code

**Validates: Requirements 2.9, 2.10, 2.11, 2.12**

**Implementation Notes**:
- Use jqwik `@Property` annotation
- Generate random room names, descriptions, member lists
- Use `MockMvc` to send HTTP requests
- Send requests WITHOUT `X-CSRF-TOKEN` header
- Assert that `mockMvc.perform()` returns status 200/201 (BUG on unfixed code)
- Assert that database state is changed (BUG on unfixed code)
- Capture failing counterexamples showing successful requests without tokens

---

## Phase 2: Implementation Tasks

### 2.1 Implement WebSocket authorization - add per-message authorization check in controller

**Description**: Modify `ChatMessageController.sendMessage()` to verify user is a member of the target room BEFORE processing the message. Throw `UnauthorizedException` if not a member.

**Acceptance Criteria**:
- `sendMessage()` method verifies user is a member of target room
- Authorization check happens BEFORE message is persisted or broadcast
- `UnauthorizedException` is thrown if user is not a member
- Error message is sent to user's error queue
- Authorization failure is logged for security auditing
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Add authorization check at start of `sendMessage()` method
- Use `roomMembershipRepository.existsByUserAndChatRoom(user, roomId)`
- Throw `UnauthorizedException` with descriptive message
- Log authorization failure with userId, roomId, timestamp
- Send error response to `/user/queue/errors`

### 2.2 Implement WebSocket authorization - add per-message authorization check in service layer

**Description**: Add authorization check in `ChatMessageService.sendMessage()` as defense-in-depth. Verify user is a member before persisting message.

**Acceptance Criteria**:
- `sendMessage()` service method verifies user is a member of target room
- Authorization check happens BEFORE message is persisted
- `UnauthorizedException` is thrown if user is not a member
- Check is independent of controller check (defense-in-depth)
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Add authorization check in service layer
- Use `roomMembershipRepository.existsByUserAndChatRoom(user, room)`
- Throw `UnauthorizedException` with descriptive message
- This prevents bypassing controller checks

### 2.3 Implement WebSocket authorization - add exception handler for authorization failures

**Description**: Add `@MessageExceptionHandler` in `ChatMessageController` to catch `UnauthorizedException` and send error response to user's error queue.

**Acceptance Criteria**:
- Exception handler catches `UnauthorizedException`
- Error response is sent to user's `/queue/errors` destination
- Error response includes error code and descriptive message
- Error response includes context (roomId, userId, operation)
- Error is logged on server
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Create `@MessageExceptionHandler` method
- Send error to `messagingTemplate.convertAndSendToUser()`
- Include error code: "UNAUTHORIZED"
- Include message: "You are not a member of this room"
- Log error with appropriate level

### 2.4 Implement WebSocket authorization - add security audit logging

**Description**: Create `SecurityAuditLogger` component to log all authorization failures with user ID, room ID, timestamp, and reason.

**Acceptance Criteria**:
- `SecurityAuditLogger` component is created
- Authorization failures are logged with userId, roomId, timestamp, reason
- Logs are written to separate audit log file
- Logs include sufficient context for security investigation
- Logs are formatted consistently
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Create `@Component` class `SecurityAuditLogger`
- Use separate logger instance for audit logs
- Log format: `AUTHORIZATION_FAILURE: userId={}, roomId={}, reason={}, timestamp={}`
- Include in all authorization failure paths

### 2.5 Implement XSS protection - create HTML sanitization utility

**Description**: Create `HtmlSanitizer` utility component that escapes HTML entities and removes dangerous patterns from message content.

**Acceptance Criteria**:
- `HtmlSanitizer` component is created
- `sanitize()` method escapes HTML entities (e.g., `<` → `&lt;`)
- `sanitize()` method removes dangerous patterns: `<script>`, `<iframe>`, `<object>`, `<embed>`, event handlers
- `containsDangerousPatterns()` method detects malicious content
- Sanitization is idempotent (sanitizing twice = sanitizing once)
- Legitimate content is preserved (special characters, unicode, emoji)
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Use `HtmlUtils.htmlEscape()` from Spring Framework
- Use regex pattern to detect dangerous patterns
- Pattern: `<script|<iframe|<object|<embed|on\w+\s*=|javascript:`
- Test with various XSS payloads and legitimate content

### 2.6 Implement XSS protection - add sanitization to message service

**Description**: Modify `ChatMessageService.sendMessage()` to sanitize message content BEFORE persisting to database.

**Acceptance Criteria**:
- Message content is sanitized before persistence
- Sanitized content is stored in database
- Sanitized content is broadcast to room subscribers
- Original content is NOT stored
- Legitimate content is preserved
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Inject `HtmlSanitizer` into service
- Call `htmlSanitizer.sanitize(content)` before creating `Message` entity
- Store sanitized content in database
- Broadcast sanitized content to subscribers

### 2.7 Implement XSS protection - add detection and logging of XSS attempts

**Description**: Add detection of XSS attempts in `ChatMessageService.sendMessage()` and log them for security auditing.

**Acceptance Criteria**:
- XSS attempts are detected using `HtmlSanitizer.containsDangerousPatterns()`
- XSS attempts are logged with userId, roomId, content, timestamp
- Logs are written to security audit log
- Logs include sufficient context for investigation
- Message is still sanitized and processed normally
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Check `htmlSanitizer.containsDangerousPatterns(content)` before sanitization
- Log XSS attempt with `securityAuditLogger.logXssAttempt()`
- Include original content in logs (for investigation)
- Continue processing message normally (sanitized)

### 2.8 Implement XSS protection - add frontend sanitization utility

**Description**: Create `sanitize.ts` utility in frontend to escape HTML content for display and detect dangerous patterns.

**Acceptance Criteria**:
- `sanitizeHtml()` function escapes HTML entities
- `isSafeContent()` function detects dangerous patterns
- Sanitization uses browser's built-in HTML parsing
- Legitimate content is preserved
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Create `frontend/lib/utils/sanitize.ts`
- Use `document.createElement('div')` and `textContent` for escaping
- Use regex pattern to detect dangerous patterns
- Export functions for use in components

### 2.9 Implement XSS protection - update message display component to use safe rendering

**Description**: Modify `MessageList.tsx` component to render message content as plain text without HTML interpretation.

**Acceptance Criteria**:
- Message content is rendered as plain text
- HTML is NOT rendered (no `dangerouslySetInnerHTML`)
- Content is properly escaped by React
- Special characters and emoji are preserved
- Message layout and styling are unchanged
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Use React's default text rendering (automatic escaping)
- Render message content in `<p>` or `<div>` tag
- Never use `dangerouslySetInnerHTML`
- Verify content is escaped in browser DevTools

### 2.10 Implement CSRF protection - configure Spring Security CSRF protection

**Description**: Configure Spring Security to enable CSRF protection for REST endpoints, excluding WebSocket endpoints (which use JWT).

**Acceptance Criteria**:
- CSRF protection is enabled in `SecurityConfig`
- CSRF token repository is configured
- CSRF token header name is set to `X-CSRF-TOKEN`
- WebSocket endpoints are excluded from CSRF (they use JWT)
- REST endpoints require CSRF token for state-changing requests
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Modify `SecurityConfig.filterChain()` method
- Use `CookieCsrfTokenRepository.withHttpOnlyFalse()`
- Set header name: `X-CSRF-TOKEN`
- Exclude `/ws/**` endpoints
- Configure `CsrfTokenRequestAttributeHandler`

### 2.11 Implement CSRF protection - add CSRF token generation on login

**Description**: Modify `AuthController.login()` to generate CSRF token and include it in login response.

**Acceptance Criteria**:
- CSRF token is generated when user logs in
- CSRF token is included in `LoginResponse`
- CSRF token is cryptographically secure
- CSRF token is unique per session
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Get `CsrfToken` from `HttpServletRequest`
- Include token in `LoginResponse` DTO
- Token is automatically generated by Spring Security
- Frontend stores token for subsequent requests

### 2.12 Implement CSRF protection - update login response DTO to include CSRF token

**Description**: Add `csrfToken` field to `LoginResponse` DTO to include CSRF token in login response.

**Acceptance Criteria**:
- `LoginResponse` includes `csrfToken` field
- CSRF token is populated from Spring Security
- CSRF token is serialized in JSON response
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Add `private String csrfToken;` field to `LoginResponse`
- Add getter/setter methods
- Populate in `AuthController.login()`
- Verify in JSON response

### 2.13 Implement CSRF protection - store CSRF token in frontend auth store

**Description**: Modify `useAuthStore` to store CSRF token from login response and make it available to API calls.

**Acceptance Criteria**:
- `AuthState` includes `csrfToken` field
- CSRF token is stored from login response
- CSRF token is available via store selector
- CSRF token is cleared on logout
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Add `csrfToken: string | null` to `AuthState`
- Store token in `login()` action
- Clear token in `logout()` action
- Export selector for accessing token

### 2.14 Implement CSRF protection - add CSRF token to API client requests

**Description**: Modify `apiCall()` function in `frontend/lib/api/client.ts` to include CSRF token in headers for state-changing requests.

**Acceptance Criteria**:
- CSRF token is included in `X-CSRF-TOKEN` header for POST, PUT, DELETE requests
- CSRF token is NOT included for GET requests
- CSRF token is retrieved from auth store
- CSRF token is included in all state-changing API calls
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Check request method in `apiCall()`
- For POST, PUT, DELETE: add `X-CSRF-TOKEN` header
- Get token from `useAuthStore.getState().csrfToken`
- Include in all state-changing requests

### 2.15 Implement CSRF protection - add CSRF token to form submissions

**Description**: Modify form components (e.g., `RoomCreateModal.tsx`) to include CSRF token in form submissions.

**Acceptance Criteria**:
- CSRF token is included in `X-CSRF-TOKEN` header for form submissions
- CSRF token is retrieved from auth store
- CSRF token is included in all state-changing form submissions
- No changes to method signature or public API
- Existing tests continue to pass

**Implementation Notes**:
- Get CSRF token from `useAuthStore`
- Add to headers in `fetch()` call
- Include for POST, PUT, DELETE requests
- Test with room creation, member management, etc.

---

## Phase 3: Fix Checking Tests

These tests verify that the fixes work correctly for all inputs where the bug condition holds.

### 3.1 Write fix checking test for WebSocket authorization enforcement

**Description**: Write a unit test that verifies unauthorized users are rejected when attempting to send messages to rooms they are not members of.

**Acceptance Criteria**:
- Test creates user and room
- Test does NOT add user to room membership
- Test attempts to send message via STOMP
- Test verifies `UnauthorizedException` is thrown
- Test verifies message is NOT persisted
- Test verifies message is NOT broadcast
- Test verifies error response is sent to user's error queue
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Do NOT call `roomMembershipRepository.save()`
- Attempt to send message via `messagingTemplate.convertAndSend()`
- Assert `UnauthorizedException` is thrown
- Assert message is NOT in database
- Verify error response in user's error queue

### 3.2 Write fix checking test for XSS sanitization on persistence

**Description**: Write a unit test that verifies message content is sanitized before persistence.

**Acceptance Criteria**:
- Test sends message with XSS payload: `<script>alert('xss')</script>`
- Test verifies message is persisted
- Test verifies persisted content is escaped: `&lt;script&gt;alert('xss')&lt;/script&gt;`
- Test verifies original payload is NOT in database
- Test verifies no scripts execute
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Add user to room membership
- Send message with XSS payload
- Retrieve message from database
- Assert content is escaped
- Assert payload is not present

### 3.3 Write fix checking test for CSRF token validation on state-changing requests

**Description**: Write a unit test that verifies state-changing requests without CSRF token are rejected.

**Acceptance Criteria**:
- Test sends POST request to `/api/rooms` WITHOUT CSRF token
- Test verifies HTTP 403 Forbidden response
- Test verifies room is NOT created
- Test verifies error message is returned
- Test uses Spring Boot test framework with MockMvc
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with `@AutoConfigureMockMvc`
- Send POST request without `X-CSRF-TOKEN` header
- Assert `mockMvc.perform()` returns status 403
- Assert room is NOT in database
- Verify error response

### 3.4 Write fix checking test for CSRF token validation with valid token

**Description**: Write a unit test that verifies state-changing requests WITH valid CSRF token are accepted.

**Acceptance Criteria**:
- Test sends POST request to `/api/rooms` WITH valid CSRF token
- Test verifies HTTP 200/201 response
- Test verifies room is created
- Test verifies state is changed in database
- Test uses Spring Boot test framework with MockMvc
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with `@AutoConfigureMockMvc`
- Get CSRF token from Spring Security
- Send POST request with `X-CSRF-TOKEN` header
- Assert `mockMvc.perform()` returns status 200/201
- Assert room is in database
- Verify state is changed

---

## Phase 4: Preservation Checking Tests

These tests verify that existing functionality is preserved for inputs where the bug condition does NOT hold.

### 4.1 Write preservation test for authorized message sending

**Description**: Write a unit test that verifies authorized room members can send messages normally after fixes.

**Acceptance Criteria**:
- Test creates user and room
- Test adds user to room membership
- Test sends message with legitimate content
- Test verifies message is persisted
- Test verifies message is broadcast to subscribers
- Test verifies message includes correct sender, timestamp, content
- Test verifies no errors are thrown
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Add user to room membership
- Send message with legitimate content
- Assert message is persisted with correct data
- Assert message is broadcast
- Assert no exceptions are thrown

### 4.2 Write preservation test for legitimate HTML content

**Description**: Write a unit test that verifies legitimate content (special characters, emoji, unicode) is preserved after sanitization.

**Acceptance Criteria**:
- Test sends message with special characters: `Hello <world> & friends! 🎉`
- Test verifies message is persisted
- Test verifies content is preserved exactly (with HTML escaping)
- Test verifies special characters are not removed
- Test verifies emoji and unicode are preserved
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Send message with special characters and emoji
- Retrieve message from database
- Assert content is preserved (with HTML escaping)
- Assert special characters are present
- Assert emoji and unicode are present

### 4.3 Write preservation test for authorized API requests with CSRF token

**Description**: Write a unit test that verifies authorized API requests with valid CSRF token continue to work normally.

**Acceptance Criteria**:
- Test sends POST request to `/api/rooms` WITH valid CSRF token
- Test verifies HTTP 200/201 response
- Test verifies room is created
- Test verifies state is changed in database
- Test verifies no additional latency is introduced
- Test uses Spring Boot test framework with MockMvc
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with `@AutoConfigureMockMvc`
- Get CSRF token from Spring Security
- Send POST request with `X-CSRF-TOKEN` header
- Assert `mockMvc.perform()` returns status 200/201
- Assert room is in database
- Verify state is changed

### 4.4 Write preservation test for WebSocket connection establishment

**Description**: Write a unit test that verifies WebSocket connection establishment continues to work for authenticated users.

**Acceptance Criteria**:
- Test establishes WebSocket connection with valid JWT token
- Test verifies connection is successful
- Test verifies user is authenticated
- Test verifies connection status is "connected"
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with WebSocket support
- Create test user and generate JWT token
- Establish WebSocket connection
- Verify connection is successful
- Verify user is authenticated

### 4.5 Write preservation test for message history retrieval

**Description**: Write a unit test that verifies message history retrieval continues to work for authorized users.

**Acceptance Criteria**:
- Test creates multiple messages in room
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

### 4.6 Write preservation test for JOIN/LEAVE system messages

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

---

## Phase 5: Integration Tests

These tests verify that all fixes work together correctly in realistic scenarios.

### 5.1 Write integration test for WebSocket authorization with multiple users

**Description**: Write an integration test that simulates multiple users attempting to send messages to rooms they are not members of, verifying all unauthorized attempts are rejected.

**Acceptance Criteria**:
- Test creates multiple test users and rooms
- Test has User A attempt to send to Room B (not a member)
- Test has User B attempt to send to Room A (not a member)
- Test verifies all unauthorized attempts are rejected
- Test verifies no messages are broadcast
- Test verifies error responses are sent to users
- Test verifies authorization failures are logged
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create 3-5 test users and 3-5 test rooms
- Have users attempt to send to rooms they're not members of
- Verify all attempts are rejected
- Verify no messages are persisted or broadcast
- Verify error responses are sent

### 5.2 Write integration test for XSS protection with various payloads

**Description**: Write an integration test that sends various XSS payloads and verifies they are all sanitized before persistence and display.

**Acceptance Criteria**:
- Test sends multiple XSS payloads: `<script>`, `<img onerror=`, `<svg onload=`, `javascript:`, event handlers
- Test verifies all payloads are sanitized before persistence
- Test verifies sanitized content is broadcast to subscribers
- Test verifies no scripts execute in browsers
- Test verifies legitimate content is preserved
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and room
- Send multiple XSS payloads
- Retrieve messages from database
- Verify all payloads are escaped
- Verify no dangerous patterns remain

### 5.3 Write integration test for CSRF protection with multiple endpoints

**Description**: Write an integration test that verifies CSRF protection is enforced across all state-changing endpoints.

**Acceptance Criteria**:
- Test sends POST, PUT, DELETE requests to various endpoints
- Test verifies requests WITHOUT CSRF token are rejected (403)
- Test verifies requests WITH valid CSRF token are accepted (200/201)
- Test verifies state is NOT changed for rejected requests
- Test verifies state IS changed for accepted requests
- Test uses Spring Boot test framework with MockMvc
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with `@AutoConfigureMockMvc`
- Test multiple endpoints: `/api/rooms`, `/api/rooms/{id}`, `/api/rooms/{id}/members`
- Send requests without CSRF token
- Verify 403 response
- Send requests with valid CSRF token
- Verify 200/201 response

### 5.4 Write integration test for end-to-end security flow

**Description**: Write an end-to-end integration test that simulates a complete secure chat flow: login, CSRF token retrieval, authorized message sending, unauthorized message rejection, XSS sanitization.

**Acceptance Criteria**:
- Test user logs in and receives CSRF token
- Test user joins room and sends authorized message
- Test user attempts to send to unauthorized room (rejected)
- Test user sends message with XSS payload (sanitized)
- Test verifies all security checks are enforced
- Test verifies authorized operations succeed
- Test verifies unauthorized operations fail
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Create test user and rooms
- Login and retrieve CSRF token
- Join room and send authorized message
- Attempt to send to unauthorized room
- Send message with XSS payload
- Verify all operations behave correctly

### 5.5 Write integration test for security audit logging

**Description**: Write an integration test that verifies all security events are logged correctly for audit trail.

**Acceptance Criteria**:
- Test triggers authorization failures and verifies they are logged
- Test triggers XSS attempts and verifies they are logged
- Test triggers CSRF failures and verifies they are logged
- Test verifies logs include userId, roomId, timestamp, reason
- Test verifies logs are written to audit log file
- Test uses Spring Boot test framework
- Test passes with fixed code

**Implementation Notes**:
- Use `@SpringBootTest` with embedded database
- Trigger various security events
- Verify logs are written to audit log
- Verify logs include sufficient context
- Verify logs are formatted consistently

---

## Summary

**Total Tasks**: 30

**Phase Breakdown**:
- Phase 1 (Exploratory Tests): 3 tasks
- Phase 2 (Implementation): 15 tasks
- Phase 3 (Fix Checking): 4 tasks
- Phase 4 (Preservation): 6 tasks
- Phase 5 (Integration): 5 tasks

**Key Files Modified/Created**:

**Backend**:
- `src/main/java/org/example/chat/controller/ChatMessageController.java` (modify)
- `src/main/java/org/example/chat/service/ChatMessageService.java` (modify)
- `src/main/java/org/example/chat/config/SecurityConfig.java` (modify)
- `src/main/java/org/example/chat/controller/AuthController.java` (modify)
- `src/main/java/org/example/chat/dto/LoginResponse.java` (modify)
- `src/main/java/org/example/chat/util/HtmlSanitizer.java` (create)
- `src/main/java/org/example/chat/util/SecurityAuditLogger.java` (create)
- `src/main/java/org/example/chat/exception/UnauthorizedException.java` (create if not exists)

**Frontend**:
- `frontend/lib/api/client.ts` (modify)
- `frontend/lib/store/authStore.ts` (modify)
- `frontend/components/chat/MessageList.tsx` (modify)
- `frontend/lib/utils/sanitize.ts` (create)

**Testing Frameworks**:
- Backend: JUnit 5 with jqwik for property-based tests
- Frontend: Jest with fast-check for property-based tests
- Integration: Spring Boot test framework with embedded database and MockMvc

**Security Considerations**:
- All authorization checks are enforced at both controller and service layers (defense-in-depth)
- All dangerous HTML patterns are escaped before persistence
- CSRF tokens are cryptographically secure and unique per session
- All security events are logged for audit trail
- No sensitive data is logged (passwords, tokens, etc.)
