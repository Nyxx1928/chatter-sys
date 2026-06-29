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
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

type RoomWithLatest = ChatRoom & { latestMessage?: string; latestTimestamp?: string };

export default function ChatsScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
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
      <View style={[styles.center, { backgroundColor: colors.surfaceSecondary }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (error && rooms.length === 0) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfaceSecondary }]}>
        <Text style={[styles.errorText, { color: colors.accentRed }]}>{error}</Text>
        <Pressable style={[styles.retryButton, { backgroundColor: colors.primary }]} onPress={loadRooms}>
          <Text style={[styles.retryText, { color: colors.textInverse }]}>Retry</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}>
      <ConnectionBanner />
      <View style={styles.searchContainer}>
        <TextInput
          style={[styles.searchInput, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
          placeholder="Search conversations..."
          placeholderTextColor={colors.textSecondary}
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
            <Text style={[styles.emptyTitle, { color: colors.textSecondary }]}>No conversations yet</Text>
            <Text style={[styles.emptySubtitle, { color: colors.textSecondary }]}>Add friends to start chatting.</Text>
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
    padding: SlackSpacing['2xl'],
  },
  searchContainer: {
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.sm,
  },
  searchInput: {
    borderWidth: 1,
    borderRadius: SlackBorderRadius.pill,
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.sm,
    fontSize: SlackTypography.bodyLg.fontSize,
  },
  emptyState: {
    padding: 40,
    alignItems: 'center',
  },
  emptyTitle: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
    marginBottom: SlackSpacing.sm,
  },
  emptySubtitle: {
    fontSize: SlackTypography.bodySm.fontSize,
    textAlign: 'center',
  },
  errorText: {
    fontSize: SlackTypography.bodyLg.fontSize,
    marginBottom: SlackSpacing.lg,
    textAlign: 'center',
  },
  retryButton: {
    borderRadius: SlackBorderRadius.pill,
    paddingHorizontal: SlackSpacing['2xl'],
    paddingVertical: SlackSpacing.md,
  },
  retryText: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
  },
});
