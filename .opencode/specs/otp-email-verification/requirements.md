# Requirements Document: OTP-Based Email Verification

## Introduction

Replace the current clickable-link email verification flow with a 6-digit one-time password (OTP) system. After a user fills in the registration form and submits, a 6-digit numeric OTP is generated, stored (hashed), and sent via email. The user enters this OTP on a web page to verify their email and complete registration. This eliminates the redirect-based flow and provides a simpler, more mobile-friendly verification experience.

## Glossary

| Term | Definition |
|---|---|
| **OTP (One-Time Password)** | A 6-digit numeric code (e.g., `482931`) generated at registration time, used once to verify the user's email. |
| **PendingRegistration** | Existing entity that temporarily holds registration data before email verification. Will be modified to store the hashed OTP and attempt counter instead of a UUID token. |
| **OTP Attempt** | A single submission of an OTP code by the user for validation. |
| **OTP Expiry** | The OTP becomes invalid after 10 minutes from generation (or upon successful use / max attempts exceeded). |
| **Hashed OTP** | The OTP is BCrypt-hashed before storage, matching the existing password hashing pattern. |
| **Resend OTP** | The user can request a new OTP if the current one expired or was not received. The old OTP is invalidated and a new one is generated and sent. |

## Requirements

### Requirement 1: OTP-Based Registration Initiation

**User Story:** As a new user, I want to register with my email and receive a 6-digit OTP, so that I can verify my email ownership without clicking a link.

#### Acceptance Criteria

1. WHEN a user submits a valid registration request (`POST /api/auth/register`), THE system SHALL generate a cryptographically random 6-digit numeric OTP (000000-999999).
2. WHEN the OTP is generated, THE system SHALL BCrypt-hash the OTP and store it in the `PendingRegistration` entity alongside an expiry timestamp (now + 10 minutes) and an attempt counter initialized to 0.
3. WHEN the OTP is stored, THE system SHALL send an email containing the 6-digit OTP code in both HTML and plain-text formats.
4. WHEN the registration request is successful, THE system SHALL return a `201 Created` response indicating the email was sent, with **no** verification URL (the `verificationUrl` field SHALL be removed from the response).
5. IF the email fails to send, THEN the system SHALL still return `201 Created` but with `emailSent: false` and an appropriate error message.
6. WHEN a registration request is made with an already-pending email, THE system SHALL invalidate the old `PendingRegistration` (if expired or unused) and create a new one (preventing duplicate pending registrations per email).

### Requirement 2: OTP Verification Endpoint

**User Story:** As a new user, I want to enter the 6-digit code from my email on the verification page, so that I can prove I own the email address and complete registration.

#### Acceptance Criteria

1. WHEN a user submits a valid OTP via `POST /api/auth/verify-otp` with `{ email, otp }`, THE system SHALL find the matching `PendingRegistration` by email.
2. WHEN the `PendingRegistration` is found and the OTP is not expired, THE system SHALL BCrypt-verify the submitted OTP against the stored hash.
3. IF the OTP matches, THEN THE system SHALL:
   - Create a `User` entity with `emailVerified = true`
   - Delete the `PendingRegistration`
   - Return `200 OK` with a success message
4. IF the OTP does not match, THEN THE system SHALL increment the attempt counter.
5. IF the attempt counter reaches 3, THEN THE system SHALL delete the `PendingRegistration` (invalidating the OTP) and return `400 Bad Request` with an appropriate message.
6. IF the OTP has expired (elapsed time > 10 minutes), THEN THE system SHALL return `400 Bad Request` with an "OTP expired" message.
7. IF no `PendingRegistration` exists for the email, THEN THE system SHALL return `400 Bad Request` with a generic message (no email enumeration).
8. WHEN a user successfully verifies and logs in (`POST /api/auth/login`), THE system SHALL continue to enforce `emailVerified = true` (existing behavior preserved).

### Requirement 3: Resend OTP

**User Story:** As a new user, I want to request a new OTP if I didn't receive the first one or it expired, so that I can still complete my registration.

#### Acceptance Criteria

1. WHEN a user requests a resend via `POST /api/auth/resend-otp` with `{ email }`, THE system SHALL find the existing `PendingRegistration` by email.
2. IF a `PendingRegistration` exists, THEN THE system SHALL generate a new 6-digit OTP, update the hashed OTP and expiry, reset the attempt counter to 0, and send a new email.
3. IF no `PendingRegistration` exists, THEN THE system SHALL return `200 OK` with a generic message (no email enumeration).
4. WHEN the resend is processed, THE system SHALL always return `200 OK` regardless of whether the email existed or was sent (security: prevents email enumeration).
5. WHEN rate limiting applies (1 resend per 1 minute per email), THE system SHALL return `429 Too Many Requests`.

### Requirement 4: Email Template Redesign

**User Story:** As a new user, I want to clearly see the 6-digit OTP in the verification email, so that I can easily read and enter it.

#### Acceptance Criteria

1. WHEN the verification email is rendered (HTML), THE system SHALL display the 6-digit OTP prominently in a large, centered, monospaced font with spacing between digits (e.g., `4 8 2 9 3 1`).
2. WHEN the verification email is rendered (plain-text fallback), THE system SHALL display the OTP on its own line, clearly labeled.
3. THE email SHALL include instructions: "Enter this code on the verification page. This code expires in 10 minutes."
4. THE email SHALL NOT contain any clickable verification link.
5. THE email SHALL indicate the code is for one-time use only.

### Requirement 5: Frontend OTP Input UI

**User Story:** As a new user, I want a clean, intuitive OTP input interface on the web, so that I can easily enter the 6-digit code and complete verification.

#### Acceptance Criteria

1. WHEN the user navigates to the OTP verification page, THE page SHALL display a 6-digit OTP input field with individual digit boxes that auto-advance on input.
2. THE page SHALL display a countdown timer showing remaining OTP validity (starting at 10:00).
3. WHEN the timer reaches 0, THE page SHALL disable the input and show a "Code expired -- request a new one" message with a resend button.
4. THE page SHALL display the email address the OTP was sent to (read from registration context / URL param).
5. WHEN the user submits the OTP, THE page SHALL show a loading state and then either:
   - Redirect to login on success with a success toast
   - Show an inline error message on failure (invalid code / expired / too many attempts)
6. WHEN the user clicks "Resend Code", THE page SHALL disable the button for 60 seconds (matching the backend rate limit) and show a countdown.
7. THE page SHALL handle the edge case where the user navigates directly without a pending registration (show an appropriate message and link to register).

### Requirement 6: Security & Rate Limiting

**User Story:** As a system administrator, I want OTP verification to be protected against brute-force and enumeration attacks.

#### Acceptance Criteria

1. WHEN a user submits an OTP for verification, THE system SHALL enforce rate limiting of 5 attempts per 1 minute per IP address on the verification endpoint.
2. WHEN a user requests a registration, THE existing rate limit of 3 registrations per 60 minutes per IP SHALL be preserved.
3. WHEN a user requests an OTP resend, THE existing rate limit of 1 resend per 1 minute per email SHALL be preserved.
4. THE OTP SHALL be stored using BCrypt hashing (matching the existing `PasswordEncoder` pattern).
5. ALL error responses for verification SHALL use generic messages to prevent email enumeration.
6. THE OTP comparison SHALL use the existing `BCryptPasswordEncoder.matches()` to avoid timing side-channels.

### Requirement 7: Existing Flow Removal & Cleanup

**User Story:** As a developer, I want the old token-based verification code removed to reduce maintenance burden and confusion.

#### Acceptance Criteria

1. THE `VerificationToken` entity, repository, and all related logic in `EmailVerificationService` for the token-based flow SHALL be removed.
2. THE `GET /api/auth/verify-email` endpoint SHALL be removed (replaced by `POST /api/auth/verify-otp`).
3. THE `POST /api/auth/resend-verification` endpoint SHALL be removed (replaced by `POST /api/auth/resend-otp`).
4. THE `PendingRegistration.token` field SHALL be replaced by `otpHash` (String), `otpExpiry` (LocalDateTime), and `attemptCount` (int).
5. THE existing `@Scheduled` cleanup of expired `PendingRegistration` entities SHALL be updated to use the new `otpExpiry` field.
6. ALL existing tests for the old verification flow SHALL be updated or replaced to test the new OTP flow.

## Scope

### In-Scope

- 6-digit numeric OTP generation, hashing, storage, and verification
- Email template redesign (HTML + plain text) to display OTP
- New `POST /api/auth/verify-otp` endpoint
- New `POST /api/auth/resend-otp` endpoint
- `PendingRegistration` entity modification (token -> otpHash + attemptCount)
- Frontend OTP input page component with timer and resend
- Removal of token-based verification (`VerificationToken`, old endpoints)
- Rate limiting for OTP verification attempts
- Unit, integration, and E2E tests for the new flow

### Out-of-Scope

- SMS-based OTP delivery (email-only for this spec)
- OTP for password reset (password reset continues to use `PasswordResetToken`)
- OTP for existing-user email change (can be addressed in a follow-up)
- CAPTCHA integration (can be added later)
- Internationalization (i18n) of email templates
