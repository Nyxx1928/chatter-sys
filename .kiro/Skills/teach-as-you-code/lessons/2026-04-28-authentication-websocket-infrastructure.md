# Lesson: Building Authentication and WebSocket Infrastructure for Real-Time Chat

## Task Context

This lesson covers the implementation of tasks 4-6.3 from the realtime-chat-system spec, focusing on building the authentication and WebSocket infrastructure. These tasks establish the security foundation and real-time communication capabilities for the chat application.

**Tasks Completed:**
- Task 4.1: JWT utility class for token generation and validation
- Task 4.2: AuthenticationService for user registration and login
- Task 4.3: Spring Security configuration
- Task 4.4: REST controllers for authentication
- Task 6.1: WebSocketConfig class for STOMP messaging
- Task 6.2: WebSocket authentication interceptor
- Task 6.3: WebSocket event listener for connection lifecycle

## Files Modified

### Created Files:
- `src/main/java/org/example/chat/security/JwtUtil.java` (created)
- `src/test/java/org/example/chat/security/JwtUtilTest.java` (created)
- `src/main/java/org/example/chat/security/JwtAuthenticationFilter.java` (created)
- `src/main/java/org/example/chat/security/SecurityConfig.java` (created)
- `src/main/java/org/example/chat/security/CustomUserDetailsService.java` (created)
- `src/test/java/org/example/chat/security/SecurityConfigTest.java` (created)
- `src/main/java/org/example/chat/dto/RegisterRequest.java` (created)
- `src/main/java/org/example/chat/dto/LoginRequest.java` (created)
- `src/main/java/org/example/chat/dto/LoginResponse.java` (created)
- `src/main/java/org/example/chat/dto/UserResponse.java` (created)
- `src/main/java/org/example/chat/dto/UpdateProfileRequest.java` (created)
- `src/main/java/org/example/chat/controller/AuthController.java` (created)
- `src/main/java/org/example/chat/controller/UserController.java` (created)
- `src/test/java/org/example/chat/controller/AuthControllerTest.java` (created)
- `src/test/java/org/example/chat/controller/UserControllerTest.java` (created)
- `src/main/java/org/example/chat/exception/ErrorResponse.java` (created)
- `src/main/java/org/example/chat/exception/GlobalExceptionHandler.java` (created)
- `src/main/java/org/example/chat/controller/README.md` (created)
- `src/main/java/org/example/chat/config/WebSocketConfig.java` (created)
- `src/test/java/org/example/chat/config/WebSocketConfigTest.java` (created)
- `src/main/java/org/example/chat/security/WebSocketAuthenticationInterceptor.java` (created)
- `src/test/java/org/example/chat/security/WebSocketAuthenticationInterceptorTest.java` (created)
- `src/main/java/org/example/chat/listener/WebSocketEventListener.java` (created)
- `src/test/java/org/example/chat/listener/WebSocketEventListenerTest.java` (created)

### Modified Files:
- `src/main/java/org/example/chat/service/AuthenticationService.java` (modified)
- `src/test/java/org/example/chat/service/AuthenticationServiceTest.java` (modified)
- `src/main/java/org/example/chat/repository/RoomMembershipRepository.java` (modified)

## Step-by-Step Changes

### Phase 1: JWT Token Management (Task 4.1)

**What we built:** A utility class to generate, validate, and extract information from JWT tokens.

**Key components:**
1. **JwtUtil class** with three core methods:
   - `generateToken(String username)`: Creates a JWT with username as subject, issued-at timestamp, and expiration time
   - `validateToken(String token)`: Verifies JWT signature and expiration, returns boolean
   - `getUsernameFromToken(String token)`: Extracts username from token claims

2. **Configuration integration**: Reads `jwt.secret` and `jwt.expiration` from application.yml

3. **Security features**:
   - Uses HMAC-SHA algorithm for signing
   - Handles all JWT exceptions gracefully (expired, malformed, invalid signature)
   - Comprehensive logging for debugging

### Phase 2: Authentication Service (Task 4.2)

**What we built:** A service layer for user registration and login with secure password handling.

**Key components:**
1. **AuthenticationService** with core methods:
   - `registerUser()`: Validates input, checks for duplicates, hashes password with BCrypt, persists user
   - `authenticateUser()`: Validates credentials, verifies password hash, returns JWT token
   - `getUserByUsername()`: Retrieves user by username
   - `updateUserProfile()`: Updates user email and display name

2. **Validation logic**:
   - Username: 3-50 characters, unique
   - Email: Valid format, max 100 characters, unique
   - Password: Minimum 8 characters, hashed with BCrypt
   - Display name: 1-100 characters

3. **Security best practices**:
   - BCryptPasswordEncoder for password hashing (injected as Spring bean)
   - Generic error messages for authentication failures (don't reveal if username exists)
   - Transaction management with @Transactional

### Phase 3: Spring Security Configuration (Task 4.3)

**What we built:** Complete Spring Security setup with JWT authentication and CORS support.

**Key components:**
1. **SecurityConfig** class:
   - Permits public access to `/api/auth/register`, `/api/auth/login`, and `/ws/**`
   - Requires authentication for all other endpoints
   - Stateless session management (no server-side sessions)
   - CSRF disabled (not needed for JWT)
   - CORS configured for `http://localhost:3000` (frontend origin)

2. **JwtAuthenticationFilter**:
   - Extends `OncePerRequestFilter` to run once per request
   - Extracts JWT from `Authorization: Bearer <token>` header
   - Validates token and loads user details
   - Sets authentication in SecurityContext

3. **CustomUserDetailsService**:
   - Implements Spring Security's `UserDetailsService`
   - Loads user from database by username
   - Converts domain User entity to Spring Security UserDetails

### Phase 4: REST API Controllers (Task 4.4)

**What we built:** RESTful endpoints for authentication and user profile management.

**Key components:**
1. **AuthController** endpoints:
   - `POST /api/auth/register`: User registration with validation
   - `POST /api/auth/login`: User login returning JWT token and user info

2. **UserController** endpoints (protected):
   - `GET /api/users/me`: Get current authenticated user's profile
   - `PUT /api/users/me`: Update current user's profile

3. **DTO classes** for clean API contracts:
   - RegisterRequest, LoginRequest, UpdateProfileRequest (input)
   - LoginResponse, UserResponse (output)
   - ErrorResponse (error handling)

4. **GlobalExceptionHandler**:
   - Handles validation errors (MethodArgumentNotValidException)
   - Handles business logic errors (IllegalArgumentException)
   - Provides consistent error response format

### Phase 5: WebSocket Configuration (Task 6.1)

**What we built:** STOMP over WebSocket configuration for real-time messaging.

**Key components:**
1. **WebSocketConfig** class:
   - Registers STOMP endpoint at `/ws` with SockJS fallback
   - Enables simple in-memory message broker with `/topic` and `/queue` prefixes
   - Sets application destination prefix to `/app`
   - Sets user destination prefix to `/user`
   - Configures CORS for frontend origin

2. **Message routing**:
   - Client sends to `/app/...` → routed to @MessageMapping methods
   - Server publishes to `/topic/...` → broadcast to all subscribers
   - Server publishes to `/queue/...` → point-to-point messaging
   - Server publishes to `/user/...` → user-specific messages

### Phase 6: WebSocket Authentication (Task 6.2)

**What we built:** JWT-based authentication for WebSocket connections.

**Key components:**
1. **WebSocketAuthenticationInterceptor**:
   - Implements `ChannelInterceptor` to intercept STOMP frames
   - Extracts JWT from `Authorization` header in STOMP CONNECT frames
   - Validates token using JwtUtil
   - Sets authenticated user principal in STOMP session
   - Rejects connections with invalid/missing tokens (returns null)

2. **Integration with WebSocketConfig**:
   - Interceptor registered in `configureClientInboundChannel()`
   - Runs before any STOMP message processing

### Phase 7: Connection Lifecycle Management (Task 6.3)

**What we built:** Event listeners to track user presence and broadcast updates.

**Key components:**
1. **WebSocketEventListener** with event handlers:
   - `handleWebSocketConnectListener()`: Marks user online, updates lastSeen, publishes presence
   - `handleWebSocketDisconnectListener()`: Marks user offline, updates lastSeen, publishes presence

2. **Presence broadcasting**:
   - Finds all rooms user is a member of
   - Publishes presence update to `/topic/presence/{roomId}` for each room
   - Includes userId, username, displayName, online status, lastSeen, roomId

3. **Database updates**:
   - Updates User.online and User.lastSeen fields
   - Persists changes to database

## Why This Approach

### JWT for Authentication
**Why JWT over sessions?**
- Stateless: No server-side session storage needed
- Scalable: Works across multiple server instances
- Mobile-friendly: Easy to store and send from mobile apps
- WebSocket-compatible: Can be sent in STOMP headers

**Why HMAC-SHA signing?**
- Symmetric key algorithm (simpler than RSA for single-server apps)
- Fast and secure for token validation
- Sufficient for this learning project's security needs

### BCrypt for Password Hashing
**Why BCrypt over plain hashing?**
- Adaptive: Can increase work factor as hardware improves
- Salt included: Each password gets unique salt automatically
- Slow by design: Makes brute-force attacks impractical
- Industry standard: Well-tested and widely trusted

### Spring Security Integration
**Why Spring Security over custom auth?**
- Battle-tested: Handles edge cases and security vulnerabilities
- Standardized: Uses industry-standard patterns (filters, authentication managers)
- Extensible: Easy to add OAuth2, LDAP, etc. later
- Integration: Works seamlessly with Spring Boot ecosystem

### STOMP over WebSocket
**Why STOMP instead of raw WebSocket?**
- Structured messaging: Provides pub/sub patterns out of the box
- Routing: Built-in support for topics and queues
- Interoperability: Standard protocol with client libraries for many languages
- Spring integration: First-class support in Spring Framework

### Event-Driven Presence Tracking
**Why event listeners over polling?**
- Real-time: Immediate updates when users connect/disconnect
- Efficient: No unnecessary database queries
- Decoupled: Presence logic separated from connection logic
- Scalable: Events can be processed asynchronously

## Alternatives Considered

### Alternative 1: Session-Based Authentication
**What:** Store user sessions in server memory or Redis
**Pros:** 
- Simpler to implement initially
- Easy to invalidate sessions server-side
- No token expiration concerns

**Cons:**
- Requires server-side state (not stateless)
- Harder to scale horizontally
- Doesn't work well with WebSocket across multiple servers
- More complex for mobile apps

**Why we chose JWT:** Better fit for real-time applications and future scalability

### Alternative 2: OAuth2 with External Provider
**What:** Use Google, GitHub, or other OAuth2 providers for authentication
**Pros:**
- No password management needed
- Users can use existing accounts
- Reduced security liability

**Cons:**
- More complex setup
- Requires internet connectivity
- Users must have accounts with provider
- Not suitable for learning project focused on fundamentals

**Why we chose custom auth:** Better for learning authentication fundamentals

### Alternative 3: Raw WebSocket Instead of STOMP
**What:** Use WebSocket API directly without STOMP protocol
**Pros:**
- Lower overhead (no STOMP frame parsing)
- More control over message format
- Simpler for very basic use cases

**Cons:**
- Must implement routing, pub/sub, and queuing manually
- No standard message format
- Harder to integrate with Spring's messaging infrastructure
- More code to maintain

**Why we chose STOMP:** Provides essential messaging patterns out of the box

### Alternative 4: Polling for Presence Updates
**What:** Frontend polls `/api/users/online` endpoint every few seconds
**Pros:**
- Simpler to implement
- Works without WebSocket
- Easier to debug

**Cons:**
- Inefficient (many unnecessary requests)
- Delayed updates (depends on polling interval)
- Higher server load
- Not truly real-time

**Why we chose event-driven:** True real-time updates with minimal overhead

## Key Concepts

### 1. JWT (JSON Web Token)
A compact, URL-safe token format consisting of three parts:
- **Header**: Algorithm and token type
- **Payload**: Claims (data) like username, expiration
- **Signature**: Cryptographic signature to verify authenticity

**Structure:** `header.payload.signature` (base64url encoded)

**Example:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTYxNjIzOTAyMiwiZXhwIjoxNjE2MzI1NDIyfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### 2. Spring Security Filter Chain
A series of filters that process HTTP requests before they reach controllers:
1. **SecurityContextPersistenceFilter**: Loads security context
2. **JwtAuthenticationFilter** (custom): Validates JWT tokens
3. **UsernamePasswordAuthenticationFilter**: Handles form login (disabled for us)
4. **ExceptionTranslationFilter**: Handles security exceptions
5. **FilterSecurityInterceptor**: Enforces authorization rules

### 3. STOMP Protocol
Simple Text Oriented Messaging Protocol - a messaging protocol on top of WebSocket:

**Key commands:**
- `CONNECT`: Establish connection
- `SUBSCRIBE`: Subscribe to destination
- `SEND`: Send message to destination
- `MESSAGE`: Receive message from subscription
- `DISCONNECT`: Close connection

**Example STOMP frame:**
```
SEND
destination:/app/chat.send/1
content-type:application/json

{"content":"Hello, world!"}
```

### 4. Pub/Sub Messaging Pattern
**Publisher-Subscriber pattern** for message distribution:
- **Publisher**: Sends messages to a topic (e.g., `/topic/room/1`)
- **Broker**: Routes messages to subscribers
- **Subscribers**: Receive messages from topics they're subscribed to

**Benefits:**
- Decoupling: Publishers don't know about subscribers
- Scalability: Multiple subscribers can receive same message
- Flexibility: Easy to add/remove subscribers

### 5. DTO (Data Transfer Object) Pattern
Objects designed specifically for transferring data between layers:
- **Purpose**: Decouple API contracts from domain models
- **Benefits**: 
  - Hide sensitive data (e.g., password hashes)
  - Control what data is exposed in API
  - Add validation annotations
  - Version API independently from domain

**Example:**
```java
// Domain model (internal)
class User {
    private String passwordHash; // Sensitive!
}

// DTO (external API)
class UserResponse {
    // No passwordHash field - hidden from API
}
```

### 6. BCrypt Password Hashing
Adaptive hashing algorithm designed for passwords:
- **Work factor**: Controls computation time (default: 10)
- **Salt**: Random value added to password before hashing
- **Output format**: `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`
  - `$2a$`: BCrypt version
  - `10$`: Work factor
  - Next 22 chars: Salt
  - Remaining chars: Hash

### 7. CORS (Cross-Origin Resource Sharing)
Security mechanism that allows/restricts cross-origin HTTP requests:
- **Same-origin policy**: Browsers block requests to different origins by default
- **CORS headers**: Server tells browser which origins are allowed
- **Preflight requests**: Browser sends OPTIONS request before actual request

**Our configuration:**
- Allowed origin: `http://localhost:3000` (frontend)
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
- Allowed headers: All
- Credentials: Enabled (for cookies and auth headers)

## Potential Pitfalls

### 1. JWT Token Expiration Handling
**Problem:** Tokens expire after 24 hours, causing authentication failures.

**Symptoms:**
- Users suddenly logged out
- API requests return 401 Unauthorized
- WebSocket connections rejected

**Solutions:**
- Implement token refresh mechanism (refresh tokens)
- Show user-friendly "session expired" message
- Automatically redirect to login page
- Store token expiration time in frontend and refresh proactively

### 2. CORS Configuration Errors
**Problem:** Frontend can't connect to backend due to CORS restrictions.

**Symptoms:**
- Browser console shows CORS errors
- Requests blocked by browser
- WebSocket connections fail

**Common mistakes:**
- Forgetting to allow credentials
- Not including OPTIONS method
- Wrong origin URL (http vs https, port mismatch)
- CORS config not applied to WebSocket endpoint

**Solution:** Verify CORS configuration matches frontend URL exactly

### 3. WebSocket Authentication Timing
**Problem:** WebSocket connection established before JWT token is available.

**Symptoms:**
- Connection rejected with authentication error
- User logged in but can't connect to WebSocket

**Solution:**
- Ensure frontend waits for login to complete before connecting
- Store JWT token before initiating WebSocket connection
- Implement retry logic with exponential backoff

### 4. Password Validation Inconsistency
**Problem:** Frontend and backend have different password requirements.

**Symptoms:**
- Frontend allows passwords that backend rejects
- Confusing error messages for users

**Solution:**
- Document password requirements clearly
- Keep validation rules in sync between frontend and backend
- Consider sharing validation rules via API endpoint

### 5. Presence Update Race Conditions
**Problem:** Multiple connect/disconnect events in quick succession cause incorrect presence state.

**Symptoms:**
- User shown as offline when they're online
- Presence updates out of order

**Solutions:**
- Use database transactions for presence updates
- Implement debouncing for rapid connect/disconnect
- Add sequence numbers to presence updates
- Consider using Redis for presence state (faster than database)

### 6. Memory Leaks in WebSocket Connections
**Problem:** WebSocket connections not properly closed, consuming server resources.

**Symptoms:**
- Server memory usage grows over time
- Connection limit reached
- Slow performance

**Solutions:**
- Ensure disconnect events are handled properly
- Implement connection timeout
- Monitor active connections
- Add health check endpoint

### 7. JWT Secret Key Security
**Problem:** JWT secret key exposed or too weak.

**Symptoms:**
- Tokens can be forged by attackers
- Security breach

**Solutions:**
- Use strong, randomly generated secret (minimum 256 bits)
- Store secret in environment variables, not in code
- Rotate secret keys periodically
- Use different secrets for dev/staging/production

### 8. Exception Handling in Event Listeners
**Problem:** Unhandled exceptions in event listeners crash the application.

**Symptoms:**
- Server crashes on connect/disconnect
- Presence updates stop working

**Solutions:**
- Wrap event listener logic in try-catch blocks
- Log exceptions with full stack traces
- Return gracefully from event handlers
- Implement circuit breaker pattern for repeated failures

## What You Learned

### Core Concepts Mastered
1. **JWT Authentication**: How to generate, validate, and use JWT tokens for stateless authentication
2. **Spring Security**: How to configure security filters, authentication, and authorization
3. **WebSocket with STOMP**: How to set up real-time bidirectional communication
4. **Event-Driven Architecture**: How to use Spring events for decoupled components
5. **DTO Pattern**: How to separate API contracts from domain models
6. **Password Security**: How to securely hash and verify passwords with BCrypt

### Technical Skills Developed
1. **Spring Boot Configuration**: Configuring security, WebSocket, and CORS
2. **REST API Design**: Creating clean, RESTful endpoints with proper HTTP methods and status codes
3. **Testing**: Writing unit tests with Mockito and MockMvc
4. **Error Handling**: Implementing global exception handlers for consistent error responses
5. **Dependency Injection**: Using Spring's DI container effectively
6. **Logging**: Adding appropriate logging for debugging and monitoring

### Architecture Patterns Applied
1. **Layered Architecture**: Separation of controllers, services, repositories
2. **Dependency Inversion**: Depending on interfaces, not implementations
3. **Single Responsibility**: Each class has one clear purpose
4. **Open/Closed Principle**: Easy to extend without modifying existing code
5. **Pub/Sub Pattern**: Decoupled message broadcasting with STOMP topics

### Real-World Skills
1. **Security Best Practices**: Implementing industry-standard authentication and authorization
2. **API Documentation**: Creating clear API documentation with examples
3. **Error Messages**: Providing user-friendly error messages without exposing sensitive information
4. **Scalability Considerations**: Designing stateless authentication for horizontal scaling
5. **Real-Time Communication**: Building responsive, real-time features

### Next Steps
Now that authentication and WebSocket infrastructure are complete, you can:
1. **Implement chat room management** (Task 8): Create, list, and manage chat rooms
2. **Build message handling** (Task 10): Send and receive messages via STOMP
3. **Add user presence tracking** (Task 12): Show online/offline status in UI
4. **Implement error handling** (Task 14): Add comprehensive error handling and logging
5. **Build the frontend** (Tasks 18-36): Create the Next.js frontend to consume these APIs

### Key Takeaways
- **Security is foundational**: Get authentication right before building features
- **Stateless is scalable**: JWT tokens enable horizontal scaling
- **Real-time requires infrastructure**: WebSocket and STOMP provide the foundation for real-time features
- **Testing is essential**: Unit tests catch bugs early and document expected behavior
- **Documentation matters**: Clear API docs help frontend developers integrate quickly
