'use client';

import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '@/lib/api/client';
import {
  acceptFriendRequest,
  declineFriendRequest,
  listFriendRequests,
  listFriends,
  sendFriendRequest
} from '@/lib/api/friends';
import { searchUsers } from '@/lib/api/users';
import { useAuthStore } from '@/lib/store/authStore';
import { Button } from '@/components/ui';
import { FriendRequestList, PublicUser, RelationshipStatus, UserSearchResult } from '@/types/domain';
import { UserSearch } from './UserSearch';

const emptyRequests: FriendRequestList = { incoming: [], outgoing: [] };

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof ApiError) {
    return error.message || fallback;
  }

  return fallback;
};

/**
 * Friends panel with search, pending requests, and online indicators.
 */
export function FriendsPanel() {
  const { token } = useAuthStore();
  const [friends, setFriends] = useState<PublicUser[]>([]);
  const [requests, setRequests] = useState<FriendRequestList>(emptyRequests);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [query, setQuery] = useState('');
  const [searchResults, setSearchResults] = useState<UserSearchResult[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  const incomingRequestIds = useMemo(() => {
    const mapping: Record<number, number> = {};
    requests.incoming.forEach((request) => {
      mapping[request.requester.id] = request.id;
    });
    return mapping;
  }, [requests.incoming]);

  const refreshPanel = async () => {
    if (!token) {
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const [friendsList, requestList] = await Promise.all([
        listFriends(token),
        listFriendRequests(token)
      ]);
      setFriends(friendsList);
      setRequests(requestList);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load friends data.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshPanel();
  }, [token]);

  useEffect(() => {
    if (!token) {
      return;
    }

    if (query.trim().length === 0) {
      setSearchResults([]);
      setSearchError(null);
      setSearchLoading(false);
      return;
    }

    setSearchLoading(true);
    setSearchError(null);

    const handle = window.setTimeout(async () => {
      try {
        const results = await searchUsers(token, query.trim());
        setSearchResults(results);
      } catch (err) {
        setSearchError(getErrorMessage(err, 'Search failed. Try again.'));
      } finally {
        setSearchLoading(false);
      }
    }, 400);

    return () => window.clearTimeout(handle);
  }, [query, token]);

  const handleSendRequest = async (userId: number) => {
    if (!token) {
      return;
    }

    try {
      await sendFriendRequest(token, userId);
      await refreshPanel();
      setSearchResults((prev) =>
        prev.map((result) =>
          result.user.id === userId
            ? { ...result, relationshipStatus: RelationshipStatus.PENDING_OUTGOING }
            : result
        )
      );
    } catch (err) {
      setSearchError(getErrorMessage(err, 'Unable to send request.'));
    }
  };

  const handleAcceptRequest = async (requestId: number) => {
    if (!token) {
      return;
    }

    try {
      await acceptFriendRequest(token, requestId);
      await refreshPanel();
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to accept request.'));
    }
  };

  const handleDeclineRequest = async (requestId: number) => {
    if (!token) {
      return;
    }

    try {
      await declineFriendRequest(token, requestId);
      await refreshPanel();
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to decline request.'));
    }
  };

  const onlineCount = friends.filter((friend) => friend.online).length;

  return (
    <div className="flex h-full flex-col gap-6 overflow-hidden">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">Friends</h3>
          <p className="text-sm text-gray-500">
            {friends.length} total · {onlineCount} online
          </p>
        </div>
        <Button size="sm" variant="secondary" onClick={refreshPanel}>
          Refresh
        </Button>
      </div>

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
          {error}
        </div>
      )}

      {loading ? (
        <div className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-600" role="status">
          Loading friends data...
        </div>
      ) : friends.length === 0 ? (
        <div className="rounded-xl border border-dashed border-gray-200 px-4 py-6 text-center text-sm text-gray-500">
          Your friends list is empty. Search for people to connect with.
        </div>
      ) : (
        <ul className="space-y-3" role="list" aria-label="Friends list">
          {friends.map((friend) => (
            <li
              key={friend.id}
              className="flex items-center justify-between rounded-xl border border-gray-200 bg-white px-4 py-3"
            >
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 text-white flex items-center justify-center font-semibold">
                  {friend.displayName.charAt(0).toUpperCase()}
                </div>
                <div>
                  <p className="text-sm font-semibold text-gray-900">
                    {friend.displayName}
                  </p>
                  <p className="text-xs text-gray-500">@{friend.username}</p>
                </div>
              </div>
              <span
                className={`inline-flex items-center gap-2 rounded-full px-2 py-1 text-xs font-medium ${
                  friend.online
                    ? 'bg-green-100 text-green-700'
                    : 'bg-gray-100 text-gray-600'
                }`}
              >
                <span
                  className={`h-2 w-2 rounded-full ${
                    friend.online ? 'bg-green-500' : 'bg-gray-400'
                  }`}
                />
                {friend.online ? 'Online' : 'Offline'}
              </span>
            </li>
          ))}
        </ul>
      )}

      <div className="space-y-3">
        <h4 className="text-sm font-semibold text-gray-900">Pending requests</h4>
        {requests.incoming.length === 0 && requests.outgoing.length === 0 ? (
          <p className="text-sm text-gray-500">No pending friend requests.</p>
        ) : (
          <div className="space-y-4">
            {requests.incoming.length > 0 && (
              <div className="space-y-2">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  Incoming
                </p>
                {requests.incoming.map((request) => (
                  <div
                    key={request.id}
                    className="flex flex-col gap-2 rounded-xl border border-gray-200 bg-white px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div>
                      <p className="text-sm font-semibold text-gray-900">
                        {request.requester.displayName}
                      </p>
                      <p className="text-xs text-gray-500">@{request.requester.username}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button size="sm" onClick={() => handleAcceptRequest(request.id)}>
                        Accept
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => handleDeclineRequest(request.id)}
                      >
                        Decline
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {requests.outgoing.length > 0 && (
              <div className="space-y-2">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  Outgoing
                </p>
                {requests.outgoing.map((request) => (
                  <div
                    key={request.id}
                    className="flex items-center justify-between rounded-xl border border-gray-200 bg-gray-50 px-4 py-3"
                  >
                    <div>
                      <p className="text-sm font-semibold text-gray-900">
                        {request.recipient.displayName}
                      </p>
                      <p className="text-xs text-gray-500">@{request.recipient.username}</p>
                    </div>
                    <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-700">
                      Requested
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="border-t border-gray-200 pt-6">
        {searchError && (
          <div className="mb-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
            {searchError}
          </div>
        )}
        <UserSearch
          query={query}
          loading={searchLoading}
          results={searchResults}
          incomingRequestIds={incomingRequestIds}
          onQueryChange={setQuery}
          onSendRequest={handleSendRequest}
          onAcceptRequest={handleAcceptRequest}
          onDeclineRequest={handleDeclineRequest}
        />
      </div>
    </div>
  );
}
