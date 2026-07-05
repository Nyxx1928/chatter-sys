# Implementation Plan: OTP-Based Email Verification

## Overview

The implementation is organized into 5 phases, each ending with a checkpoint. The approach is bottom-up: data layer first, then service logic, then controllers, then email templates, then frontend. Old code is removed in a dedicated cleanup phase after the new flow is fully functional. Tests are written alongside implementation, not as a separate phase.

---

## Tasks

### Phase 1: Data Layer Changes

- [ ] **1. Modify `PendingRegistration` entity**
  - Remove `token` (String) and `expiryDate` (LocalDateTime) fields
  - Add `otpHash` (String, `@Column(nullable = false)`)
  - Add `otpExpiry` (LocalDateTime, `@Column(nullable = false)`)
  - Add `attemptCount` (int, default 0, `@Column(nullable = false)`)
  - Update `isExpired()` to compare `LocalDateTime.now()` against `otpExpiry`
  - Add `isMaxAttemptsExceeded()` returning `attemptCount >= 3`
  - Add `incrementAttempts()` incrementing `attemptCount` by 1
  - _Requirements: 1.2, 2.5, 2.6, 7.4_

- [ ] **2. Update `PendingRegistrationRepository`**
  - Remove `findByToken(String token)`
  - Rename `deleteByExpiryDateBefore(LocalDateTime)` -> `deleteByOtpExpiryBefore(LocalDateTime dateTime)`
  - Verify `findByEmail(String email)` exists and returns `Optional<PendingRegistration>`
  - _Requirements: 7.4, 7.5_

- [ ] **3. Create DTOs**
  - Create `VerifyOtpRequest` record: `(@NotBlank @Email String email, @NotBlank @Pattern(regexp = "\\d{6}") String otp)`
  - Create `ResendOtpRequest` record: `(@NotBlank @Email String email)`
  - Create `VerifyOtpResponse` record: `(boolean success, String message)`
  - _Requirements: 2.1, 3.1_

- [ ] **4. Checkpoint -- Data layer compiles and tests pass**
  - Run `mvn compile` -- no compilation errors from entity/repo changes
  - Write `PendingRegistrationTest` covering `isExpired()`, `isMaxAttemptsExceeded()`, `incrementAttempts()`
  - All existing tests that don't touch the old flow still pass

---

### Phase 2: Service Layer

- [ ] **5. Add OTP generation utility**
  - Add `private String generateOtp()` to `RegistrationService`
  - Uses `SecureRandom` to generate a 6-digit numeric string (`String.format("%06d", ...)`)
  - _Requirements: 1.1_

- [ ] **6. Rewrite `RegistrationService.initiateRegistration()`**
  - Generate 6-digit OTP, BCrypt-hash via existing `passwordEncoder`
  - Store `otpHash`, `otpExpiry` (now + 10 min), `attemptCount = 0` in `PendingRegistration`
  - Update `RegistrationResult` record: remove `verificationUrl` field
  - Handle duplicate pending email: if existing `PendingRegistration` found, delete it first (or overwrite)
  - _Requirements: 1.1, 1.2, 1.6, 7.4_

- [ ] **7. Rewrite `RegistrationService.completeRegistration()` -> `verifyOtp()`**
  - Rename method to `verifyOtp(String email, String rawOtp)`
  - Return new `OtpVerificationResult(boolean success, String message)` record
  - Find `PendingRegistration` by email
  - If not found: return failure with generic "Invalid code" message
  - If expired: return failure with "Code expired" message
  - If `isMaxAttemptsExceeded()`: delete `PendingRegistration`, return failure with "Too many attempts"
  - BCrypt-verify raw OTP against `otpHash`
  - On match: create `User` with `emailVerified = true`, delete `PendingRegistration`, return success
  - On mismatch: increment attempts, save. If now max attempts: delete and return "Too many attempts". Otherwise return "Invalid code. N attempts remaining."
  - _Requirements: 2.1 through 2.7_

- [ ] **8. Rewrite `RegistrationService.resendVerificationEmail()` -> `resendOtp()`**
  - Rename method to `resendOtp(String email)`
  - Find `PendingRegistration` by email
  - If not found: return silently (don't throw, don't reveal)
  - If found: generate new OTP, update `otpHash` + `otpExpiry`, reset `attemptCount = 0`, send new email
  - _Requirements: 3.1, 3.2, 3.4_

- [ ] **9. Update `cleanupExpiredPendingRegistrations()`**
  - Change repository call to `pendingRegistrationRepository.deleteByOtpExpiryBefore(LocalDateTime.now())`
  - _Requirements: 7.5_

- [ ] **10. Add rate limit for OTP verification**
  - Add `checkOtpVerification(String clientIp)` method to `RateLimiterService`
  - Capacity: 5, refill interval: 1 minute, key pattern: `otp_verify:<IP>`
  - _Requirements: 6.1_

- [ ] **11. Write `RegistrationServiceTest` for OTP flow**
  - Test `verifyOtp()`: success case, wrong OTP, expired, max attempts, not found
  - Test `initiateRegistration()`: OTP stored hashed, email sent, duplicate handling
  - Test `resendOtp()`: new OTP overwrites old, attempts reset, not-found returns silently
  - Mock `BrevoEmailService` and `PasswordEncoder` where needed
  - _Requirements: 1.x, 2.x, 3.x_

- [ ] **12. Checkpoint -- Service layer fully tested**
  - Run `mvn test -pl . -Dtest="RegistrationServiceTest"` -- all pass
  - Run `mvn test` -- no unexpected failures in other tests

---

### Phase 3: Controller & Email Layer

- [ ] **13. Add `POST /api/auth/verify-otp` to `AuthController`**
  - Accept `@Valid @RequestBody VerifyOtpRequest`
  - Call `rateLimiterService.checkOtpVerification(clientIp)`
  - Delegate to `registrationService.verifyOtp(email, otp)`
  - Return `200` with `VerifyOtpResponse` on success, appropriate error on failure
  - _Requirements: 2.1_

- [ ] **14. Add `POST /api/auth/resend-otp` to `AuthController`**
  - Accept `@Valid @RequestBody ResendOtpRequest`
  - Delegate to `registrationService.resendOtp(email)`
  - Always return `200 { "message": "If the email is pending verification, a new code has been sent." }`
  - Rate limiting handled by existing `RateLimiterService` (resend bucket already exists)
  - _Requirements: 3.1, 3.4_

- [ ] **15. Update `POST /api/auth/register` response**
  - Remove `verificationUrl` from `RegistrationResponse` record
  - Remove `exposeVerificationLink` logic from the endpoint
  - Return simplified response: `{ message, emailSent, errorMessage? }`
  - _Requirements: 1.4_

- [ ] **16. Add `sendOtpEmail()` to `BrevoEmailService`**
  - Implement HTML template with large centered monospaced OTP, dashed border, clear label
  - Implement plain-text fallback
  - Format OTP with spaces between digits in the display (but raw 6-digit in the plain-text for copy-paste)
  - Replace call in `RegistrationService` from `sendVerificationEmail(email, url)` to `sendOtpEmail(email, otp)`
  - _Requirements: 4.1 through 4.5_

- [ ] **17. Update `SecurityConfig` public endpoints**
  - Remove `/api/auth/verify-email` and `/api/auth/resend-verification`
  - Add `/api/auth/verify-otp` and `/api/auth/resend-otp`
  - _Requirements: 7.2, 7.3_

- [ ] **18. Update `AuthenticationService`**
  - Remove `verificationUrl` from `RegistrationResult` record
  - Inline the `emailVerified` check in `authenticateUser()` (remove dependency on `EmailVerificationService`)
  - _Requirements: 2.8, 7.1_

- [ ] **19. Write/update controller tests**
  - `AuthControllerTest`: test `verify-otp` (success, invalid, expired, max attempts, rate limited)
  - `AuthControllerTest`: test `resend-otp` (always returns 200)
  - `AuthControllerTest`: test `register` response no longer contains `verificationUrl`
  - Update `EmailVerificationControllerTest` -- can be deleted or repurposed
  - _Requirements: 1.x, 2.x, 3.x_

- [ ] **20. Checkpoint -- API endpoints functional**
  - Start application with `mvn spring-boot:run`
  - Test manually with curl:
    - `curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' -d '{...}'`
    - `curl -X POST localhost:8080/api/auth/verify-otp -H 'Content-Type: application/json' -d '{"email":"...","otp":"..."}'`
    - `curl -X POST localhost:8080/api/auth/resend-otp -H 'Content-Type: application/json' -d '{"email":"..."}'`
  - `mvn test` -- all controller tests pass

---

### Phase 4: Cleanup -- Remove Old Verification Code

- [ ] **21. Delete `VerificationToken` entity and repository**
  - Delete `src/main/java/.../entity/VerificationToken.java`
  - Delete `src/main/java/.../repository/VerificationTokenRepository.java`
  - _Requirements: 7.1_

- [ ] **22. Delete `EmailVerificationService`**
  - Delete `src/main/java/.../service/EmailVerificationService.java`
  - Move `isEmailVerified()` static helper to `User` entity or `AuthenticationService` (if still needed)
  - _Requirements: 7.1_

- [ ] **23. Delete `EmailVerificationController`**
  - Delete `src/main/java/.../controller/EmailVerificationController.java`
  - _Requirements: 7.2, 7.3_

- [ ] **24. Delete old DTOs**
  - Delete `ResendVerificationRequest.java`
  - _Requirements: 7.3_

- [ ] **25. Clean up `BrevoEmailService`**
  - Remove `sendVerificationEmail(String to, String verificationUrl)` method
  - Remove old HTML template (now replaced by `sendOtpEmail`)
  - _Requirements: 7.1, 4.3_

- [ ] **26. Checkpoint -- Old code fully removed, compilation clean**
  - `mvn compile` -- no compilation errors, no references to deleted classes
  - `mvn test` -- all tests pass, no stale test references

---

### Phase 5: Integration & E2E Tests + Frontend

- [ ] **27. Write `OtpVerificationE2EIT` integration test**
  - Register a user via `MockMvc`
  - Extract the OTP (mock Brevo to capture it, or read from `PendingRegistration` via test-only method)
  - Verify OTP via `POST /api/auth/verify-otp` -> success
  - Login -> success with JWT
  - _Requirements: 1.1, 2.1, 2.8_

- [ ] **28. Write integration test for OTP expiry**
  - Register -> use clock manipulation (or short expiry config) -> wait -> verify with valid OTP -> expect expiry error
  - _Requirements: 2.6_

- [ ] **29. Write integration test for max attempts**
  - Register -> submit 3 wrong OTPs -> 4th attempt -> expect "Too many attempts" error
  - Register again with same email -> works (old PendingRegistration was deleted)
  - _Requirements: 2.5_

- [ ] **30. Write integration test for resend OTP**
  - Register -> resend -> old OTP fails, new OTP works
  - _Requirements: 3.2_

- [ ] **31. Write integration test for rate limiting**
  - 6 verify-otp calls in quick succession -> 6th returns 429
  - _Requirements: 6.1_

- [ ] **32. Update `FullAuthFlowE2EIT` for new flow**
  - Replace token-based verification steps with OTP verification
  - Ensure all existing E2E scenarios still pass with the new flow
  - _Requirements: 2.8, 7.6_

- [ ] **33. Write property-based test for OTP generation**
  - jqwik: `generateOtp()` always returns exactly 6 digits (`\\d{6}`)
  - jqwik: BCrypt round-trip -- for any 6-digit string, `encode -> matches(original)` is true, `matches(other)` is false
  - _Requirements: 1.1, 6.4_

- [ ] **34. Checkpoint -- All tests pass**
  - `mvn verify` -- all unit + integration tests green
  - No regressions in existing test suites

- [ ] **35. Build frontend OTP verification page**
  - Create route `/verify-otp` with email query param
  - Implement 6-digit input with individual boxes, auto-advance, paste support
  - Implement countdown timer (10 minutes, synced from server or page-load timestamp)
  - Implement submit -> loading -> success/error states
  - Implement "Resend Code" button with 60s cooldown
  - Handle edge cases: missing email param, expired, max attempts, network errors
  - Add accessibility: `aria-label` on inputs, `role="alert"` on errors, `aria-live` on timer
  - _Requirements: 5.1 through 5.7_

- [ ] **36. Checkpoint -- Full flow works end-to-end**
  - Manual test: register via frontend -> receive email (check Brevo logs) -> enter OTP -> verify success -> login
  - Manual test: expire timer -> resend -> new OTP works
  - Manual test: enter 3 wrong codes -> locked out -> re-register works

---

## Notes

- Tasks marked with `*` could theoretically be deferred, but I haven't marked any -- the full replacement means every task is required for correctness.
- The `@Scheduled` cleanup cron expression and `application.yml` values (base URL, Brevo config) don't need changes.
- Database schema change (`pending_registrations` column renames) will be handled by Hibernate's `ddl-auto: update`. Verify the migration works on a test DB before deploying to production. In production with a real PostgreSQL, you'll want a manual migration (`ALTER TABLE ... DROP COLUMN token, ADD COLUMN otp_hash ...`) or use Flyway/Liquibase.
- The frontend tasks (35) assume a React + TypeScript stack. If the frontend uses a different framework, adjust accordingly.
