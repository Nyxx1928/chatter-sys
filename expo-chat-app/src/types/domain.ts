export enum MessageType {
  TEXT = 'TEXT',
  SYSTEM = 'SYSTEM',
  JOIN = 'JOIN',
  LEAVE = 'LEAVE'
}

export enum MemberRole {
  OWNER = 'OWNER',
  MODERATOR = 'MODERATOR',
  MEMBER = 'MEMBER'
}

export interface User {
  id: number;
  username: string;
  email: string;
  displayName: string;
  createdAt: string;
  lastSeen: string | null;
  online: boolean;
  emailVerified?: boolean;
  verificationUrl?: string;
  verificationEmailSent?: boolean;
}

export interface PublicUser {
  id: number;
  username: string;
  displayName: string;
  lastSeen: string | null;
  online: boolean;
}

export enum RelationshipStatus {
  NONE = 'NONE',
  PENDING_INCOMING = 'PENDING_INCOMING',
  PENDING_OUTGOING = 'PENDING_OUTGOING',
  FRIENDS = 'FRIENDS'
}

export type RoomType = 'GROUP' | 'DIRECT';

export interface ChatRoom {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
  createdBy: User;
  roomType: RoomType;
  otherParticipant?: PublicUser;
}

export interface Message {
  id: number;
  senderId: number;
  senderUsername: string;
  senderDisplayName: string;
  chatRoomId: number;
  content: string;
  timestamp: string;
  messageType: MessageType;
}

export interface RoomMembership {
  id: number;
  userId: number;
  chatRoomId: number;
  joinedAt: string;
  role: MemberRole;
}

export interface FriendRequest {
  id: number;
  requester: PublicUser;
  recipient: PublicUser;
  createdAt: string;
}

export interface FriendRequestList {
  incoming: FriendRequest[];
  outgoing: FriendRequest[];
}

export interface Friendship {
  friend: PublicUser;
  createdAt: string;
  dmRoomId: number;
}

export interface UserSearchResult {
  user: PublicUser;
  relationshipStatus: RelationshipStatus;
}
