import { useFonts } from 'expo-font';
import {
  NotoSans_400Regular,
  NotoSans_500Medium,
  NotoSans_600SemiBold,
  NotoSans_700Bold,
} from '@expo-google-fonts/noto-sans';
import {
  NotoSansDisplay_400Regular,
  NotoSansDisplay_500Medium,
  NotoSansDisplay_600SemiBold,
  NotoSansDisplay_700Bold,
} from '@expo-google-fonts/noto-sans-display';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useCallback, useEffect, useRef } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import 'react-native-reanimated';

import { useAuthStore } from '@/src/stores/authStore';
import { useColorScheme } from '@/components/useColorScheme';
import { SlackColors } from '@/constants/Colors';

SplashScreen.preventAutoHideAsync();

export { ErrorBoundary } from 'expo-router';

export default function RootLayout() {
  const [loaded, error] = useFonts({
    SpaceMono: require('../assets/fonts/SpaceMono-Regular.ttf'),
    NotoSans_400Regular,
    NotoSans_500Medium,
    NotoSans_600SemiBold,
    NotoSans_700Bold,
    NotoSansDisplay_400Regular,
    NotoSansDisplay_500Medium,
    NotoSansDisplay_600SemiBold,
    NotoSansDisplay_700Bold,
  });

  useEffect(() => {
    if (error) throw error;
  }, [error]);

  useEffect(() => {
    if (loaded) {
      SplashScreen.hideAsync();
    }
  }, [loaded]);

  if (!loaded) {
    return null;
  }

  return <RootLayoutNav />;
}

function RootLayoutNav() {
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isInitialized = useAuthStore((s) => s.isInitialized);
  const isChecking = useAuthStore((s) => s.isChecking);
  const validateSession = useAuthStore((s) => s.validateSession);
  const notifHandlerRef = useRef<{ remove: () => void } | null>(null);

  useEffect(() => {
    validateSession();
  }, []);

  const setupNotifications = useCallback(async () => {
    try {
      const Notifications = await import('expo-notifications');
      Notifications.setNotificationHandler({
        handleNotification: async () => ({
          shouldShowAlert: true,
          shouldShowBanner: true,
          shouldShowList: true,
          shouldPlaySound: true,
          shouldSetBadge: false,
        }),
      });
      const sub = Notifications.addNotificationResponseReceivedListener((response) => {
        const data = response.notification.request.content.data;
        if (data?.roomId) {
        }
      });
      notifHandlerRef.current = sub;
    } catch {
    }
  }, []);

  useEffect(() => {
    setupNotifications();
    return () => {
      notifHandlerRef.current?.remove();
    };
  }, [setupNotifications]);

  if (!isInitialized || isChecking) {
    return (
      <View style={[styles.splash, { backgroundColor: colors.surfacePrimary }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <Stack screenOptions={{ headerShown: false }}>
      {isAuthenticated ? (
        <Stack.Screen name="(tabs)" />
      ) : (
        <Stack.Screen name="(auth)" />
      )}
    </Stack>
  );
}

const styles = StyleSheet.create({
  splash: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
});
