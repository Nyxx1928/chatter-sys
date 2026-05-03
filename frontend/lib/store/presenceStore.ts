'use client';

import { create } from 'zustand';

/**
 * Presence store for tracking online/offline status of users.
 * This store maintains a map of user IDs to their online status,
 * which can be updated via STOMP presence messages or API responses.
 */

type PresenceState = {
  // Map of userId to online status
  presenceMap: Record<number, boolean>;
  
  // Update a single user's presence
  updatePresence: (userId: number, online: boolean) => void;
  
  // Batch update multiple users' presence (from API responses)
  batchUpdatePresence: (updates: Array<{ userId: number; online: boolean }>) => void;
  
  // Get online status for a user (returns undefined if unknown)
  isOnline: (userId: number) => boolean | undefined;
  
  // Clear all presence data
  clearPresence: () => void;
};

export const usePresenceStore = create<PresenceState>((set, get) => ({
  presenceMap: {},
  
  updatePresence: (userId, online) => {
    set((state) => ({
      presenceMap: {
        ...state.presenceMap,
        [userId]: online
      }
    }));
  },
  
  batchUpdatePresence: (updates) => {
    set((state) => {
      const newMap = { ...state.presenceMap };
      updates.forEach(({ userId, online }) => {
        newMap[userId] = online;
      });
      return { presenceMap: newMap };
    });
  },
  
  isOnline: (userId) => {
    return get().presenceMap[userId];
  },
  
  clearPresence: () => {
    set({ presenceMap: {} });
  }
}));
