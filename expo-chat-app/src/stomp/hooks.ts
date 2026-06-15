import { IMessage } from '@stomp/stompjs';
import { useEffect } from 'react';
import { useConnectionStore } from '../stores/connectionStore';

type MessageHandler<T> = (payload: T) => void;

export const useStompSubscription = <T>(
  destination: string | null,
  onMessage: MessageHandler<T>
): void => {
  const { client, connected } = useConnectionStore();

  useEffect(() => {
    if (!client || !connected || !destination) return undefined;

    const subscription = client.subscribe(destination, (message: IMessage) => {
      if (!message.body) return;
      try {
        const payload = JSON.parse(message.body) as T;
        onMessage(payload);
      } catch { /* ignore malformed payloads */ }
    });

    return () => { subscription.unsubscribe(); };
  }, [client, connected, destination, onMessage]);
};
