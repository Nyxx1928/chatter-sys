# Requirements Document

## Introduction

The Direct Messaging (DM) feature adds private one-on-one chat rooms between friends to the existing real-time chat system. When two users become friends (via the existing friend request/acceptance flow), a private DM room is automatically created and made visible to both users in their chat room list. Only the two friends involved can send, receive, and view messages in that room — no other user can join or view it. The feature builds on the existing group chat infrastructure (ChatRoom, RoomMembership, STOMP messaging) and the existing Friendship model from the social-discovery-and-room-management spec.

## Glossary

- **DM_Room**: A private ChatRoom automatically created between exactly two friends when a Friendship is established. It is distinct from group Chat_Rooms in that membership is fixed, immutable, and limited to the two friends.
- **Friendship**: A mutual relationship between two Users established when a friend request is accepted (defined in the social-discovery-and-room-management spec).
- **DM_Participant**: One of the two Users who are members of a DM_Room.
- **Chat_Server**: The Spring Boot application that manages connections, message routing, and data persistence.
- **Chat_Client**: The Next.js frontend application.
- **DM_Service**: The backend service component responsible for creating and managing DM_Rooms.
- **Room_List**: The list of chat rooms displayed to a User in the Chat_Client sidebar, containing both group Chat_Rooms and DM_Rooms.
- **STOMP_Broker**: The Spring Boot in-memory message broker that routes real-time messages over WebSocket.
- **Message_Repository**: The database persistence layer that stores Message history.
- **RoomMembership_Repository**: The database persistence layer that stores room membership records.

## Requirements

### Requirement 1: Automatic DM Room Creation on Friend Acceptance

**User Story:** As a user, I want a private chat room to be automatically created when I accept a friend request, so that I can immediately start a one-on-one conversation without any extra steps.

#### Acceptance Criteria

1. WHEN a User accepts a friend request, THE DM_Service SHALL create a DM_Room for the two newly-friended Users.
2. WHEN a DM_Room is created, THE DM_Service SHALL add both DM_Participants as members of the DM_Room with a fixed membership that cannot be altered.
3. WHEN a DM_Room is created, THE DM_Service SHALL persist the DM_Room and both memberships to the database before returning a response to the Chat_Client.
4. IF a DM_Room already exists between two Users, THEN THE DM_Service SHALL NOT create a duplicate DM_Room and SHALL return the existing DM_Room.
5. THE DM_Service SHALL assign the DM_Room a system-generated name derived from the two DM_Participants' usernames so that it is uniquely identifiable.

### Requirement 2: DM Room Visibility in Room List

**User Story:** As a user, I want to see my DM rooms in my chat room list alongside group rooms, so that I can access all my conversations from one place.

#### Acceptance Criteria

1. WHEN a User requests their Room_List, THE Chat_Server SHALL include all DM_Rooms where the User is a DM_Participant.
2. WHEN a User requests their Room_List, THE Chat_Server SHALL include a type indicator that distinguishes DM_Rooms from group Chat_Rooms in the response payload.
3. WHEN the Chat_Client renders the Room_List, THE Chat_Client SHALL display DM_Rooms with the other DM_Participant's display name as the room label, rather than the system-generated room name.
4. WHEN the Chat_Client renders the Room_List, THE Chat_Client SHALL display a distinct visual indicator (such as a person icon) on DM_Room entries to differentiate them from group Chat_Rooms.
5. WHEN a new DM_Room is created, THE Chat_Client SHALL update the Room_List to include the new DM_Room without requiring a full page reload.

### Requirement 3: Private Messaging in DM Rooms

**User Story:** As a user, I want to send and receive messages in a DM room, so that I can have a private conversation with my friend.

#### Acceptance Criteria

1. WHEN a DM_Participant sends a message to a DM_Room, THE Chat_Server SHALL persist the message and broadcast it to the other DM_Participant via the STOMP_Broker.
2. WHEN a DM_Participant sends a message to a DM_Room, THE STOMP_Broker SHALL deliver the message only to the two DM_Participants subscribed to that DM_Room's topic.
3. WHEN a DM_Participant requests message history for a DM_Room, THE Chat_Server SHALL return messages in chronological order from the Message_Repository.
4. THE Chat_Client SHALL allow a DM_Participant to send messages using the same MessageInput component used for group Chat_Rooms.
5. THE Chat_Client SHALL display DM messages using the same MessageList component used for group Chat_Rooms.

### Requirement 4: Access Control for DM Rooms

**User Story:** As a user, I want my DM conversations to be private, so that only my friend and I can read or send messages in our DM room.

#### Acceptance Criteria

1. WHEN a User who is not a DM_Participant attempts to send a message to a DM_Room, THE Chat_Server SHALL reject the request and return a 403 Forbidden response.
2. WHEN a User who is not a DM_Participant attempts to retrieve message history for a DM_Room, THE Chat_Server SHALL reject the request and return a 403 Forbidden response.
3. WHEN a User who is not a DM_Participant attempts to retrieve details of a DM_Room, THE Chat_Server SHALL reject the request and return a 403 Forbidden response.
4. THE Chat_Server SHALL prevent any User from being added to a DM_Room as a member after the DM_Room is created.
5. THE Chat_Server SHALL prevent any User from deleting a DM_Room.

### Requirement 5: DM Room Persistence and History

**User Story:** As a user, I want my DM message history to be preserved, so that I can read past conversations when I return to the app.

#### Acceptance Criteria

1. THE Chat_Server SHALL persist all DM messages to the Message_Repository before broadcasting them via the STOMP_Broker.
2. WHEN a DM_Participant opens a DM_Room, THE Chat_Client SHALL load and display the message history from the Chat_Server.
3. THE Chat_Server SHALL return DM message history paginated, with a default page size of 50 messages ordered from oldest to newest.
4. WHEN the Chat_Server starts, THE Chat_Server SHALL load existing DM_Room and membership data from the database, preserving all prior DM conversations.

### Requirement 6: DM Room Identification and Type Differentiation

**User Story:** As a developer, I want DM rooms to be clearly typed in the data model, so that the system can apply different rules and rendering logic to DM rooms versus group rooms.

#### Acceptance Criteria

1. THE Chat_Server SHALL store a room type field on each ChatRoom record that distinguishes DM_Rooms (type: `DIRECT`) from group Chat_Rooms (type: `GROUP`).
2. WHEN the Chat_Server returns a ChatRoom in any API response, THE Chat_Server SHALL include the room type field in the response payload.
3. THE Chat_Client SHALL define a TypeScript type that includes the room type field and use it consistently across all components that handle ChatRoom data.
4. WHEN the Chat_Client determines whether to show invite or delete controls, THE Chat_Client SHALL suppress those controls for DM_Rooms.

### Requirement 7: Accessibility and UX Consistency

**User Story:** As a user, I want the DM feature to feel consistent with the rest of the chat UI, so that I can use it without learning new interaction patterns.

#### Acceptance Criteria

1. WHEN DM_Room entries are rendered in the Room_List, THE Chat_Client SHALL provide ARIA labels that identify the room as a direct message with the other participant's name.
2. WHEN a DM_Room is selected, THE Chat_Client SHALL display the other DM_Participant's display name and online status in the chat header, consistent with the existing room header pattern.
3. THE Chat_Client SHALL support keyboard navigation to DM_Room entries in the Room_List with the same tab order and focus behavior as group Chat_Room entries.
4. WHEN a DM_Room is open on a mobile viewport, THE Chat_Client SHALL display the same back-button navigation pattern used for group Chat_Rooms.
