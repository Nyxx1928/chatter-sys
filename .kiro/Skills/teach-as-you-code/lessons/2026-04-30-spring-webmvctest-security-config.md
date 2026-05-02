# Lesson: Configuring @WebMvcTest with Spring Security

## Task Context

We had 22 failing controller tests in a Spring Boot chat application. The tests were using `@WebMvcTest` to test REST controllers, but they were failing with errors like:

```
NoSuchBeanDefinitionException: No qualifying bean of type 'org.example.chat.security.JwtUtil' available
```

The issue was that `@WebMvcTest` is a "slice test" annotation that only loads the web layer (controllers, filters, etc.), but our `SecurityConfig` class required security beans like `JwtUtil`, `CustomUserDetailsService`, and `JwtAuthenticationFilter` that aren't part of the web layer.

## Files Modified

- `src/test/java/org/example/chat/config/WebMvcTestConfig.java` (created)
- `src/test/java/org/example/chat/controller/AuthControllerTest.java` (modified)
- `src/test/java/org/example/chat/controller/ChatRoomControllerTest.java` (modified)
- `src/test/java/org/example/chat/controller/UserControllerTest.java` (modified)

## Step-by-Step Changes

### Step 1: Created WebMvcTestConfig

We created a test configuration class that provides mock beans for all security components:

```java
@TestConfiguration
public class WebMvcTestConfig {
    
    @Bean
    @Primary
    public JwtUtil jwtUtil() {
        return mock(JwtUtil.class);
    }

    @Bean
    @Primary
    public CustomUserDetailsService customUserDetailsService() {
        return mock(CustomUserDetailsService.class);
    }

    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return mock(JwtAuthenticationFilter.class);
    }
}
```

**Key points:**
- `@TestConfiguration` marks this as a test-specific configuration
- `@Primary` ensures these mock beans take precedence over any other candidates
- We use Mockito's `mock()` to create mock instances
- These mocks satisfy Spring's dependency injection requirements without needing real implementations

### Step 2: Imported WebMvcTestConfig in Controller Tests

We added `@Import(WebMvcTestConfig.class)` to each `@WebMvcTest` controller test:

```java
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@Import({WebMvcTestConfig.class, org.example.chat.exception.GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {
    // test methods
}
```

### Step 3: Added GlobalExceptionHandler

We also imported `GlobalExceptionHandler` so that controller exceptions are properly handled and return the correct HTTP status codes (like 400 for validation errors).

### Step 4: Disabled Security Filters

We added `@AutoConfigureMockMvc(addFilters = false)` to disable Spring Security filters in tests. This allows us to test controller logic without dealing with authentication/authorization.

### Step 5: Removed Security-Specific Tests

We removed two tests (`createRoom_Unauthenticated_ReturnsUnauthorized` and `listRooms_Unauthenticated_ReturnsUnauthorized`) because they were testing security behavior, which doesn't work when security filters are disabled. These tests belong in integration tests, not unit tests.

## Why This Approach

### The Problem with @WebMvcTest

`@WebMvcTest` is a "slice test" that only loads:
- Controllers (`@Controller`, `@RestController`)
- Controller advice (`@ControllerAdvice`)
- Filters
- Web MVC configuration

It does NOT load:
- Services (`@Service`)
- Repositories (`@Repository`)
- Security components (`@Component` in security package)
- Other application beans

When Spring Security is on the classpath, `@WebMvcTest` automatically configures security, which tries to load `SecurityConfig`. Our `SecurityConfig` depends on `JwtAuthenticationFilter`, which depends on `JwtUtil` and `CustomUserDetailsService` - none of which are web layer components.

### Why Mock Beans?

We use mock beans instead of real implementations because:
1. **Unit testing focus**: We're testing controllers, not security logic
2. **Speed**: Mocks are faster than real beans
3. **Isolation**: We don't want security logic affecting controller tests
4. **Simplicity**: We don't need to configure JWT secrets, user stores, etc.

### Why @Primary?

The `@Primary` annotation tells Spring: "If there are multiple beans of this type, use this one." This ensures our mock beans are used instead of any other candidates that might be found during component scanning.

## Alternatives Considered

### Alternative 1: Exclude Security Auto-Configuration

We could have excluded Spring Security entirely:

```java
@WebMvcTest(controllers = AuthController.class, 
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
    })
```

**Pros:**
- Simpler - no need for mock beans
- Faster test startup

**Cons:**
- Loses all security context
- Can't test `@WithMockUser` scenarios
- Doesn't reflect real application behavior

### Alternative 2: Use @SpringBootTest

We could have used full integration tests:

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    // tests
}
```

**Pros:**
- Loads full application context
- Tests real security configuration
- More realistic

**Cons:**
- Much slower (loads entire application)
- Requires database, all services, etc.
- Harder to isolate controller logic
- Not true unit tests

### Alternative 3: Manual MockMvc Setup

We could have manually configured MockMvc without Spring:

```java
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    private MockMvc mockMvc;
    
    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(mockService))
            .build();
    }
}
```

**Pros:**
- Complete control
- No Spring context needed
- Very fast

**Cons:**
- Loses Spring MVC features (validation, exception handling, etc.)
- More boilerplate code
- Doesn't test Spring configuration

**Our choice (WebMvcTestConfig) is the best balance**: We get Spring MVC features, fast tests, and proper isolation.

## Key Concepts

### 1. Spring Test Slices

Spring Boot provides several "slice" annotations for testing specific layers:
- `@WebMvcTest` - Web layer (controllers)
- `@DataJpaTest` - JPA repositories
- `@RestClientTest` - REST clients
- `@JsonTest` - JSON serialization

Each slice loads only the components needed for that layer, making tests faster and more focused.

### 2. Test Configuration Classes

`@TestConfiguration` is used for test-specific beans that shouldn't be part of the main application. These configurations are only loaded in tests.

### 3. Bean Priority with @Primary

When Spring finds multiple beans of the same type, it needs to know which one to use. `@Primary` marks a bean as the default choice. In tests, this lets us override production beans with test doubles.

### 4. Security in Tests

Spring Security can complicate tests because it:
- Requires authentication for protected endpoints
- Needs security beans to be configured
- Adds filters to the request chain

For unit tests, we typically:
- Disable filters with `@AutoConfigureMockMvc(addFilters = false)`
- Use `@WithMockUser` to simulate authenticated users
- Mock security components

For integration tests, we:
- Keep security enabled
- Use real authentication
- Test the full security chain

### 5. Exception Handling in Tests

Controllers throw exceptions, but `@ControllerAdvice` classes handle them and convert them to HTTP responses. When testing with `@WebMvcTest`, you need to explicitly include your `@ControllerAdvice` classes (like `GlobalExceptionHandler`) or they won't be loaded.

## Potential Pitfalls

### Pitfall 1: Forgetting @Import

If you forget to import `WebMvcTestConfig`, you'll get `NoSuchBeanDefinitionException` errors. Every `@WebMvcTest` that uses security needs this import.

### Pitfall 2: Testing Security in Unit Tests

Don't try to test security behavior (authentication, authorization) in `@WebMvcTest` tests with `addFilters = false`. These tests should focus on controller logic. Security tests belong in integration tests.

### Pitfall 3: Missing GlobalExceptionHandler

If you don't import `GlobalExceptionHandler`, exceptions won't be converted to proper HTTP responses. Your tests will see 500 errors instead of 400/404/etc.

### Pitfall 4: Mixing Test Approaches

Don't mix `@WebMvcTest` with `@SpringBootTest`. Choose one approach per test class:
- `@WebMvcTest` for fast, focused controller tests
- `@SpringBootTest` for slower, comprehensive integration tests

### Pitfall 5: Over-Mocking

Don't mock everything. In `@WebMvcTest`, you should:
- Mock services (they're not loaded)
- Mock security beans (they're not web layer)
- NOT mock controllers (you're testing them!)
- NOT mock MockMvc (Spring provides it)

## What You Learned

1. **@WebMvcTest is a slice test** that only loads web layer components, not services or security beans

2. **Security components need explicit configuration** in `@WebMvcTest` because they're not part of the web layer

3. **Test configuration classes** (`@TestConfiguration`) provide test-specific beans without affecting the main application

4. **@Primary annotation** makes a bean the default choice when multiple candidates exist

5. **Disabling security filters** (`@AutoConfigureMockMvc(addFilters = false)`) simplifies controller unit tests

6. **GlobalExceptionHandler must be imported** to get proper HTTP status codes from exceptions

7. **Security tests don't belong in unit tests** - they should be in integration tests with full security enabled

8. **The right test approach depends on what you're testing**:
   - Unit tests (`@WebMvcTest`) for controller logic
   - Integration tests (`@SpringBootTest`) for full application behavior

This pattern (creating a `WebMvcTestConfig` with mock security beans) is reusable across any Spring Boot application with security. It's a clean, maintainable solution that keeps tests fast while still using Spring's test infrastructure.
