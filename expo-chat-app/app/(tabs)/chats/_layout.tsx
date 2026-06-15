import { Stack } from 'expo-router';

export default function ChatsLayout() {
  return (
    <Stack>
      <Stack.Screen name="index" options={{ title: 'Chats', headerLargeTitle: true }} />
      <Stack.Screen name="[roomId]" options={{ headerShown: false }} />
    </Stack>
  );
}
