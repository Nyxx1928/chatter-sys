import { Stack } from 'expo-router';
import { useColorScheme } from '@/components/useColorScheme';
import { SlackColors } from '@/constants/Colors';

export default function ContactsLayout() {
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  return (
    <Stack
      screenOptions={{
        contentStyle: { backgroundColor: colors.surfaceSecondary },
      }}
    >
      <Stack.Screen name="index" options={{ title: 'Contacts' }} />
      <Stack.Screen name="add" options={{ title: 'Add Friend', presentation: 'modal' }} />
      <Stack.Screen name="requests" options={{ title: 'Friend Requests' }} />
    </Stack>
  );
}
