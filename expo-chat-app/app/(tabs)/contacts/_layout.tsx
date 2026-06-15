import { Stack } from 'expo-router';

export default function ContactsLayout() {
  return (
    <Stack>
      <Stack.Screen name="index" options={{ title: 'Contacts' }} />
      <Stack.Screen name="add" options={{ title: 'Add Friend', presentation: 'modal' }} />
      <Stack.Screen name="requests" options={{ title: 'Friend Requests' }} />
    </Stack>
  );
}
