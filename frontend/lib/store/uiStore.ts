'use client';

import { create } from 'zustand';

type UiState = {
  showFriendsPanel: boolean;
  toggleFriendsPanel: () => void;
  setFriendsPanel: (open: boolean) => void;
};

export const useUiStore = create<UiState>((set) => ({
  showFriendsPanel: false,
  toggleFriendsPanel: () => set((s) => ({ showFriendsPanel: !s.showFriendsPanel })),
  setFriendsPanel: (open) => set({ showFriendsPanel: open }),
}));
