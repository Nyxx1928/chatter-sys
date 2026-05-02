# Lesson: Fixing CI Phase 2 Test Failures - Comprehensive Test Suite Repair

## Task Context

After implementing authorization checks and custom exceptions in the chat application, 19 tests were failing in CI Phase 2. The failures fell into three main categories:

1. **ApplicationContext loading failures** (8 tests) - Missing mock dependencies
2. **Tests expecting old behavior** (7 tests) - Wrong exception types and HTTP status codes
3. **Real implementation issues** (4 tests) - Message ordering, pagination, and DTO structure

This lesson documents the systematic approach to fixing all test failures and making the CI pipeline pass.

## Files Modified

- `src/test/java/org/example/chat/controller/ChatRoomControllerTest.java` (modified)
- `src/test/java/org/example/chat/security/SecurityConfigTest.java` (modified)
- `src/test/java/org/example/chat/controller/MessageHistoryControllerTest.java` (modified)
- `src/test/java/org/example/chat/service/ChatRoomServiceTest.java` (modified)
- `src/test/java/org/example/chat/security/JwtUtilTest.java` (modified)
- `src/test/java/org/example/chat/integration/MessageIntegrationTest.java` (modified)
- `src/main/java/org/example/chat/repository/MessageRepository.java` (modified)
- `src/main/java/org/example/chat/service/ChatMessageService.java` (modified)
- `src/test/java/org/example/chat/service/ChatMessageServiceTest.java` (modified)

## Step-by-Step Changes

### Priority 1: Fix ChatRoomControllerTest (8 errors)

**Problem**: The `ChatRoomController` now requires `RoomMembershipRepository` as a dependency (added for authorization checks), but the test didn't mock it.

**Solution**:
1. Added import for `RoomMembership` and `RoomNotFoundException`
2. Added `@MockBean` for `RoomMembershipRepository`
3. Updated `getRoomById_ExistingRoom_ReturnsRoom()` to mock:
   - User repository lookup
   - Room service call
   - Membership check
4. Updated exception expectations from `IllegalArgumentException` to `RoomNotFoundException`
5. Changed expected HTTP status from `400 Bad Request` to `404 Not Found`

```java
// Before: Missing mock
@MockBean
private ChatRoomService chatRoomService;

@MockBean
private UserRepository userRepository;

// After: Added missing mock
@MockBean
private ChatRoomService chatRoomService;

@MockBean
private UserRepository userRepository;

@MockBean
private RoomMembershipRepository roomMembershipRepository;
```

### Priority 2: Fix SecurityConfigTest (2 failures)

**Problem**: Tests expected `403 Forbidden` for unauthenticated requests, but Spring Security was configured to return `401 Unauthorized`.

**Solution**: Updated test expectations to match the actual security configuration.

```java
// Before: Wrong expectation
mockMvc.perform(get("/api/users/me"))
    .andExpect(status().isForbidden()); // 403

// After: Correct expectation
mockMvc.perform(get("/api/users/me"))
    .andExpect(status().isUnauthorized()); // 401
```

**Key Insight**: 
- `401 Unauthorized` = Authentication failed (no valid credentials)
- `403 Forbidden` = Authorization failed (valid credentials, but insufficient permissions)

### Priority 3: Fix MessageHistoryControllerTest (2 failures)

**Problem**: Tests expected `IllegalArgumentException` but the controller now throws specific exceptions:
- `RoomNotFoundException` for missing rooms
- `UnauthorizedException` for non-members

**Solution**: Updated exception expectations to match the new exception hierarchy.

```java
// Before: Generic exception
assertThrows(IllegalArgumentException.class, () -> {
    controller.getMessageHistory(1L, pageable, userDetails);
});

// After: Specific exception
assertThrows(RoomNotFoundException.class, () -> {
    controller.getMessageHistory(1L, pageable, userDetails);
});
```

### Priority 4: Fix ChatRoomServiceTest (2 failures)

**Problem**: Service now throws `RoomNotFoundException` instead of `IllegalArgumentException` for missing rooms.

**Solution**: Updated test expectations and assertions.

```java
// Before
IllegalArgumentException exception = assertThrows(
    IllegalArgumentException.class,
    () -> chatRoomService.getRoomById(roomId)
);
assertEquals("Chat room not found", exception.getMessage());

// After
RoomNotFoundException exception = assertThrows(
    RoomNotFoundException.class,
    () -> chatRoomService.getRoomById(roomId)
);
assertTrue(exception.getMessage().contains("999"));
```

### Priority 5: Fix JwtUtilTest (1 failure)

**Problem**: Test was tampering with JWT token by changing the last character, but this didn't reliably produce an invalid signature.

**Solution**: Created a token with a different secret key to guarantee signature mismatch.

```java
// Before: Unreliable tampering
String tamperedToken = token.substring(0, token.length() - 1) + "X";

// After: Guaranteed signature mismatch
String differentSecret = "different-secret-key-for-jwt-token-generation-minimum-256-bits-required";
SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

String tokenWithDifferentSignature = Jwts.builder()
    .subject(username)
    .issuedAt(now)
    .expiration(expiryDate)
    .signWith(differentKey)
    .compact();
```

### Priority 6: Fix MessageIntegrationTest (4 failures)

**Problem 1**: Message ordering was wrong - repository returned newest first, tests expected oldest first.

**Solution**: Changed repository method from `findByChatRoomOrderByTimestampDesc` to `findByChatRoomOrderByTimestampAsc`.

```java
// Repository interface
Page<Message> findByChatRoomOrderByTimestampAsc(ChatRoom room, Pageable pageable);

// Service implementation
Page<Message> messages = messageRepository.findByChatRoomOrderByTimestampAsc(chatRoom, pageable);
```

**Problem 2**: Pagination parameter was wrong - test used `limit` but Spring uses `size`.

**Solution**: Changed test parameter from `limit` to `size`.

```java
// Before: Wrong parameter
.param("limit", "5")

// After: Correct parameter
.param("size", "5")
```

**Problem 3**: Sender information path was wrong - DTO has flat structure, not nested.

**Solution**: Updated JSON path from `$[*].sender.username` to `$[*].senderUsername`.

```java
// Before: Nested path
.andExpect(jsonPath("$[*].sender.username", everyItem(is("messageuser"))));

// After: Flat path
.andExpect(jsonPath("$[*].senderUsername", everyItem(is("messageuser"))));
```

**Problem 4**: Test mocks in `ChatMessageServiceTest` still referenced old method name.

**Solution**: Updated all mock calls to use the new method name.

```java
// Before
when(messageRepository.findByChatRoomOrderByTimestampDesc(testRoom, pageable))
    .thenReturn(expectedPage);

// After
when(messageRepository.findByChatRoomOrderByTimestampAsc(testRoom, pageable))
    .thenReturn(expectedPage);
```

## Why This Approach

### 1. Systematic Prioritization
We fixed issues in order of impact:
- ApplicationContext failures first (blocked 8 tests)
- Simple expectation updates next (quick wins)
- Real implementation issues last (required code changes)

### 2. Test-Driven Verification
Each fix was verified by running the specific test class before moving to the next issue.

### 3. Minimal Changes
We only changed what was necessary:
- Updated test expectations to match new behavior
- Fixed real bugs (message ordering, pagination)
- Didn't refactor working code

### 4. Consistency
All similar issues were fixed the same way:
- All `IllegalArgumentException` → `RoomNotFoundException` changes
- All `403` → `401` changes for authentication
- All repository method name updates

## Alternatives Considered

### Alternative 1: Keep Old Exception Types
**Rejected**: Using generic `IllegalArgumentException` loses semantic meaning. Specific exceptions enable proper HTTP status codes (404 for not found, 403 for unauthorized).

### Alternative 2: Change Implementation to Match Tests
**Rejected**: Tests were expecting incorrect behavior (wrong status codes, wrong ordering). Fixing the implementation was the right choice.

### Alternative 3: Disable Failing Tests
**Rejected**: This would hide real issues. All tests should pass in CI.

## Key Concepts

### 1. Spring Security Status Codes
- **401 Unauthorized**: No valid authentication credentials
- **403 Forbidden**: Valid credentials but insufficient permissions
- Configure via `AuthenticationEntryPoint` in security config

### 2. Custom Exception Hierarchy
```
ChatApplicationException (base)
├── RoomNotFoundException (404)
├── UserNotFoundException (404)
├── UnauthorizedException (403)
├── ValidationException (400)
└── WebSocketException (500)
```

### 3. Spring Data Pagination
- Default parameter: `size` (not `limit`)
- Default page: `page` (0-indexed)
- Default sort: `sort` (e.g., `sort=timestamp,asc`)

### 4. DTO Structure
- Flat structure is simpler for JSON serialization
- Nested objects require careful null handling
- Use `@JsonProperty` for custom field names if needed

### 5. Repository Query Methods
- Method names define query behavior
- `OrderBy{Field}Asc` = ascending order
- `OrderBy{Field}Desc` = descending order
- Order matters for user experience (chat messages should be oldest first)

## Potential Pitfalls

### 1. Forgetting to Update Test Mocks
**Problem**: Changed repository method name but forgot to update test mocks.
**Solution**: Search for all usages of the old method name before changing it.

### 2. Inconsistent Exception Handling
**Problem**: Some methods throw `IllegalArgumentException`, others throw specific exceptions.
**Solution**: Use specific exceptions consistently across the codebase.

### 3. Wrong HTTP Status Codes
**Problem**: Returning 400 Bad Request for resource not found.
**Solution**: Use proper status codes: 404 for not found, 403 for unauthorized, 400 for validation errors.

### 4. Test Expectations vs Reality
**Problem**: Tests expect behavior that doesn't match requirements.
**Solution**: Verify requirements before deciding whether to fix code or tests.

### 5. Message Ordering Confusion
**Problem**: Unclear whether messages should be oldest-first or newest-first.
**Solution**: Chat applications typically show oldest messages first (chronological order).

## What You Learned

### 1. Test Failure Categories
- **Compilation errors**: Missing dependencies, wrong method signatures
- **Assertion failures**: Wrong expectations, outdated tests
- **Real bugs**: Implementation doesn't match requirements

### 2. Fixing Strategy
1. Fix compilation errors first (tests won't run otherwise)
2. Fix simple assertion updates next (quick wins)
3. Fix real implementation issues last (requires more thought)

### 3. Spring Test Mocking
- `@MockBean` creates mocks in Spring context
- All dependencies must be mocked in `@WebMvcTest`
- Forgot mocks cause ApplicationContext loading failures

### 4. Exception Design
- Specific exceptions enable proper HTTP status codes
- Custom exception hierarchy improves error handling
- Global exception handler maps exceptions to responses

### 5. Integration Test Patterns
- Test actual HTTP requests/responses
- Verify JSON structure matches DTO
- Check HTTP status codes match expectations
- Test both success and error cases

### 6. Pagination Best Practices
- Use Spring's standard parameter names (`size`, `page`, `sort`)
- Return appropriate data structure (List for simple, Page for metadata)
- Document pagination parameters in API docs

### 7. Message Ordering
- Chat messages should be chronological (oldest first)
- Repository method names should reflect sort order
- Consider user experience when choosing sort direction

### 8. Test Maintenance
- Keep tests in sync with implementation changes
- Update test expectations when behavior changes intentionally
- Don't disable tests - fix them or remove them

## Final Results

**Before**: 19 test failures across multiple test classes
**After**: All 162 tests passing

```
[INFO] Tests run: 162, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test Breakdown
- ChatRoomControllerTest: 8 tests ✓
- SecurityConfigTest: 4 tests ✓
- MessageHistoryControllerTest: 6 tests ✓
- ChatRoomServiceTest: 17 tests ✓
- JwtUtilTest: 13 tests ✓
- MessageIntegrationTest: 8 tests ✓
- ChatMessageServiceTest: 12 tests ✓
- Plus 94 other tests ✓

The systematic approach to categorizing, prioritizing, and fixing test failures ensured that all issues were resolved efficiently and correctly.
