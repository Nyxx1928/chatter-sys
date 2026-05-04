# API Testing Guide

Quick reference for testing the backend REST API endpoints.

## Prerequisites

- Backend running on `http://localhost:8080`
- `curl` installed (or use Postman/Insomnia)

## Quick Test Flow

### 1. Register a New User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "password123",
    "displayName": "Alice Smith"
  }'
```

**Expected:** User object with ID

### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "password123"
  }'
```

**Expected:** JWT token and user object

**Save the token!** You'll need it for authenticated requests.

```bash
# Save token to variable (Linux/Mac)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo $TOKEN
```

### 3. Get Current User Profile

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4. Create a Chat Room

```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "name": "General",
    "description": "General discussion room"
  }'
```

**Expected:** Room object with ID

### 5. List All Rooms

```bash
curl -X GET http://localhost:8080/api/rooms \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 6. Get Room Details

```bash
curl -X GET http://localhost:8080/api/rooms/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 7. Get Room Members

```bash
curl -X GET http://localhost:8080/api/rooms/1/members \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 8. Get Message History

```bash
# Get first page (default 20 messages)
curl -X GET "http://localhost:8080/api/rooms/1/messages?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## Windows PowerShell Commands

If you're using PowerShell on Windows, use these commands:

### Register User

```powershell
$body = @{
    username = "alice"
    email = "alice@example.com"
    password = "password123"
    displayName = "Alice Smith"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

### Login and Save Token

```powershell
$loginBody = @{
    username = "alice"
    password = "password123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $loginBody

$token = $response.token
Write-Host "Token: $token"
```

### Create Room

```powershell
$roomBody = @{
    name = "General"
    description = "General discussion room"
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

Invoke-RestMethod -Uri "http://localhost:8080/api/rooms" `
  -Method Post `
  -Headers $headers `
  -Body $roomBody
```

### List Rooms

```powershell
$headers = @{
    "Authorization" = "Bearer $token"
}

Invoke-RestMethod -Uri "http://localhost:8080/api/rooms" `
  -Method Get `
  -Headers $headers
```

## Complete Test Script (Bash)

Save this as `test-api.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "Testing Real-Time Chat API"
echo "=========================="
echo ""

# 1. Register user
echo "1. Registering user..."
curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "displayName": "Test User"
  }' | jq '.'

echo ""
echo "2. Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
echo "Token: ${TOKEN:0:50}..."

echo ""
echo "3. Getting current user..."
curl -s -X GET $BASE_URL/api/users/me \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo ""
echo "4. Creating chat room..."
ROOM_RESPONSE=$(curl -s -X POST $BASE_URL/api/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Test Room",
    "description": "A test chat room"
  }')

echo $ROOM_RESPONSE | jq '.'
ROOM_ID=$(echo $ROOM_RESPONSE | jq -r '.id')

echo ""
echo "5. Listing all rooms..."
curl -s -X GET $BASE_URL/api/rooms \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo ""
echo "6. Getting room details..."
curl -s -X GET $BASE_URL/api/rooms/$ROOM_ID \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo ""
echo "7. Getting room members..."
curl -s -X GET $BASE_URL/api/rooms/$ROOM_ID/members \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo ""
echo "8. Getting message history..."
curl -s -X GET "$BASE_URL/api/rooms/$ROOM_ID/messages?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo ""
echo "=========================="
echo "API Testing Complete!"
```

Make it executable:
```bash
chmod +x test-api.sh
./test-api.sh
```

## Using Postman

### Import as Collection

1. Open Postman
2. Create new Collection: "Chat API"
3. Add these requests:

**Variables:**
- `baseUrl`: `http://localhost:8080`
- `token`: (will be set after login)

**Requests:**

1. **Register** - POST `{{baseUrl}}/api/auth/register`
2. **Login** - POST `{{baseUrl}}/api/auth/login`
   - In Tests tab, add: `pm.environment.set("token", pm.response.json().token);`
3. **Get Profile** - GET `{{baseUrl}}/api/users/me`
   - Authorization: Bearer Token `{{token}}`
4. **Create Room** - POST `{{baseUrl}}/api/rooms`
   - Authorization: Bearer Token `{{token}}`
5. **List Rooms** - GET `{{baseUrl}}/api/rooms`
   - Authorization: Bearer Token `{{token}}`

## Common Issues

### 401 Unauthorized

- Token expired (24 hours) - login again
- Token not included in Authorization header
- Token format incorrect (must be `Bearer YOUR_TOKEN`)

### 403 Forbidden

- User doesn't have permission for this action
- Not a member of the room

### 404 Not Found

- Room or user doesn't exist
- Check the ID in the URL

### 400 Bad Request

- Invalid JSON format
- Missing required fields
- Validation errors (check response message)

## Next Steps

Once the API is working:

1. Test WebSocket connections (requires STOMP client)
2. Run the frontend (see `frontend/SETUP.md`)
3. Test end-to-end with both backend and frontend
