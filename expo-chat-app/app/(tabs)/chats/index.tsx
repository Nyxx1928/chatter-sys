import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, TextInput, View } from 'react-native';
import { FlashList } from '@shopify/flash-list';
import { useRouter } from 'expo-router';
import { Text } from '@/components/Themed';
import RoomListItem from '@/src/components/RoomListItem';
import ConnectionBanner from '@/src/components/ConnectionBanner';
import { useChatStore } from '@/src/stores/chatStore';
import { useAuthStore } from '@/src/stores/authStore';
import { useConnectionStore } from '@/src/stores/connectionStore';
import { useStompSubscription } from '@/src/stomp/hooks';
import { listRooms } from '@/src/api/rooms';
import { ChatRoom } from '@/src/types/domain';
import { RoomMessagePayload } from '@/src/types/stomp';

type RoomWithLatest = ChatRoom & { latestMessage?: string; latestTimestamp?: string };

export default function ChatsScreen() {
  const router = useRouter();
  const token = useAuthStore((s) => s.token);
  const rooms = useChatStore((s) => s.rooms);
  const setRooms = useChatStore((s) => s.setRooms);
  const messages = useChatStore((s) => s.messages);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState('');

  const directRooms: RoomWithLatest[] = rooms
    .filter((r) => r.roomType === 'DIRECT')
    .map((r) => {
      const roomMessages = messages.get(r.id.toString());
      const latest = roomMessages?.[roomMessages.length - 1];
      return {
        ...r,
        latestMessage: latest?.content,
        latestTimestamp: latest?.timestamp,
      };
    });

  const filteredRooms = filter
    ? directRooms.filter((r) =>
        r.otherParticipant?.displayName.toLowerCase().includes(filter.toLowerCase())
      )
    : directRooms;

  const loadRooms = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const data = await listRooms(token);
      setRooms(data);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to load rooms');
    } finally {
      setLoading(false);
    }
  }, [token, setRooms]);

  useEffect(() => {
    loadRooms();
  }, [loadRooms]);

  useStompSubscription<RoomMessagePayload>('/topic/rooms', () => {
    loadRooms();
  });

  const handleRoomPress = (roomId: number) => {
    router.push(`/(tabs)/chats/${roomId}`);
  };

  const renderItem = useCallback(
    ({ item }: { item: RoomWithLatest }) => (
      <RoomListItem
        room={item}
        latestMessage={item.latestMessage}
        latestMessageTimestamp={item.latestTimestamp}
        onPress={() => handleRoomPress(item.id)}
      />
    ),
    []
  );

  if (loading && rooms.length === 0) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#2f95dc" />
      </View>
    );
  }

  if (error && rooms.length === 0) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error}</Text>
        <Pressable style={styles.retryButton} onPress={loadRooms}>
          <Text style={styles.retryText}>Retry</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <ConnectionBanner />
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.searchInput}
          placeholder="Search conversations..."
          placeholderTextColor="#9ca3af"
          value={filter}
          onChangeText={setFilter}
          autoCapitalize="none"
          autoCorrect={false}
        />
      </View>
      <FlashList
        data={filteredRooms}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderItem}
        onRefresh={loadRooms}
        refreshing={loading}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>No conversations yet</Text>
            <Text style={styles.emptySubtitle}>Add friends to start chatting.</Text>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  searchContainer: {
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  searchInput: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 16,
    color: '#1f2937',
    backgroundColor: '#f9fafb',
  },
  emptyState: {
    padding: 40,
    alignItems: 'center',
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#6b7280',
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#9ca3af',
    textAlign: 'center',
  },
  errorText: {
    color: '#dc2626',
    fontSize: 16,
    marginBottom: 16,
    textAlign: 'center',
  },
  retryButton: {
    backgroundColor: '#2f95dc',
    borderRadius: 8,
    paddingHorizontal: 24,
    paddingVertical: 12,
  },
  retryText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
