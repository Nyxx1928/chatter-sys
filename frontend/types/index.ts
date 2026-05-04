/**
 * Central export file for all TypeScript type definitions.
 * Import types from this file for convenience.
 */

// Domain types
export type {
  User,
  PublicUser,
  ChatRoom,
  Message,
  RoomMembership,
  FriendRequest,
  FriendRequestList,
  Friendship,
  UserSearchResult
} from './domain';

export {
  MessageType,
  MemberRole,
  RelationshipStatus
} from './domain';

// API types
export type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UpdateProfileRequest,
  CreateRoomRequest,
  FriendRequestCreateRequest,
  MessageHistoryResponse,
  PaginationParams
} from './api';

// STOMP types
export type {
  StompMessage,
  ChatMessagePayload,
  PresencePayload,
  JoinLeavePayload,
  RoomMessagePayload,
  StompErrorPayload
} from './stomp';
