# Implementation Plan

## Phase 1: Exploration Tests (BEFORE Fix)

- [x] 1. Write bug condition exploration tests
  - **Property 1: Bug Condition** - CI Phase 2 Test Failures
  - **CRITICAL**: These tests MUST FAIL on unfixed code - failure confirms the bugs exist
  - **DO NOT attempt to fix the tests or the code when they fail**
  - **NOTE**: These tests encode the expected behavior - they will validate the fixes when they pass after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bugs exist
  - Run the existing integration test suite on UNFIXED code to observe failures
  - The tests are already written in `src/test/java/org/example/chat/integration/`
  - Execute: `mvn test -Dtest=AuthenticationIntegrationTest,MessageHistoryIntegrationTest,ChatRoomIntegrationTest`
  - **EXPECTED OUTCOME**: 15 tests FAIL (this is correct - it proves the bugs exist)
  - Document counterexamples found:
    - Authentication: Unauthenticated requests return 403 instead of 401
    - Pagination: Message history returns `{content: [...], pageable: {...}}` instead of `[...]`
    - NullPointer: Null createdBy causes NullPointerException
    - Not Found: Nonexistent rooms return 400 instead of 404
    - Authorization: Non-member access returns 400/500 instead of 403
  - Mark task complete when tests are run and failures are documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 1.11, 1.12, 1.13, 1.14, 1.15, 1.16_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-Buggy Behavior Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-buggy inputs:
    - Authenticated requests to protected endpoints work correctly
    - Public endpoints (`/api/auth/register`, `/api/auth/login`) work without authentication
    - Message history returns correct content, ordering, and metadata for authenticated members
    - Room creation adds creator as OWNER
    - Member retrieval returns correct member lists
    - Validation errors return 400 with appropriate messages
  - Write property-based tests capturing observed behavior patterns from Preservation Requirements
  - Property-based testing generates many test cases for stronger guarantees
  - Create test file: `src/test/java/org/example/chat/integration/PreservationPropertyTest.java`
  - Test cases:
    - Authenticated requests to all endpoints continue to work
    - Public endpoints remain accessible without authentication
    - Message history content and ordering unchanged
    - Room creation behavior unchanged
    - Member retrieval unchanged
    - Validation error responses unchanged
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 3.11, 3.12, 3.13, 3.14_

## Phase 2: Implementation

- [x] 3. Fix Category 1: Authentication Status Code (401 instead of 403)

  - [x] 3.1 Implement custom AuthenticationEntryPoint in SecurityConfig
    - Create custom `AuthenticationEntryPoint` bean that returns 401 Unauthorized
    - Configure SecurityFilterChain to use the custom entry point
    - File: `src/main/java/org/example/chat/security/SecurityConfig.java`
    - Add bean method:
      ```java
      @Bean
      public AuthenticationEntryPoint authenticationEntryPoint() {
          return (request, response, authException) -> {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setContentType("application/json");
              response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
          };
      }
      ```
    - Modify securityFilterChain to add:
      ```java
      .exceptionHandling(exception -> exception
          .authenticationEntryPoint(authenticationEntryPoint())
      )
      ```
    - _Bug_Condition: isBugCondition(input) where input.endpoint IN ['/api/rooms/{id}/messages', '/api/rooms', '/api/users/me'] AND input.authHeader IS NULL_
    - _Expected_Behavior: result.statusCode == 401 for unauthenticated requests to protected endpoints_
    - _Preservation: Authenticated requests continue to work unchanged (Requirements 3.1, 3.2)_
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 3.2 Verify authentication tests now pass
    - **Property 1: Expected Behavior** - Authentication Returns 401
    - **IMPORTANT**: Re-run the SAME tests from task 1 - do NOT write new tests
    - Run: `mvn test -Dtest=AuthenticationIntegrationTest`
    - **EXPECTED OUTCOME**: Authentication tests PASS (confirms 401 status code fix)
    - Verify tests that previously failed with 403 now pass with 401
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 3.3 Verify preservation tests still pass
    - **Property 2: Preservation** - Authenticated Requests Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run: `mvn test -Dtest=PreservationPropertyTest`
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm authenticated requests still work correctly

- [x] 4. Fix Category 2: Pagination Response Structure (Array instead of Page)

  - [x] 4.1 Change MessageHistoryController return type to List
    - Change return type from `ResponseEntity<Page<MessageResponse>>` to `ResponseEntity<List<MessageResponse>>`
    - Convert Page to List using `page.getContent()`
    - File: `src/main/java/org/example/chat/controller/MessageHistoryController.java`
    - Update method signature:
      ```java
      @GetMapping("/{roomId}/messages")
      public ResponseEntity<List<MessageResponse>> getMessageHistory(...)
      ```
    - Update return statement:
      ```java
      Page<Message> messages = chatMessageService.getMessageHistory(roomId, pageable);
      List<MessageResponse> response = messages.map(MessageResponse::from).getContent();
      return ResponseEntity.ok(response);
      ```
    - _Bug_Condition: isBugCondition(input) where input.endpoint == '/api/rooms/{id}/messages' AND input.authenticated == true AND input.isMember == true_
    - _Expected_Behavior: result.body IS Array AND NOT EXISTS result.body.content AND NOT EXISTS result.body.pageable_
    - _Preservation: Message content, ordering, and metadata unchanged; pagination parameters still respected (Requirements 3.3, 3.4, 3.5)_
    - _Requirements: 2.5, 2.6, 2.7, 2.8, 2.9_

  - [x] 4.2 Verify pagination tests now pass
    - **Property 1: Expected Behavior** - Message History Returns Array
    - **IMPORTANT**: Re-run the SAME tests from task 1 - do NOT write new tests
    - Run: `mvn test -Dtest=MessageHistoryIntegrationTest`
    - **EXPECTED OUTCOME**: Pagination tests PASS (confirms array structure)
    - Verify tests that previously failed with paginated objects now pass with arrays
    - _Requirements: 2.5, 2.6, 2.7, 2.8, 2.9_

  - [x] 4.3 Verify preservation tests still pass
    - **Property 2: Preservation** - Message History Functionality Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run: `mvn test -Dtest=PreservationPropertyTest`
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm message content, ordering, and pagination parameters still work

- [ ] 5. Fix Category 3: NullPointerException in ChatRoomResponse

  - [x] 5.1 Add null check in ChatRoomResponse.from()
    - Add null check before calling `UserResponse.from()`
    - Return null for createdBy if the user is null
    - File: `src/main/java/org/example/chat/dto/ChatRoomResponse.java`
    - Update factory method:
      ```java
      public static ChatRoomResponse from(ChatRoom chatRoom) {
          UserResponse createdByResponse = chatRoom.getCreatedBy() != null 
              ? UserResponse.from(chatRoom.getCreatedBy()) 
              : null;
          
          return new ChatRoomResponse(
              chatRoom.getId(),
              chatRoom.getName(),
              chatRoom.getDescription(),
              chatRoom.getCreatedAt(),
              createdByResponse
          );
      }
      ```
    - _Bug_Condition: isBugCondition(input) where EXISTS room WHERE room.id == input.roomId AND room.createdBy IS NULL_
    - _Expected_Behavior: result.statusCode != 500 AND (result.body.createdBy IS NULL OR result.body.createdBy IS defined)_
    - _Preservation: Room retrieval for non-null createdBy unchanged (Requirements 3.7, 3.8)_
    - _Requirements: 2.10, 2.11, 2.12_

  - [x] 5.2 Verify null createdBy tests now pass
    - **Property 1: Expected Behavior** - Null createdBy Handled Gracefully
    - **IMPORTANT**: Re-run the SAME tests from task 1 - do NOT write new tests
    - Run: `mvn test -Dtest=ChatRoomIntegrationTest`
    - **EXPECTED OUTCOME**: NullPointerException tests PASS (confirms null handling)
    - Verify tests that previously failed with NPE now pass
    - _Requirements: 2.10, 2.11, 2.12_

  - [x] 5.3 Verify preservation tests still pass
    - **Property 2: Preservation** - Room Retrieval Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run: `mvn test -Dtest=PreservationPropertyTest`
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm room retrieval for non-null createdBy still works

- [ ] 6. Fix Category 4: Resource Not Found Status Code (404 instead of 400)

  - [x] 6.1 Replace IllegalArgumentException with RoomNotFoundException
    - Replace `IllegalArgumentException` with `RoomNotFoundException` when room is not found
    - Update MessageHistoryController, ChatRoomController, and ChatRoomService
    - Files:
      - `src/main/java/org/example/chat/controller/MessageHistoryController.java`
      - `src/main/java/org/example/chat/controller/ChatRoomController.java`
      - `src/main/java/org/example/chat/service/ChatRoomService.java`
    - In MessageHistoryController:
      ```java
      ChatRoom chatRoom = chatRoomRepository.findById(roomId)
          .orElseThrow(() -> new RoomNotFoundException(roomId));
      ```
    - In ChatRoomService.getRoomById():
      ```java
      public ChatRoom getRoomById(Long roomId) {
          logger.debug("Retrieving chat room by ID: {}", roomId);
          return chatRoomRepository.findById(roomId)
              .orElseThrow(() -> new RoomNotFoundException(roomId));
      }
      ```
    - Add import: `import org.example.chat.exception.RoomNotFoundException;`
    - _Bug_Condition: isBugCondition(input) where input.endpoint IN ['/api/rooms/{id}/messages', '/api/rooms/{id}'] AND NOT EXISTS room WHERE room.id == input.roomId_
    - _Expected_Behavior: result.statusCode == 404 for nonexistent rooms_
    - _Preservation: Existing room retrieval unchanged (Requirements 3.7, 3.8)_
    - _Requirements: 2.13, 2.14_

  - [x] 6.2 Verify resource not found tests now pass
    - **Property 1: Expected Behavior** - Nonexistent Resources Return 404
    - **IMPORTANT**: Re-run the SAME tests from task 1 - do NOT write new tests
    - Run: `mvn test -Dtest=ChatRoomIntegrationTest,MessageHistoryIntegrationTest`
    - **EXPECTED OUTCOME**: Not found tests PASS (confirms 404 status code)
    - Verify tests that previously failed with 400 now pass with 404
    - _Requirements: 2.13, 2.14_

  - [x] 6.3 Verify preservation tests still pass
    - **Property 2: Preservation** - Existing Room Retrieval Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run: `mvn test -Dtest=PreservationPropertyTest`
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm existing room retrieval still works correctly

- [ ] 7. Fix Category 5: Authorization Check Status Code (403 instead of 400/500)

  - [x] 7.1 Replace IllegalArgumentException with UnauthorizedException for membership validation
    - Replace `IllegalArgumentException` with `UnauthorizedException` when membership validation fails
    - Add membership validation to ChatRoomController.getRoomById()
    - Files:
      - `src/main/java/org/example/chat/controller/MessageHistoryController.java`
      - `src/main/java/org/example/chat/controller/ChatRoomController.java`
    - In MessageHistoryController:
      ```java
      RoomMembership membership = roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
          .orElseThrow(() -> new UnauthorizedException("User is not a member of this chat room"));
      ```
    - In ChatRoomController.getRoomById(), add membership validation:
      ```java
      @GetMapping("/{id}")
      public ResponseEntity<ChatRoomResponse> getRoomById(
              @PathVariable Long id,
              @AuthenticationPrincipal UserDetails userDetails) {
          
          logger.debug("Retrieving chat room with ID: {}", id);

          try {
              // Get the authenticated user
              User currentUser = userRepository.findByUsername(userDetails.getUsername())
                  .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

              // Get the chat room
              ChatRoom chatRoom = chatRoomService.getRoomById(id);
              
              // Validate that the user is a member of the room
              roomMembershipRepository.findByUserAndChatRoom(currentUser, chatRoom)
                  .orElseThrow(() -> new UnauthorizedException("User is not a member of this chat room"));

              ChatRoomResponse response = ChatRoomResponse.from(chatRoom);
              logger.debug("Retrieved chat room: {}", chatRoom.getName());
              return ResponseEntity.ok(response);
          } catch (RoomNotFoundException e) {
              logger.warn("Chat room not found: {}", id);
              throw e;
          } catch (UnauthorizedException e) {
              logger.warn("Unauthorized access to chat room: {}", id);
              throw e;
          }
      }
      ```
    - Add imports:
      - `import org.example.chat.exception.UnauthorizedException;`
      - `import org.example.chat.repository.UserRepository;`
      - `import org.example.chat.repository.RoomMembershipRepository;`
    - Add required dependencies to ChatRoomController constructor
    - _Bug_Condition: isBugCondition(input) where input.endpoint IN ['/api/rooms/{id}/messages', '/api/rooms/{id}'] AND input.authenticated == true AND NOT EXISTS membership WHERE membership.userId == input.userId AND membership.roomId == input.roomId_
    - _Expected_Behavior: result.statusCode == 403 for non-member access_
    - _Preservation: Member access unchanged (Requirements 3.8, 3.9)_
    - _Requirements: 2.15, 2.16_

  - [x] 7.2 Verify authorization tests now pass
    - **Property 1: Expected Behavior** - Unauthorized Access Returns 403
    - **IMPORTANT**: Re-run the SAME tests from task 1 - do NOT write new tests
    - Run: `mvn test -Dtest=ChatRoomIntegrationTest,MessageHistoryIntegrationTest`
    - **EXPECTED OUTCOME**: Authorization tests PASS (confirms 403 status code)
    - Verify tests that previously failed with 400/500 now pass with 403
    - _Requirements: 2.15, 2.16_

  - [x] 7.3 Verify preservation tests still pass
    - **Property 2: Preservation** - Member Access Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run: `mvn test -Dtest=PreservationPropertyTest`
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm member access still works correctly

## Phase 3: Final Validation

- [x] 8. Checkpoint - Ensure all tests pass
  - Run complete integration test suite: `mvn test`
  - Verify all 15 previously failing tests now pass
  - Verify all preservation tests still pass
  - Verify no new test failures introduced
  - Review test output for any warnings or issues
  - If any tests fail, investigate and fix before proceeding
  - Ask the user if questions arise
