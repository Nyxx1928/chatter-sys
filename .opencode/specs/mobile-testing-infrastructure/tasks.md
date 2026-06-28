# Implementation Plan: Mobile Testing Infrastructure

## Overview

Implement the testing infrastructure in three phases: (1) test runner configuration, (2) store tests, (3) API client tests and CI integration. Each phase has a checkpoint for validation before proceeding.

## Tasks

### Phase 1: Test Runner Configuration

- [ ] 1. **Add Vitest dev dependency and config**
  - Run `npm install -D vitest` in `expo-chat-app/`
  - Create `expo-chat-app/vitest.config.ts` with node environment, globals enabled
  - Add `"test": "vitest run"` script to `package.json`
  - Add `"test:watch": "vitest"` script to `package.json`
  - Verify: `npm test` exits 0 with "No test files found"
  - _Requirements: 1.1, 1.2_

- [ ] 2. **Write a smoke test to validate the setup**
  - Create `expo-chat-app/src/smoke.test.ts` with `it('runs', () => expect(1 + 1).toBe(2))`
  - Verify: `npm test` runs and passes
  - Remove smoke test after verification
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 3. **Checkpoint — Runner Verified**
  - `npm test` runs without errors
  - TypeScript imports from `src/` resolve correctly in test files
  - Ask user if questions arise before proceeding

### Phase 2: Store Unit Tests

- [ ] 4. **Write `authStore` tests**
  - **File**: `src/stores/authStore.test.ts`
  - Test `login` with valid credentials → sets token and user, `isAuthenticated=true`
  - Test `login` with API failure → throws ApiError, state unchanged
  - Test `logout` → resets all fields to initial state
  - Test `validateSession` with valid token → loads user, sets authenticated
  - Test `validateSession` with expired/invalid token → clears auth, sets unauthenticated
  - Test `register` → sets user, `isAuthenticated=false`
  - _Requirements: 2.1, 2.2_

- [ ] 5. **Write `chatStore` tests**
  - **File**: `src/stores/chatStore.test.ts`
  - Test `addMessage` appends to correct room's message list
  - Test `addMessage` creates new room entry if room doesn't exist yet
  - Test `prependMessages` adds to front of existing messages, deduplicates by id
  - Test `loadMessages` replaces all messages for a room
  - Test `updateMessageStatus` changes `_status` on matching message id
  - Test `confirmMessage` replaces temp message with confirmed server message
  - Test `clearRoomMessages` removes room from messages map
  - Test pagination tracking updates correctly
  - _Requirements: 2.3, 2.4_

- [ ] 6. **Write `connectionStore` tests**
  - **File**: `src/stores/connectionStore.test.ts`
  - Test connect sets `connected=true`, stores client reference
  - Test disconnect sets `connected=false`, updates presence
  - Test reconnect reuses or recreates client
  - _Requirements: 2.5_

- [ ] 7. **Write `presenceStore` tests**
  - **File**: `src/stores/presenceStore.test.ts`
  - Test `setOnline` marks user as online
  - Test `setOffline` marks user as offline
  - Test `batchUpdate` applies multiple presence changes atomically
  - _Requirements: 2.5_

- [ ] 8. **Checkpoint — Store Tests Complete**
  - All 20+ store tests pass
  - Tests run without native module errors (node environment)
  - Ask user if questions arise before proceeding

### Phase 3: API Client Tests and CI Integration

- [ ] 9. **Write `apiCall` unit tests**
  - **File**: `src/api/client.test.ts`
  - Test successful 200 JSON response → returns parsed body
  - Test 204 no-content → returns undefined
  - Test 400 Bad Request → throws ApiError with status 400
  - Test 401 Unauthorized → throws ApiError (for protected endpoints)
  - Test 500 Server Error → throws ApiError with status 500
  - Test network failure → throws NetworkError
  - Test non-JSON response → falls back to text parsing
  - Test `Authorization: Bearer <token>` header present when token provided
  - Test `Authorization` header absent when no token provided
  - Test `X-CSRF-TOKEN` header present for POST/PUT/DELETE when csrfToken exists
  - Test `X-CSRF-TOKEN` header absent for GET requests
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ] 10. **Update CI workflow to run mobile tests**
  - Edit `.github/workflows/ci.yml`:
    - Rename `phase4-mobile-check` to `phase4-mobile-check-and-test`
    - Add `- name: Run mobile tests` step: `npx vitest run`
    - Update the summary line in `phase7-ci-summary` accordingly
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 11. **Write integration tests for API modules** *(optional)*
  - **Files**: `src/api/auth.test.ts`, `src/api/rooms.test.ts`, etc.
  - Mock `apiCall` at the module level
  - Test that each API module calls `apiCall` with correct path, method, and body
  - Test that auth API calls pass token correctly
  - _Requirements: 3.1, 3.2_

- [ ] 12. **Checkpoint — Full Integration Complete**
  - All store tests pass
  - All API client tests pass
  - CI runs mobile tests on push and PR
  - `npm test` exits 0 locally
  - Ask user if questions arise before writing

### Phase 4: Final Validation

- [ ] 13. **Run complete test suite**
  - Run `npm test` → all tests pass
  - Run `npx tsc --noEmit` → no type errors
  - Verify CI behavior with a dry-run or push to a test branch
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 14. **Document testing conventions**
  - Add a brief TESTING.md or section in AGENTS.md covering:
    - How to run tests: `npm test` / `npm run test:watch`
    - Naming convention: `method_Scenario_ExpectedResult`
    - Where to place tests: `src/<module>/<name>.test.ts`
  - _Requirements: 1.1_
