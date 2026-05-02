# Lesson: Spring Security Authentication vs Authorization - Fixing 401 vs 403 Status Codes

## Task Context

We're fixing a bug in the chat application where unauthenticated requests to protected endpoints return **403 Forbidden** instead of **401 Unauthorized**. This violates HTTP semantics and causes integration tests to fail.

The bug affects endpoints like:
- `GET /api/rooms/{id}/messages`
- `POST /api/rooms`
- `GET /api/users/me`
- `PUT /api/users/me`

When a request arrives without authentication credentials (no JWT token), Spring Security's default behavior returns 403, which is incorrect. The correct response should be 401.

## Files Modified

- `src/main/java/org/example/chat/security/SecurityConfig.java` (modified)

## Step-by-Step Changes

### 1. Added Import for AuthenticationEntryPoint and HttpServletResponse

First, we imported the necessary classes:
```java
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
```

These imports give us access to:
- `AuthenticationEntryPoint`: The interface for handling authentication failures
- `HttpServletResponse`: To set the HTTP status code and write the response body

### 2. Created Custom AuthenticationEntryPoint Bean

We added a new bean method that creates a custom authentication entry point:

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

**What this does:**
- Returns a lambda implementation of `AuthenticationEntryPoint`
- Sets the HTTP status to 401 (SC_UNAUTHORIZED = 401)
- Sets the content type to JSON
- Writes a JSON error response with a clear message

**Why a lambda?** `AuthenticationEntryPoint` is a functional interface with a single method `commence()`, so we can implement it concisely with a lambda expression.

### 3. Configured SecurityFilterChain to Use Custom Entry Point

We modified the `securityFilterChain` method to register our custom entry point:

```java
.exceptionHandling(exception -> exception
    .authenticationEntryPoint(authenticationEntryPoint())
)
```

This configuration tells Spring Security: "When authentication fails (no credentials provided), use our custom entry point instead of the default one."

**Placement matters:** We added this configuration between the authorization rules and session management, keeping the configuration logically organized.

## Why This Approach

### HTTP Status Code Semantics

The HTTP specification defines clear meanings for authentication/authorization status codes:

- **401 Unauthorized**: "You need to authenticate first" (no credentials provided or invalid credentials)
- **403 Forbidden**: "I know who you are, but you don't have permission" (valid credentials but insufficient permissions)

Spring Security's default behavior conflates these two cases, returning 403 for both. This is confusing for API consumers and violates REST best practices.

### Custom AuthenticationEntryPoint

Spring Security provides the `AuthenticationEntryPoint` interface specifically for customizing authentication failure responses. This is the recommended approach rather than:
- Using a global exception handler (which runs after Spring Security)
- Modifying the JWT filter (which is for processing tokens, not handling failures)
- Using access denied handlers (which are for authorization failures, not authentication failures)

### JSON Response Format

We return a JSON error response because:
- The API is RESTful and all other responses are JSON
- Frontend clients expect consistent JSON error formats
- It provides a clear, machine-readable error message

## Alternatives Considered

### 1. Global Exception Handler

We could have used `@ControllerAdvice` to catch authentication exceptions:

```java
@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
    return ResponseEntity.status(401).body(new ErrorResponse("Unauthorized", "Authentication required"));
}
```

**Why we didn't:** Spring Security's authentication failures happen at the filter level, before requests reach controllers. The global exception handler wouldn't catch these cases.

### 2. Custom Filter

We could have created a custom filter to intercept authentication failures:

```java
public class AuthenticationFailureFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        // Custom logic
    }
}
```

**Why we didn't:** This is more complex and duplicates functionality that `AuthenticationEntryPoint` already provides. It's also harder to maintain and test.

### 3. Modify JwtAuthenticationFilter

We could have modified the existing JWT filter to return 401 when no token is present.

**Why we didn't:** The JWT filter's responsibility is to validate tokens when present, not to handle missing tokens. Separation of concerns keeps the code cleaner.

## Key Concepts

### 1. Authentication vs Authorization

**Authentication** answers: "Who are you?"
- Verifying identity (username/password, JWT token, API key)
- Result: You are user "john@example.com"

**Authorization** answers: "What are you allowed to do?"
- Checking permissions (roles, scopes, resource ownership)
- Result: You can read this room but not delete it

**In Spring Security:**
- Authentication failures → `AuthenticationEntryPoint` → 401 Unauthorized
- Authorization failures → `AccessDeniedHandler` → 403 Forbidden

### 2. Spring Security Filter Chain

Spring Security processes requests through a chain of filters:

1. **CorsFilter**: Handles CORS preflight requests
2. **CsrfFilter**: Validates CSRF tokens (disabled in our case)
3. **JwtAuthenticationFilter**: Extracts and validates JWT tokens (our custom filter)
4. **UsernamePasswordAuthenticationFilter**: Handles form login (not used in our API)
5. **ExceptionTranslationFilter**: Catches authentication/authorization exceptions
6. **FilterSecurityInterceptor**: Enforces authorization rules

When authentication fails, the `ExceptionTranslationFilter` catches the exception and delegates to the configured `AuthenticationEntryPoint`.

### 3. Functional Interfaces and Lambdas

`AuthenticationEntryPoint` is a functional interface:

```java
@FunctionalInterface
public interface AuthenticationEntryPoint {
    void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) 
        throws IOException, ServletException;
}
```

Because it has only one abstract method, we can implement it with a lambda:

```java
(request, response, authException) -> {
    // Implementation
}
```

This is more concise than creating a separate class:

```java
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        // Implementation
    }
}
```

### 4. Bean Configuration

The `@Bean` annotation tells Spring to manage this object:

```java
@Bean
public AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) -> { ... };
}
```

Spring will:
- Create the bean at startup
- Make it available for dependency injection
- Use it when we reference `authenticationEntryPoint()` in the security configuration

## Potential Pitfalls

### 1. Order of Configuration Matters

If you configure exception handling AFTER adding filters, it might not work correctly:

```java
// WRONG ORDER
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
.exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint()))
```

**Why?** Spring Security builds the filter chain in the order you configure it. Exception handling should be configured before filters that might throw exceptions.

**Correct order:**
```java
.authorizeHttpRequests(...)
.exceptionHandling(...)  // Configure exception handling
.sessionManagement(...)
.addFilterBefore(...)    // Then add filters
```

### 2. Don't Forget Content-Type

If you forget to set the content type:

```java
response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
response.getWriter().write("{\"error\":\"Unauthorized\"}");  // Missing content type!
```

The browser/client might not parse the JSON correctly, treating it as plain text.

**Always set:**
```java
response.setContentType("application/json");
```

### 3. AuthenticationEntryPoint vs AccessDeniedHandler

Don't confuse these two:

- **AuthenticationEntryPoint**: For authentication failures (401)
  - No credentials provided
  - Invalid credentials
  - Expired credentials

- **AccessDeniedHandler**: For authorization failures (403)
  - Valid credentials but insufficient permissions
  - User is authenticated but not authorized for this resource

If you use `AuthenticationEntryPoint` for authorization failures, you'll get the wrong status code.

### 4. Exception Handling in Filters

Exceptions thrown in filters don't automatically reach `@ControllerAdvice` handlers. That's why we need `AuthenticationEntryPoint` - it's specifically designed to handle exceptions at the filter level.

### 5. Testing Authentication Failures

When testing, make sure to test WITHOUT authentication headers:

```java
// WRONG - This tests authorization, not authentication
mockMvc.perform(get("/api/rooms/1/messages")
    .header("Authorization", "Bearer invalid-token"))
    .andExpect(status().isUnauthorized());

// RIGHT - This tests authentication
mockMvc.perform(get("/api/rooms/1/messages"))  // No Authorization header
    .andExpect(status().isUnauthorized());
```

## What You Learned

1. **HTTP Status Code Semantics**: 401 means "authenticate first", 403 means "you're authenticated but not authorized"

2. **Spring Security Architecture**: Authentication failures are handled by `AuthenticationEntryPoint`, not global exception handlers

3. **Functional Interfaces**: `AuthenticationEntryPoint` can be implemented concisely with a lambda expression

4. **Security Configuration**: The `exceptionHandling()` method configures how Spring Security handles authentication and authorization failures

5. **Filter Chain Order**: Exception handling configuration should come before filter registration in the security configuration

6. **JSON Error Responses**: Always set `Content-Type: application/json` when returning JSON error responses

7. **Separation of Concerns**: Authentication entry points handle authentication failures, access denied handlers handle authorization failures

This fix ensures our API follows HTTP standards and provides clear, consistent error responses to clients.
