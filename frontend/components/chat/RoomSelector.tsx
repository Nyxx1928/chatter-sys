'use client';

import { ChatRoom, Message } from '../../types/domain';

export interface RoomSelectorProps {
  rooms: ChatRoom[];
  currentRoomId?: number;
  latestMessages?: Record<number, Message>;
  onRoomSelect: (room: ChatRoom) => void;
  onRoomDelete?: (room: ChatRoom) => void;
  canDeleteRoom?: (room: ChatRoom) => boolean;
  emptyStateTitle?: string;
  emptyStateDescription?: string;
  className?: string;
}

/**
 * RoomSelector component for room navigation.
 * Displays available chat rooms and allows users to switch between them.
 * Follows mobile-first design principles with Tailwind CSS.
 * 
 * Requirements: 15.4, 17.3
 */
export function RoomSelector({
  rooms,
  currentRoomId,
  latestMessages = {},
  onRoomSelect,
  onRoomDelete,
  canDeleteRoom,
  emptyStateTitle = 'No rooms available',
  emptyStateDescription = 'Create a room to start chatting',
  className = ''
}: RoomSelectorProps) {
  // Format date to readable format
  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInDays = Math.floor(
      (now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24)
    );

    if (diffInDays === 0) {
      return 'Today';
    } else if (diffInDays === 1) {
      return 'Yesterday';
    } else if (diffInDays < 7) {
      return `${diffInDays} days ago`;
    } else {
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
      });
    }
  };

  // Render a single room item
  const renderRoom = (room: ChatRoom) => {
    const isActive = room.id === currentRoomId;
    const isDirect = room.roomType === 'DIRECT';
    const showDelete = Boolean(onRoomDelete && !isDirect && (canDeleteRoom ? canDeleteRoom(room) : false));
    const latest = latestMessages[room.id];
    const roomLabel = isDirect
      ? (room.otherParticipant?.displayName ?? room.name)
      : room.name;

    return (
      <li key={room.id} className="relative">
        <button
          onClick={() => onRoomSelect(room)}
          className={`w-full min-h-[44px] text-left px-3 py-2.5 rounded-xl transition-colors flex items-center gap-3 ${
              isActive
                ? 'bg-slack-primary/20 border border-slack-primary/40'
                : 'hover:bg-slack-surface-tertiary border border-transparent'
          }`}
          aria-current={isActive ? 'page' : undefined}
          aria-label={isDirect ? `Direct message with ${room.otherParticipant?.displayName ?? room.name}` : undefined}
        >
          {/* Room avatar */}
          <div className="shrink-0 w-11 h-11 rounded-full bg-slack-primary flex items-center justify-center text-slack-text-inverse font-semibold text-sm relative">
            {isDirect ? (
              /* Person icon for DM rooms */
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5" aria-hidden="true">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" />
              </svg>
            ) : (
              room.name.charAt(0).toUpperCase()
            )}
            {/* Online indicator if there's activity */}
            {latest && (
              <span className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-slack-accent-green border-2 border-slack-surface-secondary" aria-hidden="true" />
            )}
          </div>

          {/* Room info */}
          <div className="flex-1 min-w-0">
            <div className="flex items-baseline justify-between gap-2 mb-0.5 min-w-0">
              <h3 className={`text-sm font-semibold truncate ${isActive ? 'text-slack-primary' : 'text-slack-text-primary'}`}>
                {roomLabel}
              </h3>
              {latest && (
                <time className="text-xs text-slack-text-secondary shrink-0">
                  {formatDate(latest.timestamp)}
                </time>
              )}
            </div>

            {/* Latest message or description */}
            {latest ? (
              <p className="text-xs text-slack-text-secondary line-clamp-1">
                <span className="font-medium text-slack-text-primary">{latest.senderDisplayName}:</span> {latest.content}
              </p>
            ) : room.description ? (
              <p className="text-xs text-slack-text-secondary line-clamp-1 italic">{room.description}</p>
            ) : (
              <p className="text-xs text-slack-text-secondary line-clamp-1 italic">No messages yet</p>
            )}
          </div>

          {/* Spacer for delete button */}
          {showDelete && <div className="w-7 shrink-0" />}
        </button>

        {/* Delete button - positioned absolutely to avoid nesting */}
        {showDelete && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onRoomDelete?.(room);
            }}
            className="absolute right-3 top-1/2 -translate-y-1/2 shrink-0 p-1.5 text-slack-text-secondary hover:bg-slack-accent-red/30 hover:text-slack-accent-red rounded-lg transition-colors z-10"
            aria-label={`Delete ${room.name}`}
          >
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
              <path strokeLinecap="round" strokeLinejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
            </svg>
          </button>
        )}
      </li>
    );
  };

  // Empty state
  if (rooms.length === 0) {
    return (
      <div
        className={`flex items-center justify-center h-full p-4 bg-slack-surface-secondary ${className}`}
        role="status"
      >
        <div className="text-center">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
            className="w-12 h-12 mx-auto text-slack-text-secondary mb-3"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M20.25 8.511c.884.284 1.5 1.128 1.5 2.097v4.286c0 1.136-.847 2.1-1.98 2.193-.34.027-.68.052-1.02.072v3.091l-3-3c-1.354 0-2.694-.055-4.02-.163a2.115 2.115 0 0 1-.825-.242m9.345-8.334a2.126 2.126 0 0 0-.476-.095 48.64 48.64 0 0 0-8.048 0c-1.131.094-1.976 1.057-1.976 2.192v4.286c0 .837.46 1.58 1.155 1.951m9.345-8.334V6.637c0-1.621-1.152-3.026-2.76-3.235A48.455 48.455 0 0 0 11.25 3c-2.115 0-4.198.137-6.24.402-1.608.209-2.76 1.614-2.76 3.235v6.226c0 1.621 1.152 3.026 2.76 3.235.577.075 1.157.14 1.74.194V21l4.155-4.155"
            />
          </svg>
          <p className="text-slack-text-secondary text-sm font-medium mb-1">{emptyStateTitle}</p>
          <p className="text-slack-text-secondary text-xs">{emptyStateDescription}</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`flex flex-col h-full bg-slack-surface-secondary ${className}`}>
      {/* Rooms list */}
      <div className="flex-1 overflow-y-auto p-2">
        <ul className="space-y-1" role="list" aria-label="Chat rooms">
          {rooms.map(renderRoom)}
        </ul>
      </div>
    </div>
  );
}
