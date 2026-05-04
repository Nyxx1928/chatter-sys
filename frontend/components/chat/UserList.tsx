'use client';

import { User } from '../../types/domain';

export interface UserListProps {
  users: User[];
  currentUserId?: number;
  className?: string;
}

/**
 * UserList component displays online users with presence indicators.
 * Shows real-time user presence status.
 * Follows mobile-first design principles with Tailwind CSS.
 * 
 * Requirements: 15.3, 17.3
 */
export function UserList({ users, currentUserId, className = '' }: UserListProps) {
  // Sort users: online first, then alphabetically by display name
  const sortedUsers = [...users].sort((a, b) => {
    if (a.online !== b.online) {
      return a.online ? -1 : 1;
    }
    return a.displayName.localeCompare(b.displayName);
  });

  // Separate online and offline users
  const onlineUsers = sortedUsers.filter((user) => user.online);
  const offlineUsers = sortedUsers.filter((user) => !user.online);

  // Render a single user item
  const renderUser = (user: User) => {
    const isCurrentUser = user.id === currentUserId;

    return (
      <li
        key={user.id}
        className="flex items-center gap-3 px-3 py-2 hover:bg-gray-50 rounded-lg transition-colors"
      >
        {/* Presence indicator */}
        <div className="relative shrink-0">
          {/* Avatar placeholder */}
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white font-medium text-sm">
            {user.displayName.charAt(0).toUpperCase()}
          </div>
          
          {/* Online status badge */}
          <div
            className={`absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white ${
              user.online ? 'bg-green-500' : 'bg-gray-400'
            }`}
            role="status"
            aria-label={user.online ? 'Online' : 'Offline'}
          />
        </div>

        {/* User info */}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-gray-900 truncate">
            {user.displayName}
            {isCurrentUser && (
              <span className="ml-2 text-xs text-gray-500 font-normal">(You)</span>
            )}
          </p>
          <p className="text-xs text-gray-500 truncate">@{user.username}</p>
        </div>

        {/* Online/Offline badge */}
        <div
          className={`shrink-0 px-2 py-1 rounded-full text-xs font-medium ${
            user.online
              ? 'bg-green-100 text-green-800'
              : 'bg-gray-100 text-gray-600'
          }`}
        >
          {user.online ? 'Online' : 'Offline'}
        </div>
      </li>
    );
  };

  // Empty state
  if (users.length === 0) {
    return (
      <div
        className={`flex items-center justify-center h-full p-4 ${className}`}
        role="status"
      >
        <p className="text-gray-500 text-center text-sm">
          No users in this room
        </p>
      </div>
    );
  }

  return (
    <div className={`flex flex-col h-full ${className}`}>
      {/* Header */}
      <div className="px-3 py-2 border-b border-gray-200">
        <h3 className="text-sm font-semibold text-gray-900">
          Members ({users.length})
        </h3>
        <p className="text-xs text-gray-500 mt-0.5">
          {onlineUsers.length} online
        </p>
      </div>

      {/* User list */}
      <div className="flex-1 overflow-y-auto">
        {/* Online users section */}
        {onlineUsers.length > 0 && (
          <div className="py-2">
            <h4 className="px-3 py-1 text-xs font-medium text-gray-500 uppercase tracking-wider">
              Online — {onlineUsers.length}
            </h4>
            <ul className="mt-1 space-y-1" role="list" aria-label="Online users">
              {onlineUsers.map(renderUser)}
            </ul>
          </div>
        )}

        {/* Offline users section */}
        {offlineUsers.length > 0 && (
          <div className="py-2">
            <h4 className="px-3 py-1 text-xs font-medium text-gray-500 uppercase tracking-wider">
              Offline — {offlineUsers.length}
            </h4>
            <ul className="mt-1 space-y-1" role="list" aria-label="Offline users">
              {offlineUsers.map(renderUser)}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
