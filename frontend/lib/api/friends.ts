import {
  FriendRequestList,
  FriendRequest,
  Friendship,
  PublicUser
} from '../../types/domain';
import { apiCall } from './client';

export const sendFriendRequest = async (
  token: string,
  recipientId: number
): Promise<FriendRequest> =>
  apiCall<FriendRequest>('/api/friends/requests', {
    method: 'POST',
    token,
    body: JSON.stringify({ recipientId })
  });

export const listFriendRequests = async (token: string): Promise<FriendRequestList> =>
  apiCall<FriendRequestList>('/api/friends/requests', {
    method: 'GET',
    token
  });

// Alias for consistency with task naming
export const listPendingRequests = listFriendRequests;

export const acceptFriendRequest = async (
  token: string,
  requestId: number
): Promise<Friendship> =>
  apiCall<Friendship>(`/api/friends/requests/${requestId}/accept`, {
    method: 'POST',
    token
  });

export const declineFriendRequest = async (
  token: string,
  requestId: number
): Promise<void> =>
  apiCall<void>(`/api/friends/requests/${requestId}/decline`, {
    method: 'POST',
    token
  });

export const listFriends = async (token: string): Promise<PublicUser[]> =>
  apiCall<PublicUser[]>('/api/friends', {
    method: 'GET',
    token
  });
