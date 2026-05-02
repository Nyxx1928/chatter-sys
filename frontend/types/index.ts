/**
 * Central export file for all TypeScript type definitions.
 * Import types from this file for convenience.
 */

// Domain types
export type {
  User,
  ChatRoom,
  Message,
  RoomMembership
} from './domain';

export {
  MessageType,
  MemberRole
} from './domain';

// API types
export type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  CreateRoomRequest,
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
