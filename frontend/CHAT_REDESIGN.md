# Chat Room Redesign

## Overview
The chat room interface has been redesigned to match a modern, dark-themed chat application layout inspired by the provided reference image.

## Key Changes

### Layout Structure
- **Icon-only left navigation sidebar** (hidden on mobile, visible on md+)
  - Slim 64px width with app logo, navigation icons, connection status, and user avatar
  - Active state indicators with purple accent
  - Icons for Home, Search, Messages, People, Files, Settings

- **Three-column layout**:
  1. **Room list panel** (left): Avatar-based room items with latest message previews
  2. **Main chat area** (center): Messages with avatars, styled input bar
  3. **Members sidebar** (right): Online/offline user list (hidden on mobile, visible on lg+)

### Color Scheme
- Background: `#13131f` (main chat area)
- Sidebar/panels: `#16162a` (darker panels)
- Nav sidebar: `#0e0e1a` (darkest)
- Borders: `border-white/5` (subtle white borders)
- Purple accent: `kiro-purple-600` for active states and own messages

### Chat Room Page (`/chat/[roomId]`)
- **Header**: Room avatar with online indicator, room name, online count, action icons (search, members toggle, more)
- **Messages**: 
  - Others' messages: Left-aligned with avatar, dark gray bubble (`#1e1e30`)
  - Own messages: Right-aligned, purple bubble (`kiro-purple-600`), no avatar
  - Sender name and timestamp above each message
  - Improved spacing and typography
- **Input bar**: 
  - Rounded container with attachment, emoji, GIF, and format icons
  - Circular purple send button
  - Auto-expanding textarea
  - Styled with `#1e1e30` background

### Room List Page (`/chat`)
- **Room list panel**: 
  - Avatar-based room items with online indicators
  - Latest message preview with sender name
  - Timestamp for last activity
  - Compact, modern design
- **Center placeholder**: "Select a room" message when no room is active
- **Friends sidebar**: Visible on xl+ screens

### Room Selector Component
- Avatar circles for each room (first letter of room name)
- Green online indicator dot if there's recent activity
- Latest message preview with sender name
- Hover and active states with purple accent
- Delete button for room owners

### Message Components
- **MessageList**: Avatar-based layout, improved empty state
- **MessageInput**: Icon buttons for attachments/emoji/GIF/formatting, circular send button
- **UserList**: Cleaner styling with online/offline sections

### Navigation
- Icon-only left sidebar with tooltips
- Connection status indicator (green/yellow/red dot)
- User avatar button for logout

## Files Modified
- `frontend/app/chat/layout.tsx` - New icon-only left nav sidebar
- `frontend/app/chat/[roomId]/page.tsx` - Redesigned room page with new header and layout
- `frontend/app/chat/page.tsx` - Three-column layout with room list panel
- `frontend/components/chat/MessageList.tsx` - Avatar-based messages, improved styling
- `frontend/components/chat/MessageInput.tsx` - Styled input bar with icon buttons
- `frontend/components/chat/RoomSelector.tsx` - Avatar-based room items
- `frontend/components/chat/UserList.tsx` - Updated styling for new sidebar

## Responsive Behavior
- **Mobile**: Single column, hamburger-style navigation
- **Tablet (md+)**: Left nav sidebar appears, room list + chat
- **Desktop (lg+)**: Room list + chat + members sidebar
- **Large desktop (xl+)**: All three columns + friends sidebar

## Design Principles
- Dark theme with subtle borders and depth
- Purple accent color for interactive elements and own messages
- Avatar-based visual hierarchy
- Consistent spacing and typography
- Smooth transitions and hover states
- Accessibility maintained (ARIA labels, semantic HTML)
