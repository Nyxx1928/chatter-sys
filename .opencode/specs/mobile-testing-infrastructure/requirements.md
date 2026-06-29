# Requirements Document: Mobile Testing Infrastructure

## Introduction

Add comprehensive testing infrastructure to the Expo React Native mobile app ("Chatter") to match the backend's testing maturity. The goal is to introduce unit tests for state stores, integration tests for API flows, and a scalable test runner, integrated into the existing CI pipeline.

## Glossary

- **Store**: A Zustand state store managing client-side app state (auth, chat, connection, presence)
- **API Client**: The HTTP client layer in src/api/client.ts handling auth headers, CSRF, and error parsing
- **STOMP**: WebSocket messaging protocol used for real-time chat
- **Critical Path**: Core user flows (auth, messaging, contacts) that must work for the app to function

## Requirements

### Requirement 1: Test Runner Setup

**User Story:** As a developer, I want a test runner configured for the Expo project so that I can write and run tests locally and in CI.

#### Acceptance Criteria

1. WHEN the developer runs 
pm test, THE system SHALL execute all test files and report results
2. WHEN the CI runs the test step, THE system SHALL execute tests and fail the build if any test fails
3. WHEN a test file imports from src/, pp/, or components/, THE system SHALL resolve imports correctly without configuration duplication

### Requirement 2: Store Unit Tests

**User Story:** As a developer, I want unit tests for all Zustand stores so that state mutations are verified in isolation.

#### Acceptance Criteria

1. WHEN uthStore.login() is called with valid credentials, THE store SHALL set isAuthenticated=true, 	oken, and user
2. WHEN uthStore.logout() is called, THE store SHALL reset to initial state and clear all fields
3. WHEN chatStore.addMessage() is called, THE message SHALL be appended to the correct room's message list
4. WHEN chatStore.updateMessageStatus() is called, THE message SHALL have its _status updated
5. WHEN connectionStore sets the connection state, THE presence SHALL reflect the correct online/offline status

### Requirement 3: API Client Unit Tests

**User Story:** As a developer, I want unit tests for the API client so that error handling, auth headers, and network failures are verified.

#### Acceptance Criteria

1. WHEN the API receives a 200 JSON response, THE client SHALL return the parsed body
2. WHEN the API receives a 4xx response, THE client SHALL throw an ApiError with the status code and message
3. WHEN the API receives a 5xx response, THE client SHALL throw an ApiError
4. WHEN the API request fails due to network error, THE client SHALL throw a NetworkError
5. WHEN a token is provided, THE client SHALL include Authorization: Bearer <token> header
6. WHEN a csrfToken is available and method is POST/PUT/DELETE, THE client SHALL include X-CSRF-TOKEN header

### Requirement 4: CI Integration

**User Story:** As a developer, I want tests to run automatically in CI so that regressions are caught before deployment.

#### Acceptance Criteria

1. WHEN a push is made to main, THE CI SHALL run mobile tests
2. WHEN a PR is opened, THE CI SHALL run mobile tests
3. WHEN mobile tests fail, THE CI SHALL mark the build as failed and prevent merge
4. WHEN mobile tests pass, THE CI SHALL include the mobile check in the status summary

## Scope

### In-Scope

- Test runner setup (Vitest)
- Store unit tests (4 stores: auth, chat, connection, presence)
- API client unit tests (error handling, headers, network failures)
- CI integration into existing workflow

### Out-of-Scope

- E2E tests (Detox, Maestro, Appium)
- UI component tests (React Native Testing Library)
- Snapshot tests
- Test coverage thresholds (can be added later)
- iOS/Android simulator-based tests in CI
