'use client';

import { RelationshipStatus, UserSearchResult } from '@/types/domain';
import { Button, Input } from '@/components/ui';

export interface UserSearchProps {
  query: string;
  loading: boolean;
  results: UserSearchResult[];
  incomingRequestIds: Record<number, number>;
  onQueryChange: (value: string) => void;
  onSendRequest: (userId: number) => void;
  onAcceptRequest: (requestId: number) => void;
  onDeclineRequest: (requestId: number) => void;
}

const statusLabelMap: Record<RelationshipStatus, string> = {
  [RelationshipStatus.NONE]: 'Not connected',
  [RelationshipStatus.PENDING_INCOMING]: 'Request received',
  [RelationshipStatus.PENDING_OUTGOING]: 'Request sent',
  [RelationshipStatus.FRIENDS]: 'Friends'
};

/**
 * User search UI with relationship actions.
 */
export function UserSearch({
  query,
  loading,
  results,
  incomingRequestIds,
  onQueryChange,
  onSendRequest,
  onAcceptRequest,
  onDeclineRequest
}: UserSearchProps) {
  const renderActions = (result: UserSearchResult) => {
    switch (result.relationshipStatus) {
      case RelationshipStatus.NONE:
        return (
          <Button
            size="sm"
            onClick={() => onSendRequest(result.user.id)}
          >
            Add friend
          </Button>
        );
      case RelationshipStatus.PENDING_OUTGOING:
        return (
          <Button size="sm" variant="secondary" disabled>
            Requested
          </Button>
        );
      case RelationshipStatus.PENDING_INCOMING: {
        const requestId = incomingRequestIds[result.user.id];
        if (!requestId) {
          return (
            <span className="text-xs font-medium text-slack-text-secondary">
              Pending request
            </span>
          );
        }
        return (
          <div className="flex items-center gap-2">
            <Button size="sm" onClick={() => onAcceptRequest(requestId)}>
              Accept
            </Button>
            <Button
              size="sm"
              variant="ghost"
              onClick={() => onDeclineRequest(requestId)}
            >
              Decline
            </Button>
          </div>
        );
      }
      case RelationshipStatus.FRIENDS:
        return (
          <span className="rounded-pill bg-slack-accent-green/20 px-3 py-1 text-xs font-semibold text-slack-accent-green">
            Friends
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div className="space-y-4">
      <Input
        label="Find people"
        placeholder="Search by username or display name"
        value={query}
        onChange={(event) => onQueryChange(event.target.value)}
        fullWidth
        helperText="Start typing to search for friends"
      />

      <div className="space-y-3">
        {loading && (
          <div className="rounded-xl border border-slack-border bg-slack-surface-tertiary p-4 text-sm text-slack-text-secondary" role="status">
            Searching for users...
          </div>
        )}

        {!loading && query.trim().length === 0 && (
          <div className="rounded-xl border border-dashed border-slack-border p-4 text-sm text-slack-text-secondary">
            Enter a name to discover new people.
          </div>
        )}

        {!loading && query.trim().length > 0 && results.length === 0 && (
          <div className="rounded-xl border border-dashed border-slack-border p-4 text-sm text-slack-text-secondary">
            No users match that search.
          </div>
        )}

        {!loading && results.length > 0 && (
          <ul className="space-y-3" role="list" aria-label="User search results">
            {results.map((result) => (
              <li
                key={result.user.id}
                className="flex flex-col gap-3 rounded-xl border border-slack-border bg-slack-surface-primary p-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="flex items-center gap-3">
                  <div className="h-12 w-12 rounded-full bg-slack-primary text-slack-text-inverse flex items-center justify-center font-semibold">
                    {result.user.displayName.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slack-text-primary">
                      {result.user.displayName}
                    </p>
                    <p className="text-xs text-slack-text-secondary">@{result.user.username}</p>
                    <p className="text-xs text-slack-text-secondary">
                      {statusLabelMap[result.relationshipStatus]}
                    </p>
                  </div>
                </div>
                <div className="flex items-center justify-between gap-2 sm:justify-end">
                  <span
                    className={`inline-flex items-center gap-1 rounded-pill px-2 py-1 text-xs font-medium ${
                      result.user.online
                        ? 'bg-slack-accent-green/20 text-slack-accent-green'
                        : 'bg-slack-surface-tertiary text-slack-text-secondary'
                    }`}
                  >
                    <span
                      className={`h-2 w-2 rounded-full ${
                        result.user.online ? 'bg-slack-accent-green' : 'bg-slack-text-secondary'
                      }`}
                    />
                    {result.user.online ? 'Online' : 'Offline'}
                  </span>
                  {renderActions(result)}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
