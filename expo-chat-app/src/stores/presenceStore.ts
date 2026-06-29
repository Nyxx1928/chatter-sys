import { create } from 'zustand';

type PresenceState = {
  presenceMap: Record<number, boolean>;
  updatePresence: (userId: number, online: boolean) => void;
  batchUpdatePresence: (updates: Array<{ userId: number; online: boolean }>) => void;
  isOnline: (userId: number) => boolean | undefined;
  clearPresence: () => void;
};

export const usePresenceStore = create<PresenceState>((set, get) => ({
  presenceMap: {},
  updatePresence: (userId, online) => {
    set((state) => ({ presenceMap: { ...state.presenceMap, [userId]: online } }));
  },
  batchUpdatePresence: (updates) => {
    set((state) => {
      const newMap = { ...state.presenceMap };
      updates.forEach(({ userId, online }) => { newMap[userId] = online; });
      return { presenceMap: newMap };
    });
  },
  isOnline: (userId) => get().presenceMap[userId],
  clearPresence: () => { set({ presenceMap: {} }); }
}));
