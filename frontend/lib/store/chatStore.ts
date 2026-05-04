'use client';

import { create } from 'zustand';
import { ChatRoom, Message } from '../../types/domain';

type ChatState = {
  rooms: ChatRoom[];
  currentRoom: ChatRoom | null;
  messages: Map<string, Message[]>;
  setRooms: (rooms: ChatRoom[]) => void;
  setCurrentRoom: (room: ChatRoom | null) => void;
  addMessage: (roomId: number, message: Message) => void;
  loadMessages: (roomId: number, messages: Message[]) => void;
};

export const useChatStore = create<ChatState>((set) => ({
  rooms: [],
  currentRoom: null,
  messages: new Map<string, Message[]>(),
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
  loadMessages: (roomId, messages) =>
    set((state) => {
      const roomKey = roomId.toString();
      const nextMessages = new Map(state.messages);

      nextMessages.set(roomKey, messages);

      return { messages: nextMessages };
    })
}));
