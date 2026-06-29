import { Client, IFrame, StompHeaders } from '@stomp/stompjs';

type StompClientOptions = {
  token?: string | null;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onStompError?: (frame: IFrame) => void;
  onWebSocketError?: (event: Event) => void;
  debug?: boolean;
};

export const createStompClient = ({
  token, onConnect, onDisconnect, onStompError, onWebSocketError, debug = false
}: StompClientOptions): Client => {
  const brokerUrl = process.env.EXPO_PUBLIC_WS_URL ?? 'ws://localhost:8080/ws';

  const connectHeaders: StompHeaders = {};
  if (token) connectHeaders.Authorization = `Bearer ${token}`;

  const client = new Client({
    webSocketFactory: () => new WebSocket(brokerUrl),
    connectHeaders,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    debug: debug ? (message) => console.log('STOMP:', message) : () => {},
    onConnect: () => { console.log('STOMP connected'); onConnect?.(); },
    onDisconnect: () => { console.log('STOMP disconnected'); onDisconnect?.(); },
    onStompError: (frame) => { console.error('STOMP error:', frame); onStompError?.(frame); },
    onWebSocketError: (event) => { console.error('WebSocket error:', event); onWebSocketError?.(event); }
  });

  return client;
};
