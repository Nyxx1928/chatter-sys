import { Message, MessageType } from './domain';

export interface StompMessage<T = unknown> {
  body: T;
  headers: Record<string, string>;
  command: string;
  isBinaryBody: boolean;
}

export interface ChatMessagePayload {
  content: string;
  messageType?: MessageType;
}

export interface PresencePayload {
  userId: number;
  username: string;
  displayName: string;
  online: boolean;
  lastSeen: string | null;
  roomId: number;
}

export interface JoinLeavePayload {
}

export type RoomMessagePayload = Message;

export interface StompErrorPayload {
  message: string;
  timestamp: string;
  status: number;
}
