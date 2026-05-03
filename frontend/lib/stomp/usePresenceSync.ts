'use client';

import { useStompSubscription } from './hooks';
import { usePresenceStore } from '../store/presenceStore';
import { useConnectionStore } from '../store/connectionStore';
import { PresencePayload } from '@/types/stomp';

/**
 * Hook to sync presence updates from STOMP to the presence store.
 * Subscribes to presence updates for a specific room and updates the global presence store.
 * 
 * @param roomId - The room ID to subscribe to, or null to skip subscription
 */
export const usePresenceSync = (roomId: number | null) => {
  const { connected } = useConnectionStore();
  const { updatePresence } = usePresenceStore();

  useStompSubscription<PresencePayload>(
    connected && roomId ? `/topic/presence/${roomId}` : null,
    (payload) => {
      updatePresence(payload.userId, payload.online);
    }
  );
};

/**
 * Hook to sync presence for multiple rooms.
 * Useful when the user is a member of multiple rooms and we want to track
 * presence across all of them.
 * 
 * @param roomIds - Array of room IDs to subscribe to
 */
export const useMultiRoomPresenceSync = (roomIds: number[]) => {
  const { connected } = useConnectionStore();
  const { updatePresence } = usePresenceStore();

  // Subscribe to each room's presence topic
  roomIds.forEach((roomId) => {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    useStompSubscription<PresencePayload>(
      connected ? `/topic/presence/${roomId}` : null,
      (payload) => {
        updatePresence(payload.userId, payload.online);
      }
    );
  });
};
