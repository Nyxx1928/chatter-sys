import { Stack } from 'expo-router';

export default function ChannelsLayout() {
  return (
    <Stack>
      <Stack.Screen name="index" options={{ title: 'Channels' }} />
      <Stack.Screen name="[roomId]" options={{ headerShown: false }} />
    </Stack>
  );
}
