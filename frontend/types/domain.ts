/**
 * Domain model types matching the Java backend entities.
 * These types represent the core business objects in the chat system.
 */

/**
 * Message type enumeration.
 * Defines the different types of messages that can be sent in the chat system.
 */
export enum MessageType {
  TEXT = 'TEXT',
  SYSTEM = 'SYSTEM',
  JOIN = 'JOIN',
  LEAVE = 'LEAVE'
}

/**
 * Member role enumeration.
 * Defines the different roles a user can have in a chat room.
 */
export enum MemberRole {
  OWNER = 'OWNER',
  MODERATOR = 'MODERATOR',
  MEMBER = 'MEMBER'
}

/**
 * User interface matching the backend User entity.
 * Represents a user in the chat system.
 */
export interface User {
  id: number;
  username: string;
  email: string;
  displayName: string;
  createdAt: string; // ISO 8601 date string
  lastSeen: string | null; // ISO 8601 date string
  online: boolean;
}

/**
 * Public user profile for search and friends views.
 */
export interface PublicUser {
  id: number;
  username: string;
  displayName: string;
  lastSeen: string | null; // ISO 8601 date string
  online: boolean;
}

/**
 * Relationship status between the current user and another user.
 */
export enum RelationshipStatus {
  NONE = 'NONE',
  PENDING_INCOMING = 'PENDING_INCOMING',
  PENDING_OUTGOING = 'PENDING_OUTGOING',
  FRIENDS = 'FRIENDS'
}

/**
 * Room type enumeration.
 * GROUP rooms are standard multi-user rooms.
 * DIRECT rooms are private one-on-one DM rooms.
 */
export type RoomType = 'GROUP' | 'DIRECT';

/**
 * ChatRoom interface matching the backend ChatRoom entity.
 * Represents a chat room where users can send messages.
 */
export interface ChatRoom {
  id: number;
  name: string;
  description: string | null;
  createdAt: string; // ISO 8601 date string
  createdBy: User;
  roomType: RoomType;
  /** Client-side only — derived from the members list after room selection. Never stored on the server. */
  otherParticipant?: PublicUser;
}

/**
 * Message interface matching the backend Message entity.
 * Represents a chat message sent by a user in a room.
 */
export interface Message {
  id: number;
  senderId: number;
  senderUsername: string;
  senderDisplayName: string;
  chatRoomId: number;
  content: string;
  timestamp: string; // ISO 8601 date string
  messageType: MessageType;
}

/**
 * RoomMembership interface matching the backend RoomMembership entity.
 * Represents a user's membership in a chat room.
 */
export interface RoomMembership {
  id: number;
  userId: number;
  chatRoomId: number;
  joinedAt: string; // ISO 8601 date string
  role: MemberRole;
}

/**
 * Friend request interface matching backend DTOs.
 */
export interface FriendRequest {
  id: number;
  requester: PublicUser;
  recipient: PublicUser;
  createdAt: string; // ISO 8601 date string
}

/**
 * Pending friend request lists.
 */
export interface FriendRequestList {
  incoming: FriendRequest[];
  outgoing: FriendRequest[];
}

/**
 * Friendship response containing the friend profile.
 */
export interface Friendship {
  friend: PublicUser;
  createdAt: string; // ISO 8601 date string
  dmRoomId: number;
}

/**
 * User search result entry.
 */
export interface UserSearchResult {
  user: PublicUser;
  relationshipStatus: RelationshipStatus;
}
