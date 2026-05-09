# Lesson: DM Tests — Frontend Unit Tests and Backend Integration Tests (Tasks 14–16)

## Task Context

- **Goal:** Write the remaining tests for the Direct Messaging feature: frontend property tests for `RoomSelector` rendering (Properties 7, 8, 9) and backend integration tests for DM room creation, room list completeness, and message history ordering (Properties 1, 4, 6).
- **Scope:** Tasks 14, 15, and 16 from the direct-messaging spec. All implementation was already complete — this task is purely about test coverage.
- **Constraints:** The frontend had no unit test framework installed. The backend uses Spring Boot integration tests with MockMvc. Property tests use fast-check (frontend) and standard JUnit 5 (backend).

## Files Modified

- `frontend/package.json` (modified) — added `test:unit` and `test:unit:run` scripts
- `frontend/jest.config.ts` (created) — Jest configuration with ts-jest, jsdom, and path aliases
- `frontend/__mocks__/styleMock.js` (created) — CSS module mock for Jest
- `frontend/tests/unit/RoomSelector.dm.test.tsx` (created) — frontend property tests for DM room rendering
- `src/test/java/org/example/chat/integration/DirectMessagingIT.java` (created) — backend integration tests for DM feature
- `.kiro/specs/direct-messaging/tasks.md` (modified) — marked tasks 14, 15, 16 as complete

## Step-by-Step Changes

### 1. Set up the frontend unit test framework

The frontend only had Playwright for e2e tests. We needed Jest + React Testing Library + fast-check for unit/property tests.

```bash
npm install --save-dev jest jest-environment-jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event fast-check ts-jest @types/jest
```

Then created `jest.config.ts` to configure:
- `testEnvironment: 'jsdom'` — simulates a browser DOM
- `ts-jest` transform — compiles TypeScript including JSX
- `moduleNameMapper` — resolves `@/` path aliases and ignores CSS imports
- `testMatch` — only picks up files in `tests/unit/`

### 2. Write frontend property tests (Task 14)

Three properties were tested in `RoomSelector.dm.test.tsx`:

**Property 7 — DM room label is always the other participant's display name:**
- Uses `fc.string()` to generate arbitrary display names
- Renders `RoomSelector` with a `DIRECT` room that has `otherParticipant.displayName` set
- Asserts the button text contains the display name
- Key insight: must call `cleanup()` manually between fast-check iterations — `afterEach` only fires between `it` blocks, not between property runs

**Property 9 — DM room ARIA label:**
- Same approach, but asserts `aria-label === "Direct message with {displayName}"`
- This validates accessibility compliance for screen readers

**Property 8 — DM room suppresses delete button:**
- Renders a `DIRECT` room with `canDeleteRoom={() => true}` and `onRoomDelete` provided
- Asserts no button with `aria-label` matching `/^delete/i` is in the document
- Also verifies the inverse: a `GROUP` room does show the delete button

### 3. Write backend integration tests (Task 15)

`DirectMessagingIT` extends `BaseIntegrationTest` and covers 7 test cases:

**Property 1 — DM room creation round-trip:**
- Creates a friendship via the API (friend request → accept)
- Asserts the DM room appears in both users' `/api/rooms` lists with `roomType: DIRECT`
- Asserts both users appear in `/api/rooms/{id}/members`

**Idempotency test:**
- Creates a friendship, then verifies only one DIRECT room exists with the expected deterministic name (`dm__min__max`)

**Property 4 — Room list completeness:**
- Creates 3 friendships for alice (with 3 different users)
- Calls `/api/rooms` and asserts all 3 DM room IDs are present
- Asserts every room in the response has a non-null `roomType` field

**Property 6 — DM message history ordering:**
- Sends 5 messages via `ChatMessageService.sendMessage()` (STOMP not available in MockMvc)
- Calls `GET /api/rooms/{id}/messages?page=0&size=50`
- Asserts messages are in ascending timestamp order

**Access control tests (Property 5):**
- Non-participant gets 403 on room details and message history
- Participant cannot delete a DM room (403)
- Participant cannot invite new members to a DM room (403)

## Why This Approach

**Frontend — fast-check property tests over example-based tests:**
Property tests generate many random inputs automatically, catching edge cases you wouldn't think to write manually. For display names, this catches special characters, whitespace-only strings, and Unicode that might break regex matching or DOM queries.

**Frontend — `cleanup()` inside the property loop:**
`@testing-library/react`'s `cleanup()` removes rendered components from the DOM. When fast-check runs 25 iterations inside a single `it` block, `afterEach` doesn't fire between them — so previous renders accumulate and `getByRole` finds multiple matching elements. Calling `cleanup()` at the end of each iteration fixes this.

**Backend — service-layer message sending:**
STOMP WebSocket connections aren't available in MockMvc integration tests. Calling `ChatMessageService.sendMessage()` directly bypasses the transport layer while still exercising the full persistence and validation logic.

**Backend — deterministic DM room naming:**
The name `dm__min(id)__max(id)` is the key to idempotency. The test verifies this by counting rooms with that exact name — if the name is wrong, the count would be 0 or >1.

## Alternatives Considered

- **Vitest instead of Jest:** Vitest is faster and has better ESM support, but the project uses Next.js 16 with a complex module resolution setup. Jest + ts-jest was the safer choice given the existing TypeScript config.
- **Playwright for frontend unit tests:** Playwright is already installed, but it's designed for e2e browser tests, not component unit tests. It would require a running dev server and can't easily test component props in isolation.
- **jqwik for backend property tests:** The spec mentioned jqwik for backend property tests, but the idempotency and completeness properties are better expressed as straightforward integration tests with real database state. jqwik's arbitrary generation doesn't add value when the interesting behavior is in the persistence layer.

## Key Concepts

- **Property-based testing:** Instead of writing specific examples, you define a property that must hold for all inputs in a domain. fast-check generates random inputs and shrinks failures to the minimal counterexample.
- **DOM cleanup in property tests:** When rendering React components inside a property loop, you must call `cleanup()` between iterations to prevent DOM accumulation.
- **MockMvc integration tests:** Spring's `MockMvc` lets you test the full HTTP stack (controllers, security, serialization) without starting a real server. `@Transactional` on the test class rolls back all DB changes after each test.
- **Deterministic naming for idempotency:** Using `min(id)__max(id)` ensures the same room name regardless of argument order, making `findByNameAndRoomType` a reliable idempotency check.

## Potential Pitfalls

- **`cleanup()` in fast-check loops:** Forgetting this causes "Found multiple elements" errors that are confusing to debug because the error message shows elements from previous iterations.
- **STOMP in integration tests:** Don't try to use STOMP WebSocket connections in MockMvc tests — they require a real server. Use the service layer directly for message operations.
- **`@Transactional` and detached entities:** After a `@Transactional` service call, JPA entities may be detached. Re-fetch them with `userRepository.findById()` before passing them to other service methods.
- **Invite endpoint URL:** The invite endpoint is `POST /api/rooms/{id}/invite?inviteeId={userId}`, not `/api/rooms/{id}/members/{userId}`. Using the wrong URL returns 500 (no route found) instead of 403.

## What You Learned

- How to set up Jest + React Testing Library + fast-check in a Next.js TypeScript project
- Why `cleanup()` must be called manually inside fast-check property loops
- How to write backend integration tests that verify DM room creation, access control, and message ordering
- The difference between testing at the service layer vs. the HTTP layer in Spring Boot tests
- How deterministic naming enables idempotency checks without complex database queries
