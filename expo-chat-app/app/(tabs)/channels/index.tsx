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
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function ChannelsScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
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
        style={[styles.channelRow, { borderBottomColor: colors.border }]}
        onPress={() => router.push(`/(tabs)/channels/${item.id}`)}
      >
        <View style={[styles.channelIcon, { backgroundColor: colors.primary }]}>
          <Ionicons name={"hash" as any} size={24} color={colors.textInverse} />
        </View>
        <View style={styles.channelContent}>
          <Text style={[styles.channelName, { color: colors.textPrimary }]}>{item.name}</Text>
          {item.latestMessage && (
            <Text style={[styles.channelPreview, { color: colors.textSecondary }]} numberOfLines={1}>
              {item.latestMessage}
            </Text>
          )}
        </View>
      </Pressable>
    ),
    [router, colors]
  );

  if (loading && groupRooms.length === 0) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfaceSecondary }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (error && groupRooms.length === 0) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfaceSecondary }]}>
        <Text style={[styles.errorText, { color: colors.accentRed }]}>{error}</Text>
        <Pressable style={[styles.retryButton, { backgroundColor: colors.primary }]} onPress={loadChannels}>
          <Text style={[styles.retryText, { color: colors.textInverse }]}>Retry</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}>
      <ConnectionBanner />
      <View style={styles.headerRow}>
        <Text style={[styles.headerTitle, { color: colors.textPrimary }]}>Channels</Text>
        <Pressable
          style={[styles.createButton, { backgroundColor: colors.primary }]}
          onPress={() => router.push('/(tabs)/channels/create')}
        >
          <Ionicons name="add" size={24} color={colors.textInverse} />
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
            <Text style={[styles.emptyTitle, { color: colors.textSecondary }]}>No channels yet</Text>
            <Text style={[styles.emptySubtitle, { color: colors.textSecondary }]}>Create one!</Text>
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
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.md,
  },
  headerTitle: {
    fontSize: SlackTypography.displayXl.fontSize,
    fontWeight: SlackTypography.displayXl.fontWeight,
    fontFamily: SlackTypography.displayXl.fontFamily,
  },
  createButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  channelRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.md,
    borderBottomWidth: 1,
  },
  channelIcon: {
    width: 44,
    height: 44,
    borderRadius: SlackBorderRadius.md,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: SlackSpacing.md,
  },
  channelContent: {
    flex: 1,
  },
  channelName: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
    marginBottom: 2,
  },
  channelPreview: {
    fontSize: SlackTypography.bodySm.fontSize,
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
