# Lesson: Setting Up Spring Boot Project Structure and Dependencies

## Task Context

This lesson covers Task 1 from the real-time chat system specification: setting up the foundational Spring Boot project structure with all necessary dependencies for building a WebSocket-based chat application.

**What we're building:** A real-time chat system that allows 10-20 concurrent users to communicate using WebSocket connections with the STOMP protocol. The backend uses Spring Boot with Java 21, PostgreSQL for data persistence, and JWT for authentication.

**Why this task matters:** Before writing any business logic, we need to establish the project foundation. This includes:
- Declaring all required dependencies (Spring Boot starters, database drivers, security libraries)
- Configuring the build system (Maven with Java 21)
- Setting up application configuration (database connection, server settings, logging)

This is like preparing your kitchen before cooking - you need all the right tools and ingredients in place.

## Files Modified

- `pom.xml` (modified)
- `src/main/resources/application.yml` (created)

## Step-by-Step Changes

### Step 1: Add Spring Boot Parent POM

**What we did:**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
    <relativePath/>
</parent>
```

**Why:** The Spring Boot parent POM provides:
- Dependency version management (no need to specify versions for most Spring dependencies)
- Default plugin configurations
- Sensible defaults for Maven build lifecycle
- Java version compatibility settings

Think of it as inheriting a pre-configured template that handles most of the boilerplate setup.

### Step 2: Add Core Spring Boot Dependencies

**Spring Boot Web Starter:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
- Provides REST API capabilities
- Includes embedded Tomcat server
- Includes Jackson for JSON serialization
- Includes Spring MVC for web controllers

**Spring Boot WebSocket Starter:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```
- Enables WebSocket support
- Includes STOMP protocol implementation
- Provides message broker functionality
- Essential for real-time bidirectional communication

**Spring Boot Data JPA Starter:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
- Provides JPA (Java Persistence API) support
- Includes Hibernate as the JPA implementation
- Enables repository pattern for database operations
- Handles object-relational mapping (ORM)

**Spring Boot Security Starter:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```
- Provides authentication and authorization
- Includes password encoding utilities
- Enables method-level security
- Protects endpoints by default

### Step 3: Add Database Driver

**PostgreSQL Driver:**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```
- JDBC driver for PostgreSQL database
- `scope>runtime</scope>` means it's only needed at runtime, not compile time
- Allows Spring Boot to connect to PostgreSQL database

### Step 4: Add JWT Dependencies

**JWT (JSON Web Token) Libraries:**
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

**Why three separate dependencies?**
- `jjwt-api`: The API interfaces (compile-time dependency)
- `jjwt-impl`: The actual implementation (runtime dependency)
- `jjwt-jackson`: JSON serialization support (runtime dependency)

This separation follows the dependency inversion principle - your code depends on interfaces, not implementations.

### Step 5: Add Validation and Lombok

**Spring Boot Validation:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
- Provides `@Valid`, `@NotNull`, `@Size`, etc. annotations
- Enables automatic request validation
- Includes Hibernate Validator

**Lombok:**
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```
- Reduces boilerplate code (getters, setters, constructors)
- Uses annotations like `@Data`, `@Builder`, `@Slf4j`
- Marked as `optional` so it doesn't propagate to dependent projects

### Step 6: Add Test Dependencies

**Spring Boot Test Starter:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```
- Includes JUnit 5, Mockito, AssertJ, Hamcrest
- Provides `@SpringBootTest` annotation
- Enables integration testing

**Spring Security Test:**
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```
- Provides `@WithMockUser` for testing secured endpoints
- Enables security context testing

### Step 7: Configure Maven Build Plugins

**Spring Boot Maven Plugin:**
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```
- Packages the application as an executable JAR
- Excludes Lombok from the final JAR (only needed at compile time)
- Enables `mvn spring-boot:run` command

**Maven Compiler Plugin:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>21</source>
        <target>21</target>
        <release>21</release>
    </configuration>
</plugin>
```
- Explicitly sets Java 21 as the source and target version
- `<release>21</release>` ensures cross-compilation compatibility

**Maven Surefire Plugin:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.2</version>
</plugin>
```
- Runs unit tests during the Maven build
- Integrates with JUnit 5

### Step 8: Create Application Configuration (application.yml)

**Database Configuration:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/chatdb
    username: ${DB_USERNAME:chatuser}
    password: ${DB_PASSWORD:chatpass}
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
```

**Key points:**
- `${DB_USERNAME:chatuser}` - Uses environment variable `DB_USERNAME`, defaults to `chatuser`
- HikariCP is the default connection pool (fast and lightweight)
- `maximum-pool-size: 20` - Sufficient for 10-20 concurrent users
- `minimum-idle: 5` - Keeps 5 connections ready for quick response

**JPA/Hibernate Configuration:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

**Key points:**
- `ddl-auto: update` - Automatically updates database schema (good for development)
- `show-sql: false` - Doesn't log SQL queries (reduces noise in production)
- `dialect: PostgreSQLDialect` - Optimizes SQL for PostgreSQL

**Server Configuration:**
```yaml
server:
  port: 8080
  error:
    include-message: always
    include-binding-errors: always
```
- Application runs on port 8080
- Error responses include detailed messages (helpful for debugging)

**Logging Configuration:**
```yaml
logging:
  level:
    root: INFO
    org.springframework.web: INFO
    org.springframework.security: INFO
    org.springframework.messaging: INFO
    org.hibernate.SQL: DEBUG
    org.example: DEBUG
```
- `root: INFO` - Default log level for all packages
- `org.hibernate.SQL: DEBUG` - Shows SQL queries for debugging
- `org.example: DEBUG` - Detailed logging for our application code

**JWT Configuration:**
```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-this-in-production-minimum-256-bits}
  expiration: 86400000 # 24 hours in milliseconds
```
- Custom configuration properties for JWT
- Secret should be at least 256 bits for HS256 algorithm
- Tokens expire after 24 hours

**WebSocket Configuration:**
```yaml
websocket:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
  message-size-limit: 128000 # 128KB
  send-buffer-size-limit: 512000 # 512KB
  send-time-limit: 20000 # 20 seconds
```
- CORS allows frontend at `http://localhost:3000` to connect
- Message size limits prevent abuse
- Send time limit prevents hanging connections

### Step 9: Create Environment-Specific Profiles

**Development Profile:**
```yaml
---
spring:
  config:
    activate:
      on-profile: dev
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop
```
- `create-drop` - Recreates database schema on each restart (clean slate for testing)
- `show-sql: true` - Logs all SQL queries for debugging

**Production Profile:**
```yaml
---
spring:
  config:
    activate:
      on-profile: prod
  jpa:
    hibernate:
      ddl-auto: validate
logging:
  level:
    root: WARN
  file:
    name: logs/chat-application.log
```
- `ddl-auto: validate` - Only validates schema, doesn't modify it (safe for production)
- `root: WARN` - Reduces log noise
- Logs written to file for persistence

## Why This Approach

### 1. Spring Boot Starters Over Individual Dependencies

**Why:** Spring Boot starters are curated dependency bundles that include everything needed for a specific feature. Instead of manually adding 10+ dependencies for web development, `spring-boot-starter-web` includes them all with compatible versions.

**Benefit:** Eliminates version conflicts and reduces configuration complexity.

### 2. YAML Over Properties Files

**Why:** YAML is more readable and supports hierarchical structure naturally.

**Comparison:**
```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/chatdb
spring.datasource.username=chatuser
spring.datasource.hikari.maximum-pool-size=20
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/chatdb
    username: chatuser
    hikari:
      maximum-pool-size: 20
```

YAML is clearer and easier to maintain.

### 3. Environment Variables for Secrets

**Why:** Hardcoding secrets in configuration files is a security risk.

```yaml
username: ${DB_USERNAME:chatuser}
```

This reads from environment variable `DB_USERNAME`, falling back to `chatuser` if not set. In production, you'd set the environment variable without exposing it in code.

### 4. Profile-Based Configuration

**Why:** Different environments (development, testing, production) need different settings.

- **Development:** Verbose logging, auto-recreate database, show SQL
- **Production:** Minimal logging, validate schema only, log to file

This prevents accidentally using development settings in production.

### 5. HikariCP Connection Pooling

**Why:** Creating a new database connection for each request is slow. Connection pooling maintains a pool of reusable connections.

**Settings:**
- `minimum-idle: 5` - Always keep 5 connections ready
- `maximum-pool-size: 20` - Never exceed 20 connections

For 10-20 concurrent users, this provides good performance without wasting resources.

## Alternatives Considered

### 1. Gradle Instead of Maven

**Maven (chosen):**
- XML-based configuration
- More verbose but explicit
- Industry standard with extensive documentation
- Better IDE support in some cases

**Gradle:**
- Groovy/Kotlin DSL
- More concise and flexible
- Faster builds with incremental compilation
- Steeper learning curve

**Decision:** Maven for this project because it's more beginner-friendly and widely used in enterprise Java.

### 2. H2 Instead of PostgreSQL

**PostgreSQL (chosen):**
- Production-grade relational database
- Better for learning real-world database operations
- Supports advanced features (JSON columns, full-text search)

**H2:**
- In-memory database
- No installation required
- Great for quick prototyping and testing
- Not suitable for production

**Decision:** PostgreSQL because this is a learning project that should mirror real-world setups.

### 3. Properties Files Instead of YAML

**YAML (chosen):**
- More readable hierarchical structure
- Less repetition
- Better for complex configurations

**Properties:**
- Simpler format
- No indentation issues
- Slightly faster parsing

**Decision:** YAML for better readability, especially with nested configurations.

### 4. Spring Session Instead of JWT

**JWT (chosen):**
- Stateless authentication
- No server-side session storage
- Scales horizontally easily
- Client stores the token

**Spring Session:**
- Server-side session management
- Can use Redis for distributed sessions
- More control over session lifecycle

**Decision:** JWT for simplicity and stateless architecture, suitable for a learning project.

## Key Concepts

### 1. Dependency Management

**Maven Coordinates:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
- `groupId`: Organization or project identifier
- `artifactId`: Specific library name
- `version`: (omitted here because parent POM manages it)

### 2. Dependency Scopes

- **compile** (default): Available at compile time and runtime
- **runtime**: Only needed at runtime (e.g., database drivers)
- **test**: Only for testing (e.g., JUnit)
- **provided**: Provided by the runtime environment (e.g., servlet API)

### 3. Spring Boot Auto-Configuration

Spring Boot automatically configures beans based on classpath dependencies:
- Sees `spring-boot-starter-web` → Configures embedded Tomcat
- Sees `postgresql` driver → Configures DataSource
- Sees `spring-boot-starter-security` → Enables security filters

This "convention over configuration" approach reduces boilerplate.

### 4. Application Properties Hierarchy

Spring Boot loads configuration in this order (later overrides earlier):
1. `application.yml` (default properties)
2. Profile-specific files (`application-dev.yml`)
3. Environment variables
4. Command-line arguments

### 5. Connection Pooling

**Without pooling:**
```
Request → Open DB Connection → Execute Query → Close Connection
(Slow: ~100ms to open connection)
```

**With pooling:**
```
Request → Get Connection from Pool → Execute Query → Return to Pool
(Fast: ~1ms to get pooled connection)
```

### 6. STOMP Protocol

STOMP (Simple Text Oriented Messaging Protocol) is a messaging protocol that works over WebSocket:
- **Pub/Sub model**: Clients subscribe to topics, server publishes messages
- **Destinations**: `/topic/room/1` (broadcast), `/queue/user/123` (point-to-point)
- **Frames**: CONNECT, SUBSCRIBE, SEND, MESSAGE, DISCONNECT

### 7. JWT Structure

A JWT has three parts separated by dots:
```
header.payload.signature
```

**Example:**
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIn0.signature
```

- **Header**: Algorithm and token type
- **Payload**: Claims (user data)
- **Signature**: Verifies token hasn't been tampered with

## Potential Pitfalls

### 1. JWT Secret Too Short

**Problem:**
```yaml
jwt:
  secret: "secret123"  # Only 72 bits!
```

**Why it's bad:** HS256 algorithm requires at least 256 bits (32 characters) for security.

**Solution:**
```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-this-in-production-minimum-256-bits}
```

Use a long, random secret and store it as an environment variable.

### 2. Using `ddl-auto: create-drop` in Production

**Problem:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Deletes all data on restart!
```

**Why it's bad:** Every time the application restarts, all data is lost.

**Solution:** Use profiles:
- Development: `create-drop` or `update`
- Production: `validate` or `none`

### 3. Exposing Sensitive Information in Logs

**Problem:**
```yaml
logging:
  level:
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # Logs SQL parameter values!
```

**Why it's bad:** May log passwords, tokens, or personal data.

**Solution:** Use `TRACE` level only in development, `INFO` or `WARN` in production.

### 4. Not Setting Connection Pool Size

**Problem:** Using default connection pool settings for all environments.

**Why it's bad:** 
- Too few connections → Requests wait for available connections
- Too many connections → Database overload

**Solution:** Calculate based on expected concurrent users:
```
Max Pool Size = (Number of Concurrent Users × 1.5) + 5
For 20 users: (20 × 1.5) + 5 = 35 connections
```

We used 20 for this learning project (10-20 users).

### 5. Forgetting to Exclude Lombok from JAR

**Problem:** Including Lombok in the final JAR increases size unnecessarily.

**Solution:**
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```

### 6. CORS Misconfiguration

**Problem:**
```yaml
websocket:
  allowed-origins: "*"  # Allows any origin!
```

**Why it's bad:** Security risk - any website can connect to your WebSocket.

**Solution:** Explicitly list allowed origins:
```yaml
websocket:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

### 7. Not Using Environment-Specific Profiles

**Problem:** Same configuration for development and production.

**Why it's bad:**
- Development needs verbose logging and auto-schema updates
- Production needs minimal logging and schema validation only

**Solution:** Use Spring profiles (`dev`, `prod`) with different settings.

## What You Learned

### Core Concepts

1. **Spring Boot Dependency Management**: How Spring Boot parent POM manages dependency versions and how starters bundle related dependencies.

2. **Maven Project Structure**: Understanding `pom.xml` structure, dependency scopes, and build plugins.

3. **Application Configuration**: Using `application.yml` for hierarchical configuration with environment variables and profiles.

4. **Connection Pooling**: Why database connection pooling is essential for performance and how to configure HikariCP.

5. **Security Best Practices**: Using environment variables for secrets, proper JWT secret length, and CORS configuration.

6. **Environment Profiles**: Separating development and production configurations for safety and appropriate logging.

### Technical Skills

1. **Adding Spring Boot Dependencies**: You can now add any Spring Boot starter and understand what it provides.

2. **Configuring Database Connections**: You know how to configure PostgreSQL with connection pooling.

3. **Setting Up WebSocket Support**: You understand the dependencies needed for WebSocket and STOMP.

4. **Configuring JWT Authentication**: You can set up JWT with proper security settings.

5. **Maven Build Configuration**: You can configure Maven plugins for Java 21 and Spring Boot.

6. **Multi-Environment Configuration**: You can create profile-specific configurations for different deployment environments.

### Next Steps

With the project structure and dependencies in place, you're ready to:

1. **Create Database Entities**: Define User, ChatRoom, Message, and RoomMembership entities with JPA annotations.

2. **Set Up Repositories**: Create Spring Data JPA repositories for database operations.

3. **Implement Authentication**: Build the JWT authentication service and security configuration.

4. **Configure WebSocket**: Set up WebSocket endpoints and STOMP message broker.

5. **Build Business Logic**: Implement services for chat rooms, messages, and user presence.

The foundation is solid - now you can build the application layer by layer!
