# Implementation Plan: Forgot Password

## Overview

Implementation is organized into four phases: backend model + service (Phase 1), backend controller + security config (Phase 2), frontend pages (Phase 3), and testing (Phase 4). Each phase has clear checkpoints. The design reuses existing patterns (`VerificationToken`, `EmailVerificationService`, `RateLimiterService`) to minimize new code.

## Tasks

### Phase 1: Backend -- Entity, Repository, Service

- [ ] 1. Create `PasswordResetToken` entity
  - Fields: `id`, `token` (unique, indexed), `user` (ManyToOne), `expiresAt`, `usedAt`, `createdAt`
  - Methods: `isExpired()`, `isUsed()`, `@PrePersist onCreate()`
  - _Requirements: 1.1, 2.1_

- [ ] 2. Create `PasswordResetTokenRepository`
  - `Optional<PasswordResetToken> findByToken(String token)`
  - `void deleteByUser(User user)` -- for account deletion cascade
  - _Requirements: 1.1, 2.1_

- [ ] 3. Create `ForgotPasswordService`
  - `initiateReset(String email)` -- check rate limit, generate 32-byte hex token via `SecureRandom`, persist `PasswordResetToken`, send email via existing `EmailService`/`BrevoEmailService`, return silently for unregistered emails
  - `resetPassword(String token, String newPassword)` -- lookup token with constant-time comparison (`MessageDigest.isEqual()`), validate not expired and not used, encode new password with `BCryptPasswordEncoder`, update `User.password`, mark token `usedAt`, audit-log the event
  - _Requirements: 1.1, 2.1, 2.2, 2.3, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3_

- [ ] 4. Integrate rate limiting for forgot-password
  - Wire `RateLimiterService` into `ForgotPasswordService.initiateReset()` -- 3 requests per 15 min per email
  - Return 429 via existing error handling pattern when exceeded
  - _Requirements: 1.4, 3.1_

- [ ] 5. Create `ForgotPasswordRequest` and `ResetPasswordRequest` DTOs
  - `ForgotPasswordRequest` -- `@NotBlank @Email String email`
  - `ResetPasswordRequest` -- `@NotBlank String token`, `@NotBlank @Size(min=8, max=100) String newPassword`
  - _Requirements: 1.1, 1.3, 2.1, 2.4_

- [ ] 6. Checkpoint -- Backend service layer complete
  - Run existing unit tests to confirm no regressions
  - Verify `ForgotPasswordService` compiles with existing `EmailService` and `RateLimiterService` interfaces
  - _Requirements: all Phase 1_

### Phase 2: Backend -- Controller + Security Config

- [ ] 7. Add endpoints to `AuthController`
  - `POST /api/auth/forgot-password` -- delegates to `ForgotPasswordService.initiateReset()`
  - `POST /api/auth/reset-password` -- delegates to `ForgotPasswordService.resetPassword()`
  - Follow existing controller patterns (`@RestController`, `@RequestMapping`, constructor injection, SLF4J logging)
  - _Requirements: 1.1, 1.2, 2.1_

- [ ] 8. Update `SecurityConfig`
  - Add `/api/auth/forgot-password` and `/api/auth/reset-password` to `permitAll()` endpoint list
  - _Requirements: 1.1, 2.1_

- [ ] 9. Checkpoint -- Backend API complete
  - Start the application, hit both endpoints via curl/Postman
  - Verify 200 for forgot-password (both registered and unregistered emails)
  - Verify 400 for invalid token in reset-password
  - Ask the user if questions arise

### Phase 3: Frontend -- Pages and Navigation

- [ ] 10. Add "Forgot Password?" link to login page
  - Link below the login form, navigates to `/forgot-password`
  - _Requirements: 5.1_

- [ ] 11. Create `ForgotPasswordPage`
  - Route: `/forgot-password`
  - Form: email input + "Send Reset Link" button
  - States: idle -> loading -> success ("Check your email for the reset link") / error
  - Follow existing page patterns (Next.js pages directory or app router, Tailwind, TypeScript)
  - _Requirements: 5.2_

- [ ] 12. Create `ResetPasswordPage`
  - Route: `/reset-password?token=<token>`
  - Extract token from URL query parameter
  - Form: new password + confirm password inputs + "Reset Password" button
  - Client-side validation: passwords match, min 8 chars
  - States: idle -> loading -> success (redirect to `/login` with success message) / error
  - Handle expired/invalid token errors from the API
  - _Requirements: 5.3, 5.4_

- [ ] 13. Checkpoint -- Frontend pages complete
  - Navigate the full flow manually in the browser
  - Verify the "Forgot Password?" link appears on login
  - Verify both pages render and submit correctly
  - Ask the user if questions arise

### Phase 4: Testing

- [ ] 14. Write unit tests for `ForgotPasswordService`
  - `initiateReset()` with registered email -> token created, email sent
  - `initiateReset()` with unregistered email -> no token, no email, returns 200
  - `resetPassword()` with valid token -> password updated, token marked used
  - `resetPassword()` with expired token -> throws/returns 400
  - `resetPassword()` with used token -> throws/returns 400
  - Rate limit exceeded -> throws rate limit error
  - Use `@ExtendWith(MockitoExtension.class)`, mock dependencies
  - _Requirements: all_

- [ ] 15. Write integration tests for controllers
  - `POST /api/auth/forgot-password` -- registered email (200 + email mock verification)
  - `POST /api/auth/forgot-password` -- unregistered email (200)
  - `POST /api/auth/forgot-password` -- invalid email (400)
  - `POST /api/auth/reset-password` -- valid token (200 + verify login with new password)
  - `POST /api/auth/reset-password` -- expired token (400)
  - `POST /api/auth/reset-password` -- used token (400)
  - `POST /api/auth/reset-password` -- weak password (400)
  - Rate limiting: 4th request returns 429
  - Use `@SpringBootTest` + `MockMvc` (follow existing test patterns)
  - _Requirements: all_

- [ ] 16. Write property-based tests with jqwik
  - Token generation: for any `User`, token is 64 hex chars, non-null, non-empty
  - Rate limiting: for any sequence of N requests where N <= limit, all succeed; for N > limit, the excess are rejected
  - _Requirements: 2.1, 3.1_

- [ ] 17. Write E2E tests (Playwright or Cypress, if the project uses one)
  - Full happy path: forgot-password -> receive email -> reset password -> login with new password
  - Expired token path
  - Follow existing E2E patterns in the frontend project
  - _Requirements: all_

- [ ] 18. Checkpoint -- All tests pass
  - `mvn test` passes with 0 failures
  - `mvn verify` (if integration tests are configured) passes
  - Property-based tests pass with sufficient samples
  - Ask the user if questions arise

## Notes

- Tasks marked with `*` are optional and can be skipped for MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation throughout development
