# Implementation Plan: Real-Time Chat System

## Overview

This implementation plan breaks down the real-time chat system into incremental coding tasks. The system uses Spring Boot with STOMP over WebSocket for the backend and Next.js with TypeScript for the frontend. Tasks are organized to build foundational components first, then layer on real-time communication, and finally integrate everything together.

## Tasks

### Backend Setup and Core Infrastructure

- [x] 1. Set up Spring Boot project structure and dependencies
  - Update pom.xml with Spring Boot parent, Spring Web, Spring WebSocket, Spring Data JPA, PostgreSQL driver, Spring Security, and JWT dependencies
  - Configure Maven build plugins and Java 21 compiler settings
  - Create application.yml with database connection, server port, and logging configuration
  - _Requirements: 8.1, 8.4, 12.1, 12.2, 12.3, 12.5_

- [x] 2. Create database entities and repositories
  - [x] 2.1 Implement User entity with JPA annotations
    - Create User class with id, username, email, passwordHash, displayName, createdAt, lastSeen, online fields
    - Add unique constraints on username and email
    - Define relationships to Message and RoomMembership entities
    - _Requirements: 1.3, 8.2_
  - [x] 2.2 Implement ChatRoom entity with JPA annotations
    - Create ChatRoom class with id, name, description, createdAt, createdBy fields
    - Add unique constraint on name
    - Define relationships to Message and RoomMembership entities
    - _Requirements: 5.2, 8.3_
  - [x] 2.3 Implement Message entity with JPA annotations
    - Create Message class with id, sender, chatRoom, content, timestamp, messageType fields
    - Add index on (chat_room_id, timestamp) for efficient history queries
    - Define MessageType enum (TEXT, SYSTEM, JOIN, LEAVE)
    - _Requirements: 3.4, 6.2, 8.1_
  - [x] 2.4 Implement RoomMembership entity with JPA annotations
    - Create RoomMembership class with id, user, chatRoom, joinedAt, role fields
    - Add unique constraint on (user_id, chat_room_id)
    - Define MemberRole enum (OWNER, MODERATOR, MEMBER)
    - _Requirements: 5.2, 5.4, 8.3_
  - [x] 2.5 Create JPA repositories for all entities
    - Create UserRepository with findByUsername, existsByUsername, existsByEmail methods
    - Create ChatRoomRepository with findByMembersContaining, findByName methods
    - Create MessageRepository with findByChatRoomOrderByTimestampDesc, findByChatRoomAndTimestampAfter methods
    - Create RoomMembershipRepository with findByChatRoom, findByUserAndChatRoom, deleteByUserAndChatRoom methods
    - _Requirements: 6.1, 6.4, 8.1, 8.2, 8.3_

- [x] 3. Checkpoint - Verify database entities and repositories
  - Ensure all tests pass, ask the user if questions arise.

### Authentication and Security

- [x] 4. Implement authentication and security infrastructure
  - [x] 4.1 Create JWT utility class for token generation and validation
    - Implement generateToken method that creates JWT with username and expiration
    - Implement validateToken method that verifies JWT signature and expiration
    - Implement getUsernameFromToken method to extract username from JWT
    - _Requirements: 1.1, 1.4, 16.4_
  - [x] 4.2 Implement AuthenticationService for user registration and login
    - Create registerUser method that validates input, hashes password, and persists User
    - Create authenticateUser method that validates credentials and returns JWT token
    - Use BCryptPasswordEncoder for password hashing
    - _Requirements: 1.1, 1.2, 1.3, 11.1_
  - [x] 4.3 Create Spring Security configuration
    - Configure HTTP security to permit authentication endpoints and require authentication for others
    - Add JWT authentication filter to validate tokens on protected endpoints
    - Configure CORS to allow frontend origin (http://localhost:3000)
    - _Requirements: 1.5, 12.3_
  - [x] 4.4 Create REST controllers for authentication
    - Implement POST /api/auth/register endpoint that accepts RegisterRequest and returns User
    - Implement POST /api/auth/login endpoint that accepts LoginRequest and returns LoginResponse with token
    - Implement GET /api/users/me endpoint that returns current authenticated User
    - Implement PUT /api/users/me endpoint that updates current User profile
    - _Requirements: 1.1, 1.2, 11.1, 11.4_

- [ ]\* 4.5 Write unit tests for authentication service
  - Test successful registration with valid credentials
  - Test registration failure with duplicate username or email
  - Test successful login with valid credentials
  - Test login failure with invalid credentials
  - Test JWT token generation and validation
  - _Requirements: 1.1, 1.2, 9.4_

- [x] 5. Checkpoint - Verify authentication flow
  - Ensure all tests pass, ask the user if questions arise.

### WebSocket and STOMP Configuration

- [x] 6. Configure WebSocket and STOMP messaging
  - [x] 6.1 Create WebSocketConfig class
    - Implement WebSocketMessageBrokerConfigurer interface
    - Register STOMP endpoint at /ws with SockJS fallback and CORS allowed origins
    - Configure message broker with /topic and /queue prefixes for subscriptions
    - Set application destination prefix to /app for client messages
    - Set user destination prefix to /user for user-specific messages
    - _Requirements: 2.1, 12.2_
  - [x] 6.2 Create WebSocket authentication interceptor
    - Implement ChannelInterceptor to extract and validate JWT from STOMP CONNECT frames
    - Set authenticated user principal in STOMP session
    - Reject connections with invalid or missing tokens
    - _Requirements: 1.5, 2.1_
  - [x] 6.3 Create WebSocket event listener for connection lifecycle
    - Implement SessionConnectEvent handler to mark User as online
    - Implement SessionDisconnectEvent handler to mark User as offline
    - Publish presence updates to /topic/presence/{roomId} for all User's rooms
    - _Requirements: 2.2, 2.3, 7.1, 7.2, 7.3, 7.5_

- [x] 7. Checkpoint - Verify WebSocket configuration
  - Ensure all tests pass, ask the user if questions arise.

### Chat Room Management

- [x] 8. Implement chat room services and controllers
  - [x] 8.1 Create ChatRoomService for room management
    - Implement createRoom method that creates ChatRoom and adds creator as OWNER
    - Implement getRoomById method that retrieves ChatRoom by id
    - Implement listRooms method that returns all available ChatRooms
    - Implement getRoomMembers method that returns Users in a ChatRoom
    - Implement addMember method that creates RoomMembership
    - Implement removeMember method that deletes RoomMembership
    - _Requirements: 5.2, 5.4, 6.4, 8.3_
  - [x] 8.2 Create REST controller for chat room operations
    - Implement POST /api/rooms endpoint that accepts CreateRoomRequest and returns ChatRoom
    - Implement GET /api/rooms endpoint that returns list of ChatRooms
    - Implement GET /api/rooms/{id} endpoint that returns ChatRoom details
    - Implement GET /api/rooms/{id}/members endpoint that returns list of Users in room
    - _Requirements: 5.1, 11.2_
  - [ ]\* 8.3 Write unit tests for ChatRoomService
    - Test room creation with valid data
    - Test room retrieval by id
    - Test listing all rooms
    - Test adding and removing members
    - Test membership validation
    - _Requirements: 5.2, 5.4, 6.4_

- [x] 9. Checkpoint - Verify chat room management
  - Ensure all tests pass, ask the user if questions arise.

### Message Handling and Broadcasting

- [x] 10. Implement message services and STOMP controllers
  - [x] 10.1 Create ChatMessageService for message operations
    - Implement sendMessage method that validates, persists Message, and broadcasts to topic
    - Use SimpMessagingTemplate to publish to /topic/room/{roomId}
    - Implement getMessageHistory method that retrieves paginated Messages from repository
    - Validate that sender is a member of the ChatRoom before sending
    - _Requirements: 3.1, 3.2, 3.3, 4.1, 6.1, 6.2, 8.1_
  - [x] 10.2 Create ChatMessageController for STOMP message handling
    - Implement @MessageMapping("/chat.send/{roomId}") that receives Message and delegates to service
    - Implement @MessageMapping("/room.join/{roomId}") that adds User to room and broadcasts JOIN message
    - Implement @MessageMapping("/room.leave/{roomId}") that removes User from room and broadcasts LEAVE message
    - Add @MessageExceptionHandler to catch errors and send to /user/queue/errors
    - _Requirements: 3.1, 3.2, 5.1, 5.3, 9.3_
  - [x] 10.3 Create REST controller for message history
    - Implement GET /api/rooms/{roomId}/messages endpoint with pagination support
    - Return messages in chronological order with page metadata
    - Validate that requesting User is a member of the ChatRoom
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 11.3_
  - [ ]\* 10.4 Write unit tests for ChatMessageService
    - Test message sending with valid data
    - Test message validation and error handling
    - Test message broadcasting to correct topic
    - Test message history retrieval with pagination
    - Test membership validation before sending
    - _Requirements: 3.2, 3.3, 3.5, 4.1, 4.2, 4.3, 4.5_

- [x] 11. Checkpoint - Verify message handling
  - Ensure all tests pass, ask the user if questions arise.

### User Presence Tracking

- [x] 12. Implement user presence service
  - [x] 12.1 Create UserPresenceService for presence tracking
    - Implement markUserOnline method that updates User.online and User.lastSeen
    - Implement markUserOffline method that updates User.online and User.lastSeen
    - Implement publishPresenceUpdate method that broadcasts to /topic/presence/{roomId}
    - Implement getOnlineUsers method that returns online Users in a ChatRoom
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  - [x] 12.2 Integrate presence service with WebSocket event listener
    - Call markUserOnline in SessionConnectEvent handler
    - Call markUserOffline in SessionDisconnectEvent handler
    - Publish presence updates to all rooms where User is a member
    - _Requirements: 7.1, 7.2, 7.3, 7.5_
  - [ ]\* 12.3 Write unit tests for UserPresenceService
    - Test marking user online updates status and timestamp
    - Test marking user offline updates status and timestamp
    - Test presence updates are published to correct topics
    - Test querying online users in a room
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 13. Checkpoint - Verify presence tracking
  - Ensure all tests pass, ask the user if questions arise.

### Error Handling and Logging

- [x] 14. Implement comprehensive error handling
  - [x] 14.1 Create custom exception classes
    - Create ChatApplicationException base class with errorCode and httpStatus fields
    - Create UserNotFoundException, RoomNotFoundException, UnauthorizedException, ValidationException, WebSocketException subclasses
    - _Requirements: 9.1, 9.2, 9.3_
  - [x] 14.2 Create global exception handler
    - Implement @ControllerAdvice class with @ExceptionHandler methods
    - Handle ChatApplicationException and return ErrorResponse with appropriate HTTP status
    - Handle MethodArgumentNotValidException for validation errors
    - Handle generic exceptions with 500 Internal Server Error
    - _Requirements: 9.1, 9.2, 11.5_
  - [x] 14.3 Add logging throughout the application
    - Add SLF4J logger to all service and controller classes
    - Log authentication attempts with success/failure status
    - Log WebSocket connection and disconnection events
    - Log message sending and broadcasting events
    - Log all exceptions with stack traces
    - Configure log levels in application.yml for dev and prod profiles
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 15. Checkpoint - Verify error handling
  - Ensure all tests pass, ask the user if questions arise.

### Backend Testing and Validation

- [ ]\* 16. Write integration tests for backend
  - [ ]\* 16.1 Write WebSocket integration tests
    - Test STOMP connection establishment with valid JWT
    - Test STOMP connection rejection with invalid JWT
    - Test message sending and receiving through STOMP
    - Test subscription to room topics
    - Test presence updates on connect/disconnect
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 4.1, 7.1, 7.2_
  - [ ]\* 16.2 Write REST API integration tests
    - Test user registration and login flow
    - Test room creation and listing
    - Test message history retrieval with pagination
    - Test authentication and authorization
    - _Requirements: 1.1, 1.2, 6.1, 6.3, 11.1, 11.2, 11.3_
  - [ ]\* 16.3 Write database integration tests with Testcontainers
    - Test User repository operations
    - Test ChatRoom repository operations
    - Test Message repository operations with pagination
    - Test RoomMembership repository operations
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 17. Checkpoint - Backend implementation complete
  - Ensure all tests pass, ask the user if questions arise.

### Frontend Project Setup

- [x] 18. Set up Next.js project with TypeScript
  - Create Next.js 14+ project with App Router and TypeScript using create-next-app
  - Configure tsconfig.json with strict mode, path aliases, and compiler options
  - Install dependencies: @stomp/stompjs, sockjs-client, zustand, tailwindcss
  - Install dev dependencies: @types/sockjs-client, @types/node, @types/react
  - Configure Tailwind CSS with mobile-first breakpoints
  - Create directory structure: app/, components/, lib/, types/, utils/
  - _Requirements: 13.1, 13.5, 17.4_

- [x] 19. Create TypeScript type definitions
  - [x] 19.1 Define domain model types
    - Create types/models.ts with User, ChatRoom, Message, RoomMembership interfaces
    - Define MessageType and MemberRole enums
    - _Requirements: 17.1_
  - [x] 19.2 Define API types
    - Create types/api.ts with LoginRequest, LoginResponse, RegisterRequest, CreateRoomRequest, MessageHistoryResponse interfaces
    - _Requirements: 17.2_
  - [x] 19.3 Define STOMP types
    - Create types/stomp.ts with StompMessage, ChatMessagePayload, PresencePayload, JoinLeavePayload interfaces
    - _Requirements: 17.1_

- [x] 20. Checkpoint - Frontend setup complete
  - Ensure all tests pass, ask the user if questions arise.

### Frontend State Management

- [x] 21. Implement Zustand stores with TypeScript
  - [x] 21.1 Create auth store
    - Create lib/store/authStore.ts with AuthState interface
    - Implement user, token, isAuthenticated state
    - Implement login, logout, register actions with type-safe API calls
    - Persist token to localStorage with typed storage utilities
    - _Requirements: 16.1, 16.2, 16.4, 17.3_
  - [x] 21.2 Create chat store
    - Create lib/store/chatStore.ts with ChatState interface
    - Implement rooms, currentRoom, messages state with Map<string, Message[]>
    - Implement addMessage, setCurrentRoom, loadMessages actions
    - _Requirements: 16.1, 16.2, 16.3, 17.3_
  - [x] 21.3 Create connection store
    - Create lib/store/connectionStore.ts with ConnectionState interface
    - Implement client, connected, connecting, error state
    - Implement connect, disconnect, sendMessage actions
    - Handle STOMP client lifecycle and subscriptions
    - _Requirements: 16.1, 16.3, 16.5, 17.3_

- [x] 22. Checkpoint - State management complete
  - Ensure all tests pass, ask the user if questions arise.

### Frontend API Integration

- [x] 23. Implement HTTP API client
  - [x] 23.1 Create base API client with error handling
    - Create lib/api/client.ts with apiCall function
    - Add Authorization header with JWT token
    - Handle HTTP errors and throw typed ApiError, NetworkError
    - _Requirements: 14.1, 14.2, 17.2_
  - [x] 23.2 Create auth API functions
    - Create lib/api/auth.ts with login, register, getCurrentUser, updateProfile functions
    - Use typed request and response interfaces
    - _Requirements: 1.1, 1.2, 11.1, 11.4, 17.2_
  - [x] 23.3 Create room API functions
    - Create lib/api/rooms.ts with createRoom, listRooms, getRoomDetails, getRoomMembers functions
    - Use typed request and response interfaces
    - _Requirements: 11.2, 17.2_
  - [x] 23.4 Create message API functions
    - Create lib/api/messages.ts with getMessageHistory function with pagination support
    - Use typed request and response interfaces
    - _Requirements: 6.1, 6.3, 11.3, 17.2_

- [x] 24. Checkpoint - API integration complete
  - Ensure all tests pass, ask the user if questions arise.

### Frontend STOMP Client Integration

- [x] 25. Implement STOMP client with TypeScript
  - [x] 25.1 Create STOMP client factory
    - Create lib/stomp/client.ts with createStompClient function
    - Configure SockJS WebSocket factory pointing to http://localhost:8080/ws
    - Add Authorization header with JWT token in connectHeaders
    - Configure reconnection delay, heartbeat intervals, and debug logging
    - _Requirements: 14.1, 14.4, 17.1_
  - [x] 25.2 Create React hooks for STOMP
    - Create lib/stomp/hooks.ts with useStompSubscription hook
    - Accept destination and callback with generic type parameter
    - Subscribe to destination when client is connected
    - Unsubscribe on cleanup
    - Parse message body as JSON with type safety
    - _Requirements: 14.1, 14.2, 16.3, 17.3_
  - [x] 25.3 Integrate STOMP client with connection store
    - Update connection store to create and manage STOMP client instance
    - Implement connect action that creates client and activates connection
    - Implement disconnect action that deactivates client
    - Implement sendMessage action that sends STOMP frames to destinations
    - Handle onStompError and onWebSocketError callbacks
    - _Requirements: 14.1, 14.3, 14.4, 16.5, 17.3_

- [x] 26. Checkpoint - STOMP integration complete
  - Ensure all tests pass, ask the user if questions arise.

### Frontend UI Components

- [ ] 27. Create reusable UI components with mobile-first design
  - [x] 27.1 Create base UI components
    - Create components/ui/Button.tsx with mobile-friendly touch targets (44x44px minimum)
    - Create components/ui/Input.tsx with mobile-optimized input fields
    - Create components/ui/Card.tsx with responsive padding and shadows
    - Use Tailwind CSS with mobile-first breakpoints
    - _Requirements: 13.2, 13.3, 13.4, 13.5, 15.5_
  - [x] 27.2 Create authentication form components
    - Create components/auth/LoginForm.tsx with username and password inputs
    - Create components/auth/RegisterForm.tsx with username, email, password, displayName inputs
    - Add form validation and error display
    - Use auth store for login and register actions
    - _Requirements: 1.1, 1.2, 15.1, 15.2, 17.3_
  - [x] 27.3 Create chat UI components
    - Create components/chat/MessageList.tsx that displays messages with sender, timestamp, content
    - Implement auto-scroll to bottom on new messages
    - Create components/chat/MessageInput.tsx with text input and send button
    - Handle Enter key to send message
    - Create components/chat/UserList.tsx that displays online users with presence indicators
    - Create components/chat/RoomSelector.tsx for room navigation
    - _Requirements: 14.2, 15.1, 15.2, 15.3, 15.4, 17.3_

- [ ] 28. Checkpoint - UI components complete
  - Ensure all tests pass, ask the user if questions arise.

### Frontend Pages and Routing

- [ ] 29. Create Next.js pages with App Router
  - [x] 29.1 Create root layout and home page
    - Create app/layout.tsx with HTML structure and global styles
    - Create app/page.tsx as landing page with navigation to login/register
    - _Requirements: 13.1, 15.4_
  - [x] 29.2 Create authentication pages
    - Create app/auth/login/page.tsx with LoginForm component
    - Create app/auth/register/page.tsx with RegisterForm component
    - Redirect to /chat after successful authentication
    - _Requirements: 1.1, 1.2, 15.1, 15.2_
  - [x] 29.3 Create chat pages
    - Create app/chat/layout.tsx with chat-specific layout
    - Create app/chat/page.tsx that displays list of available rooms
    - Create app/chat/[roomId]/page.tsx that displays MessageList, MessageInput, UserList for specific room
    - Protect chat pages with authentication check
    - _Requirements: 5.1, 14.2, 15.1, 15.2, 15.3, 15.4_

- [ ] 30. Checkpoint - Pages and routing complete
  - Ensure all tests pass, ask the user if questions arise.

### Frontend Real-Time Integration

- [ ] 31. Integrate STOMP subscriptions in chat pages
  - [x] 31.1 Connect to STOMP on authentication
    - In chat layout, connect to STOMP server when user is authenticated
    - Pass JWT token to STOMP client
    - Display connection status indicator
    - Handle connection errors and reconnection
    - _Requirements: 14.1, 14.3, 14.4, 16.1_
  - [ ] 31.2 Subscribe to room messages
    - In chat room page, subscribe to /topic/room/{roomId} when room is loaded
    - Use useStompSubscription hook with ChatMessagePayload type
    - Add received messages to chat store
    - Update UI to display new messages
    - _Requirements: 4.1, 4.2, 14.2, 16.3_
  - [ ] 31.3 Subscribe to presence updates
    - In chat room page, subscribe to /topic/presence/{roomId}
    - Use useStompSubscription hook with PresencePayload type
    - Update user list with online/offline status
    - _Requirements: 7.3, 14.2, 16.3_
  - [ ] 31.4 Implement message sending
    - In MessageInput component, send message using connection store sendMessage action
    - Send to /app/chat.send/{roomId} destination
    - Include message content and metadata in STOMP frame body
    - _Requirements: 3.1, 14.3, 16.3_
  - [ ] 31.5 Implement room join/leave
    - Send STOMP frame to /app/room.join/{roomId} when entering room
    - Send STOMP frame to /app/room.leave/{roomId} when leaving room
    - Handle JOIN and LEAVE system messages in message list
    - _Requirements: 5.1, 5.3, 16.3, 16.5_

- [ ] 32. Checkpoint - Real-time integration complete
  - Ensure all tests pass, ask the user if questions arise.

### Frontend Accessibility and Polish

- [ ] 33. Implement accessibility features
  - [ ] 33.1 Add semantic HTML and ARIA labels
    - Use semantic HTML elements (nav, main, article, aside) in layouts
    - Add ARIA labels to interactive components (buttons, inputs, links)
    - Add role attributes where appropriate
    - _Requirements: 18.1, 18.2_
  - [ ] 33.2 Implement keyboard navigation
    - Ensure all interactive elements are keyboard accessible
    - Add visible focus indicators with Tailwind focus: utilities
    - Test tab order and keyboard shortcuts
    - _Requirements: 18.3, 18.5_
  - [ ] 33.3 Verify color contrast and responsive design
    - Check color contrast ratios meet WCAG AA standards
    - Test responsive layout on mobile (< 768px), tablet (768px-1024px), desktop (> 1024px)
    - Verify touch targets are 44x44px minimum on mobile
    - _Requirements: 13.2, 13.3, 13.4, 15.5, 18.4_

- [ ] 34. Checkpoint - Accessibility complete
  - Ensure all tests pass, ask the user if questions arise.

### Frontend Testing

- [ ]\* 35. Write frontend tests
  - [ ]\* 35.1 Write component unit tests with Jest and React Testing Library
    - Test MessageList renders messages correctly
    - Test MessageInput sends message on Enter key
    - Test UserList displays online users
    - Test authentication forms validate input
    - _Requirements: 15.1, 15.2, 15.3_
  - [ ]\* 35.2 Write store unit tests
    - Test auth store login, logout, register actions
    - Test chat store addMessage, setCurrentRoom actions
    - Test connection store connect, disconnect, sendMessage actions
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_
  - [ ]\* 35.3 Write API integration tests with MSW
    - Mock API endpoints with Mock Service Worker
    - Test login, register, room creation, message history API calls
    - Test error handling for failed API calls
    - _Requirements: 1.1, 1.2, 6.1, 11.1, 11.2, 11.3_

- [ ] 36. Checkpoint - Frontend testing complete
  - Ensure all tests pass, ask the user if questions arise.

### Integration and Deployment

- [ ] 37. End-to-end integration testing
  - [ ] 37.1 Test complete user flow
    - Start backend server on port 8080
    - Start frontend dev server on port 3000
    - Test user registration and login
    - Test creating and joining chat rooms
    - Test sending and receiving messages in real-time
    - Test presence updates when users connect/disconnect
    - Test message history loading
    - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1, 6.1, 7.1, 14.1_
  - [ ] 37.2 Test concurrent users
    - Open multiple browser windows/tabs
    - Login with different users
    - Join same chat room
    - Send messages from multiple users simultaneously
    - Verify all users receive all messages
    - Verify presence updates work correctly
    - _Requirements: 4.2, 4.3, 10.1, 10.2, 10.3_
  - [ ] 37.3 Test error scenarios
    - Test connection loss and reconnection
    - Test invalid authentication
    - Test sending messages to non-existent rooms
    - Test exceeding concurrent connection limit
    - Verify error messages are user-friendly
    - _Requirements: 2.4, 9.1, 9.2, 9.3, 10.5, 14.4_

- [ ] 38. Create deployment documentation
  - Document backend build and run instructions (Maven, Java 21)
  - Document frontend build and run instructions (npm/yarn, Node.js)
  - Document PostgreSQL database setup and configuration
  - Document environment variables and configuration files
  - Document CORS and security configuration for production
  - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [ ] 39. Final checkpoint - Implementation complete
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation throughout development
- Backend tasks (1-17) can be developed independently from frontend tasks (18-36)
- Integration tasks (37-39) require both backend and frontend to be complete
- The system is designed for 10-20 concurrent users as a learning project
- STOMP over WebSocket provides structured pub/sub messaging for real-time communication
- TypeScript provides compile-time type safety throughout the frontend
- Mobile-first design ensures the UI works well on all device sizes
