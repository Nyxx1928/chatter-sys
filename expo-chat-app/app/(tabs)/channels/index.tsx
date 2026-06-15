import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, View } from 'react-native';
import { FlashList } from '@shopify/flash-list';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Text } from '@/components/Themed';
import ConnectionBanner from '@/src/components/ConnectionBanner';
import { useChatStore } from '@/src/stores/chatStore';
import { useAuthStore } from '@/src/stores/authStore';
import { useStompSubscription } from '@/src/stomp/hooks';
import { listRooms } from '@/src/api/rooms';
import { ChatRoom } from '@/src/types/domain';
import { RoomMessagePayload } from '@/src/types/stomp';

export default function ChannelsScreen() {
  const router = useRouter();
  const token = useAuthStore((s) => s.token);
  const rooms = useChatStore((s) => s.rooms);
  const setRooms = useChatStore((s) => s.setRooms);
  const messages = useChatStore((s) => s.messages);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const groupRooms = rooms
    .filter((r) => r.roomType === 'GROUP')
    .map((r) => {
      const roomMessages = messages.get(r.id.toString());
      const latest = roomMessages?.[roomMessages.length - 1];
      return { ...r, latestMessage: latest?.content };
    });

  const loadChannels = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const data = await listRooms(token);
      setRooms(data);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to load channels');
    } finally {
      setLoading(false);
    }
  }, [token, setRooms]);

  useEffect(() => {
    loadChannels();
  }, [loadChannels]);

  useStompSubscription<RoomMessagePayload>('/topic/rooms', () => {
    loadChannels();
  });

  const renderItem = useCallback(
    ({ item }: { item: ChatRoom & { latestMessage?: string } }) => (
      <Pressable
        style={styles.channelRow}
        onPress={() => router.push(`/(tabs)/channels/${item.id}`)}
      >
        <View style={styles.channelIcon}>
          <Ionicons name={"hash" as any} size={24} color="#fff" />
        </View>
        <View style={styles.channelContent}>
          <Text style={styles.channelName}>{item.name}</Text>
          {item.latestMessage && (
            <Text style={styles.channelPreview} numberOfLines={1}>
              {item.latestMessage}
            </Text>
          )}
        </View>
      </Pressable>
    ),
    [router]
  );

  if (loading && groupRooms.length === 0) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#2f95dc" />
      </View>
    );
  }

  if (error && groupRooms.length === 0) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error}</Text>
        <Pressable style={styles.retryButton} onPress={loadChannels}>
          <Text style={styles.retryText}>Retry</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <ConnectionBanner />
      <View style={styles.headerRow}>
        <Text style={styles.headerTitle}>Channels</Text>
        <Pressable
          style={styles.createButton}
          onPress={() => router.push('/(tabs)/channels/create')}
        >
          <Ionicons name="add" size={24} color="#fff" />
        </Pressable>
      </View>
      <FlashList
        data={groupRooms}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderItem}
        onRefresh={loadChannels}
        refreshing={loading}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>No channels yet</Text>
            <Text style={styles.emptySubtitle}>Create one!</Text>
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
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: 'bold',
  },
  createButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#2f95dc',
    justifyContent: 'center',
    alignItems: 'center',
  },
  channelRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  channelIcon: {
    width: 44,
    height: 44,
    borderRadius: 10,
    backgroundColor: '#6b7280',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  channelContent: {
    flex: 1,
  },
  channelName: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 2,
  },
  channelPreview: {
    fontSize: 14,
    color: '#6b7280',
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
