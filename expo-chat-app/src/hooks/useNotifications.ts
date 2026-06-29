import { useEffect, useRef } from 'react';
import { useRouter } from 'expo-router';
import { useNotificationStore } from '../stores/notificationStore';
import { useAuthStore } from '../stores/authStore';

export function useNotifications() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const token = useAuthStore((s) => s.token);
  const requestPermissionsAndRegister = useNotificationStore((s) => s.requestPermissionsAndRegister);
  const unregister = useNotificationStore((s) => s.unregister);
  const notifHandlerRef = useRef<{ remove: () => void } | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !token) {
      unregister();
      return;
    }

    requestPermissionsAndRegister();

    const setupListener = async () => {
      try {
        const Notifications = await import('expo-notifications');
        const sub = Notifications.addNotificationResponseReceivedListener((response) => {
          const data = response.notification.request.content.data;
          if (data?.roomId) {
            router.push(`/(tabs)/chats/${data.roomId}`);
          }
        });
        notifHandlerRef.current = sub;
      } catch {
        // Silently handle
      }
    };

    setupListener();

    return () => {
      notifHandlerRef.current?.remove();
    };
  }, [isAuthenticated, token, requestPermissionsAndRegister, unregister, router]);
}
