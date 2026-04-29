# REST API Controllers

This package contains REST controllers for the chat application.

## AuthController

Handles user authentication operations.

### Endpoints

#### POST /api/auth/register
Registers a new user.

**Request Body:**
```json
{
  "username": "string (3-50 chars, required)",
  "email": "string (valid email, max 100 chars, required)",
  "password": "string (min 8 chars, required)",
  "displayName": "string (1-100 chars, required)"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "displayName": "Test User",
  "createdAt": "2024-01-01T12:00:00",
  "lastSeen": null,
  "online": false
}
```

**Error Response (400 Bad Request):**
```json
{
  "message": "Validation failed",
  "timestamp": "2024-01-01T12:00:00",
  "status": 400,
  "errors": {
    "username": "Username is required",
    "email": "Email must be valid"
  }
}
```

#### POST /api/auth/login
Authenticates a user and returns a JWT token.

**Request Body:**
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "displayName": "Test User",
    "createdAt": "2024-01-01T12:00:00",
    "lastSeen": "2024-01-01T12:30:00",
    "online": false
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "message": "Invalid username or password",
  "timestamp": "2024-01-01T12:00:00",
  "status": 400
}
```

## UserController

Handles user profile operations. All endpoints require authentication.

### Endpoints

#### GET /api/users/me
Gets the current authenticated user's profile.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "displayName": "Test User",
  "createdAt": "2024-01-01T12:00:00",
  "lastSeen": "2024-01-01T12:30:00",
  "online": true
}
```

#### PUT /api/users/me
Updates the current authenticated user's profile.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Request Body (all fields optional):**
```json
{
  "email": "newemail@example.com",
  "displayName": "New Display Name"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "newemail@example.com",
  "displayName": "New Display Name",
  "createdAt": "2024-01-01T12:00:00",
  "lastSeen": "2024-01-01T12:30:00",
  "online": true
}
```

**Error Response (400 Bad Request):**
```json
{
  "message": "Email already exists",
  "timestamp": "2024-01-01T12:00:00",
  "status": 400
}
```

## Error Handling

All controllers use the `GlobalExceptionHandler` to provide consistent error responses:

- **400 Bad Request**: Validation errors or business logic errors (e.g., duplicate username)
- **500 Internal Server Error**: Unexpected errors

Error responses follow this structure:
```json
{
  "message": "Error description",
  "timestamp": "2024-01-01T12:00:00",
  "status": 400,
  "errors": {
    "field": "error message"
  }
}
```

## Authentication

- `/api/auth/register` and `/api/auth/login` are public endpoints
- All other endpoints require a valid JWT token in the `Authorization` header
- Token format: `Bearer <jwt-token>`
- Tokens are obtained from the login endpoint

## CORS Configuration

The API is configured to accept requests from `http://localhost:3000` (frontend origin) with credentials enabled.
