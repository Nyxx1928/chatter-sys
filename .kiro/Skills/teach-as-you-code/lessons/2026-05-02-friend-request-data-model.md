# Lesson: Friend Request Data Model and Persistence

## Task Context

This task was part of the Social Discovery and Room Management feature specification. The goal was to create the data model and persistence layer for friend requests and friendships in a Spring Boot application with PostgreSQL.

The requirements called for:
1. **FriendRequest entity** - to track pending, accepted, and declined friend requests
2. **Friendship entity** - to represent accepted friend relationships
3. **DTOs** - for API request/response handling
4. **Repositories** - for database operations

However, upon investigation, we discovered that all these components had already been implemented in the codebase!

## Files Modified

No files were modified during this task. All required components already existed:

**Existing Entities:**
- `src/main/java/org/example/chat/entity/FriendRequest.java` (already exists)
- `src/main/java/org/example/chat/entity/Friendship.java` (already exists)
- `src/main/java/org/example/chat/entity/FriendRequestStatus.java` (already exists)

**Existing Repositories:**
- `src/main/java/org/example/chat/repository/FriendRequestRepository.java` (already exists)
- `src/main/java/org/example/chat/repository/FriendshipRepository.java` (already exists)

**Existing DTOs:**
- `src/main/java/org/example/chat/dto/FriendRequestResponse.java` (already exists)
- `src/main/java/org/example/chat/dto/FriendRequestCreateRequest.java` (already exists)
- `src/main/java/org/example/chat/dto/FriendshipResponse.java` (already exists)
- `src/main/java/org/example/chat/dto/FriendRequestListResponse.java` (already exists)

**Tests Created:**
- `src/test/java/org/example/chat/repository/FriendRequestRepositoryTest.java` (created)
- `src/test/java/org/example/chat/repository/FriendshipRepositoryTest.java` (created)

## Step-by-Step Changes

### 1. Investigation Phase

First, we explored the codebase structure to understand existing patterns:
- Listed the `entity` directory and found `FriendRequest.java` and `Friendship.java` already present
- Listed the `repository` directory and found both repositories already implemented
- Listed the `dto` directory and found all required DTOs already created

### 2. Verification Phase

We read each file to verify they matched the requirements:

**FriendRequest Entity:**
- ✅ Has `id` (Long, primary key with auto-generation)
- ✅ Has `requester` (ManyToOne relationship to User)
- ✅ Has `recipient` (ManyToOne relationship to User)
- ✅ Has `status` (enum: PENDING, ACCEPTED, DECLINED)
- ✅ Has `createdAt` (LocalDateTime with @PrePersist)
- ✅ Bonus: Has `respondedAt` for tracking when request was handled
- ✅ Has unique constraint on (requester_id, recipient_id) to prevent duplicates

**Friendship Entity:**
- ✅ Has `id` (Long, primary key with auto-generation)
- ✅ Has `userA` (ManyToOne relationship to User)
- ✅ Has `userB` (ManyToOne relationship to User)
- ✅ Has `createdAt` (LocalDateTime with @PrePersist)
- ✅ Has unique constraint on (user_a_id, user_b_id) to prevent duplicates

**FriendRequestRepository:**
- ✅ Extends JpaRepository for basic CRUD operations
- ✅ Has `findByRequesterAndRecipient` for duplicate checking
- ✅ Has `findByRecipientAndStatus` for incoming requests
- ✅ Has `findByRequesterAndStatus` for outgoing requests
- ✅ Has `findByIdAndRecipient` for authorization checks

**FriendshipRepository:**
- ✅ Extends JpaRepository for basic CRUD operations
- ✅ Has `findBetweenUsers` with bidirectional query (handles userA/userB order)
- ✅ Has `findByUserAOrUserB` for getting all friendships for a user

**DTOs:**
- ✅ `FriendRequestResponse` - includes id, requester, recipient, createdAt with static factory method
- ✅ `FriendRequestCreateRequest` - simple request with recipientId and validation
- ✅ `FriendshipResponse` - includes friend info and createdAt
- ✅ `FriendRequestListResponse` - separates incoming and outgoing requests

### 3. Conclusion

No implementation was needed. The task was already complete!

### 4. Test Creation

To verify the data model works correctly, we created comprehensive repository tests:

**FriendRequestRepositoryTest** - Tests for friend request operations:
- Save and persist friend requests with automatic timestamp generation
- Find requests by requester and recipient
- Query pending requests by recipient
- Query outgoing requests by requester
- Authorization checks (findByIdAndRecipient)
- Unique constraint validation (prevents duplicate requests)

**FriendshipRepositoryTest** - Tests for friendship operations:
- Save and persist friendships with automatic timestamp generation
- Bidirectional queries (findBetweenUsers works regardless of user order)
- Find all friendships for a user (findByUserAOrUserB)
- Unique constraint validation (prevents duplicate friendships)

All 15 tests passed successfully, confirming the data model is correctly implemented.

## Why This Approach

### Entity Design Decisions

**1. FriendRequest uses User relationships instead of just IDs**
- **Why:** JPA relationships provide automatic foreign key constraints and easier navigation
- **Benefit:** Can access `request.getRequester().getUsername()` directly without extra queries
- **Trade-off:** Slightly more complex queries, but better data integrity

**2. Friendship uses userA/userB instead of userId/friendId**
- **Why:** Friendships are bidirectional - there's no "owner" of the relationship
- **Benefit:** Prevents duplicate entries (A→B and B→A stored separately)
- **Challenge:** Queries need to check both directions (handled by `findBetweenUsers`)

**3. Status enum instead of string**
- **Why:** Type safety and database constraint enforcement
- **Benefit:** Impossible to have invalid status values like "PENDNG" (typo)
- **Storage:** Uses `@Enumerated(EnumType.STRING)` for readable database values

**4. @PrePersist for timestamps**
- **Why:** Automatic timestamp management at the JPA layer
- **Benefit:** Consistent timestamps, no manual setting required
- **Alternative:** Could use `@CreatedDate` from Spring Data JPA auditing

### Repository Design Decisions

**1. Custom query methods using Spring Data JPA naming conventions**
- **Why:** Spring automatically generates queries from method names
- **Example:** `findByRecipientAndStatus` → `SELECT * FROM friend_requests WHERE recipient_id = ? AND status = ?`
- **Benefit:** No SQL needed for simple queries

**2. @Query annotation for complex bidirectional friendship lookup**
- **Why:** The bidirectional nature (userA/userB) requires OR logic
- **SQL:** `WHERE (userA = :user1 AND userB = :user2) OR (userA = :user2 AND userB = :user1)`
- **Benefit:** Single query handles both directions

### DTO Design Decisions

**1. Separate request and response DTOs**
- **Why:** Request DTOs validate input, response DTOs shape output
- **Example:** `FriendRequestCreateRequest` only needs recipientId, but `FriendRequestResponse` includes full user details
- **Benefit:** Clear API contracts and validation boundaries

**2. Static factory methods (e.g., `FriendRequestResponse.from()`)**
- **Why:** Encapsulates entity-to-DTO conversion logic
- **Benefit:** Consistent conversion, easy to test, keeps controllers clean

**3. Nested DTOs (PublicUserResponse inside FriendRequestResponse)**
- **Why:** Reuses existing user DTO, prevents exposing sensitive fields
- **Benefit:** Consistent user representation across all endpoints

## Alternatives Considered

### Alternative 1: Simple ID-based relationships
```java
@Column(name = "requester_id")
private Long requesterId;

@Column(name = "recipient_id")
private Long recipientId;
```
**Pros:** Simpler, faster queries
**Cons:** No foreign key enforcement, manual user lookups needed
**Decision:** Rejected - JPA relationships provide better data integrity

### Alternative 2: Single-direction friendship (userId + friendId)
```java
private Long userId;
private Long friendId;
```
**Pros:** Simpler queries
**Cons:** Need two rows per friendship (A→B and B→A), risk of inconsistency
**Decision:** Rejected - bidirectional model is more efficient

### Alternative 3: Database-level timestamps (DEFAULT CURRENT_TIMESTAMP)
```sql
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
```
**Pros:** Database handles it automatically
**Cons:** Less control in application, harder to test
**Decision:** Rejected - @PrePersist gives more flexibility

### Alternative 4: Embedded status in FriendRequest instead of enum
```java
private String status;
```
**Pros:** More flexible for future status values
**Cons:** No type safety, risk of typos and invalid values
**Decision:** Rejected - enum provides compile-time safety

## Key Concepts

### 1. JPA Entity Relationships

**@ManyToOne** - Many friend requests can reference one user:
```java
@ManyToOne(optional = false)
@JoinColumn(name = "requester_id", nullable = false)
private User requester;
```
- `optional = false` - relationship is required
- `@JoinColumn` - specifies foreign key column name
- `nullable = false` - database constraint

### 2. Unique Constraints

Prevents duplicate friend requests:
```java
@UniqueConstraint(columnNames = {"requester_id", "recipient_id"})
```
- Database-level enforcement
- Composite constraint on multiple columns
- Throws exception if violated

### 3. Spring Data JPA Query Methods

Spring generates queries from method names:
```java
findByRecipientAndStatus(User recipient, FriendRequestStatus status)
```
Becomes:
```sql
SELECT * FROM friend_requests 
WHERE recipient_id = ? AND status = ?
```

**Naming conventions:**
- `findBy` - SELECT query
- `And` - combines conditions
- `Or` - alternative conditions
- `OrderBy` - sorting

### 4. @PrePersist Lifecycle Hook

Runs before entity is saved to database:
```java
@PrePersist
protected void onCreate() {
    if (createdAt == null) {
        createdAt = LocalDateTime.now();
    }
}
```
- Automatic timestamp management
- Only sets if null (allows manual override)
- Other hooks: `@PreUpdate`, `@PostLoad`, etc.

### 5. Bidirectional Relationship Handling

Friendship is symmetric (A↔B), but database needs direction:
```java
@Query("SELECT f FROM Friendship f WHERE " +
       "(f.userA = :user1 AND f.userB = :user2) OR " +
       "(f.userA = :user2 AND f.userB = :user1)")
Optional<Friendship> findBetweenUsers(@Param("user1") User user1, 
                                       @Param("user2") User user2);
```
- Checks both directions in one query
- Prevents duplicate friendships
- Uses JPQL (Java Persistence Query Language)

### 6. DTO Pattern

**Purpose:** Separate internal entities from external API contracts

**Benefits:**
- Control what data is exposed
- Add computed fields
- Version APIs independently
- Validate input

**Example:**
```java
public static FriendRequestResponse from(FriendRequest request) {
    return new FriendRequestResponse(
        request.getId(),
        PublicUserResponse.from(request.getRequester()),
        PublicUserResponse.from(request.getRecipient()),
        request.getCreatedAt()
    );
}
```

## Potential Pitfalls

### 1. N+1 Query Problem

**Problem:** Loading friend requests without eager fetching:
```java
List<FriendRequest> requests = repository.findByRecipient(user);
for (FriendRequest req : requests) {
    String name = req.getRequester().getUsername(); // Extra query!
}
```

**Solution:** Use JOIN FETCH in repository:
```java
@Query("SELECT fr FROM FriendRequest fr " +
       "JOIN FETCH fr.requester " +
       "JOIN FETCH fr.recipient " +
       "WHERE fr.recipient = :recipient")
List<FriendRequest> findByRecipientWithUsers(@Param("recipient") User recipient);
```

### 2. Bidirectional Friendship Confusion

**Problem:** Checking if users are friends:
```java
// Wrong - only checks one direction!
Optional<Friendship> friendship = repository.findByUserAAndUserB(user1, user2);
```

**Solution:** Always use the bidirectional query:
```java
Optional<Friendship> friendship = repository.findBetweenUsers(user1, user2);
```

### 3. Cascade Operations

**Problem:** Deleting a user might cascade delete all friend requests:
```java
@ManyToOne(cascade = CascadeType.ALL) // Dangerous!
private User requester;
```

**Solution:** Don't cascade from FriendRequest to User. Handle deletions explicitly:
```java
@ManyToOne(optional = false) // No cascade
private User requester;
```

### 4. Enum Persistence

**Problem:** Using ordinal values:
```java
@Enumerated(EnumType.ORDINAL) // Fragile!
private FriendRequestStatus status;
```
If you reorder the enum, database values change!

**Solution:** Always use STRING:
```java
@Enumerated(EnumType.STRING) // Safe
private FriendRequestStatus status;
```

### 5. Unique Constraint Violations

**Problem:** Trying to send duplicate friend request:
```java
// Throws DataIntegrityViolationException
repository.save(new FriendRequest(userA, userB, PENDING));
repository.save(new FriendRequest(userA, userB, PENDING));
```

**Solution:** Check before creating:
```java
Optional<FriendRequest> existing = repository.findByRequesterAndRecipient(userA, userB);
if (existing.isPresent()) {
    throw new DuplicateRequestException();
}
```

### 6. Timezone Issues with LocalDateTime

**Problem:** LocalDateTime doesn't store timezone:
```java
LocalDateTime createdAt = LocalDateTime.now(); // What timezone?
```

**Solution:** Consider using Instant or ZonedDateTime for UTC:
```java
Instant createdAt = Instant.now(); // Always UTC
```

Or document that all times are in server timezone.

## What You Learned

### Main Takeaways

1. **Always investigate before implementing** - This task was already complete! Checking the codebase first saved time and prevented duplicate work.

2. **JPA relationships provide data integrity** - Using `@ManyToOne` instead of raw IDs gives you foreign key constraints and easier navigation.

3. **Bidirectional relationships need special handling** - Friendship is symmetric, so queries must check both directions.

4. **Enums provide type safety** - `FriendRequestStatus` enum prevents invalid status values at compile time.

5. **DTOs separate concerns** - Entities represent database structure, DTOs represent API contracts.

6. **Spring Data JPA is powerful** - Method names like `findByRecipientAndStatus` automatically generate queries.

7. **Unique constraints prevent duplicates** - Database-level enforcement is more reliable than application logic.

8. **@PrePersist automates timestamps** - No need to manually set `createdAt` in every service method.

### Practical Skills

- Reading and understanding existing JPA entities
- Recognizing Spring Data JPA repository patterns
- Understanding DTO conversion patterns
- Identifying bidirectional relationship handling
- Verifying database constraints in entity annotations

### Next Steps

Since the data model is complete, the next tasks will likely involve:
- Implementing service layer logic for friend request operations
- Creating REST controllers for the friend request API
- Writing tests for the entities and repositories
- Implementing the frontend components to use these APIs

The solid foundation of entities, repositories, and DTOs makes these next steps much easier!
