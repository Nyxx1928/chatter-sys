import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, StyleSheet, View } from 'react-native';
import { FlashList } from '@shopify/flash-list';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Text } from '@/components/Themed';
import PresenceDot from '@/src/components/PresenceDot';
import ConnectionBanner from '@/src/components/ConnectionBanner';
import { useAuthStore } from '@/src/stores/authStore';
import { usePresenceStore } from '@/src/stores/presenceStore';
import { useChatStore } from '@/src/stores/chatStore';
import { listFriends, removeFriend, listFriendRequests } from '@/src/api/friends';
import { PublicUser } from '@/src/types/domain';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function ContactsScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const setPendingRequestCount = useAuthStore((s) => s.setPendingRequestCount);
  const rooms = useChatStore((s) => s.rooms);
  const presenceMap = usePresenceStore((s) => s.presenceMap);
  const [friends, setFriends] = useState<PublicUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const [friendList, requests] = await Promise.all([
        listFriends(token),
        listFriendRequests(token),
      ]);
      setFriends(friendList);
      setPendingRequestCount(
        requests.incoming.length + requests.outgoing.length
      );
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to load contacts');
    } finally {
      setLoading(false);
    }
  }, [token, setPendingRequestCount]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleChat = useCallback(
    (friend: PublicUser) => {
      const dmRoom = rooms.find(
        (r) => r.roomType === 'DIRECT' && r.otherParticipant?.id === friend.id
      );
      if (dmRoom) {
        router.push(`/(tabs)/chats/${dmRoom.id}`);
      }
    },
    [rooms, router]
  );

  const handleRemoveFriend = useCallback(
    (friend: PublicUser) => {
      Alert.alert(
        'Remove Friend',
        `Are you sure you want to remove ${friend.displayName} from your friends?`,
        [
          { text: 'Cancel', style: 'cancel' },
          {
            text: 'Remove',
            style: 'destructive',
            onPress: async () => {
              if (!token) return;
              try {
                await removeFriend(token, friend.id);
                setFriends((prev) => prev.filter((f) => f.id !== friend.id));
              } catch (err: unknown) {
                const apiErr = err as { message?: string };
                Alert.alert('Error', apiErr.message || 'Failed to remove friend');
              }
            },
          },
        ]
      );
    },
    [token]
  );

  const renderItem = useCallback(
    ({ item }: { item: PublicUser }) => {
      const isOnline = presenceMap[item.id] ?? false;
      return (
        <Pressable
          style={[styles.friendRow, { borderBottomColor: colors.border }]}
          onLongPress={() => handleRemoveFriend(item)}
          onPress={() => handleChat(item)}
        >
          <View style={styles.avatarContainer}>
            <View style={[styles.avatar, { backgroundColor: colors.primary }]}>
              <Text style={[styles.avatarText, { color: colors.textInverse }]}>
                {item.displayName.charAt(0).toUpperCase()}
              </Text>
            </View>
            <View style={styles.presenceContainer}>
              <PresenceDot online={isOnline} size={10} />
            </View>
          </View>
          <View style={styles.friendInfo}>
            <Text
              style={[styles.friendName, { color: colors.textPrimary }]}
              numberOfLines={1}
            >
              {item.displayName}
            </Text>
            <Text
              style={[styles.friendUsername, { color: colors.textSecondary }]}
              numberOfLines={1}
            >
              @{item.username}
            </Text>
          </View>
          <Pressable
            style={[styles.chatButton, { backgroundColor: colors.primary }]}
            onPress={() => handleChat(item)}
          >
            <Ionicons name="chatbubble" size={16} color={colors.textInverse} />
          </Pressable>
        </Pressable>
      );
    },
    [colors, handleChat, handleRemoveFriend, presenceMap]
  );

  if (loading && friends.length === 0) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfaceSecondary }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (error && friends.length === 0) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfaceSecondary }]}>
        <Text style={[styles.errorText, { color: colors.accentRed }]}>{error}</Text>
        <Pressable style={[styles.retryButton, { backgroundColor: colors.primary }]} onPress={loadData}>
          <Text style={[styles.retryText, { color: colors.textInverse }]}>Retry</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}>
      <ConnectionBanner />
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <Text style={[styles.headerTitle, { color: colors.textPrimary }]}>Contacts</Text>
        <View style={styles.headerActions}>
          <Pressable
            style={[styles.headerButton, { backgroundColor: colors.surfaceTertiary }]}
            onPress={() => router.push('/(tabs)/contacts/requests')}
          >
            <Ionicons name="notifications" size={18} color={colors.textPrimary} />
            <Text style={[styles.headerButtonText, { color: colors.textPrimary }]}>Requests</Text>
          </Pressable>
          <Pressable
            style={[styles.addButton, { backgroundColor: colors.primary }]}
            onPress={() => router.push('/(tabs)/contacts/add')}
          >
            <Ionicons name="person-add" size={18} color={colors.textInverse} />
          </Pressable>
        </View>
      </View>
      <FlashList
        data={friends}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderItem}
        onRefresh={loadData}
        refreshing={loading}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Ionicons name="people-outline" size={48} color={colors.textSecondary} />
            <Text style={[styles.emptyTitle, { color: colors.textSecondary }]}>No friends yet</Text>
            <Text style={[styles.emptySubtitle, { color: colors.textSecondary }]}>
              Search for users to add!
            </Text>
            <Pressable
              style={[styles.addFriendButton, { backgroundColor: colors.primary }]}
              onPress={() => router.push('/(tabs)/contacts/add')}
            >
              <Text style={[styles.addFriendText, { color: colors.textInverse }]}>Add Friends</Text>
            </Pressable>
          </View>
        }
        contentContainerStyle={styles.listContent}
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
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.md,
    borderBottomWidth: 1,
  },
  headerTitle: {
    fontSize: SlackTypography.displayMd.fontSize,
    fontWeight: '600',
  },
  headerActions: {
    flexDirection: 'row',
    gap: SlackSpacing.sm,
  },
  headerButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: SlackSpacing.md,
    paddingVertical: SlackSpacing.xs,
    borderRadius: SlackBorderRadius.pill,
    gap: SlackSpacing.xs,
  },
  headerButtonText: {
    fontSize: SlackTypography.bodySm.fontSize,
    fontWeight: '500',
  },
  addButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  listContent: {
    flexGrow: 1,
  },
  friendRow: {
    flexDirection: 'row',
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.md,
    alignItems: 'center',
    borderBottomWidth: 1,
  },
  avatarContainer: {
    position: 'relative',
    marginRight: SlackSpacing.md,
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    fontSize: 20,
    fontWeight: '600',
  },
  presenceContainer: {
    position: 'absolute',
    bottom: 0,
    right: 0,
  },
  friendInfo: {
    flex: 1,
    marginRight: SlackSpacing.md,
  },
  friendName: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
  },
  friendUsername: {
    fontSize: SlackTypography.bodySm.fontSize,
  },
  chatButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyState: {
    padding: 40,
    alignItems: 'center',
  },
  emptyTitle: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
    marginTop: SlackSpacing.lg,
    marginBottom: SlackSpacing.sm,
  },
  emptySubtitle: {
    fontSize: SlackTypography.bodySm.fontSize,
    textAlign: 'center',
    marginBottom: SlackSpacing.lg,
  },
  addFriendButton: {
    borderRadius: SlackBorderRadius.pill,
    paddingHorizontal: SlackSpacing['2xl'],
    paddingVertical: SlackSpacing.md,
  },
  addFriendText: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
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
