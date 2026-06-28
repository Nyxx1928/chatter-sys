import { Stack } from 'expo-router';
import { useColorScheme } from '@/components/useColorScheme';
import { SlackColors } from '@/constants/Colors';

export default function ProfileLayout() {
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  return (
    <Stack
      screenOptions={{
        contentStyle: { backgroundColor: colors.surfaceSecondary },
      }}
    >
      <Stack.Screen name="index" options={{ title: 'Profile' }} />
    </Stack>
  );
}
