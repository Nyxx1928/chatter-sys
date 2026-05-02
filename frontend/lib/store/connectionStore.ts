'use client';

import { Client, IFrame } from '@stomp/stompjs';
import { create } from 'zustand';
import { createStompClient } from '../stomp/client';
import { getStoredToken } from '../../utils/storage';

type ConnectionState = {
  client: Client | null;
  connected: boolean;
  connecting: boolean;
  error: string | null;
  connect: (token?: string | null) => void;
  disconnect: () => void;
  sendMessage: (destination: string, payload: unknown) => void;
};

const resolveStompError = (frame: IFrame): string =>
  frame.headers['message'] ?? 'STOMP error encountered.';

export const useConnectionStore = create<ConnectionState>((set, get) => ({
  client: null,
  connected: false,
  connecting: false,
  error: null,
  connect: (token) => {
    const { connected, connecting } = get();
    if (connected || connecting) {
      return;
    }

    set({ connecting: true, error: null });

    const client = createStompClient({
      token: token ?? getStoredToken(),
      onConnect: () => {
        set({ connected: true, connecting: false, error: null });
        
        // Subscribe to user-specific error queue
        client.subscribe('/user/queue/errors', (message) => {
          try {
            const errorData = JSON.parse(message.body);
            set({ error: errorData.message || 'An error occurred' });
          } catch {
            set({ error: 'An error occurred' });
          }
        });
      },
      onDisconnect: () => {
        set({ connected: false, connecting: false });
      },
      onStompError: (frame) => {
        set({
          error: resolveStompError(frame),
          connected: false,
          connecting: false
        });
      },
      onWebSocketError: () => {
        set({
          error: 'WebSocket error encountered.',
          connected: false,
          connecting: false
        });
      }
    });

    set({ client });
    client.activate();
  },
  disconnect: () => {
    const { client } = get();
    if (!client) {
      return;
    }

    client.deactivate();
    set({ client: null, connected: false, connecting: false });
  },
  sendMessage: (destination, payload) => {
    const { client, connected } = get();

    if (!client || !connected) {
      set({ error: 'Cannot send message while disconnected.' });
      return;
    }

    client.publish({
      destination,
      body: JSON.stringify(payload ?? {})
    });
  }
}));
