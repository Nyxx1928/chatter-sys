# Implementation Plan: OAuth Email Verification Integration

## Overview

This implementation plan breaks down the OAuth email verification integration feature into discrete coding tasks. The feature extends the existing authentication system to support OAuth 2.0 providers (Google and GitHub) while maintaining consistent email verification requirements across all authentication methods. The implementation leverages Spring Security's built-in OAuth2 client support and integrates with the existing JWT-based authentication system.

## Tasks

- [ ] 1. Set up database schema and entities for OAuth providers
  - [ ] 1.1 Create database migration for oauth_providers table
    - Create Flyway migration script with oauth_providers table definition
    - Add indexes for user_id and provider_name columns
    - Add unique constraint on (provider_name, provider_user_id)
    - Modify users table to make password_hash nullable
    - _Requirements: 3.1, 3.2, 3.4_
  
  - [ ] 1.2 Create OAuthProvider entity class
    - Implement JPA entity with all required fields (id, user_id, provider_name, provider_user_id, email, display_name, profile_picture_url, access_token, refresh_token, token_expiry, linked_at, last_used, display_name_manually_set)
    - Add relationship mapping to User entity
    - Add validation annotations for required fields
    - _Requirements: 3.1, 3.2, 11.3_
  
  - [ ] 1.3 Update User entity to support OAuth authentication
    - Make passwordHash field nullable
    - Add OneToMany relationship to OAuthProvider entities
    - Implement helper methods: hasPassword(), hasOAuthProvider(), canUnlinkProvider()
    - _Requirements: 6.1, 7.1, 7.2_
  
  - [ ] 1.4 Create OAuthProviderRepository interface
    - Extend JpaRepository with OAuthProvider entity
    - Add custom query methods: findByProviderNameAndProviderUserId(), findByUserId(), findByUserIdAndProviderName()
    - _Requirements: 3.1, 3.3, 3.4_

- [ ] 2. Implement OAuth2 user info abstraction layer
  - [ ] 2.1 Create OAuth2UserInfo interface and provider implementations
    - Define OAuth2UserInfo interface with methods: getProviderId(), getEmail(), getName(), getProfilePictureUrl(), getEmailVerified()
    - Implement GoogleOAuth2UserInfo class extracting data from Google OAuth attributes
    - Implement GithubOAuth2UserInfo class extracting data from GitHub OAuth attributes
    - Add OAuth2UserInfoFactory to create appropriate implementation based on provider name
    - _Requirements: 2.3, 2.4, 4.1_
  
  - [ ]* 2.2 Write unit tests for OAuth2UserInfo implementations
    - Test GoogleOAuth2UserInfo attribute extraction
    - Test GithubOAuth2UserInfo attribute extraction
    - Test OAuth2UserInfoFactory provider selection
    - Test handling of missing or invalid attributes
    - _Requirements: 2.4, 8.4_

- [ ] 3. Implement core OAuth2 user service and authentication logic
  - [ ] 3.1 Create CustomOAuth2User wrapper class
    - Implement OAuth2User interface wrapping the default OAuth2User
    - Add User entity reference for accessing application user data
    - Implement getAuthorities() to return user roles
    - _Requirements: 2.5, 6.1_
  
  - [ ] 3.2 Implement CustomOAuth2UserService for user loading and account creation
    - Extend DefaultOAuth2UserService
    - Override loadUser() to process OAuth authentication
    - Implement processOAuthUser() to handle new user creation, account linking, and existing user updates
    - Implement createNewOAuthUser() for first-time OAuth users
    - Implement linkOAuthToExistingUser() for linking OAuth to existing accounts
    - Implement updateExistingOAuthUser() for returning users
    - Add email verification logic based on provider's email_verified flag
    - Add username generation logic with uniqueness handling
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 3.6, 4.1, 4.2, 5.1, 5.2_
  
  - [ ]* 3.3 Write property test for OAuth email verification status handling
    - **Property 1: OAuth Email Verification Status Handling**
    - **Validates: Requirements 4.1, 4.2, 5.5**
    - Generate random OAuth user info with verified and unverified emails
    - Verify verified emails skip verification token creation
    - Verify unverified emails trigger verification token creation
  
  - [ ]* 3.4 Write property test for OAuth email change detection
    - **Property 2: OAuth Email Change Detection and Re-verification**
    - **Validates: Requirements 5.1, 5.2, 5.3**
    - Generate random OAuth authentications with email changes
    - Verify email updates reset email_verified flag
    - Verify new verification tokens are created
  
  - [ ]* 3.5 Write property test for OAuth user creation
    - **Property 3: OAuth User Creation with Required Fields**
    - **Validates: Requirements 3.1, 3.2**
    - Generate random first-time OAuth authentications
    - Verify new user accounts are created with OAuth provider info
  
  - [ ]* 3.6 Write property test for OAuth account linking
    - **Property 4: OAuth Account Linking by Email**
    - **Validates: Requirements 3.3**
    - Generate random OAuth authentications with existing user emails
    - Verify OAuth provider is linked to existing account
    - Verify no duplicate users are created
  
  - [ ]* 3.7 Write property test for OAuth username generation
    - **Property 6: OAuth Username Generation and Uniqueness**
    - **Validates: Requirements 3.5, 3.6**
    - Generate random OAuth user info without usernames
    - Verify unique usernames are generated from display names
    - Verify numeric suffixes are appended for conflicts

- [ ] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement OAuth2 authentication handlers
  - [ ] 5.1 Create HttpCookieOAuth2AuthorizationRequestRepository
    - Implement OAuth2AuthorizationRequestRepository interface
    - Store OAuth2 authorization requests in HTTP-only cookies
    - Implement cookie serialization and deserialization
    - Add cookie expiry configuration (5 minutes)
    - _Requirements: 9.1, 9.2_
  
  - [ ] 5.2 Create OAuth2AuthenticationSuccessHandler
    - Extend SimpleUrlAuthenticationSuccessHandler
    - Extract User from CustomOAuth2User principal
    - Generate JWT token using JwtUtil
    - Build redirect URL to frontend with token and emailVerified parameters
    - Clear authorization request cookies
    - _Requirements: 2.5, 4.3, 6.2, 6.3_
  
  - [ ] 5.3 Create OAuth2AuthenticationFailureHandler
    - Extend SimpleUrlAuthenticationFailureHandler
    - Log detailed error information server-side
    - Build redirect URL to frontend login page with generic error message
    - Clear authorization request cookies
    - _Requirements: 8.1, 8.2, 8.3, 8.5_
  
  - [ ]* 5.4 Write unit tests for OAuth2 authentication handlers
    - Test success handler JWT token generation
    - Test success handler redirect URL construction
    - Test failure handler error message handling
    - Test cookie clearing in both handlers
    - _Requirements: 2.5, 8.1, 8.2_

- [ ] 6. Implement OAuth provider management service
  - [ ] 6.1 Create OAuthProviderService for provider management operations
    - Implement getLinkedProviders() to list user's OAuth providers
    - Implement linkProvider() to add OAuth provider to existing account
    - Implement unlinkProvider() with validation for last authentication method
    - Implement canUnlinkProvider() validation logic
    - Implement updateProviderTokens() for token refresh
    - Add audit logging for all provider management operations
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 10.6, 12.2_
  
  - [ ]* 6.2 Write property test for OAuth provider linkage uniqueness
    - **Property 5: OAuth Provider Linkage Uniqueness**
    - **Validates: Requirements 3.4**
    - Generate random attempts to link same provider twice
    - Verify duplicate linkages are rejected
  
  - [ ]* 6.3 Write property test for OAuth provider unlinking validation
    - **Property 10: OAuth Provider Unlinking Validation**
    - **Validates: Requirements 7.1, 7.2, 7.3**
    - Generate random unlink requests with various authentication method combinations
    - Verify last authentication method cannot be unlinked
    - Verify unlinking is allowed when multiple methods exist
  
  - [ ]* 6.4 Write property test for OAuth provider unlinking data preservation
    - **Property 11: OAuth Provider Unlinking Data Preservation**
    - **Validates: Requirements 7.5**
    - Generate random successful unlink operations
    - Verify user account and email verification status remain intact

- [ ] 7. Implement OAuth provider management REST API
  - [ ] 7.1 Create OAuthProviderController with REST endpoints
    - Implement GET /api/oauth/providers to list linked providers
    - Implement DELETE /api/oauth/providers/{providerName} to unlink provider
    - Implement POST /api/oauth/link/{providerName} to initiate provider linking
    - Add authentication requirement using @AuthenticationPrincipal
    - Add error handling for OAuthProviderUnlinkException
    - _Requirements: 10.3, 10.4, 10.5, 10.6_
  
  - [ ] 7.2 Create OAuthProviderResponse DTO
    - Implement record with fields: providerName, email, displayName, linkedAt, lastUsed
    - _Requirements: 10.5, 10.6_
  
  - [ ]* 7.3 Write unit tests for OAuthProviderController
    - Test GET /api/oauth/providers returns linked providers
    - Test DELETE /api/oauth/providers/{providerName} unlinks provider
    - Test DELETE returns error when unlinking last auth method
    - Test POST /api/oauth/link/{providerName} returns authorization URL
    - _Requirements: 10.3, 10.4, 10.6_

- [ ] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Implement OAuth security configuration
  - [ ] 9.1 Update SecurityConfig to enable OAuth2 login
    - Add oauth2Login() configuration to SecurityFilterChain
    - Configure CustomOAuth2UserService as userService
    - Configure OAuth2AuthenticationSuccessHandler and OAuth2AuthenticationFailureHandler
    - Configure HttpCookieOAuth2AuthorizationRequestRepository
    - Add OAuth2 endpoints to permitAll() list
    - _Requirements: 1.1, 1.2, 2.1, 9.1, 9.2_
  
  - [ ] 9.2 Create application.yml OAuth2 client configuration
    - Add Google OAuth2 client registration with client-id, client-secret, scope, redirect-uri
    - Add GitHub OAuth2 client registration with client-id, client-secret, scope, redirect-uri
    - Add Google OAuth2 provider configuration with authorization-uri, token-uri, user-info-uri
    - Add GitHub OAuth2 provider configuration with authorization-uri, token-uri, user-info-uri
    - Add custom OAuth configuration properties (use-pkce, authorization-request-cookie-expiry, authorized-redirect-uris)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 9.1, 9.3, 9.4_
  
  - [ ]* 9.3 Write property test for OAuth state parameter validation
    - **Property 13: OAuth State Parameter Validation**
    - **Validates: Requirements 9.2**
    - Generate random OAuth callbacks with valid and invalid state parameters
    - Verify invalid state results in authentication failure
  
  - [ ]* 9.4 Write property test for OAuth redirect URI validation
    - **Property 16: OAuth Redirect URI Validation**
    - **Validates: Requirements 9.3**
    - Generate random OAuth redirect URIs
    - Verify non-whitelisted URIs are rejected

- [ ] 10. Implement OAuth token encryption and security
  - [ ] 10.1 Create TokenEncryptionService for secure token storage
    - Implement encrypt() method using AES encryption
    - Implement decrypt() method for retrieving tokens
    - Add encryption key configuration from environment variables
    - _Requirements: 9.5, 12.1_
  
  - [ ] 10.2 Update OAuthProviderService to encrypt tokens before storage
    - Encrypt access_token before saving to database
    - Encrypt refresh_token before saving to database
    - Decrypt tokens when retrieving from database
    - _Requirements: 9.5, 12.1_
  
  - [ ]* 10.3 Write property test for OAuth token secure storage
    - **Property 14: OAuth Token Secure Storage**
    - **Validates: Requirements 9.5, 12.1**
    - Generate random OAuth tokens
    - Verify tokens are encrypted before database storage
    - Verify encrypted tokens can be decrypted correctly

- [ ] 11. Implement OAuth profile synchronization
  - [ ] 11.1 Add profile synchronization logic to CustomOAuth2UserService
    - Update user display name from OAuth data if not manually set
    - Update user profile picture URL from OAuth data
    - Respect display_name_manually_set flag
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_
  
  - [ ]* 11.2 Write property test for OAuth profile synchronization
    - **Property 17: OAuth Profile Synchronization with Manual Override**
    - **Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5**
    - Generate random OAuth authentications with profile data
    - Verify profile updates only when not manually set
    - Verify manual settings are preserved

- [ ] 12. Implement OAuth token refresh mechanism
  - [ ] 12.1 Add token refresh logic to OAuthProviderService
    - Implement refreshAccessToken() method
    - Check token expiry before OAuth API calls
    - Use refresh token to obtain new access token
    - Update stored tokens on successful refresh
    - Handle refresh failures by marking provider for re-authentication
    - _Requirements: 12.1, 12.2, 12.3, 12.4_
  
  - [ ]* 12.2 Write property test for OAuth token refresh
    - **Property 18: OAuth Token Refresh with Fallback**
    - **Validates: Requirements 12.2, 12.3**
    - Generate random expired tokens with refresh tokens
    - Verify new access tokens are obtained via refresh
    - Verify re-authentication is required when refresh fails

- [ ] 13. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Implement OAuth exception handling
  - [ ] 14.1 Create OAuth-specific exception classes
    - Create OAuthAuthenticationException extending ChatApplicationException
    - Create OAuthProviderLinkingException extending ChatApplicationException
    - Create OAuthProviderUnlinkException extending ChatApplicationException
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  
  - [ ] 14.2 Update GlobalExceptionHandler for OAuth exceptions
    - Add handler for OAuthAuthenticationException returning 401 with generic message
    - Add handler for OAuthProviderLinkingException returning 400 with specific message
    - Add handler for OAuthProviderUnlinkException returning 400 with specific message
    - Ensure sensitive information is not exposed in error responses
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 9.6_
  
  - [ ]* 14.3 Write property test for OAuth user info validation
    - **Property 12: OAuth User Info Validation**
    - **Validates: Requirements 2.4, 8.4**
    - Generate random OAuth user info with missing required fields
    - Verify errors are returned for incomplete provider data

- [ ] 15. Implement OAuth audit logging
  - [ ] 15.1 Add audit logging to OAuth operations
    - Log OAuth authentication attempts (success and failure)
    - Log email change events from OAuth providers
    - Log provider linking and unlinking operations
    - Log token refresh operations
    - Ensure no sensitive information (tokens, passwords) is logged
    - _Requirements: 5.4, 7.4, 8.5, 9.6, 12.4_
  
  - [ ]* 15.2 Write property test for OAuth operation audit logging
    - **Property 15: OAuth Operation Audit Logging**
    - **Validates: Requirements 5.4, 7.4, 8.5, 9.6, 12.4**
    - Generate random OAuth operations
    - Verify audit log entries are created
    - Verify sensitive information is not exposed in logs

- [ ] 16. Implement hybrid user authentication support
  - [ ] 16.1 Update AuthenticationService to support hybrid users
    - Validate email verification for both password and OAuth authentication
    - Support adding OAuth providers to password-based accounts
    - Support setting passwords on OAuth-only accounts
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  
  - [ ]* 16.2 Write property test for hybrid user authentication
    - **Property 8: Hybrid User Authentication with Email Verification**
    - **Validates: Requirements 6.1, 6.2, 6.3**
    - Generate random hybrid users with various authentication methods
    - Verify email verification is validated for both password and OAuth auth
  
  - [ ]* 16.3 Write property test for OAuth provider addition to existing accounts
    - **Property 9: OAuth Provider Addition to Existing Accounts**
    - **Validates: Requirements 6.4, 6.5**
    - Generate random existing password-based users
    - Verify OAuth providers can be linked
    - Generate random OAuth-only users
    - Verify passwords can be set to create hybrid accounts

- [ ] 17. Implement OAuth email verification flow integration
  - [ ] 17.1 Ensure OAuth users use existing email verification flow
    - Verify EmailVerificationService works for OAuth-created users
    - Verify verification token creation for OAuth users with unverified emails
    - Verify verification token validation updates email_verified flag
    - _Requirements: 4.3, 4.4, 4.5_
  
  - [ ]* 17.2 Write property test for OAuth user email verification flow
    - **Property 7: OAuth User Email Verification Flow Equivalence**
    - **Validates: Requirements 4.4, 4.5**
    - Generate random OAuth-created users requiring verification
    - Verify verification flow functions identically to password-based users
  
  - [ ]* 17.3 Write property test for OAuth unverified email access restriction
    - **Property 20: OAuth Unverified Email Access Restriction**
    - **Validates: Requirements 4.3**
    - Generate random OAuth users with unverified emails
    - Verify login is allowed but feature access is restricted

- [ ] 18. Implement OAuth response format consistency
  - [ ] 18.1 Ensure OAuth API responses match existing authentication patterns
    - Verify OAuth login response format matches /api/auth/login
    - Verify OAuth callback response format matches existing patterns
    - Verify OAuth provider management responses match existing patterns
    - Use consistent error response structure across all OAuth endpoints
    - _Requirements: 10.5_
  
  - [ ]* 18.2 Write property test for OAuth response format consistency
    - **Property 19: OAuth Response Format Consistency**
    - **Validates: Requirements 10.5**
    - Generate random OAuth operations
    - Verify response formats match existing authentication endpoint patterns

- [ ] 19. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 20. Create integration tests for OAuth flows
  - [ ]* 20.1 Write integration test for complete Google OAuth login flow
    - Mock Google OAuth provider endpoints using WireMock
    - Test authorization redirect
    - Test callback handling with authorization code
    - Test JWT token generation
    - Test redirect to frontend with token
    - _Requirements: 2.1, 2.2, 2.3, 2.5_
  
  - [ ]* 20.2 Write integration test for complete GitHub OAuth login flow
    - Mock GitHub OAuth provider endpoints using WireMock
    - Test authorization redirect
    - Test callback handling with authorization code
    - Test JWT token generation
    - Test redirect to frontend with token
    - _Requirements: 2.1, 2.2, 2.3, 2.5_
  
  - [ ]* 20.3 Write integration test for OAuth account linking
    - Test linking Google provider to existing password account
    - Test linking GitHub provider to existing password account
    - Test linking multiple OAuth providers to same account
    - Test email conflict handling during linking
    - _Requirements: 3.3, 6.4_
  
  - [ ]* 20.4 Write integration test for OAuth email verification
    - Test email verification for OAuth users with unverified emails
    - Test automatic verification for OAuth users with verified emails
    - Test email change detection and re-verification
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3_
  
  - [ ]* 20.5 Write integration test for OAuth provider management
    - Test listing linked providers via GET /api/oauth/providers
    - Test unlinking provider with multiple auth methods
    - Test preventing unlinking last auth method
    - _Requirements: 7.1, 7.2, 7.3, 7.5, 10.6_

- [ ] 21. Implement frontend OAuth integration
  - [ ] 21.1 Create OAuth login buttons component
    - Create OAuthLoginButtons React component
    - Add "Continue with Google" button redirecting to /oauth2/authorization/google
    - Add "Continue with GitHub" button redirecting to /oauth2/authorization/github
    - Add appropriate icons and styling
    - _Requirements: 10.1_
  
  - [ ] 21.2 Create OAuth callback page
    - Create /auth/oauth-callback page to handle OAuth redirects
    - Extract token and emailVerified query parameters
    - Store JWT token in localStorage
    - Redirect to /auth/verify-email-required if email not verified
    - Redirect to /chat if email verified
    - _Requirements: 10.2, 4.3_
  
  - [ ] 21.3 Create linked providers management UI
    - Create LinkedProvidersPanel component
    - Fetch linked providers from GET /api/oauth/providers
    - Display provider cards with name, email, and linked date
    - Add unlink button calling DELETE /api/oauth/providers/{providerName}
    - Handle unlink errors (last auth method)
    - _Requirements: 10.3, 10.4, 10.6_
  
  - [ ] 21.4 Add OAuth login buttons to login and register pages
    - Add OAuthLoginButtons to /auth/login page
    - Add OAuthLoginButtons to /auth/register page
    - Add visual separator between traditional and OAuth login
    - _Requirements: 10.1_

- [ ] 22. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at reasonable breaks
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Integration tests verify end-to-end OAuth flows
- The implementation leverages Spring Security's built-in OAuth2 client support
- OAuth tokens are encrypted before storage for security
- Email verification is enforced consistently across all authentication methods
- The frontend uses Next.js/React for OAuth UI components

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3", "1.4"] },
    { "id": 2, "tasks": ["2.1", "2.2"] },
    { "id": 3, "tasks": ["3.1"] },
    { "id": 4, "tasks": ["3.2", "3.3", "3.4", "3.5", "3.6", "3.7"] },
    { "id": 5, "tasks": ["5.1", "5.2", "5.3", "5.4"] },
    { "id": 6, "tasks": ["6.1", "6.2", "6.3", "6.4"] },
    { "id": 7, "tasks": ["7.1", "7.2", "7.3"] },
    { "id": 8, "tasks": ["9.1", "9.2", "9.3", "9.4"] },
    { "id": 9, "tasks": ["10.1"] },
    { "id": 10, "tasks": ["10.2", "10.3"] },
    { "id": 11, "tasks": ["11.1", "11.2"] },
    { "id": 12, "tasks": ["12.1", "12.2"] },
    { "id": 13, "tasks": ["14.1"] },
    { "id": 14, "tasks": ["14.2", "14.3"] },
    { "id": 15, "tasks": ["15.1", "15.2"] },
    { "id": 16, "tasks": ["16.1", "16.2", "16.3"] },
    { "id": 17, "tasks": ["17.1", "17.2", "17.3"] },
    { "id": 18, "tasks": ["18.1", "18.2"] },
    { "id": 19, "tasks": ["20.1", "20.2", "20.3", "20.4", "20.5"] },
    { "id": 20, "tasks": ["21.1", "21.2"] },
    { "id": 21, "tasks": ["21.3", "21.4"] }
  ]
}
```
