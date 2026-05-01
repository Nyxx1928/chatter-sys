/**
 * STOMP WebSocket message types.
 * These types define the structure of messages sent and received over WebSocket connections.
 */

import { Message, MessageType } from './domain';

/**
 * Generic STOMP message structure.
 * Represents the base structure of a STOMP frame.
 */
export interface StompMessage<T = unknown> {
  body: T;
  headers: Record<string, string>;
  command: string;
  isBinaryBody: boolean;
}

/**
 * Chat message payload sent to /app/chat.send/{roomId}.
 * This is the structure of messages sent by clients when sending chat messages.
 */
export interface ChatMessagePayload {
  content: string;
  messageType?: MessageType;
}

/**
 * Presence payload received from /topic/presence/{roomId}.
 * Broadcasts user online/offline status changes to room members.
 */
export interface PresencePayload {
  userId: number;
  username: string;
  displayName: string;
  online: boolean;
  lastSeen: string | null; // ISO 8601 date string
  roomId: number;
}

/**
 * Join/Leave payload for room join and leave operations.
 * Sent to /app/room.join/{roomId} or /app/room.leave/{roomId}.
 * The backend doesn't require a body for these operations.
 */
// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export interface JoinLeavePayload {
  // No body required - roomId is in the destination path
  // This interface exists for type safety and future extensibility
}

/**
 * Message received from /topic/room/{roomId}.
 * This is the full message structure broadcast to all room subscribers.
 * This type alias makes it clear this is a broadcast message.
 */
export type RoomMessagePayload = Message;

/**
 * Error payload received from /user/queue/errors.
 * Sent when an error occurs during STOMP message processing.
 */
export interface StompErrorPayload {
  message: string;
  timestamp: string; // ISO 8601 date string
  status: number;
}
