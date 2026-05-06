'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
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
import { usePresenceStore } from '@/lib/store/presenceStore';
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
  const { presenceMap, batchUpdatePresence } = usePresenceStore();
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

  const fetchPanelData = useCallback(async () => {
    if (!token) {
      return null;
    }

    const [friendsList, requestList] = await Promise.all([
      listFriends(token),
      listFriendRequests(token)
    ]);

    return { friendsList, requestList };
  }, [token]);

  const refreshPanel = useCallback(async () => {
    if (!token) {
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const panelData = await fetchPanelData();

      if (!panelData) {
        return;
      }

      const { friendsList, requestList } = panelData;
      setFriends(friendsList);
      setRequests(requestList);
      
      // Sync presence data from API response to presence store
      batchUpdatePresence(
        friendsList.map((friend) => ({
          userId: friend.id,
          online: friend.online
        }))
      );
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load friends data.'));
    } finally {
      setLoading(false);
    }
  }, [fetchPanelData, token, batchUpdatePresence]);

  useEffect(() => {
    if (!token) {
      return;
    }

    let isActive = true;

    const loadInitialPanel = async () => {
      try {
        const panelData = await fetchPanelData();

        if (!panelData || !isActive) {
          return;
        }

        const { friendsList, requestList } = panelData;
        setFriends(friendsList);
        setRequests(requestList);
        batchUpdatePresence(
          friendsList.map((friend) => ({
            userId: friend.id,
            online: friend.online
          }))
        );
      } catch (err) {
        if (isActive) {
          setError(getErrorMessage(err, 'Failed to load friends data.'));
        }
      } finally {
        if (isActive) {
          setLoading(false);
        }
      }
    };

    void loadInitialPanel();

    return () => {
      isActive = false;
    };
  }, [token, fetchPanelData, batchUpdatePresence]);

  const handleQueryChange = useCallback((value: string) => {
    setQuery(value);

    if (value.trim().length === 0) {
      setSearchResults([]);
      setSearchError(null);
      setSearchLoading(false);
      return;
    }

    setSearchLoading(true);
    setSearchError(null);
  }, []);

  useEffect(() => {
    if (!token) {
      return;
    }

    if (query.trim().length === 0) {
      return;
    }

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

  // Merge friends list with real-time presence data from the presence store
  const friendsWithPresence = useMemo(() => {
    return friends.map((friend) => {
      // Use presence store data if available, otherwise fall back to API data
      const onlineStatus = presenceMap[friend.id] ?? friend.online;
      return {
        ...friend,
        online: onlineStatus
      };
    });
  }, [friends, presenceMap]);

  const onlineCount = friendsWithPresence.filter((friend) => friend.online).length;
  const isSearching = query.trim().length > 0;

  return (
    <div className="flex h-full flex-col gap-6 overflow-hidden">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-kiro-slate-100">Friends</h3>
          <p className="text-sm text-kiro-slate-500">
            {friendsWithPresence.length} total · {onlineCount} online
          </p>
        </div>
        <Button size="sm" variant="ghost" onClick={refreshPanel}>
          Refresh
        </Button>
      </div>

      {error && (
        <div className="rounded-xl border border-red-900/50 bg-red-950/40 px-4 py-3 text-sm text-red-400" role="alert">
          {error}
        </div>
      )}

      {loading ? (
        <div className="rounded-xl border border-kiro-ink-900 bg-kiro-ink-900/60 px-4 py-3 text-sm text-kiro-slate-400" role="status">
          Loading friends data...
        </div>
      ) : friendsWithPresence.length === 0 ? (
        <div className="rounded-xl border border-dashed border-kiro-ink-900 px-4 py-6 text-center text-sm text-kiro-slate-500">
          Your friends list is empty. Search for people to connect with.
        </div>
      ) : (
        <ul className="space-y-3" role="list" aria-label="Friends list">
          {friendsWithPresence.map((friend) => (
            <li
              key={friend.id}
              className="flex items-center justify-between rounded-xl border border-kiro-ink-900 bg-kiro-ink-950/60 px-4 py-3"
            >
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-gradient-to-br from-kiro-purple-500 to-kiro-purple-700 text-white flex items-center justify-center font-semibold">
                  {friend.displayName.charAt(0).toUpperCase()}
                </div>
                <div>
                  <p className="text-sm font-semibold text-kiro-slate-100">
                    {friend.displayName}
                  </p>
                  <p className="text-xs text-kiro-slate-500">@{friend.username}</p>
                </div>
              </div>
              <span
                className={`inline-flex items-center gap-2 rounded-full px-2 py-1 text-xs font-medium ${
                  friend.online
                    ? 'bg-green-900/40 text-green-400'
                    : 'bg-kiro-ink-900/60 text-kiro-slate-500'
                }`}
                aria-label={friend.online ? `${friend.displayName} is online` : `${friend.displayName} is offline`}
              >
                <span
                  className={`h-2 w-2 rounded-full ${
                    friend.online ? 'bg-green-500' : 'bg-kiro-slate-500'
                  }`}
                  aria-hidden="true"
                />
                {friend.online ? 'Online' : 'Offline'}
              </span>
            </li>
          ))}
        </ul>
      )}

      <div className="space-y-3">
        <h4 className="text-sm font-semibold text-kiro-slate-200">Pending requests</h4>
        {requests.incoming.length === 0 && requests.outgoing.length === 0 ? (
          <p className="text-sm text-kiro-slate-500">No pending friend requests.</p>
        ) : (
          <div className="space-y-4">
            {requests.incoming.length > 0 && (
              <div className="space-y-2">
                <p className="text-xs font-semibold text-kiro-slate-500 uppercase tracking-wide">
                  Incoming
                </p>
                {requests.incoming.map((request) => (
                  <div
                    key={request.id}
                    className="flex flex-col gap-2 rounded-xl border border-kiro-ink-900 bg-kiro-ink-950/60 px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div>
                      <p className="text-sm font-semibold text-kiro-slate-100">
                        {request.requester.displayName}
                      </p>
                      <p className="text-xs text-kiro-slate-500">@{request.requester.username}</p>
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
                <p className="text-xs font-semibold text-kiro-slate-500 uppercase tracking-wide">
                  Outgoing
                </p>
                {requests.outgoing.map((request) => (
                  <div
                    key={request.id}
                    className="flex items-center justify-between rounded-xl border border-kiro-ink-900 bg-kiro-ink-900/40 px-4 py-3"
                  >
                    <div>
                      <p className="text-sm font-semibold text-kiro-slate-100">
                        {request.recipient.displayName}
                      </p>
                      <p className="text-xs text-kiro-slate-500">@{request.recipient.username}</p>
                    </div>
                    <span className="rounded-full bg-kiro-purple-700/30 px-3 py-1 text-xs font-medium text-kiro-purple-400">
                      Requested
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="border-t border-kiro-ink-900 pt-6">
        {searchError && (
          <div className="mb-3 rounded-xl border border-red-900/50 bg-red-950/40 px-4 py-3 text-sm text-red-400" role="alert">
            {searchError}
          </div>
        )}
        <UserSearch
          query={query}
          loading={isSearching && searchLoading}
          results={isSearching ? searchResults : []}
          incomingRequestIds={incomingRequestIds}
          onQueryChange={handleQueryChange}
          onSendRequest={handleSendRequest}
          onAcceptRequest={handleAcceptRequest}
          onDeclineRequest={handleDeclineRequest}
        />
      </div>
    </div>
  );
}
