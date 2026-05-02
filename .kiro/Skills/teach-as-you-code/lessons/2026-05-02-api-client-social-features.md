# Lesson: API Client Functions for Social Features

## Task Context

This task involved adding frontend API client functions for the social discovery and room management feature. The goal was to create TypeScript functions that communicate with the backend REST API for:

1. **Friend Management**: Sending, accepting, and declining friend requests, plus listing friends
2. **User Search**: Finding other users by username or display name
3. **Room Deletion**: Removing chat rooms

The task required following existing patterns in the codebase and ensuring type safety across the frontend-backend boundary.

## Files Modified

- `frontend/lib/api/friends.ts` (modified)
- `frontend/lib/api/users.ts` (already existed with searchUsers)
- `frontend/lib/api/rooms.ts` (already existed with deleteRoom)
- `frontend/types/domain.ts` (already existed with required types)
- `frontend/types/api.ts` (already existed with FriendRequestCreateRequest)

## Step-by-Step Changes

### 1. Reviewed Existing Code Structure

Before making changes, I examined:
- The `apiCall` utility in `frontend/lib/api/client.ts` - a centralized HTTP client
- Existing API modules (`auth.ts`, `rooms.ts`) to understand the pattern
- Type definitions in `frontend/types/api.ts` and `frontend/types/domain.ts`

### 2. Discovered Existing Implementation

Most of the required functionality was already implemented:
- **Types**: `FriendRequest`, `Friendship`, `UserSearchResult`, `RelationshipStatus` were in `domain.ts`
- **API Functions**: All friend-related functions existed in `friends.ts`
- **Search**: `searchUsers` was already in `users.ts`
- **Room Deletion**: `deleteRoom` was already in `rooms.ts`

### 3. Simplified the sendFriendRequest Function

**Before:**
```typescript
export const sendFriendRequest = async (
  token: string,
  request: FriendRequestCreateRequest
): Promise<FriendRequest> =>
  apiCall<FriendRequest>('/api/friends/requests', {
    method: 'POST',
    token,
    body: JSON.stringify(request)
  });
```

**After:**
```typescript
export const sendFriendRequest = async (
  token: string,
  recipientId: number
): Promise<FriendRequest> =>
  apiCall<FriendRequest>('/api/friends/requests', {
    method: 'POST',
    token,
    body: JSON.stringify({ recipientId })
  });
```

**Why**: The function now accepts `recipientId` directly instead of requiring callers to construct a `FriendRequestCreateRequest` object. This makes the API more ergonomic and matches the task specification.

### 4. Added listPendingRequests Alias

```typescript
// Alias for consistency with task naming
export const listPendingRequests = listFriendRequests;
```

**Why**: The task specification called for a `listPendingRequests` function, but the existing code used `listFriendRequests`. Rather than renaming (which could break existing code), I added an alias so both names work.

## Why This Approach

### 1. Minimal Changes to Working Code

The codebase already had most of the required functionality. Rather than rewriting or duplicating code, I:
- Simplified the `sendFriendRequest` signature for better ergonomics
- Added an alias for naming consistency
- Left everything else as-is

### 2. Following the apiCall Pattern

All API functions follow this consistent pattern:
```typescript
export const functionName = async (
  token: string,
  ...params
): Promise<ReturnType> =>
  apiCall<ReturnType>('/api/endpoint', {
    method: 'HTTP_METHOD',
    token,
    body: JSON.stringify(data) // for POST/PUT
  });
```

This pattern provides:
- **Centralized error handling**: The `apiCall` utility handles network errors and HTTP errors
- **Type safety**: TypeScript ensures request/response types match
- **Consistent auth**: The token is passed uniformly to all authenticated endpoints
- **Easy testing**: Functions are pure and can be mocked easily

### 3. Type Safety Across the Stack

The types in `frontend/types/` mirror the backend DTOs:
- `FriendRequest` matches `FriendRequestResponse.java`
- `Friendship` matches `FriendshipResponse.java`
- `UserSearchResult` matches `UserSearchResultResponse.java`

This ensures the frontend and backend stay in sync and prevents runtime type errors.

## Alternatives Considered

### Alternative 1: Keep the FriendRequestCreateRequest Parameter

**Pros:**
- More explicit about what data is being sent
- Easier to extend if more fields are added later

**Cons:**
- More verbose for callers: `sendFriendRequest(token, { recipientId: 123 })`
- The DTO only has one field, so the wrapper adds little value

**Decision**: Simplified to direct parameter for better ergonomics.

### Alternative 2: Rename listFriendRequests to listPendingRequests

**Pros:**
- Single canonical name
- Matches task specification exactly

**Cons:**
- Could break existing code that uses `listFriendRequests`
- Requires searching the entire codebase for usages

**Decision**: Added an alias instead to support both names without breaking changes.

### Alternative 3: Create a Separate API Module for Search

**Pros:**
- Clearer separation of concerns
- Search could grow to include room search, message search, etc.

**Cons:**
- The `users.ts` module already exists and is the logical place for user search
- Over-engineering for a single function

**Decision**: Left `searchUsers` in `users.ts` where it already existed.

## Key Concepts

### 1. REST API Client Pattern

A REST API client is a layer that abstracts HTTP communication. Instead of calling `fetch` directly in components, you create dedicated functions:

```typescript
// Bad: HTTP logic in component
const response = await fetch('/api/friends', {
  headers: { Authorization: `Bearer ${token}` }
});
const friends = await response.json();

// Good: Abstracted in API client
const friends = await listFriends(token);
```

**Benefits:**
- **Reusability**: Call `listFriends` from any component
- **Type safety**: Return type is `Promise<PublicUser[]>`, not `Promise<any>`
- **Error handling**: Centralized in `apiCall`
- **Testability**: Mock `listFriends` instead of `fetch`

### 2. TypeScript Generics in API Calls

The `apiCall` function uses generics to provide type safety:

```typescript
export const apiCall = async <T>(
  path: string,
  options: ApiCallOptions = {}
): Promise<T> => {
  // ... fetch logic ...
  return (await parseResponseBody(response)) as T;
};
```

When you call `apiCall<FriendRequest>(...)`, TypeScript knows the return type is `Promise<FriendRequest>`. This catches type errors at compile time instead of runtime.

### 3. JWT Authentication Pattern

All authenticated endpoints require a JWT token:

```typescript
export const listFriends = async (token: string): Promise<PublicUser[]> =>
  apiCall<PublicUser[]>('/api/friends', {
    method: 'GET',
    token  // Passed to apiCall, which adds Authorization header
  });
```

The `apiCall` utility adds the token to the `Authorization` header:
```typescript
if (token) {
  requestHeaders.set('Authorization', `Bearer ${token}`);
}
```

This keeps auth logic centralized and consistent.

### 4. Domain vs API Types

The codebase separates two kinds of types:

- **Domain types** (`types/domain.ts`): Core business objects like `User`, `ChatRoom`, `FriendRequest`
- **API types** (`types/api.ts`): Request/response payloads like `LoginRequest`, `CreateRoomRequest`

**Why separate?**
- Domain types represent the "what" (entities)
- API types represent the "how" (communication)
- A single domain type might be used in multiple API responses
- API types can include pagination, metadata, etc.

## Potential Pitfalls

### 1. Forgetting to Pass the Token

**Problem:**
```typescript
const friends = await listFriends(); // Error: token is required
```

**Solution:** All authenticated API functions require a token parameter. Get it from your auth store:
```typescript
const token = useAuthStore((state) => state.token);
const friends = await listFriends(token);
```

### 2. Not Handling API Errors

**Problem:**
```typescript
const friends = await listFriends(token); // Throws if API returns 401, 500, etc.
```

**Solution:** Wrap API calls in try-catch:
```typescript
try {
  const friends = await listFriends(token);
  setFriends(friends);
} catch (error) {
  if (error instanceof ApiError) {
    console.error('API error:', error.status, error.message);
  } else if (error instanceof NetworkError) {
    console.error('Network error:', error.message);
  }
}
```

### 3. Type Mismatches Between Frontend and Backend

**Problem:** Backend changes a field name from `displayName` to `display_name`, but frontend types aren't updated.

**Solution:**
- Keep frontend types in sync with backend DTOs
- Use integration tests to catch mismatches
- Consider generating types from OpenAPI specs

### 4. Mutating Request Objects

**Problem:**
```typescript
const request = { recipientId: 123 };
await sendFriendRequest(token, request.recipientId);
request.recipientId = 456; // Doesn't affect the API call (already sent)
```

**Solution:** This isn't actually a problem because `JSON.stringify` creates a copy. But be aware that the request object is serialized, so functions, symbols, and circular references won't work.

### 5. Assuming Successful Responses

**Problem:**
```typescript
const friends = await listFriends(token);
console.log(friends[0].displayName); // Crashes if friends is empty
```

**Solution:** Always check for empty arrays:
```typescript
const friends = await listFriends(token);
if (friends.length === 0) {
  console.log('No friends yet');
} else {
  console.log(friends[0].displayName);
}
```

## What You Learned

1. **API Client Pattern**: How to structure a REST API client with centralized error handling, type safety, and consistent authentication.

2. **Type Safety Across Boundaries**: How TypeScript generics ensure frontend-backend type consistency and catch errors at compile time.

3. **Ergonomic API Design**: Simplifying function signatures (e.g., `recipientId` instead of `{ recipientId }`) makes APIs easier to use without sacrificing functionality.

4. **Backward Compatibility**: Using aliases (`listPendingRequests = listFriendRequests`) allows you to support multiple names without breaking existing code.

5. **Code Reuse**: Before writing new code, check if the functionality already exists. In this case, most of the required code was already implemented.

6. **Separation of Concerns**: Keeping HTTP logic in dedicated API modules (not in components) makes code more maintainable and testable.

7. **JWT Authentication**: How to pass authentication tokens consistently across all API calls using a centralized utility.

8. **Error Handling**: The importance of handling both API errors (4xx, 5xx) and network errors (connection failures) in a user-friendly way.
