# Lesson: Room Deletion UI with Confirmation and Redirection

## Task Context

This lesson covers Task 10 from the social-discovery-and-room-management spec: implementing a room deletion UI with proper authorization, confirmation dialogs, and user redirection. The implementation satisfies Requirements 6.1 (authorized user confirms deletion), 6.2 (room removed and users redirected), and 7.1 (accessibility).

The room deletion feature allows room owners to permanently delete chat rooms, removing all associated messages and memberships. This is a destructive action that requires careful UX design to prevent accidental deletions while maintaining a smooth user experience.

## Files Modified

- `frontend/app/chat/page.tsx` (modified) - Added room deletion UI to the room list page
- `frontend/app/chat/[roomId]/page.tsx` (modified) - Added room deletion UI to individual room pages
- `frontend/lib/api/rooms.ts` (already had deleteRoom function)
- `frontend/components/chat/RoomSelector.tsx` (already had delete button support)
- `frontend/components/ui/Modal.tsx` (already existed with accessibility features)
- `frontend/components/ui/Button.tsx` (already had danger variant)

## Step-by-Step Changes

### 1. Room List Page Deletion UI (`frontend/app/chat/page.tsx`)

**State Management:**
Added state variables to manage the deletion flow:
- `deleteTarget`: Tracks which room is being deleted (null when no deletion in progress)
- `deleteError`: Stores error messages if deletion fails
- `isDeleting`: Loading state during the API call

**Delete Handler:**
```typescript
const handleDeleteRoom = async () => {
  if (!token || !deleteTarget) return;

  try {
    setIsDeleting(true);
    setDeleteError(null);
    await deleteRoom(token, deleteTarget.id);
    setDeleteTarget(null);
    await loadRooms(); // Refresh the room list
  } catch (err) {
    console.error('Failed to delete room:', err);
    setDeleteError('Failed to delete room. Please try again.');
  } finally {
    setIsDeleting(false);
  }
};
```

**RoomSelector Integration:**
Connected the RoomSelector component with deletion callbacks:
```typescript
<RoomSelector
  rooms={filteredRooms}
  onRoomSelect={handleRoomSelect}
  onRoomDelete={(room) => {
    setDeleteError(null);
    setDeleteTarget(room);
  }}
  canDeleteRoom={(room) => room.createdBy?.id === user?.id}
  // ... other props
/>
```

**Confirmation Modal:**
Added a Modal component that appears when `deleteTarget` is set:
```typescript
<Modal
  open={Boolean(deleteTarget)}
  title="Delete room"
  onClose={() => setDeleteTarget(null)}
  footer={
    <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
      <Button variant="secondary" onClick={() => setDeleteTarget(null)}>
        Cancel
      </Button>
      <Button variant="danger" onClick={handleDeleteRoom} disabled={isDeleting}>
        {isDeleting ? 'Deleting...' : 'Delete room'}
      </Button>
    </div>
  }
>
  <p className="text-sm text-gray-600">
    Deleting <span className="font-semibold text-gray-900">{deleteTarget?.name}</span> will remove all
    messages and memberships. This action cannot be undone.
  </p>
  {deleteError && (
    <p className="mt-3 text-sm text-red-600" role="alert">
      {deleteError}
    </p>
  )}
</Modal>
```

### 2. Individual Room Page Deletion UI (`frontend/app/chat/[roomId]/page.tsx`)

**State Management:**
Similar state variables for managing the deletion flow:
- `showDeleteModal`: Boolean to control modal visibility
- `deleteError`: Error message storage
- `isDeleting`: Loading state

**Delete Handler with Redirect:**
```typescript
const handleDeleteRoom = async () => {
  if (!token) return;

  try {
    setIsDeleting(true);
    setDeleteError(null);
    await deleteRoom(token, parseInt(roomId));
    setShowDeleteModal(false);
    router.push('/chat'); // Redirect to room list
  } catch (err) {
    console.error('Failed to delete room:', err);
    setDeleteError('Failed to delete room. Please try again.');
  } finally {
    setIsDeleting(false);
  }
};
```

**Delete Button in Header:**
Added a delete button in the room header, visible only to room owners:
```typescript
{user?.id === room.createdBy?.id && (
  <Button
    variant="danger"
    size="sm"
    onClick={() => setShowDeleteModal(true)}
  >
    Delete room
  </Button>
)}
```

**Confirmation Modal:**
Same modal pattern as the room list page, ensuring consistent UX.

### 3. Authorization Logic

Both pages implement the same authorization check:
```typescript
user?.id === room.createdBy?.id
```

This ensures only the room creator can see and use the delete button. The backend also enforces this authorization, providing defense in depth.

### 4. RoomSelector Component Support

The RoomSelector component already had built-in support for deletion:
- `onRoomDelete` callback prop
- `canDeleteRoom` function prop for authorization
- Delete button with trash icon
- Proper ARIA labels (`aria-label={Delete ${room.name}}`)

## Why This Approach

### 1. Two-Step Confirmation Pattern
The implementation uses a two-step process (click delete → confirm in modal) to prevent accidental deletions. This is a standard UX pattern for destructive actions.

### 2. Explicit Warning Message
The modal clearly states the consequences: "will remove all messages and memberships. This action cannot be undone." This helps users make informed decisions.

### 3. Optimistic UI Updates
After successful deletion, the room list page refreshes the room list, and the individual room page redirects to `/chat`. This provides immediate feedback without requiring manual navigation.

### 4. Authorization at Multiple Levels
- **UI Level**: Delete button only visible to room owners
- **Component Level**: `canDeleteRoom` function controls visibility
- **Backend Level**: API endpoint validates ownership (defense in depth)

### 5. Consistent Error Handling
Both pages use the same error handling pattern:
- Try-catch blocks around API calls
- User-friendly error messages
- Error state displayed in the modal
- Console logging for debugging

### 6. Loading States
The `isDeleting` state disables the delete button during the API call, preventing duplicate requests and providing visual feedback.

## Alternatives Considered

### 1. Inline Confirmation (Not Chosen)
**Alternative**: Use a simple `window.confirm()` dialog.
**Why Not**: Native browser dialogs are not customizable, don't match the app's design, and have poor accessibility support.

### 2. Undo Pattern (Not Chosen)
**Alternative**: Allow deletion with an "undo" option for a few seconds.
**Why Not**: Room deletion is a complex operation affecting multiple users and data. An undo mechanism would require soft deletes and complex state management. The confirmation dialog is simpler and more reliable.

### 3. Multi-Step Wizard (Not Chosen)
**Alternative**: Use a multi-step process with additional confirmations.
**Why Not**: Overkill for this use case. A single confirmation modal with clear messaging is sufficient.

### 4. Type-to-Confirm (Not Chosen)
**Alternative**: Require users to type the room name to confirm deletion (like GitHub).
**Why Not**: This pattern is typically used for critical infrastructure (repositories, databases). Chat rooms are less critical, and the extra friction isn't justified.

### 5. Soft Delete with Archive (Not Chosen)
**Alternative**: Archive rooms instead of permanently deleting them.
**Why Not**: The requirements specify permanent deletion. Archiving would add complexity without a clear user benefit in this context.

## Key Concepts

### 1. Destructive Action UX Pattern
Destructive actions (delete, remove, destroy) require special UX treatment:
- **Visibility**: Use warning colors (red) to signal danger
- **Confirmation**: Require explicit confirmation before executing
- **Clarity**: Explain what will be deleted and that it's permanent
- **Reversibility**: If possible, provide undo; if not, make irreversibility clear

### 2. Modal Dialog Accessibility
The Modal component implements several accessibility features:
- **Focus Management**: Automatically focuses the first interactive element
- **Keyboard Support**: Escape key closes the modal
- **ARIA Attributes**: `role="dialog"`, `aria-modal="true"`, `aria-label`
- **Focus Trap**: Keeps focus within the modal while open
- **Focus Restoration**: Returns focus to the trigger element on close

### 3. Authorization Patterns
The implementation demonstrates defense in depth:
- **UI Layer**: Hide controls from unauthorized users
- **API Layer**: Validate permissions on the server
- **Never Trust the Client**: Always validate on the backend

### 4. State Management for Async Operations
The deletion flow demonstrates a common pattern:
```typescript
const [isLoading, setIsLoading] = useState(false);
const [error, setError] = useState<string | null>(null);

const handleAction = async () => {
  try {
    setIsLoading(true);
    setError(null);
    await apiCall();
    // Success handling
  } catch (err) {
    setError('User-friendly message');
  } finally {
    setIsLoading(false);
  }
};
```

### 5. Conditional Rendering Patterns
The code uses several React conditional rendering patterns:
- **Boolean && JSX**: `{condition && <Component />}`
- **Ternary**: `{condition ? <A /> : <B />}`
- **Boolean()**: `open={Boolean(deleteTarget)}` converts truthy/falsy to boolean

### 6. Responsive Design Considerations
The modal footer uses responsive flexbox:
```typescript
<div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
```
This stacks buttons vertically on mobile and horizontally on larger screens.

## Potential Pitfalls

### 1. Race Conditions
**Problem**: User clicks delete multiple times rapidly.
**Solution**: The `isDeleting` state disables the button during the API call.

### 2. Stale State After Deletion
**Problem**: Room list shows deleted room until manual refresh.
**Solution**: Call `loadRooms()` after successful deletion to refresh the list.

### 3. Navigation After Deletion
**Problem**: User is stuck on a deleted room's page.
**Solution**: Individual room page redirects to `/chat` after deletion.

### 4. Error Recovery
**Problem**: Deletion fails but modal closes, losing error context.
**Solution**: Keep modal open on error and display error message inline.

### 5. Authorization Bypass
**Problem**: User modifies client code to show delete button.
**Solution**: Backend validates ownership; unauthorized requests return 403.

### 6. Null/Undefined Checks
**Problem**: Accessing properties on null/undefined objects.
**Solution**: Use optional chaining (`room.createdBy?.id`) and null checks (`if (!token || !deleteTarget) return`).

### 7. Memory Leaks
**Problem**: Setting state after component unmounts.
**Solution**: The try-finally pattern ensures `setIsDeleting(false)` runs, but for long-running operations, consider cleanup in useEffect.

### 8. Accessibility Oversight
**Problem**: Screen reader users don't understand the delete action.
**Solution**: Use semantic HTML, ARIA labels, and role attributes. The Modal component handles focus management.

## What You Learned

In this lesson, you learned how to implement a complete room deletion feature with:

1. **Two-step confirmation pattern** to prevent accidental deletions
2. **Authorization checks** at both UI and API levels
3. **Accessible modal dialogs** with proper focus management and keyboard support
4. **Error handling and loading states** for async operations
5. **Responsive design** that works on mobile and desktop
6. **User redirection** after destructive actions
7. **Consistent UX patterns** across multiple pages

The implementation demonstrates production-ready patterns for destructive actions in React applications, balancing user safety with usability. The code is maintainable, accessible, and follows React best practices for state management and async operations.

Key takeaways:
- Always confirm destructive actions with clear, explicit warnings
- Implement authorization at multiple layers (UI, API)
- Provide immediate feedback through loading states and error messages
- Use accessible components with proper ARIA attributes and keyboard support
- Handle edge cases like race conditions and stale state
- Follow consistent patterns across your application for predictable UX
