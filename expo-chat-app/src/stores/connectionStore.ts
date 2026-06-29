import AsyncStorage from '@react-native-async-storage/async-storage';
import { Client, IFrame } from '@stomp/stompjs';
import { create } from 'zustand';
import { createStompClient } from '../stomp/client';
import { getStoredToken } from '../utils/storage';

const QUEUE_KEY = 'offline_message_queue';
const MAX_RETRY_DELAY = 30000;

type QueuedMessage = {
  destination: string;
  payload: unknown;
  retryCount: number;
  nextRetry: number;
};

type ConnectionState = {
  client: Client | null;
  connected: boolean;
  connecting: boolean;
  error: string | null;
  messageQueue: QueuedMessage[];
  connect: (token?: string | null) => void;
  disconnect: () => void;
  sendMessage: (destination: string, payload: unknown) => void;
  flushQueue: () => void;
};

const loadQueue = async (): Promise<QueuedMessage[]> => {
  try {
    const raw = await AsyncStorage.getItem(QUEUE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

const saveQueue = async (queue: QueuedMessage[]) => {
  try {
    await AsyncStorage.setItem(QUEUE_KEY, JSON.stringify(queue));
  } catch {
    // Ignore storage errors
  }
};

const resolveStompError = (frame: IFrame): string =>
  frame.headers['message'] ?? 'STOMP error encountered.';

export const useConnectionStore = create<ConnectionState>((set, get) => ({
  client: null,
  connected: false,
  connecting: false,
  error: null,
  messageQueue: [],
  connect: (token) => {
    const { connected, connecting } = get();
    if (connected || connecting) return;
    set({ connecting: true, error: null });

    const client = createStompClient({
      token: token ?? getStoredToken(),
      onConnect: () => {
        set({ connected: true, connecting: false, error: null });
        client.subscribe('/user/queue/errors', (message) => {
          try {
            const errorData = JSON.parse(message.body);
            set({ error: errorData.message || 'An error occurred' });
          } catch {
            set({ error: 'An error occurred' });
          }
        });
        get().flushQueue();
      },
      onDisconnect: () => { set({ connected: false, connecting: false }); },
      onStompError: (frame) => { set({ error: resolveStompError(frame), connected: false, connecting: false }); },
      onWebSocketError: () => { set({ error: 'WebSocket error encountered.', connected: false, connecting: false }); }
    });

    set({ client });
    client.activate();
  },
  disconnect: () => {
    const { client } = get();
    if (!client) return;
    client.deactivate();
    set({ client: null, connected: false, connecting: false });
  },
  sendMessage: (destination, payload) => {
    const { client, connected } = get();
    if (!client || !connected) {
      const { messageQueue } = get();
      const now = Date.now();
      const item: QueuedMessage = { destination, payload, retryCount: 0, nextRetry: now };
      const updated = [...messageQueue, item];
      set({ messageQueue: updated });
      saveQueue(updated);
      return;
    }
    client.publish({ destination, body: JSON.stringify(payload ?? {}) });
  },
  flushQueue: () => {
    const { client, connected, messageQueue } = get();
    if (!client || !connected || messageQueue.length === 0) return;

    const now = Date.now();
    const remaining: QueuedMessage[] = [];

    for (const item of messageQueue) {
      if (item.nextRetry > now) {
        remaining.push(item);
        continue;
      }

      try {
        client.publish({ destination: item.destination, body: JSON.stringify(item.payload ?? {}) });
      } catch {
        const retryCount = item.retryCount + 1;
        const delay = Math.min(1000 * Math.pow(2, retryCount - 1), MAX_RETRY_DELAY);
        remaining.push({ ...item, retryCount, nextRetry: now + delay });
      }
    }

    set({ messageQueue: remaining });
    saveQueue(remaining);

    if (remaining.length > 0) {
      setTimeout(() => {
        const state = get();
        if (state.connected) state.flushQueue();
      }, 1000);
    }
  }
}));

loadQueue().then((queue) => {
  useConnectionStore.setState({ messageQueue: queue });
});
