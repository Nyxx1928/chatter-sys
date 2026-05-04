'use client';

import { ChatRoom } from '../../types/domain';

export interface RoomSelectorProps {
  rooms: ChatRoom[];
  currentRoomId?: number;
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
    const showDelete = Boolean(onRoomDelete && (canDeleteRoom ? canDeleteRoom(room) : false));

    return (
      <li key={room.id}>
        <div
          className={`w-full text-left px-4 py-3 rounded-lg transition-colors focus-within:ring-2 focus-within:ring-blue-500 focus-within:ring-offset-2 ${
            isActive
              ? 'bg-blue-50 border-2 border-blue-500'
              : 'bg-white border-2 border-gray-200 hover:border-gray-300 hover:bg-gray-50'
          }`}
        >
          <div className="flex items-start gap-3">
            <button
              onClick={() => onRoomSelect(room)}
              className="flex-1 text-left focus:outline-none"
              aria-current={isActive ? 'page' : undefined}
            >
              {/* Room name */}
              <div className="flex items-start justify-between gap-2 mb-1">
                <h3
                  className={`text-base font-semibold truncate ${
                    isActive ? 'text-blue-900' : 'text-gray-900'
                  }`}
                >
                  {room.name}
                </h3>

                {/* Active indicator */}
                {isActive && (
                  <span className="shrink-0 px-2 py-0.5 bg-blue-600 text-white text-xs font-medium rounded-full">
                    Active
                  </span>
                )}
              </div>

              {/* Room description */}
              {room.description && (
                <p className="text-sm text-gray-600 line-clamp-2 mb-2">
                  {room.description}
                </p>
              )}

              {/* Room metadata */}
              <div className="flex items-center gap-3 text-xs text-gray-500">
                <span className="flex items-center gap-1">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                    className="w-4 h-4"
                    aria-hidden="true"
                  >
                    <path d="M10 9a3 3 0 100-6 3 3 0 000 6zM6 8a2 2 0 11-4 0 2 2 0 014 0zM1.49 15.326a.78.78 0 01-.358-.442 3 3 0 014.308-3.516 6.484 6.484 0 00-1.905 3.959c-.023.222-.014.442.025.654a4.97 4.97 0 01-2.07-.655zM16.44 15.98a4.97 4.97 0 002.07-.654.78.78 0 00.357-.442 3 3 0 00-4.308-3.517 6.484 6.484 0 011.907 3.96 2.32 2.32 0 01-.026.654zM18 8a2 2 0 11-4 0 2 2 0 014 0zM5.304 16.19a.844.844 0 01-.277-.71 5 5 0 019.947 0 .843.843 0 01-.277.71A6.975 6.975 0 0110 18a6.974 6.974 0 01-4.696-1.81z" />
                  </svg>
                  <span>Created by {room.createdBy.displayName}</span>
                </span>
                <span aria-hidden="true">•</span>
                <time dateTime={room.createdAt}>
                  {formatDate(room.createdAt)}
                </time>
              </div>
            </button>

            {showDelete && (
              <button
                type="button"
                onClick={() => onRoomDelete?.(room)}
                className="shrink-0 rounded-lg p-2 text-gray-500 hover:bg-red-50 hover:text-red-600 focus:outline-none focus:ring-2 focus:ring-red-500"
                aria-label={`Delete ${room.name}`}
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  className="h-5 w-5"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"
                  />
                </svg>
              </button>
            )}
          </div>
        </div>
      </li>
    );
  };

  // Empty state
  if (rooms.length === 0) {
    return (
      <div
        className={`flex items-center justify-center h-full p-4 ${className}`}
        role="status"
      >
        <div className="text-center">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
            className="w-12 h-12 mx-auto text-gray-400 mb-3"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M20.25 8.511c.884.284 1.5 1.128 1.5 2.097v4.286c0 1.136-.847 2.1-1.98 2.193-.34.027-.68.052-1.02.072v3.091l-3-3c-1.354 0-2.694-.055-4.02-.163a2.115 2.115 0 01-.825-.242m9.345-8.334a2.126 2.126 0 00-.476-.095 48.64 48.64 0 00-8.048 0c-1.131.094-1.976 1.057-1.976 2.192v4.286c0 .837.46 1.58 1.155 1.951m9.345-8.334V6.637c0-1.621-1.152-3.026-2.76-3.235A48.455 48.455 0 0011.25 3c-2.115 0-4.198.137-6.24.402-1.608.209-2.76 1.614-2.76 3.235v6.226c0 1.621 1.152 3.026 2.76 3.235.577.075 1.157.14 1.74.194V21l4.155-4.155"
            />
          </svg>
          <p className="text-gray-500 text-sm font-medium mb-1">
              {emptyStateTitle}
          </p>
          <p className="text-gray-400 text-xs">
              {emptyStateDescription}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className={`flex flex-col h-full ${className}`}>
      {/* Header */}
      <div className="px-4 py-3 border-b border-gray-200">
        <h2 className="text-lg font-semibold text-gray-900">Chat Rooms</h2>
        <p className="text-sm text-gray-500 mt-0.5">
          {rooms.length} {rooms.length === 1 ? 'room' : 'rooms'} available
        </p>
      </div>

      {/* Rooms list */}
      <div className="flex-1 overflow-y-auto p-4">
        <ul className="space-y-3" role="list" aria-label="Chat rooms">
          {rooms.map(renderRoom)}
        </ul>
      </div>
    </div>
  );
}
