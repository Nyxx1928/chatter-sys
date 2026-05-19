# Friend Suggestion & Profile Enhancement Feature

## Overview

This document outlines the implementation plan for two new features:
1. **Friend Suggestion** - Suggest potential friends to verified users on the contacts page
2. **Profile Enhancement** - Allow users to enrich their profiles with additional personal information

---

## 1. Friend Suggestion Feature

### 1.1 Requirements

- Only **verified users** (`emailVerified = true`) can see and use friend suggestions
- Suggestions appear on the **Contacts page** as a list of random usernames
- Users should not see:
  - Themselves in suggestions
  - Users they are already friends with
  - Users with pending friend requests (sent or received)
  - Blocked users (if blocking feature exists)

### 1.2 Database Changes

#### New Entity: `UserHobby` (join table for User-Hobby many-to-many)

```
user_id (FK) | hobby_id (FK)
```

#### New Entity: `Hobby`

```
id (PK)
name (unique, not null)
```

#### Updated Entity: `User`

Add the following fields to the `User` entity:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `gender` | `String` | length 20, nullable | User's gender (e.g., male, female, non-binary, prefer not to say) |
| `bio` | `String` | length 500, nullable | Short user biography |
| `dateOfBirth` | `LocalDate` | nullable | User's date of birth (optional, for age display) |
| `location` | `String` | length 100, nullable | User's location/city |
| `avatarUrl` | `String` | length 255, nullable | Profile picture URL |
| `hobbies` | `List<Hobby>` | many-to-many, cascade | User's hobbies/interests |

### 1.3 API Endpoints

#### Friend Suggestion Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| `GET` | `/api/friends/suggestions` | Get random friend suggestions (verified users only) | Yes |
| `GET` | `/api/friends/suggestions?count=N` | Get N random suggestions (default 10) | Yes |

**Response Format:**
```json
{
  "suggestions": [
    {
      "id": 1,
      "username": "cooluser123",
      "displayName": "Cool User",
      "bio": "Love coding and gaming",
      "gender": "male",
      "location": "New York",
      "hobbies": ["gaming", "coding", "music"],
      "mutualFriends": 3,
      "commonHobbies": ["gaming"]
    }
  ]
}
```

**Error Responses:**
- `403 Forbidden` - User is not email verified
- `401 Unauthorized` - Not authenticated

### 1.4 Suggestion Algorithm

The friend suggestion logic should prioritize:

1. **Common hobbies** - Users sharing hobbies get higher priority
2. **Mutual friends** - Users with common friends get higher priority
3. **Random selection** - Fill remaining slots with random verified users

**Pseudocode:**
```
function getSuggestions(currentUser, count):
    if not currentUser.emailVerified:
        throw ForbiddenException

    excludedIds = [currentUser.id] + currentFriends + pendingRequestUsers

    candidates = all verified users where id not in excludedIds

    scoredCandidates = candidates.map(user => {
        score = 0
        score += count(commonHobbies(currentUser, user)) * 10
        score += count(mutualFriends(currentUser, user)) * 5
        return { user, score }
    })

    weighted = scoredCandidates with score > 0
    random = scoredCandidates with score == 0

    result = top N from weighted (by score)
    fill remaining slots with random users

    return result
```

---

## 2. Profile Enhancement Feature

### 2.1 Requirements

- Users can update their profile with additional fields
- All new fields are **optional**
- Profile data is visible to other users (for friend suggestions and social features)
- Input validation and sanitization required

### 2.2 Updated DTOs

#### `UpdateProfileRequest` (Enhanced)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
    private String displayName;

    @Size(max = 20, message = "Gender cannot exceed 20 characters")
    private String gender;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    private LocalDate dateOfBirth;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    @Size(max = 255, message = "Avatar URL cannot exceed 255 characters")
    private String avatarUrl;

    private List<String> hobbies;
}
```

#### `UserProfileResponse` (New DTO for public profile view)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String displayName;
    private String gender;
    private String bio;
    private Integer age; // calculated from dateOfBirth
    private String location;
    private String avatarUrl;
    private List<String> hobbies;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeen;
    private Boolean online;
    private Boolean emailVerified;
}
```

### 2.3 API Endpoints

#### Profile Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| `GET` | `/api/users/me` | Get current user's full profile (existing, needs update) | Yes |
| `PUT` | `/api/users/me` | Update current user's profile (existing, needs update) | Yes |
| `GET` | `/api/users/{id}/profile` | Get public profile of a user by ID | Yes |
| `GET` | `/api/users/{username}/profile` | Get public profile of a user by username | Yes |

#### Hobby Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| `GET` | `/api/hobbies` | Get list of all available hobbies | Yes |
| `POST` | `/api/hobbies` | Create a new hobby (admin only) | Yes (Admin) |

### 2.4 Validation Rules

| Field | Validation |
|-------|------------|
| `gender` | Must be one of: male, female, non-binary, other, prefer-not-to-say (case insensitive) |
| `bio` | Max 500 characters, HTML sanitized |
| `dateOfBirth` | Must be a valid date in the past, user must be at least 13 years old |
| `location` | Max 100 characters, alphanumeric and spaces only |
| `avatarUrl` | Must be a valid URL if provided |
| `hobbies` | Max 10 hobbies per user, each hobby max 30 characters |

---

## 3. Implementation Files Checklist

### Backend (Java/Spring Boot)

#### New Files to Create

- [ ] `src/main/java/org/example/chat/entity/Hobby.java`
- [ ] `src/main/java/org/example/chat/repository/HobbyRepository.java`
- [ ] `src/main/java/org/example/chat/dto/FriendSuggestionResponse.java`
- [ ] `src/main/java/org/example/chat/dto/UserProfileResponse.java`
- [ ] `src/main/java/org/example/chat/dto/HobbyResponse.java`
- [ ] `src/main/java/org/example/chat/service/FriendSuggestionService.java`
- [ ] `src/main/java/org/example/chat/controller/FriendSuggestionController.java`
- [ ] `src/main/java/org/example/chat/controller/HobbyController.java`
- [ ] `src/test/java/org/example/chat/service/FriendSuggestionServiceTest.java`

#### Files to Modify

- [ ] `src/main/java/org/example/chat/entity/User.java` - Add new fields and hobby relationship
- [ ] `src/main/java/org/example/chat/dto/UpdateProfileRequest.java` - Add new fields
- [ ] `src/main/java/org/example/chat/dto/UserResponse.java` - Add new fields
- [ ] `src/main/java/org/example/chat/dto/PublicUserResponse.java` - Add new fields
- [ ] `src/main/java/org/example/chat/repository/UserRepository.java` - Add suggestion query methods
- [ ] `src/main/java/org/example/chat/controller/UserController.java` - Update profile endpoints
- [ ] `src/main/java/org/example/chat/service/AuthenticationService.java` - Update profile logic
- [ ] `src/main/java/org/example/chat/controller/FriendController.java` - Add suggestion endpoint
- [ ] `src/main/java/org/example/chat/service/FriendService.java` - Add suggestion logic
- [ ] `src/main/java/org/example/chat/security/SecurityConfig.java` - Add new endpoint permissions

### Database Migration

Create a Flyway/Liquibase migration script or run the following SQL:

```sql
-- Add new columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS location VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(255);

-- Create hobbies table
CREATE TABLE IF NOT EXISTS hobbies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) UNIQUE NOT NULL
);

-- Create user_hobbies join table
CREATE TABLE IF NOT EXISTS user_hobbies (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hobby_id BIGINT NOT NULL REFERENCES hobbies(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, hobby_id)
);

-- Create indexes for suggestion queries
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);
CREATE INDEX IF NOT EXISTS idx_users_gender ON users(gender);
CREATE INDEX IF NOT EXISTS idx_user_hobbies_user_id ON user_hobbies(user_id);
CREATE INDEX IF NOT EXISTS idx_user_hobbies_hobby_id ON user_hobbies(hobby_id);

-- Seed some default hobbies
INSERT INTO hobbies (name) VALUES
    ('gaming'),
    ('coding'),
    ('music'),
    ('reading'),
    ('sports'),
    ('cooking'),
    ('traveling'),
    ('photography'),
    ('art'),
    ('movies'),
    ('fitness'),
    ('hiking'),
    ('anime'),
    ('writing'),
    ('dancing')
ON CONFLICT (name) DO NOTHING;
```

---

## 4. Frontend Considerations

### Contacts Page Updates

- Add a "Suggested Friends" section above or below the existing friends list
- Display suggestion cards with:
  - Username and display name
  - Avatar (if available)
  - Bio preview (truncated)
  - Common hobbies badges
  - Mutual friend count
  - "Add Friend" button on each suggestion
- "Refresh Suggestions" button to get new random suggestions
- Show loading state while fetching suggestions
- Show message if no suggestions available

### Profile Page Updates

- Add editable fields for:
  - Gender (dropdown/select)
  - Bio (textarea with character counter)
  - Date of birth (date picker)
  - Location (text input)
  - Avatar URL (text input or file upload)
  - Hobbies (multi-select with search or tag input)
- Display profile completeness percentage
- Save/cancel buttons for editing
- Preview mode for how profile appears to others

---

## 5. Security Considerations

1. **Verification Gate**: Friend suggestions endpoint must verify `emailVerified = true`
2. **Input Sanitization**: All user-provided text fields must be HTML sanitized (use existing `HtmlSanitizer`)
3. **Rate Limiting**: Suggestion endpoint should be rate-limited to prevent abuse
4. **Privacy**: Users should have option to hide certain profile fields from public view (future enhancement)
5. **XSS Prevention**: Bio and other text fields must be escaped on the frontend

---

## 6. Testing Plan

### Unit Tests

- `FriendSuggestionServiceTest`
  - Test suggestions exclude current user
  - Test suggestions exclude existing friends
  - Test suggestions exclude pending requests
  - Test suggestions require email verification
  - Test scoring algorithm with common hobbies
  - Test scoring algorithm with mutual friends
  - Test random fallback when no scored candidates

- `ProfileUpdateTest`
  - Test valid profile updates
  - Test validation for each field
  - Test HTML sanitization on bio
  - Test age calculation from date of birth

### Integration Tests

- Test full suggestion API flow
- Test profile update API flow
- Test hobby CRUD operations

---

## 7. Future Enhancements (Out of Scope)

- Friend suggestion based on chat activity patterns
- "People you may know" based on room memberships
- Profile visibility settings (public, friends only, private)
- Avatar file upload with image processing
- Profile badges/achievements
- Friend suggestion notification emails
