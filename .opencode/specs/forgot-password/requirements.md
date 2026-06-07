# Requirements Document

## Introduction

The Forgot Password feature allows users who have forgotten their password to securely reset it via their registered email. The system sends a time-limited reset link; the user clicks the link and sets a new password. This matches the existing email-verification pattern already in the application.

## Glossary

- **Reset Token**: A cryptographically secure, time-limited token sent to the user's email to authorize a password reset.
- **ForgotPasswordRequest**: The DTO containing the user's email address submitted on the forgot-password form.
- **ResetPasswordRequest**: The DTO containing the reset token and new password submitted when the user clicks the reset link.

## Requirements

### Requirement 1: Initiate Password Reset

**User Story:** As a registered user who has forgotten my password, I want to request a password reset by entering my email, so that I can receive a reset link.

#### Acceptance Criteria

1. WHEN a user submits `POST /api/auth/forgot-password` with a valid registered email, THE system SHALL generate a reset token, persist it, and send an email containing a reset link to that email.
2. WHEN a user submits `POST /api/auth/forgot-password` with an unregistered email, THE system SHALL return `200 OK` (same response as success) to prevent email enumeration.
3. WHEN a user submits `POST /api/auth/forgot-password` with an invalid email format, THE system SHALL return `400 BAD_REQUEST` with a validation error.
4. WHEN a rate limit is exceeded for forgot-password requests, THE system SHALL return `429 TOO_MANY_REQUESTS`.

### Requirement 2: Validate Reset Token and Reset Password

**User Story:** As a user with a valid reset link, I want to set a new password so that I can regain access to my account.

#### Acceptance Criteria

1. WHEN a user submits `POST /api/auth/reset-password` with a valid, non-expired reset token and a valid new password, THE system SHALL update the user's password and invalidate the token.
2. WHEN a user submits `POST /api/auth/reset-password` with an expired reset token, THE system SHALL return `400 BAD_REQUEST` with a token-expired error.
3. WHEN a user submits `POST /api/auth/reset-password` with an invalid or already-used reset token, THE system SHALL return `400 BAD_REQUEST` with an invalid-token error.
4. WHEN a user submits `POST /api/auth/reset-password` with a password that fails validation (e.g., too short), THE system SHALL return `400 BAD_REQUEST` with validation errors.
5. WHEN a user successfully resets their password, THE system SHALL invalidate all existing JWT sessions for that user (optional: invalidate token upon next request).

### Requirement 3: Security and Rate Limiting

**User Story:** As a system administrator, I want the forgot-password flow to be resistant to abuse, so that user accounts remain secure.

#### Acceptance Criteria

1. WHEN a single email address receives more than 3 reset requests within 15 minutes, THE system SHALL rate-limit further requests (returns `429 TOO_MANY_REQUESTS`).
2. THE reset token SHALL expire after 15 minutes.
3. THE reset token SHALL be single-use (invalidated after successful reset).
4. THE system SHALL use a constant-time comparison when validating reset tokens to prevent timing attacks.
5. THE response for both registered and unregistered email addresses SHALL be identical (`200 OK`) to prevent email enumeration.

### Requirement 4: Email Notification

**User Story:** As a user, I want to receive a clear email with a reset link, so that I can easily reset my password.

#### Acceptance Criteria

1. THE email SHALL contain a one-click reset link pointing to the frontend reset-password page with the token as a query parameter.
2. THE email SHALL clearly state that the link expires in 15 minutes.
3. THE email SHALL use the existing email infrastructure (Brevo API with SMTP fallback) consistent with verification emails.
4. THE email SHALL include the user's username for personalization.

### Requirement 5: Frontend Pages

**User Story:** As a user, I want a simple UI to request a reset and set a new password.

#### Acceptance Criteria

1. THE frontend SHALL provide a "Forgot Password?" link on the login page.
2. THE frontend SHALL provide a forgot-password form (email input + submit button).
3. THE frontend SHALL provide a reset-password page (token input from URL, new password + confirm password inputs).
4. THE frontend SHALL display success/error messages appropriately.

## Scope

### In-Scope
- `POST /api/auth/forgot-password` endpoint (public)
- `POST /api/auth/reset-password` endpoint (public)
- Reset token generation, persistence, expiry, single-use invalidation
- Email notification via existing `EmailService` / `BrevoEmailService`
- Rate limiting on forgot-password requests (reuse existing `RateLimiterService` pattern)
- `PasswordResetToken` entity + repository
- `ForgotPasswordRequest` and `ResetPasswordRequest` DTOs
- Security audit logging (reuse existing `SecurityAuditLogger`)
- "Forgot Password?" link on login page (frontend)
- Forgot-password form page (email input + submit) (frontend)
- Reset-password page (token from URL, new password + confirm) (frontend)
- Unit, integration, and E2E tests

### Out-of-Scope
- CAPTCHA integration (can be added later)
- SMS-based password reset
- Admin-initiated password reset
