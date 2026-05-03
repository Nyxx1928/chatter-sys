# Lesson: Implementing Room Deletion Endpoint with Authorization

## Task Context

This lesson covers implementing a DELETE endpoint for chat rooms in a Spring Boot REST API. The endpoint allows room owners and moderators to delete rooms, with proper authorization checks and cascade deletion of related data (memberships and messages).

**Requirements:**
- DELETE /api/rooms/{id} endpoint
- Only room owner or moderator can delete
- Return 403 if user is not authorized
- Return 404 if room doesn't exist
- Delete room, memberships, and messages (cascade delete)
- Return 204 No Content on success

**Spec Requirements:** 6.1, 6.3, 6.4, 8.3

## Files Modified

- `src/main/java/org/example/chat/controller/ChatRoomController.java` (modified)
- `src/main/java/org/example/chat/service/ChatRoomService.java` (modified)
- `src/test/java/org/example/chat/controller/ChatRoomControllerTest.java` (modified)
- `src/test/java/org/example/chat/service/ChatRoomServiceTest.java` (modified)

## Step-by-Step Changes

### 1. Controller Layer - DELETE Endpoint

The controller already had the DELETE endpoint implemented:

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteRoom(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails) {
    logger.info("Room deletion request for room ID: {} by user: {}", id, userDetails.getUsername());

    User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

    chatRoomService.deleteRoom(id, currentUser.getId());
    return ResponseEntity.noContent().build();
}
```

**Key aspects:**
- Uses `@DeleteMapping("/{id}")` to handle DELETE requests
- Extracts authenticated user from `@AuthenticationPrincipal`
- Delegates authorization and deletion logic to service layer
- Returns `204 No Content` on success (standard for successful DELETE)

### 2. Service Layer - Authorization and Deletion Logic

The service layer already had the `deleteRoom` method:

```java
@Transactional
public void deleteRoom(Long roomId, Long userId) {
    logger.info("Deleting chat room ID: {} by user ID: {}", roomId, userId);

    ChatRoom room = getRoomById(roomId);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

    RoomMembership membership = roomMembershipRepository.findByUserAndChatRoom(user, room)
            .orElseThrow(() -> new UnauthorizedException("User is not a member of this room"));

    if (membership.getRole() != MemberRole.OWNER && membership.getRole() != MemberRole.MODERATOR) {
        throw new UnauthorizedException("Only owners or moderators can delete rooms");
    }

    chatRoomRepository.delete(room);
    logger.info("Deleted chat room ID: {}", roomId);
}
```

**Authorization checks:**
1. Verify room exists (throws `RoomNotFoundException` if not)
2. Verify user exists (throws `UserNotFoundException` if not)
3. Verify user is a member (throws `UnauthorizedException` if not)
4. Verify user has OWNER or MODERATOR role (throws `UnauthorizedException` if not)

**Cascade deletion:**
The `ChatRoom` entity has cascade configuration:

```java
@OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
private List<Message> messages = new ArrayList<>();

@OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
private List<RoomMembership> memberships = new ArrayList<>();
```

This means when we delete a room, JPA automatically deletes all associated messages and memberships.

### 3. Controller Tests

Added three test cases to `ChatRoomControllerTest`:

```java
@Test
@WithMockUser(username = "testuser")
void deleteRoom_AsOwner_ReturnsNoContent() throws Exception {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    mockMvc.perform(delete("/api/rooms/1")
            .with(csrf()))
        .andExpect(status().isNoContent());

    verify(chatRoomService).deleteRoom(1L, 1L);
}

@Test
@WithMockUser(username = "testuser")
void deleteRoom_Unauthorized_ReturnsForbidden() throws Exception {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    doThrow(new UnauthorizedException("Only owners or moderators can delete rooms"))
        .when(chatRoomService).deleteRoom(1L, 1L);

    mockMvc.perform(delete("/api/rooms/1")
            .with(csrf()))
        .andExpect(status().isForbidden());
}

@Test
@WithMockUser(username = "testuser")
void deleteRoom_RoomNotFound_ReturnsNotFound() throws Exception {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    doThrow(new RoomNotFoundException(999L))
        .when(chatRoomService).deleteRoom(999L, 1L);

    mockMvc.perform(delete("/api/rooms/999")
            .with(csrf()))
        .andExpect(status().isNotFound());
}
```

### 4. Service Tests

Added six test cases to `ChatRoomServiceTest`:

1. **deleteRoom_AsOwner_DeletesRoom** - Verifies owner can delete
2. **deleteRoom_AsModerator_DeletesRoom** - Verifies moderator can delete
3. **deleteRoom_AsMember_ThrowsUnauthorizedException** - Verifies regular member cannot delete
4. **deleteRoom_UserNotFound_ThrowsUserNotFoundException** - Verifies user existence check
5. **deleteRoom_RoomNotFound_ThrowsRoomNotFoundException** - Verifies room existence check
6. **deleteRoom_UserNotMember_ThrowsUnauthorizedException** - Verifies membership check

### 5. Fixed Pre-existing Test Issue

The test `addMember_AlreadyMember_ThrowsException` was failing because the service behavior had changed to return the existing membership instead of throwing an exception. Updated the test:

```java
@Test
void addMember_AlreadyMember_ReturnsExistingMembership() {
    // ... setup ...
    
    RoomMembership result = chatRoomService.addMember(roomId, userId, MemberRole.MEMBER);

    assertNotNull(result);
    assertEquals(testMembership, result);
    verify(roomMembershipRepository, never()).save(any(RoomMembership.class));
}
```

## Why This Approach

### 1. Separation of Concerns
- **Controller**: Handles HTTP concerns (request/response, status codes)
- **Service**: Handles business logic (authorization, deletion)
- **Repository**: Handles data access

### 2. Proper HTTP Status Codes
- **204 No Content**: Standard for successful DELETE operations
- **403 Forbidden**: User is authenticated but not authorized
- **404 Not Found**: Room doesn't exist

### 3. Cascade Delete via JPA
Using `CascadeType.ALL` on entity relationships is cleaner than manual deletion:
- Automatic cleanup of related data
- Database referential integrity maintained
- Less code to maintain
- Transaction-safe

### 4. Authorization Before Deletion
Check permissions before attempting deletion to provide clear error messages and prevent unnecessary database operations.

### 5. Transactional Consistency
The `@Transactional` annotation ensures all operations (authorization checks and deletion) happen atomically.

## Alternatives Considered

### 1. Manual Deletion of Related Data
```java
// Not recommended
messageRepository.deleteAllByChatRoom(room);
roomMembershipRepository.deleteAllByChatRoom(room);
chatRoomRepository.delete(room);
```

**Why cascade is better:**
- Less code
- Automatic handling by JPA
- Consistent with entity relationships
- Less prone to errors

### 2. Soft Delete
```java
room.setDeleted(true);
room.setDeletedAt(LocalDateTime.now());
chatRoomRepository.save(room);
```

**When to use:**
- Need audit trail
- Need to restore deleted rooms
- Regulatory requirements

**Why hard delete is fine here:**
- No requirement for restoration
- Simpler implementation
- Cleaner database

### 3. Authorization at Controller Level
```java
@PreAuthorize("hasRole('OWNER') or hasRole('MODERATOR')")
```

**Why service-level is better:**
- Role is per-room, not global
- Need to check membership first
- More flexible authorization logic

## Key Concepts

### 1. RESTful DELETE Operations
- Use HTTP DELETE method
- Return 204 No Content on success (no response body needed)
- Idempotent: deleting twice should not cause errors

### 2. Authorization vs Authentication
- **Authentication**: Who are you? (handled by Spring Security)
- **Authorization**: What can you do? (handled in service layer)

### 3. JPA Cascade Types
- `CascadeType.ALL`: All operations cascade (persist, merge, remove, refresh, detach)
- `CascadeType.REMOVE`: Only delete operations cascade
- Defined on the parent entity's relationship

### 4. Exception-Driven Flow Control
Using custom exceptions for different error scenarios:
- `RoomNotFoundException` → 404
- `UserNotFoundException` → 404
- `UnauthorizedException` → 403

### 5. Test Coverage Strategy
**Controller tests**: Mock service, verify HTTP behavior
**Service tests**: Mock repositories, verify business logic

## Potential Pitfalls

### 1. Forgetting CSRF Token in Tests
```java
// Wrong
mockMvc.perform(delete("/api/rooms/1"))

// Right
mockMvc.perform(delete("/api/rooms/1").with(csrf()))
```

### 2. Not Using @Transactional
Without `@Transactional`, cascade delete might fail or leave orphaned records.

### 3. Checking Role Before Membership
```java
// Wrong order
if (membership.getRole() != MemberRole.OWNER) {
    throw new UnauthorizedException("...");
}
// membership might be null!

// Right order
RoomMembership membership = roomMembershipRepository.findByUserAndChatRoom(user, room)
    .orElseThrow(() -> new UnauthorizedException("User is not a member"));
if (membership.getRole() != MemberRole.OWNER) {
    throw new UnauthorizedException("...");
}
```

### 4. Returning Wrong Status Code
```java
// Wrong - 200 OK with empty body
return ResponseEntity.ok().build();

// Right - 204 No Content
return ResponseEntity.noContent().build();
```

### 5. Not Verifying Cascade Configuration
If cascade is not configured, you'll get foreign key constraint violations when trying to delete a room with messages or memberships.

### 6. Testing with Wrong Mock Behavior
```java
// Wrong - doesn't match actual service behavior
when(chatRoomService.deleteRoom(1L, 1L)).thenReturn(something);

// Right - void method, use doThrow for exceptions
doThrow(new UnauthorizedException("..."))
    .when(chatRoomService).deleteRoom(1L, 1L);
```

## What You Learned

1. **RESTful DELETE endpoints** return 204 No Content on success
2. **Authorization checks** should happen in the service layer for complex, resource-specific permissions
3. **JPA cascade delete** automatically handles related entity cleanup
4. **Custom exceptions** map to appropriate HTTP status codes via global exception handler
5. **Test coverage** should include success cases and all error scenarios (unauthorized, not found, etc.)
6. **@Transactional** ensures atomic operations when deleting related data
7. **MockMvc testing** requires CSRF tokens for state-changing operations
8. **Service tests** should verify business logic independently of HTTP concerns
