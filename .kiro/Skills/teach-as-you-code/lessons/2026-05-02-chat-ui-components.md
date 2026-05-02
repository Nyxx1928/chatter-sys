# Lesson: Building Chat UI Components with React and TypeScript

## Task Context

This lesson covers the implementation of four essential chat UI components for a real-time chat system: MessageList, MessageInput, UserList, and RoomSelector. These components are built using React, TypeScript, and Tailwind CSS, following mobile-first design principles and accessibility best practices.

The task is part of the Frontend UI Components phase (Task 27.3) in the realtime-chat-system spec. These components integrate with existing Zustand stores (chat store, connection store) and will be used in the chat pages to provide a complete real-time messaging experience.

## Files Modified

- `frontend/components/chat/MessageList.tsx` (created)
- `frontend/components/chat/MessageInput.tsx` (created)
- `frontend/components/chat/UserList.tsx` (created)
- `frontend/components/chat/RoomSelector.tsx` (created)
- `frontend/components/chat/index.ts` (created)
- `frontend/components/chat/README.md` (created)

## Step-by-Step Changes

### 1. MessageList Component

**Purpose:** Display chat messages with sender information, timestamps, and content. Auto-scroll to bottom when new messages arrive.

**Key Implementation Details:**

```typescript
const messagesEndRef = useRef<HTMLDivElement>(null);
const prevMessageCountRef = useRef(messages.length);

useEffect(() => {
  if (messages.length > prevMessageCountRef.current) {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }
  prevMessageCountRef.current = messages.length;
}, [messages]);
```

**What this does:**
- Uses two refs: one for the scroll anchor element, one to track previous message count
- Only auto-scrolls when new messages are added (not on initial load or re-renders)
- Smooth scroll behavior for better UX
- Scroll anchor is placed at the bottom of the message list

**Message Rendering:**
- System messages (JOIN, LEAVE, SYSTEM) are centered and styled differently
- Text messages show sender name, timestamp, and content
- Own messages are right-aligned with blue background
- Other users' messages are left-aligned with gray background
- Message bubbles have max-width (85% on mobile, 75% on tablet, 65% on desktop)

**Timestamp Formatting:**
- Shows time only for today's messages (e.g., "2:30 PM")
- Shows date and time for older messages (e.g., "Jan 22, 2:30 PM")
- Uses `toLocaleString` for locale-aware formatting

### 2. MessageInput Component

**Purpose:** Text input with send button. Handles Enter key to send, Shift+Enter for new line.

**Key Implementation Details:**

```typescript
const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
};
```

**What this does:**
- Intercepts Enter key press
- Sends message only if Shift is not held
- Prevents default behavior (new line) when sending
- Allows Shift+Enter for multi-line messages

**Auto-Resize Textarea:**

```typescript
const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
  const textarea = e.target;
  setContent(textarea.value);
  
  textarea.style.height = 'auto';
  const newHeight = Math.min(textarea.scrollHeight, 150);
  textarea.style.height = `${newHeight}px`;
};
```

**What this does:**
- Resets height to 'auto' to get accurate scrollHeight
- Calculates new height based on content
- Caps maximum height at 150px (prevents textarea from taking over screen)
- Provides smooth growing/shrinking as user types

**Character Limit:**
- Default 2000 characters (configurable via props)
- Shows character count when within 100 characters of limit
- Displays in red when over limit
- Disables send button when over limit

### 3. UserList Component

**Purpose:** Display online and offline users with presence indicators.

**Key Implementation Details:**

**User Sorting:**
```typescript
const sortedUsers = [...users].sort((a, b) => {
  if (a.online !== b.online) {
    return a.online ? -1 : 1;
  }
  return a.displayName.localeCompare(b.displayName);
});
```

**What this does:**
- Sorts online users first
- Within each group (online/offline), sorts alphabetically by display name
- Uses `localeCompare` for proper alphabetical sorting (handles special characters)

**Presence Indicators:**
- Avatar with user's initial (gradient background)
- Small badge on avatar (green for online, gray for offline)
- Text badge showing "Online" or "Offline"
- Separate sections for online and offline users

**User Information:**
- Display name (primary)
- Username with @ prefix (secondary)
- "(You)" indicator for current user
- Hover effect for better interactivity

### 4. RoomSelector Component

**Purpose:** Display available chat rooms and allow navigation between them.

**Key Implementation Details:**

**Active Room Highlighting:**
```typescript
const isActive = room.id === currentRoomId;

className={`... ${
  isActive
    ? 'bg-blue-50 border-2 border-blue-500'
    : 'bg-white border-2 border-gray-200 hover:border-gray-300'
}`}
```

**What this does:**
- Compares room ID with current room ID
- Applies distinct styling for active room (blue background and border)
- Shows "Active" badge on current room
- Uses `aria-current="page"` for accessibility

**Date Formatting:**
```typescript
const formatDate = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const diffInDays = Math.floor(
    (now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24)
  );
  
  if (diffInDays === 0) return 'Today';
  if (diffInDays === 1) return 'Yesterday';
  if (diffInDays < 7) return `${diffInDays} days ago`;
  return date.toLocaleDateString(...);
};
```

**What this does:**
- Calculates days since room creation
- Shows relative dates for recent rooms (Today, Yesterday, X days ago)
- Shows absolute date for older rooms
- Provides context without cluttering the UI

**Room Information:**
- Room name (primary, truncated if too long)
- Description (secondary, line-clamped to 2 lines)
- Creator information with icon
- Creation date
- Active indicator badge

## Why This Approach

### Component Composition
Each component has a single, clear responsibility:
- **MessageList**: Display messages
- **MessageInput**: Capture user input
- **UserList**: Show user presence
- **RoomSelector**: Navigate between rooms

This separation makes components:
- Easier to test in isolation
- Reusable in different contexts
- Simpler to maintain and debug

### Controlled Components
All components use controlled inputs (state managed by React):
- Predictable behavior
- Easy to validate and transform input
- Simple to implement features like character limits

### Refs for DOM Manipulation
Used refs for:
- Auto-scroll (need direct DOM access for `scrollIntoView`)
- Textarea auto-resize (need to measure scrollHeight)
- Tracking previous values (avoid unnecessary effects)

This is appropriate because:
- These operations require direct DOM access
- They don't affect React's rendering logic
- They're performance-sensitive operations

### Mobile-First Design
All components start with mobile styles and scale up:
```css
/* Mobile first (default) */
max-w-[85%]

/* Tablet and up */
sm:max-w-[75%]

/* Desktop and up */
md:max-w-[65%]
```

Benefits:
- Ensures mobile experience is prioritized
- Easier to add features than remove them
- Better performance on mobile devices

### Accessibility Features
Every component includes:
- Semantic HTML (`<nav>`, `<main>`, `<article>`)
- ARIA labels and roles (`role="log"`, `aria-live="polite"`)
- Keyboard navigation support
- Screen reader hints (`sr-only` class)
- Sufficient color contrast (WCAG AA)
- Minimum 44x44px touch targets

## Alternatives Considered

### 1. Virtual Scrolling for MessageList
**Considered:** Using a library like `react-window` for virtual scrolling.

**Decision:** Not implemented for MVP.

**Reasoning:**
- Adds complexity and dependencies
- Only beneficial for very long message lists (1000+ messages)
- This is a learning project targeting 10-20 concurrent users
- Can be added later if performance becomes an issue

### 2. Rich Text Editor for MessageInput
**Considered:** Using a rich text editor (e.g., Draft.js, Slate) for formatting.

**Decision:** Simple textarea for MVP.

**Reasoning:**
- Rich text editors are complex and heavy
- Requirements only specify text messages
- Simpler UX for users (no formatting confusion)
- Can be added as an enhancement later

### 3. Infinite Scroll for Message History
**Considered:** Loading older messages as user scrolls up.

**Decision:** Not implemented in this task (handled by parent component).

**Reasoning:**
- MessageList is a presentational component
- Data fetching should be handled by container/page component
- Keeps component focused on display logic
- Easier to test and reuse

### 4. WebSocket Connection Status in MessageInput
**Considered:** Showing connection status directly in MessageInput.

**Decision:** Simple disabled state with error message.

**Reasoning:**
- Connection status is global, not input-specific
- Should be shown in a global header/banner
- Keeps component simple and focused
- Disabled state is sufficient for preventing sends

## Key Concepts

### 1. React Refs
Refs provide a way to access DOM nodes or React elements directly:
```typescript
const messagesEndRef = useRef<HTMLDivElement>(null);
messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
```

**When to use refs:**
- Accessing DOM APIs (focus, scroll, measure)
- Storing mutable values that don't trigger re-renders
- Integrating with non-React libraries

**When NOT to use refs:**
- Managing component state (use `useState`)
- Triggering re-renders (use `setState`)
- Passing data to children (use props)

### 2. Controlled vs Uncontrolled Components
**Controlled:** React state is the "single source of truth"
```typescript
<input value={content} onChange={(e) => setContent(e.target.value)} />
```

**Uncontrolled:** DOM is the source of truth
```typescript
<input ref={inputRef} />
// Access value with inputRef.current.value
```

**We use controlled components because:**
- Easier to validate and transform input
- Predictable behavior
- Better for forms with complex logic

### 3. TypeScript Generics in Props
```typescript
export interface MessageListProps {
  messages: Message[];
  currentUserId?: number;
}
```

**Benefits:**
- Type safety at compile time
- IntelliSense in IDE
- Self-documenting code
- Catches errors before runtime

### 4. Tailwind CSS Utility Classes
```typescript
className="flex items-center gap-3 px-3 py-2"
```

**Benefits:**
- No CSS file management
- Consistent spacing and sizing
- Responsive design with breakpoint prefixes (`sm:`, `md:`, `lg:`)
- Purged unused styles in production

### 5. ARIA Attributes for Accessibility
```typescript
<div role="log" aria-live="polite" aria-label="Chat messages">
```

**Common ARIA attributes:**
- `role`: Defines element's purpose (log, button, navigation)
- `aria-label`: Provides accessible name
- `aria-live`: Announces dynamic content changes
- `aria-current`: Indicates current item in navigation
- `aria-describedby`: Links to description element

## Potential Pitfalls

### 1. Auto-Scroll on Every Render
**Problem:** Scrolling to bottom on every render, even when user scrolled up to read history.

**Solution:** Only auto-scroll when new messages are added:
```typescript
if (messages.length > prevMessageCountRef.current) {
  messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
}
```

**Lesson:** Track previous state to detect actual changes, not just re-renders.

### 2. Textarea Height Not Resetting
**Problem:** Textarea grows but doesn't shrink when content is deleted.

**Solution:** Reset height to 'auto' before calculating new height:
```typescript
textarea.style.height = 'auto';
const newHeight = Math.min(textarea.scrollHeight, 150);
textarea.style.height = `${newHeight}px`;
```

**Lesson:** When measuring DOM elements, reset dynamic styles first to get accurate measurements.

### 3. Enter Key Creating New Line Before Send
**Problem:** Pressing Enter adds a new line and then sends the message.

**Solution:** Prevent default behavior before handling send:
```typescript
if (e.key === 'Enter' && !e.shiftKey) {
  e.preventDefault(); // Prevent new line
  handleSend();
}
```

**Lesson:** Always call `preventDefault()` when overriding default browser behavior.

### 4. Stale Closures in Event Handlers
**Problem:** Event handlers capturing old state values.

**Solution:** Use functional state updates or include dependencies in useCallback:
```typescript
// Functional update
setContent(prev => prev + newText);

// Or use useCallback with dependencies
const handleSend = useCallback(() => {
  onSend(content);
}, [content, onSend]);
```

**Lesson:** Be aware of closure scope in React hooks and event handlers.

### 5. Accessibility Attributes on Wrong Elements
**Problem:** Adding `aria-label` to non-interactive elements or using wrong roles.

**Solution:** Follow ARIA authoring practices:
- Use semantic HTML first (`<button>`, `<nav>`, `<main>`)
- Add ARIA only when semantic HTML isn't sufficient
- Test with screen readers (NVDA, JAWS, VoiceOver)

**Lesson:** Semantic HTML is better than ARIA. ARIA is for filling gaps, not replacing proper HTML.

### 6. Not Handling Empty States
**Problem:** Components showing nothing or breaking when data is empty.

**Solution:** Always provide empty state UI:
```typescript
if (messages.length === 0) {
  return (
    <div className="...">
      <p>No messages yet. Start the conversation!</p>
    </div>
  );
}
```

**Lesson:** Every component should handle empty, loading, and error states gracefully.

## What You Learned

### Technical Skills
1. **React Refs**: How to use refs for DOM manipulation (scroll, measure)
2. **Controlled Components**: Managing form state with React
3. **TypeScript Props**: Defining and exporting component prop interfaces
4. **Tailwind CSS**: Building responsive, mobile-first UIs with utility classes
5. **Accessibility**: Implementing ARIA attributes and semantic HTML

### Design Patterns
1. **Component Composition**: Breaking UI into small, focused components
2. **Presentational Components**: Separating display logic from data fetching
3. **Mobile-First Design**: Starting with mobile and scaling up
4. **Empty States**: Providing feedback when no data is available

### Best Practices
1. **Auto-Scroll Logic**: Only scroll on new content, not every render
2. **Keyboard Shortcuts**: Supporting Enter to send, Shift+Enter for new line
3. **Character Limits**: Showing feedback before hitting the limit
4. **Presence Indicators**: Visual feedback for online/offline status
5. **Active State Highlighting**: Clear indication of current selection

### Real-World Applications
These patterns apply to:
- **Chat applications**: Slack, Discord, WhatsApp Web
- **Comment systems**: Reddit, YouTube, blog comments
- **Collaborative tools**: Notion, Google Docs comments
- **Customer support**: Intercom, Zendesk chat
- **Social media**: Twitter, Facebook messaging

### Next Steps
To extend these components:
1. Add message reactions (emoji responses)
2. Implement message editing and deletion
3. Add typing indicators ("User is typing...")
4. Support file uploads and image previews
5. Add message search and filtering
6. Implement infinite scroll for message history
7. Add user mentions (@username)
8. Support markdown formatting in messages
