# Lesson: Understanding the Database Structure and Using SQL Commands

## Task Context

You asked to learn about the database in this repository - how to see tables, columns, rows, and how to use SQL commands. This is a Spring Boot chat application that uses PostgreSQL as its database, with JPA/Hibernate for object-relational mapping (ORM).

The application manages a real-time chat system with users, chat rooms, messages, friendships, and friend requests. Understanding the database structure is crucial for debugging, data analysis, and feature development.

## Files Modified

No files were modified for this lesson - this is a learning session about existing database structure.

## Step-by-Step Changes

### 1. Understanding the Database Technology Stack

**What's being used:**
- **PostgreSQL**: A powerful, open-source relational database
- **Spring Data JPA**: Java Persistence API for database operations
- **Hibernate**: The ORM implementation that converts Java objects to database tables
- **HikariCP**: Connection pool manager (configured in `application.yml`)

**Configuration location:** `src/main/resources/application.yml`

Key configuration:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/chatdb
    username: chatuser
    password: chatpass
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update  # Automatically creates/updates tables
    show-sql: false     # Set to true to see SQL queries in logs
```

### 2. Database Schema Overview

The application has **6 main tables**:

#### Table 1: `users`
Stores user account information.

| Column Name    | Type          | Constraints           | Description                    |
|----------------|---------------|-----------------------|--------------------------------|
| id             | BIGINT        | PRIMARY KEY, AUTO     | Unique user identifier         |
| username       | VARCHAR(50)   | UNIQUE, NOT NULL      | Login username                 |
| email          | VARCHAR(100)  | UNIQUE, NOT NULL      | User email address             |
| password_hash  | VARCHAR       | NOT NULL              | Encrypted password             |
| display_name   | VARCHAR(100)  |                       | Display name in chat           |
| created_at     | TIMESTAMP     | NOT NULL              | Account creation time          |
| last_seen      | TIMESTAMP     |                       | Last activity timestamp        |
| online         | BOOLEAN       | NOT NULL, DEFAULT false | Current online status        |

**Java Entity:** `src/main/java/org/example/chat/entity/User.java`

#### Table 2: `chat_rooms`
Stores chat room information.

| Column Name    | Type          | Constraints           | Description                    |
|----------------|---------------|-----------------------|--------------------------------|
| id             | BIGINT        | PRIMARY KEY, AUTO     | Unique room identifier         |
| name           | VARCHAR(100)  | UNIQUE, NOT NULL      | Room name                      |
| description    | VARCHAR(500)  |                       | Room description               |
| created_at     | TIMESTAMP     | NOT NULL              | Room creation time             |
| created_by_id  | BIGINT        | FOREIGN KEY → users   | Creator user ID                |

**Java Entity:** `src/main/java/org/example/chat/entity/ChatRoom.java`

#### Table 3: `messages`
Stores all chat messages.

| Column Name    | Type          | Constraints           | Description                    |
|----------------|---------------|-----------------------|--------------------------------|
| id             | BIGINT        | PRIMARY KEY, AUTO     | Unique message identifier      |
| sender_id      | BIGINT        | FOREIGN KEY → users, NOT NULL | Message sender       |
| chat_room_id   | BIGINT        | FOREIGN KEY → chat_rooms, NOT NULL | Target room    |
| content        | TEXT          | NOT NULL              | Message text content           |
| timestamp      | TIMESTAMP     | NOT NULL              | Message sent time              |
| message_type   | VARCHAR(20)   |                       | TEXT or SYSTEM                 |

**Index:** `idx_room_timestamp` on (chat_room_id, timestamp) for fast message retrieval

**Java Entity:** `src/main/java/org/example/chat/entity/Message.java`

#### Table 4: `friendships`
Stores established friendships between users.

| Column Name    | Type          | Constraints           | Description                    |
|----------------|---------------|-----------------------|--------------------------------|
| id             | BIGINT        | PRIMARY KEY, AUTO     | Unique friendship identifier   |
| user_a_id      | BIGINT        | FOREIGN KEY → users, NOT NULL | First user          |
| user_b_id      | BIGINT        | FOREIGN KEY → users, NOT NULL | Second user         |
| created_at     | TIMESTAMP     | NOT NULL              | Friendship creation time       |

**Unique Constraint:** (user_a_id, user_b_id) - prevents duplicate friendships

**Java Entity:** `src/main/java/org/example/chat/entity/Friendship.java`

#### Table 5: `friend_requests`
Stores pending, accepted, or rejected friend requests.

| Column Name    | Type          | Constraints           | Description                    |
|----------------|---------------|-----------------------|--------------------------------|
| id             | BIGINT        | PRIMARY KEY, AUTO     | Unique request identifier      |
| requester_id   | BIGINT        | FOREIGN KEY → users, NOT NULL | User who sent request |
| recipient_id   | BIGINT        | FOREIGN KEY → users, NOT NULL | User who receives    |
| status         | VARCHAR(20)   | NOT NULL              | PENDING, ACCEPTED, REJECTED    |
| created_at     | TIMESTAMP     | NOT NULL              | Request creation time          |
| responded_at   | TIMESTAMP     |                       | Response time                  |

**Unique Constraint:** (requester_id, recipient_id) - prevents duplicate requests

**Java Entity:** `src/main/java/org/example/chat/entity/FriendRequest.java`

#### Table 6: `room_memberships`
Stores which users are members of which rooms.

| Column Name    | Type          | Constraints           | Description                    |
|----------------|---------------|-----------------------|--------------------------------|
| id             | BIGINT        | PRIMARY KEY, AUTO     | Unique membership identifier   |
| user_id        | BIGINT        | FOREIGN KEY → users, NOT NULL | Member user ID      |
| chat_room_id   | BIGINT        | FOREIGN KEY → chat_rooms, NOT NULL | Room ID         |
| joined_at      | TIMESTAMP     | NOT NULL              | Join timestamp                 |
| role           | VARCHAR(20)   |                       | OWNER, MODERATOR, MEMBER       |

**Unique Constraint:** (user_id, chat_room_id) - user can only join a room once

**Java Entity:** `src/main/java/org/example/chat/entity/RoomMembership.java`

### 3. How to Connect to the Database

#### Option A: Using psql (PostgreSQL Command Line)

**Step 1:** Install PostgreSQL client tools if not already installed
- Windows: Download from https://www.postgresql.org/download/windows/
- Mac: `brew install postgresql`
- Linux: `sudo apt-get install postgresql-client`

**Step 2:** Connect to the database
```bash
# Local development database
psql -h localhost -p 5432 -U chatuser -d chatdb

# You'll be prompted for password: chatpass
```

**Step 3:** You're now in the PostgreSQL interactive terminal!

#### Option B: Using pgAdmin (GUI Tool)

1. Download pgAdmin from https://www.pgadmin.org/
2. Create a new server connection:
   - Host: localhost
   - Port: 5432
   - Database: chatdb
   - Username: chatuser
   - Password: chatpass

#### Option C: Using IntelliJ IDEA Database Tools

1. Open IntelliJ IDEA
2. View → Tool Windows → Database
3. Click "+" → Data Source → PostgreSQL
4. Enter connection details:
   - Host: localhost
   - Port: 5432
   - Database: chatdb
   - User: chatuser
   - Password: chatpass
5. Test Connection → OK

### 4. Essential SQL Commands

#### Viewing Tables

```sql
-- List all tables in the database
\dt

-- Or using SQL
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public';
```

#### Viewing Table Structure

```sql
-- Describe a table (shows columns, types, constraints)
\d users

-- Or using SQL
SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'users'
ORDER BY ordinal_position;
```

#### Viewing Data (SELECT Queries)

```sql
-- View all users
SELECT * FROM users;

-- View specific columns
SELECT id, username, email, online FROM users;

-- View with conditions
SELECT * FROM users WHERE online = true;

-- View with sorting
SELECT * FROM users ORDER BY created_at DESC;

-- View with limit
SELECT * FROM users LIMIT 10;

-- Count rows
SELECT COUNT(*) FROM users;

-- View recent messages in a room
SELECT m.id, u.username, m.content, m.timestamp
FROM messages m
JOIN users u ON m.sender_id = u.id
WHERE m.chat_room_id = 1
ORDER BY m.timestamp DESC
LIMIT 50;

-- View all chat rooms with member counts
SELECT cr.id, cr.name, cr.description, COUNT(rm.id) as member_count
FROM chat_rooms cr
LEFT JOIN room_memberships rm ON cr.id = rm.chat_room_id
GROUP BY cr.id, cr.name, cr.description
ORDER BY member_count DESC;

-- View friendships for a specific user
SELECT 
    CASE 
        WHEN f.user_a_id = 1 THEN u2.username
        ELSE u1.username
    END as friend_username
FROM friendships f
JOIN users u1 ON f.user_a_id = u1.id
JOIN users u2 ON f.user_b_id = u2.id
WHERE f.user_a_id = 1 OR f.user_b_id = 1;
```

#### Inserting Data

```sql
-- Insert a new user (password should be hashed in real application)
INSERT INTO users (username, email, password_hash, display_name, created_at, online)
VALUES ('testuser', 'test@example.com', '$2a$10$hashedpassword', 'Test User', NOW(), false);

-- Insert a chat room
INSERT INTO chat_rooms (name, description, created_at, created_by_id)
VALUES ('General Chat', 'A room for general discussion', NOW(), 1);

-- Insert a message
INSERT INTO messages (sender_id, chat_room_id, content, timestamp, message_type)
VALUES (1, 1, 'Hello, world!', NOW(), 'TEXT');
```

#### Updating Data

```sql
-- Update user online status
UPDATE users SET online = true, last_seen = NOW() WHERE id = 1;

-- Update room description
UPDATE chat_rooms SET description = 'New description' WHERE id = 1;

-- Update friend request status
UPDATE friend_requests 
SET status = 'ACCEPTED', responded_at = NOW() 
WHERE id = 1;
```

#### Deleting Data

```sql
-- Delete a specific message
DELETE FROM messages WHERE id = 100;

-- Delete old messages (older than 30 days)
DELETE FROM messages WHERE timestamp < NOW() - INTERVAL '30 days';

-- Delete a user (careful with foreign key constraints!)
DELETE FROM users WHERE id = 5;
```

#### Advanced Queries

```sql
-- Find users with most messages
SELECT u.username, COUNT(m.id) as message_count
FROM users u
LEFT JOIN messages m ON u.id = m.sender_id
GROUP BY u.id, u.username
ORDER BY message_count DESC
LIMIT 10;

-- Find most active chat rooms
SELECT cr.name, COUNT(m.id) as message_count
FROM chat_rooms cr
LEFT JOIN messages m ON cr.id = m.chat_room_id
GROUP BY cr.id, cr.name
ORDER BY message_count DESC;

-- Find pending friend requests for a user
SELECT u.username as requester, fr.created_at
FROM friend_requests fr
JOIN users u ON fr.requester_id = u.id
WHERE fr.recipient_id = 1 AND fr.status = 'PENDING'
ORDER BY fr.created_at DESC;
```

### 5. Viewing SQL Queries Generated by Hibernate

To see what SQL queries your Java code generates:

**Step 1:** Edit `src/main/resources/application.yml`

```yaml
spring:
  jpa:
    show-sql: true  # Change from false to true
    properties:
      hibernate:
        format_sql: true  # Makes SQL readable
```

**Step 2:** Run the application

**Step 3:** Watch the console/logs - you'll see SQL like:

```sql
Hibernate: 
    select
        u1_0.id,
        u1_0.created_at,
        u1_0.display_name,
        u1_0.email,
        u1_0.last_seen,
        u1_0.online,
        u1_0.password_hash,
        u1_0.username 
    from
        users u1_0 
    where
        u1_0.username=?
```

### 6. Database Migrations and Schema Updates

The application uses `hibernate.ddl-auto: update` which means:

- **On first run:** Hibernate creates all tables automatically
- **On subsequent runs:** Hibernate adds new columns/tables if entities change
- **Warning:** It does NOT remove columns or handle complex migrations

**For production**, you should use:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Only validates, doesn't change schema
```

And use a migration tool like **Flyway** or **Liquibase** for controlled schema changes.

### 7. Useful Database Maintenance Commands

```sql
-- Check database size
SELECT pg_size_pretty(pg_database_size('chatdb'));

-- Check table sizes
SELECT 
    table_name,
    pg_size_pretty(pg_total_relation_size(quote_ident(table_name))) as size
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY pg_total_relation_size(quote_ident(table_name)) DESC;

-- View active connections
SELECT * FROM pg_stat_activity WHERE datname = 'chatdb';

-- Vacuum (clean up) the database
VACUUM ANALYZE;

-- Check for missing indexes
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats
WHERE schemaname = 'public'
ORDER BY n_distinct DESC;
```

## Why This Approach

### Why PostgreSQL?
- **ACID compliance**: Ensures data consistency for critical operations like friend requests
- **JSON support**: Can store complex data if needed
- **Full-text search**: Useful for searching messages and users
- **Mature and reliable**: Battle-tested in production environments

### Why JPA/Hibernate?
- **Object-Relational Mapping**: Write Java code instead of SQL
- **Type safety**: Compile-time checking of database operations
- **Automatic schema generation**: Speeds up development
- **Database independence**: Can switch databases with minimal code changes

### Why These Table Relationships?
- **users ↔ messages**: One-to-many (one user sends many messages)
- **chat_rooms ↔ messages**: One-to-many (one room contains many messages)
- **users ↔ room_memberships ↔ chat_rooms**: Many-to-many through join table
- **users ↔ friendships**: Many-to-many (users can have multiple friends)
- **users ↔ friend_requests**: Tracks the request lifecycle before friendship

## Alternatives Considered

### Alternative 1: NoSQL Database (MongoDB)
**Pros:**
- Flexible schema
- Good for rapid prototyping
- Built-in horizontal scaling

**Cons:**
- No ACID transactions across collections
- Complex queries are harder
- Relationships require manual management

**Why not chosen:** Chat applications need strong consistency for friend requests and room memberships.

### Alternative 2: In-Memory Database (H2, Redis)
**Pros:**
- Extremely fast
- Simple setup

**Cons:**
- Data lost on restart (unless configured for persistence)
- Limited query capabilities
- Not suitable for production chat history

**Why not chosen:** We need persistent storage for messages and user data.

### Alternative 3: Direct JDBC (No ORM)
**Pros:**
- Full control over SQL
- Potentially better performance
- No "magic" behavior

**Cons:**
- Much more boilerplate code
- Manual mapping between objects and tables
- Harder to maintain

**Why not chosen:** JPA provides good balance of productivity and performance.

## Key Concepts

### 1. Primary Keys
Every table has an `id` column that uniquely identifies each row. It's auto-generated using `IDENTITY` strategy.

### 2. Foreign Keys
Columns like `sender_id`, `chat_room_id` reference the `id` of another table, creating relationships.

### 3. Indexes
Special data structures that speed up queries. The `messages` table has an index on `(chat_room_id, timestamp)` to quickly fetch recent messages.

### 4. Unique Constraints
Prevent duplicate data. For example, `(user_a_id, user_b_id)` in friendships ensures you can't be friends with someone twice.

### 5. Cascade Operations
When you delete a user, what happens to their messages? The `@OneToMany(cascade = CascadeType.ALL)` annotation controls this.

### 6. Transactions
Database operations are wrapped in transactions - either all succeed or all fail. This prevents partial updates.

### 7. Connection Pooling
HikariCP maintains a pool of database connections (5-20) to avoid the overhead of creating new connections for each request.

### 8. N+1 Query Problem
If you fetch 10 users and then fetch their messages, that's 1 + 10 = 11 queries. Use `JOIN FETCH` to solve this:

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.messages WHERE u.id = :id")
User findUserWithMessages(@Param("id") Long id);
```

## Potential Pitfalls

### 1. Forgetting to Close Connections
**Problem:** If you use raw JDBC, forgetting to close connections causes connection pool exhaustion.

**Solution:** Use try-with-resources or let Spring Data JPA handle it:
```java
try (Connection conn = dataSource.getConnection()) {
    // Use connection
} // Automatically closed
```

### 2. SQL Injection
**Problem:** Building SQL with string concatenation allows attackers to inject malicious SQL.

**Bad:**
```java
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
```

**Good:** Use parameterized queries (JPA does this automatically):
```java
@Query("SELECT u FROM User u WHERE u.username = :username")
User findByUsername(@Param("username") String username);
```

### 3. Lazy Loading Exceptions
**Problem:** Accessing a lazy-loaded relationship outside a transaction throws `LazyInitializationException`.

**Solution:** Use `@Transactional` or fetch eagerly:
```java
@Transactional
public List<Message> getUserMessages(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();
    return user.getMessages(); // Works because we're in a transaction
}
```

### 4. Cascade Delete Disasters
**Problem:** Deleting a user cascades to delete all their messages, which might not be desired.

**Solution:** Use `CascadeType.PERSIST` and `CascadeType.MERGE` instead of `ALL`, or set messages to null before deleting user.

### 5. Not Using Indexes
**Problem:** Queries on large tables without indexes are extremely slow.

**Solution:** Add indexes on frequently queried columns:
```java
@Table(name = "messages", indexes = {
    @Index(name = "idx_room_timestamp", columnList = "chat_room_id,timestamp")
})
```

### 6. Storing Passwords in Plain Text
**Problem:** If the database is compromised, all passwords are exposed.

**Solution:** Always hash passwords (this app uses BCrypt):
```java
String hashedPassword = passwordEncoder.encode(rawPassword);
```

### 7. Not Handling Null Values
**Problem:** Querying for null values requires special syntax.

**Wrong:** `WHERE last_seen = NULL`
**Right:** `WHERE last_seen IS NULL`

### 8. Time Zone Issues
**Problem:** `LocalDateTime` doesn't store time zone information.

**Solution:** Use `Instant` or `ZonedDateTime` for absolute timestamps, or ensure all servers use UTC.

## What You Learned

1. **Database Structure**: This chat application uses 6 tables (users, chat_rooms, messages, friendships, friend_requests, room_memberships) with clear relationships.

2. **Connection Methods**: You can connect to PostgreSQL using psql command line, pgAdmin GUI, or IntelliJ IDEA's database tools.

3. **Essential SQL Commands**:
   - `SELECT` to query data
   - `INSERT` to add data
   - `UPDATE` to modify data
   - `DELETE` to remove data
   - `JOIN` to combine tables
   - `GROUP BY` and `COUNT` for aggregations

4. **JPA/Hibernate**: The application uses ORM to map Java objects to database tables automatically, reducing boilerplate code.

5. **Configuration**: Database settings are in `application.yml`, including connection details, pool size, and Hibernate behavior.

6. **Debugging**: Enable `show-sql: true` to see generated SQL queries in logs.

7. **Schema Management**: `ddl-auto: update` automatically creates/updates tables during development, but production should use `validate` with migration tools.

8. **Performance**: Indexes, connection pooling, and avoiding N+1 queries are crucial for good performance.

9. **Security**: Always use parameterized queries, hash passwords, and be careful with cascade deletes.

10. **Relationships**: Understanding foreign keys, join tables, and cascade operations is essential for working with relational databases.

**Next Steps:**
- Try connecting to the database and running some SELECT queries
- Experiment with JOIN queries to see how tables relate
- Enable `show-sql: true` and watch what SQL your Java code generates
- Try creating a simple query in a repository interface
- Learn about database migrations with Flyway or Liquibase for production use
