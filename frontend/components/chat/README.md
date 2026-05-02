# Chat Components

This directory contains the chat UI components for the real-time chat system. These components are built with React, TypeScript, and Tailwind CSS following mobile-first design principles.

## Components

### MessageList

Displays chat messages with sender information, timestamps, and content.

**Features:**
- Auto-scroll to bottom on new messages
- Different styling for own messages vs. others
- System message support (JOIN, LEAVE, SYSTEM)
- Responsive message bubbles with max-width
- Accessible with ARIA labels and live regions
- Empty state when no messages

**Props:**
- `messages: Message[]` - Array of messages to display
- `currentUserId?: number` - ID of the current user (for styling own messages)
- `className?: string` - Additional CSS classes

**Usage:**
```tsx
import { MessageList } from '@/components/chat';

<MessageList
  messages={messages}
  currentUserId={currentUser.id}
/>
```

### MessageInput

Text input component for sending messages with a send button.

**Features:**
- Auto-resizing textarea (max 150px height)
- Enter key to send (Shift+Enter for new line)
- Character count when approaching limit (default 2000)
- Disabled state when disconnected
- Mobile-friendly with 44px minimum touch target
- Accessible with ARIA labels and keyboard hints

**Props:**
- `onSend: (content: string) => void` - Callback when message is sent
- `disabled?: boolean` - Disable input when disconnected
- `placeholder?: string` - Input placeholder text
- `maxLength?: number` - Maximum character limit (default 2000)
- `className?: string` - Additional CSS classes

**Usage:**
```tsx
import { MessageInput } from '@/components/chat';

<MessageInput
  onSend={handleSendMessage}
  disabled={!connected}
/>
```

### UserList

Displays online and offline users in the current chat room.

**Features:**
- Presence indicators (green for online, gray for offline)
- Sorted by online status, then alphabetically
- Avatar placeholders with user initials
- Online/Offline badges
- Separate sections for online and offline users
- Empty state when no users

**Props:**
- `users: User[]` - Array of users to display
- `currentUserId?: number` - ID of the current user (marked with "You")
- `className?: string` - Additional CSS classes

**Usage:**
```tsx
import { UserList } from '@/components/chat';

<UserList
  users={roomUsers}
  currentUserId={currentUser.id}
/>
```

### RoomSelector

Room navigation component for switching between chat rooms.

**Features:**
- List of available rooms with descriptions
- Active room highlighting
- Room metadata (creator, creation date)
- Responsive cards with hover states
- Empty state when no rooms
- Accessible with ARIA current page indicator

**Props:**
- `rooms: ChatRoom[]` - Array of available rooms
- `currentRoomId?: number` - ID of the currently active room
- `onRoomSelect: (room: ChatRoom) => void` - Callback when room is selected
- `className?: string` - Additional CSS classes

**Usage:**
```tsx
import { RoomSelector } from '@/components/chat';

<RoomSelector
  rooms={availableRooms}
  currentRoomId={currentRoom?.id}
  onRoomSelect={handleRoomSelect}
/>
```

## Design Principles

### Mobile-First
All components are designed mobile-first with responsive breakpoints:
- Mobile: < 768px (single column, full width)
- Tablet: 768px - 1024px (optimized layout)
- Desktop: > 1024px (multi-column layout)

### Touch Targets
All interactive elements meet the 44x44px minimum touch target size for mobile accessibility.

### Accessibility
- Semantic HTML elements
- ARIA labels and roles
- Keyboard navigation support
- Screen reader friendly
- Sufficient color contrast (WCAG AA)

### Type Safety
All components are fully typed with TypeScript:
- Props interfaces exported
- Domain model types from `types/domain.ts`
- Strict null checks enabled

## Integration

These components integrate with:
- **Zustand stores**: `useChatStore`, `useConnectionStore`, `useAuthStore`
- **STOMP client**: Real-time message subscriptions
- **REST API**: Message history, room data, user data

## Requirements Mapping

- **Requirement 14.2**: Frontend STOMP client integration with real-time message display
- **Requirement 15.1**: Message list UI component with sender, timestamp, content
- **Requirement 15.2**: Message input UI component with send button
- **Requirement 15.3**: User list UI component with online status
- **Requirement 15.4**: Room selector UI component for navigation
- **Requirement 17.3**: TypeScript type definitions for component props and state

## Testing

Components should be tested with:
- **Unit tests**: Jest + React Testing Library
- **Interaction tests**: User events (click, type, keyboard)
- **Accessibility tests**: jest-axe for automated a11y checks
- **Visual tests**: Snapshot tests for UI consistency

Example test:
```tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { MessageInput } from './MessageInput';

test('sends message on Enter key', () => {
  const onSend = jest.fn();
  render(<MessageInput onSend={onSend} />);
  
  const input = screen.getByRole('textbox');
  fireEvent.change(input, { target: { value: 'Hello' } });
  fireEvent.keyDown(input, { key: 'Enter' });
  
  expect(onSend).toHaveBeenCalledWith('Hello');
});
```
