# Integration Testing Guide

This document provides comprehensive instructions for end-to-end integration testing of the Real-Time Chat System.

## Prerequisites

### Backend Requirements
- Java 21 installed
- PostgreSQL database running
- Maven installed
- Backend application built

### Frontend Requirements
- Node.js 18+ installed
- npm or yarn installed
- Frontend dependencies installed

### Database Setup
```bash
# Create database
createdb chatdb

# Or using psql
psql -U postgres
CREATE DATABASE chatdb;
\q
```

## Starting the Servers

### 1. Start Backend Server

```bash
# From project root
mvn spring-boot:run

# Or if already built
java -jar target/chat-application-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
Started ChatApplication in X.XXX seconds
```

**Verify Backend:**
- Server running on: http://localhost:8080
- Health check: http://localhost:8080/actuator/health (if actuator enabled)
- WebSocket endpoint: ws://localhost:8080/ws

### 2. Start Frontend Server

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

**Expected Output:**
```
ready - started server on 0.0.0.0:3000, url: http://localhost:3000
```

**Verify Frontend:**
- Application running on: http://localhost:3000
- Should see landing page with login/register options

## Test Scenarios

### Task 37.1: Test Complete User Flow

This test verifies the entire user journey from registration to real-time messaging.

#### Test Steps:

**1. User Registration**
- [ ] Navigate to http://localhost:3000
- [ ] Click "Register" or navigate to /auth/register
- [ ] Fill in registration form:
  - Username: testuser1
  - Email: testuser1@example.com
  - Display Name: Test User One
  - Password: password123
  - Confirm Password: password123
- [ ] Click "Create Account"
- [ ] Verify redirect to login page
- [ ] Verify success message (if implemented)

**Expected Result:** User successfully registered and redirected to login.

**2. User Login**
- [ ] Enter credentials:
  - Username: testuser1
  - Password: password123
- [ ] Click "Log In"
- [ ] Verify redirect to /chat
- [ ] Verify connection status shows "Connected" (green indicator)

**Expected Result:** User successfully logged in and STOMP connection established.

**3. View Chat Rooms**
- [ ] Verify chat rooms list is displayed
- [ ] Verify room count is shown
- [ ] Verify "Refresh" button is present
- [ ] Click "Refresh" to reload rooms

**Expected Result:** Available chat rooms are displayed.

**4. Join Chat Room**
- [ ] Click on a chat room from the list
- [ ] Verify redirect to /chat/{roomId}
- [ ] Verify room name is displayed in header
- [ ] Verify message list is displayed (may be empty)
- [ ] Verify message input is enabled
- [ ] Verify user list is displayed (desktop) or accessible (mobile)

**Expected Result:** Successfully entered chat room with all UI elements visible.

**5. Send Messages**
- [ ] Type a message in the input field: "Hello, this is my first message!"
- [ ] Press Enter or click Send button
- [ ] Verify message appears in message list
- [ ] Verify message shows correct sender name
- [ ] Verify message shows timestamp
- [ ] Verify message input is cleared after sending

**Expected Result:** Message successfully sent and displayed.

**6. View Message History**
- [ ] Scroll up in message list
- [ ] Verify previous messages are displayed (if any)
- [ ] Verify messages are in chronological order
- [ ] Verify pagination works (if implemented)

**Expected Result:** Message history is accessible and properly ordered.

**7. View User Presence**
- [ ] Open user list (sidebar on desktop, modal on mobile)
- [ ] Verify current user is listed
- [ ] Verify current user shows "Online" status
- [ ] Verify online/offline indicators are visible

**Expected Result:** User presence information is displayed correctly.

**8. Leave Room**
- [ ] Click "Back to Rooms" or navigate to /chat
- [ ] Verify redirect to rooms list
- [ ] Verify LEAVE system message was sent (check in another browser)

**Expected Result:** Successfully left room and returned to rooms list.

**9. Logout**
- [ ] Click "Logout" button
- [ ] Verify redirect to home page
- [ ] Verify connection status shows "Disconnected"
- [ ] Verify cannot access /chat without authentication

**Expected Result:** Successfully logged out and STOMP connection closed.

### Task 37.2: Test Concurrent Users

This test verifies real-time message delivery and presence updates with multiple users.

#### Setup:
- Open 3 different browsers or incognito windows
- Register 3 different users:
  - User 1: testuser1 / password123
  - User 2: testuser2 / password123
  - User 3: testuser3 / password123

#### Test Steps:

**1. Multiple Users Join Same Room**
- [ ] User 1: Login and join "General" room
- [ ] User 2: Login and join "General" room
- [ ] User 3: Login and join "General" room
- [ ] Verify all users see JOIN system messages for other users
- [ ] Verify user list shows all 3 users as online

**Expected Result:** All users successfully joined and see each other online.

**2. Real-Time Message Broadcasting**
- [ ] User 1: Send message "Hello from User 1"
- [ ] Verify User 2 and User 3 receive message immediately
- [ ] User 2: Send message "Hello from User 2"
- [ ] Verify User 1 and User 3 receive message immediately
- [ ] User 3: Send message "Hello from User 3"
- [ ] Verify User 1 and User 2 receive message immediately

**Expected Result:** All messages are delivered to all users within 100ms.

**3. Message Order Consistency**
- [ ] User 1: Send "Message 1"
- [ ] User 2: Send "Message 2"
- [ ] User 3: Send "Message 3"
- [ ] Verify all users see messages in same order
- [ ] Verify timestamps are consistent

**Expected Result:** Message order is consistent across all clients.

**4. Rapid Message Sending**
- [ ] User 1: Send 10 messages rapidly (press Enter repeatedly)
- [ ] Verify all messages are delivered to all users
- [ ] Verify no messages are lost
- [ ] Verify no duplicate messages

**Expected Result:** All messages delivered without loss or duplication.

**5. Presence Updates**
- [ ] User 3: Logout or close browser tab
- [ ] Verify User 1 and User 2 see User 3 go offline
- [ ] Verify LEAVE system message appears
- [ ] Verify user list updates to show User 3 as offline
- [ ] User 3: Login and rejoin room
- [ ] Verify User 1 and User 2 see User 3 come online
- [ ] Verify JOIN system message appears

**Expected Result:** Presence updates are delivered in real-time to all users.

**6. Room Switching**
- [ ] User 1: Leave "General" room and join "Random" room
- [ ] Verify User 2 and User 3 see LEAVE message in "General"
- [ ] User 2: Send message in "General"
- [ ] Verify User 1 does NOT receive message (in different room)
- [ ] User 1: Send message in "Random"
- [ ] Verify User 2 and User 3 do NOT receive message

**Expected Result:** Messages are only delivered to users in the same room.

**7. Concurrent Room Access**
- [ ] User 1: In "General" room
- [ ] User 2: In "General" room
- [ ] User 3: In "Random" room
- [ ] Send messages from all users simultaneously
- [ ] Verify messages are delivered to correct rooms only
- [ ] Verify no cross-room message leakage

**Expected Result:** Room isolation is maintained with concurrent access.

### Task 37.3: Test Error Scenarios

This test verifies error handling and recovery mechanisms.

#### Test Steps:

**1. Invalid Authentication**
- [ ] Navigate to /auth/login
- [ ] Enter invalid credentials:
  - Username: nonexistent
  - Password: wrongpassword
- [ ] Click "Log In"
- [ ] Verify error message is displayed
- [ ] Verify user is NOT logged in
- [ ] Verify no redirect occurs

**Expected Result:** Clear error message displayed, login prevented.

**2. Duplicate Registration**
- [ ] Navigate to /auth/register
- [ ] Try to register with existing username
- [ ] Verify error message: "Username is already taken"
- [ ] Try to register with existing email
- [ ] Verify error message: "Email is already registered"

**Expected Result:** Duplicate registration prevented with clear error messages.

**3. Connection Loss and Reconnection**
- [ ] Login and join a chat room
- [ ] Stop the backend server (Ctrl+C)
- [ ] Verify connection status shows "Disconnected" (red indicator)
- [ ] Verify error banner is displayed
- [ ] Try to send a message
- [ ] Verify message input is disabled or shows error
- [ ] Restart backend server
- [ ] Wait for automatic reconnection (5 seconds)
- [ ] Verify connection status shows "Connected" (green indicator)
- [ ] Verify can send messages again

**Expected Result:** Connection loss detected, automatic reconnection successful.

**4. Invalid Message Content**
- [ ] Login and join a chat room
- [ ] Try to send empty message (only whitespace)
- [ ] Verify message is not sent
- [ ] Try to send message exceeding 2000 characters
- [ ] Verify character count warning appears
- [ ] Verify send button is disabled

**Expected Result:** Invalid messages prevented with clear feedback.

**5. Unauthorized Room Access**
- [ ] Login as User 1
- [ ] Note a room ID from the rooms list
- [ ] Logout
- [ ] Try to access /chat/{roomId} directly without authentication
- [ ] Verify redirect to /auth/login
- [ ] Verify cannot access room without authentication

**Expected Result:** Unauthorized access prevented, redirect to login.

**6. Network Errors**
- [ ] Login and join a chat room
- [ ] Open browser DevTools → Network tab
- [ ] Throttle network to "Slow 3G"
- [ ] Send a message
- [ ] Verify message eventually sends (may take longer)
- [ ] Verify no error messages for slow network
- [ ] Restore normal network speed

**Expected Result:** Application handles slow network gracefully.

**7. Browser Tab Close/Reopen**
- [ ] Login and join a chat room
- [ ] Close browser tab
- [ ] Reopen browser and navigate to /chat
- [ ] Verify user is still logged in (token persisted)
- [ ] Verify STOMP connection is re-established
- [ ] Verify can send and receive messages

**Expected Result:** Session persists across tab close/reopen.

**8. Multiple Tabs Same User**
- [ ] Login in Tab 1
- [ ] Open Tab 2 with same browser
- [ ] Navigate to /chat in Tab 2
- [ ] Verify both tabs show "Connected"
- [ ] Join same room in both tabs
- [ ] Send message from Tab 1
- [ ] Verify message appears in both tabs

**Expected Result:** Multiple tabs work correctly with same user.

**9. Exceeding Connection Limit**
- [ ] Open 21 browser windows/tabs
- [ ] Login with different users in each
- [ ] Verify first 20 connections succeed
- [ ] Verify 21st connection is rejected or shows error
- [ ] Close one connection
- [ ] Verify new connection can now be established

**Expected Result:** Connection limit enforced, clear error message.

**10. Server Restart During Active Session**
- [ ] Login and join a chat room with multiple users
- [ ] Restart backend server
- [ ] Verify all clients detect disconnection
- [ ] Verify all clients attempt reconnection
- [ ] Verify all clients successfully reconnect
- [ ] Verify message history is still available
- [ ] Verify can send new messages

**Expected Result:** Graceful handling of server restart with automatic recovery.

## Verification Checklist

### Functional Requirements
- [ ] User registration works correctly
- [ ] User login works correctly
- [ ] JWT authentication is enforced
- [ ] STOMP connection establishes on login
- [ ] Chat rooms list displays correctly
- [ ] Can join chat rooms
- [ ] Can send messages
- [ ] Messages are delivered in real-time
- [ ] Message history loads correctly
- [ ] User presence updates in real-time
- [ ] Can leave chat rooms
- [ ] Logout works correctly

### Real-Time Communication
- [ ] Messages delivered within 100ms
- [ ] Message order is consistent
- [ ] No message loss
- [ ] No duplicate messages
- [ ] Presence updates are immediate
- [ ] Room isolation is maintained
- [ ] Multiple concurrent users work correctly

### Error Handling
- [ ] Invalid credentials rejected
- [ ] Duplicate registration prevented
- [ ] Connection loss detected
- [ ] Automatic reconnection works
- [ ] Invalid messages prevented
- [ ] Unauthorized access blocked
- [ ] Network errors handled gracefully
- [ ] Connection limit enforced

### User Experience
- [ ] UI is responsive and smooth
- [ ] Loading states are clear
- [ ] Error messages are user-friendly
- [ ] Connection status is visible
- [ ] Keyboard navigation works
- [ ] Mobile layout works correctly
- [ ] Desktop layout works correctly

## Performance Metrics

### Expected Performance
- **Message Delivery**: < 100ms under normal load
- **Connection Establishment**: < 2 seconds
- **Reconnection**: < 5 seconds
- **Message History Load**: < 1 second for 50 messages
- **Room List Load**: < 500ms

### Load Testing (Optional)
- [ ] 10 concurrent users: All messages delivered
- [ ] 20 concurrent users: All messages delivered
- [ ] 100 messages/minute: No performance degradation
- [ ] 1000 messages total: Message history loads quickly

## Common Issues and Solutions

### Issue: Backend won't start
**Symptoms:** Port 8080 already in use
**Solution:** 
```bash
# Find process using port 8080
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill the process or change port in application.yml
```

### Issue: Frontend won't start
**Symptoms:** Port 3000 already in use
**Solution:**
```bash
# Kill process or use different port
PORT=3001 npm run dev
```

### Issue: Database connection failed
**Symptoms:** "Connection refused" or "Database does not exist"
**Solution:**
```bash
# Ensure PostgreSQL is running
pg_ctl status  # Check status
pg_ctl start   # Start if not running

# Create database if missing
createdb chatdb
```

### Issue: STOMP connection fails
**Symptoms:** "Disconnected" status, no real-time updates
**Solution:**
1. Check backend logs for WebSocket errors
2. Verify CORS configuration allows frontend origin
3. Check browser console for WebSocket errors
4. Verify JWT token is valid

### Issue: Messages not delivered
**Symptoms:** Messages sent but not received by other users
**Solution:**
1. Check both users are in the same room
2. Verify STOMP subscriptions are active
3. Check backend logs for message broadcasting
4. Verify database persistence is working

## Test Results Template

```markdown
## Test Execution Results

**Date:** YYYY-MM-DD
**Tester:** [Name]
**Environment:** Development

### Task 37.1: Complete User Flow
- [ ] Registration: PASS/FAIL
- [ ] Login: PASS/FAIL
- [ ] View Rooms: PASS/FAIL
- [ ] Join Room: PASS/FAIL
- [ ] Send Messages: PASS/FAIL
- [ ] View History: PASS/FAIL
- [ ] View Presence: PASS/FAIL
- [ ] Leave Room: PASS/FAIL
- [ ] Logout: PASS/FAIL

**Notes:** [Any issues or observations]

### Task 37.2: Concurrent Users
- [ ] Multiple Users Join: PASS/FAIL
- [ ] Message Broadcasting: PASS/FAIL
- [ ] Message Order: PASS/FAIL
- [ ] Rapid Messaging: PASS/FAIL
- [ ] Presence Updates: PASS/FAIL
- [ ] Room Switching: PASS/FAIL
- [ ] Concurrent Access: PASS/FAIL

**Notes:** [Any issues or observations]

### Task 37.3: Error Scenarios
- [ ] Invalid Auth: PASS/FAIL
- [ ] Duplicate Registration: PASS/FAIL
- [ ] Connection Loss: PASS/FAIL
- [ ] Invalid Messages: PASS/FAIL
- [ ] Unauthorized Access: PASS/FAIL
- [ ] Network Errors: PASS/FAIL
- [ ] Tab Close/Reopen: PASS/FAIL
- [ ] Multiple Tabs: PASS/FAIL
- [ ] Connection Limit: PASS/FAIL
- [ ] Server Restart: PASS/FAIL

**Notes:** [Any issues or observations]

### Overall Assessment
**Status:** PASS/FAIL
**Issues Found:** [Count]
**Critical Issues:** [Count]
**Recommendations:** [List any recommendations]
```

## Next Steps

After completing integration testing:
1. Document any bugs found
2. Create GitHub issues for bugs
3. Prioritize and fix critical issues
4. Re-test after fixes
5. Proceed to deployment documentation (Task 38)
