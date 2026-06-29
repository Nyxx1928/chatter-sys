import { create } from 'zustand';
import { Platform } from 'react-native';
import { registerPushToken, unregisterPushToken } from '../api/notifications';
import { getStoredToken } from '../utils/storage';

type NotificationState = {
  pushToken: string | null;
  permissionGranted: boolean;
  requestPermissionsAndRegister: () => Promise<void>;
  unregister: () => Promise<void>;
};

export const useNotificationStore = create<NotificationState>((set, get) => ({
  pushToken: null,
  permissionGranted: false,
  requestPermissionsAndRegister: async () => {
    try {
      const Notifications = await import('expo-notifications');
      const { status } = await Notifications.requestPermissionsAsync();
      const granted = status === 'granted';
      set({ permissionGranted: granted });

      if (!granted) return;

      const pushTokenData = await Notifications.getExpoPushTokenAsync();
      const token = pushTokenData.data;
      set({ pushToken: token });

      const authToken = getStoredToken();
      if (authToken) {
        await registerPushToken(
          authToken,
          token,
          Platform.OS === 'ios' ? 'ios' : 'android'
        );
      }
    } catch {
      // Silently handle permission/registration errors
    }
  },
  unregister: async () => {
    const { pushToken } = get();
    if (!pushToken) return;
    try {
      const authToken = getStoredToken();
      if (authToken) {
        await unregisterPushToken(authToken, pushToken);
      }
    } catch {
      // Silently handle unregister errors
    }
    set({ pushToken: null, permissionGranted: false });
  },
}));
