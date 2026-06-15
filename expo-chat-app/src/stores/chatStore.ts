import { create } from 'zustand';
import { ChatRoom, Message } from '../types/domain';

export type MessageStatus = 'sending' | 'sent' | 'failed';

export interface MessageWithStatus extends Message {
  _status?: MessageStatus;
}

type RoomPagination = {
  currentPage: number;
  hasMore: boolean;
};

type ChatState = {
  rooms: ChatRoom[];
  currentRoom: ChatRoom | null;
  messages: Map<string, MessageWithStatus[]>;
  pagination: Record<string, RoomPagination>;
  setRooms: (rooms: ChatRoom[]) => void;
  setCurrentRoom: (room: ChatRoom | null) => void;
  addMessage: (roomId: number, message: MessageWithStatus) => void;
  prependMessages: (roomId: number, messages: MessageWithStatus[], page: number, hasMore: boolean) => void;
  loadMessages: (roomId: number, messages: MessageWithStatus[]) => void;
  updateMessageStatus: (roomId: number, messageId: number, status: MessageStatus) => void;
  confirmMessage: (roomId: number, tempId: number, confirmed: Message) => void;
  clearRoomMessages: (roomId: number) => void;
};

export const useChatStore = create<ChatState>((set) => ({
  rooms: [],
  currentRoom: null,
  messages: new Map<string, MessageWithStatus[]>(),
  pagination: {},
  setRooms: (rooms) => set({ rooms }),
  setCurrentRoom: (room) => set({ currentRoom: room }),
  addMessage: (roomId, message) =>
    set((state) => {
      const roomKey = roomId.toString();
      const nextMessages = new Map(state.messages);
      const existing = nextMessages.get(roomKey) ?? [];
      nextMessages.set(roomKey, [...existing, message]);
      return { messages: nextMessages };
    }),
  prependMessages: (roomId, messages, page, hasMore) =>
    set((state) => {
      const roomKey = roomId.toString();
      const nextMessages = new Map(state.messages);
      const existing = nextMessages.get(roomKey) ?? [];
      const existingIds = new Set(existing.map((m) => m.id));
      const uniqueNew = messages.filter((m) => !existingIds.has(m.id));
      nextMessages.set(roomKey, [...uniqueNew, ...existing]);
      return {
        messages: nextMessages,
        pagination: { ...state.pagination, [roomKey]: { currentPage: page, hasMore } },
      };
    }),
  loadMessages: (roomId, messages) =>
    set((state) => {
      const roomKey = roomId.toString();
      const nextMessages = new Map(state.messages);
      nextMessages.set(roomKey, messages);
      return {
        messages: nextMessages,
        pagination: { ...state.pagination, [roomKey]: { currentPage: 0, hasMore: messages.length > 0 } },
      };
    }),
  updateMessageStatus: (roomId, messageId, status) =>
    set((state) => {
      const roomKey = roomId.toString();
      const nextMessages = new Map(state.messages);
      const existing = nextMessages.get(roomKey) ?? [];
      nextMessages.set(
        roomKey,
        existing.map((m) => (m.id === messageId ? { ...m, _status: status } : m))
      );
      return { messages: nextMessages };
    }),
  confirmMessage: (roomId, tempId, confirmed) =>
    set((state) => {
      const roomKey = roomId.toString();
      const nextMessages = new Map(state.messages);
      const existing = nextMessages.get(roomKey) ?? [];
      nextMessages.set(
        roomKey,
        existing.map((m) => (m.id === tempId ? { ...confirmed, _status: 'sent' as const } : m))
      );
      return { messages: nextMessages };
    }),
  clearRoomMessages: (roomId) =>
    set((state) => {
      const roomKey = roomId.toString();
      const nextMessages = new Map(state.messages);
      nextMessages.delete(roomKey);
      const nextPagination = { ...state.pagination };
      delete nextPagination[roomKey];
      return { messages: nextMessages, pagination: nextPagination };
    }),
}));
