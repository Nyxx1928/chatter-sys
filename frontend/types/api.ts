/**
 * API request and response types matching the Java backend DTOs.
 * These types define the structure of data sent to and received from the REST API.
 */

import { User, Message } from './domain';

/**
 * Login request payload.
 * Sent to POST /api/auth/login
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Login response payload.
 * Received from POST /api/auth/login
 */
export interface LoginResponse {
  token: string;
  user: User;
  csrfToken: string;
}

/**
 * Registration request payload.
 * Sent to POST /api/auth/register
 */
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName: string;
}

/**
 * Update profile request payload.
 * Sent to PUT /api/users/me
 */
export interface UpdateProfileRequest {
  email?: string;
  displayName?: string;
}

/**
 * Create room request payload.
 * Sent to POST /api/rooms
 */
export interface CreateRoomRequest {
  name: string;
  description?: string;
}

/**
 * Friend request creation payload.
 * Sent to POST /api/friends/requests
 */
export interface FriendRequestCreateRequest {
  recipientId: number;
}

/**
 * Message history response payload.
 * Received from GET /api/rooms/{roomId}/messages
 * Uses Spring Data's Page structure for pagination.
 */
export interface MessageHistoryResponse {
  content: Message[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

/**
 * Simplified pagination parameters for API requests.
 */
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}
