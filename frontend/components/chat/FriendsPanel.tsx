'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError } from '@/lib/api/client';
import {
  acceptFriendRequest,
  declineFriendRequest,
  listFriendRequests,
  listFriends,
  removeFriend,
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
export function FriendsPanel({
  onDmRoomCreated,
  onOpenDm,
}: {
  onDmRoomCreated?: (dmRoomId: number) => void;
  onOpenDm?: (friendId: number) => void;
} = {}) {
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
      const friendship = await acceptFriendRequest(token, requestId);
      await refreshPanel();
      if (onDmRoomCreated && friendship.dmRoomId) {
        onDmRoomCreated(friendship.dmRoomId);
      }
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

  const handleRemoveFriend = async (friendId: number, displayName: string) => {
    if (!token) return;
    if (!window.confirm(`Remove ${displayName} from your friends?`)) return;
    try {
      await removeFriend(token, friendId);
      await refreshPanel();
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to remove friend.'));
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
          <h3 className="text-lg font-semibold text-slack-text-primary">Friends</h3>
          <p className="text-sm text-slack-text-secondary">
            {friendsWithPresence.length} total · {onlineCount} online
          </p>
        </div>
        <Button size="sm" variant="ghost" onClick={refreshPanel}>
          Refresh
        </Button>
      </div>

      {error && (
        <div className="rounded-xl border border-slack-accent-red/30 bg-slack-accent-red/10 px-4 py-3 text-sm text-slack-accent-red" role="alert">
          {error}
        </div>
      )}

      {loading ? (
        <div className="rounded-xl border border-slack-border bg-slack-surface-tertiary px-4 py-3 text-sm text-slack-text-secondary" role="status">
          Loading friends data...
        </div>
      ) : friendsWithPresence.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slack-border px-4 py-6 text-center text-sm text-slack-text-secondary">
          Your friends list is empty. Search for people to connect with.
        </div>
      ) : (
        <ul className="space-y-3" role="list" aria-label="Friends list">
          {friendsWithPresence.map((friend) => (
            <li
              key={friend.id}
              className="flex items-center justify-between rounded-xl border border-slack-border bg-slack-surface-primary px-4 py-3"
            >
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-slack-primary text-slack-text-inverse flex items-center justify-center font-semibold">
                  {friend.displayName.charAt(0).toUpperCase()}
                </div>
                <div>
                  <p className="text-sm font-semibold text-slack-text-primary">
                    {friend.displayName}
                  </p>
                  <p className="text-xs text-slack-text-secondary">@{friend.username}</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span
                  className={`inline-flex items-center gap-2 rounded-pill px-2 py-1 text-xs font-medium ${
                    friend.online
                      ? 'bg-slack-accent-green/20 text-slack-accent-green'
                      : 'bg-slack-surface-tertiary text-slack-text-secondary'
                  }`}
                  aria-label={friend.online ? `${friend.displayName} is online` : `${friend.displayName} is offline`}
                >
                  <span
                    className={`h-2 w-2 rounded-full ${
                      friend.online ? 'bg-slack-accent-green' : 'bg-slack-text-secondary'
                    }`}
                    aria-hidden="true"
                  />
                  {friend.online ? 'Online' : 'Offline'}
                </span>
                {onOpenDm && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => onOpenDm(friend.id)}
                    aria-label={`Message ${friend.displayName}`}
                    title="Open direct message"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M8.625 12a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H8.25m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H12m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 0 1-2.555-.337A5.972 5.972 0 0 1 5.41 20.97a5.969 5.969 0 0 1-.474-.065 4.48 4.48 0 0 0 .978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25Z" />
                    </svg>
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => handleRemoveFriend(friend.id, friend.displayName)}
                  aria-label={`Remove ${friend.displayName} from friends`}
                  title="Remove friend"
                  className="text-slack-accent-red hover:text-slack-accent-red/80 hover:bg-slack-accent-red/20"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M22 10.5h-6m-2.25-4.125a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0ZM4 19.235v-.11a6.375 6.375 0 0 1 12.75 0v.109A12.318 12.318 0 0 1 10.374 21c-2.331 0-4.512-.645-6.374-1.766Z" />
                  </svg>
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <div className="space-y-3">
        <h4 className="text-sm font-semibold text-slack-text-primary">Pending requests</h4>
        {requests.incoming.length === 0 && requests.outgoing.length === 0 ? (
          <p className="text-sm text-slack-text-secondary">No pending friend requests.</p>
        ) : (
          <div className="space-y-4">
            {requests.incoming.length > 0 && (
              <div className="space-y-2">
                <p className="text-xs font-semibold text-slack-text-secondary uppercase tracking-wide">
                  Incoming
                </p>
                {requests.incoming.map((request) => (
                  <div
                    key={request.id}
                    className="flex flex-col gap-2 rounded-xl border border-slack-border bg-slack-surface-primary px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div>
                      <p className="text-sm font-semibold text-slack-text-primary">
                        {request.requester.displayName}
                      </p>
                      <p className="text-xs text-slack-text-secondary">@{request.requester.username}</p>
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
                <p className="text-xs font-semibold text-slack-text-secondary uppercase tracking-wide">
                  Outgoing
                </p>
                {requests.outgoing.map((request) => (
                  <div
                    key={request.id}
                    className="flex items-center justify-between rounded-xl border border-slack-border bg-slack-surface-tertiary px-4 py-3"
                  >
                    <div>
                      <p className="text-sm font-semibold text-slack-text-primary">
                        {request.recipient.displayName}
                      </p>
                      <p className="text-xs text-slack-text-secondary">@{request.recipient.username}</p>
                    </div>
                    <span className="rounded-pill bg-slack-primary/30 px-3 py-1 text-xs font-medium text-slack-primary">
                      Requested
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="border-t border-slack-border pt-6">
        {searchError && (
          <div className="mb-3 rounded-xl border border-slack-accent-red/30 bg-slack-accent-red/10 px-4 py-3 text-sm text-slack-accent-red" role="alert">
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
