import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, StyleSheet, View } from 'react-native';
import { FlashList } from '@shopify/flash-list';
import { Ionicons } from '@expo/vector-icons';
import { Text } from '@/components/Themed';
import { useAuthStore } from '@/src/stores/authStore';
import {
  listFriendRequests,
  acceptFriendRequest,
  declineFriendRequest,
} from '@/src/api/friends';
import { FriendRequest, FriendRequestList } from '@/src/types/domain';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

type Section = {
  title: string;
  data: FriendRequest[];
  type: 'incoming' | 'outgoing';
};

export default function FriendRequestsScreen() {
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
  const token = useAuthStore((s) => s.token);
  const setPendingRequestCount = useAuthStore((s) => s.setPendingRequestCount);
  const [requests, setRequests] = useState<FriendRequestList>({ incoming: [], outgoing: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [acceptingIds, setAcceptingIds] = useState<Set<number>>(new Set());

  const loadRequests = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const data = await listFriendRequests(token);
      setRequests(data);
      setPendingRequestCount(data.incoming.length + data.outgoing.length);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to load requests');
    } finally {
      setLoading(false);
    }
  }, [token, setPendingRequestCount]);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  const handleAccept = useCallback(
    async (requestId: number) => {
      if (!token) return;
      setAcceptingIds((prev) => new Set(prev).add(requestId));
      try {
        await acceptFriendRequest(token, requestId);
        setRequests((prev) => ({
          ...prev,
          incoming: prev.incoming.filter((r) => r.id !== requestId),
        }));
        setPendingRequestCount(
          requests.incoming.filter((r) => r.id !== requestId).length +
            requests.outgoing.length
        );
      } catch (err: unknown) {
        const apiErr = err as { message?: string };
        Alert.alert('Error', apiErr.message || 'Failed to accept request');
      } finally {
        setAcceptingIds((prev) => {
          const next = new Set(prev);
          next.delete(requestId);
          return next;
        });
      }
    },
    [token, requests, setPendingRequestCount]
  );

  const handleDecline = useCallback(
    async (requestId: number) => {
      if (!token) return;
      try {
        await declineFriendRequest(token, requestId);
        setRequests((prev) => ({
          ...prev,
          incoming: prev.incoming.filter((r) => r.id !== requestId),
        }));
        setPendingRequestCount(
          requests.incoming.filter((r) => r.id !== requestId).length +
            requests.outgoing.length
        );
      } catch (err: unknown) {
        const apiErr = err as { message?: string };
        Alert.alert('Error', apiErr.message || 'Failed to decline request');
      }
    },
    [token, requests, setPendingRequestCount]
  );

  const sections: Section[] = [];
  if (requests.incoming.length > 0) {
    sections.push({ title: 'Incoming Requests', data: requests.incoming, type: 'incoming' });
  }
  if (requests.outgoing.length > 0) {
    sections.push({ title: 'Sent Requests', data: requests.outgoing, type: 'outgoing' });
  }

  const renderIncoming = useCallback(
    ({ item }: { item: FriendRequest }) => {
      const isAccepting = acceptingIds.has(item.id);
      return (
        <View style={[styles.requestRow, { borderBottomColor: colors.border }]}>
          <View style={[styles.avatar, { backgroundColor: colors.primary }]}>
            <Text style={[styles.avatarText, { color: colors.textInverse }]}>
              {item.requester.displayName.charAt(0).toUpperCase()}
            </Text>
          </View>
          <View style={styles.requestInfo}>
            <Text
              style={[styles.requestName, { color: colors.textPrimary }]}
              numberOfLines={1}
            >
              {item.requester.displayName}
            </Text>
            <Text
              style={[styles.requestUsername, { color: colors.textSecondary }]}
              numberOfLines={1}
            >
              @{item.requester.username}
            </Text>
          </View>
          <View style={styles.requestActions}>
            <Pressable
              style={[styles.acceptButton, { backgroundColor: colors.accentGreen }]}
              onPress={() => handleAccept(item.id)}
              disabled={isAccepting}
            >
              {isAccepting ? (
                <ActivityIndicator size="small" color="#fff" />
              ) : (
                <Text style={styles.acceptText}>Accept</Text>
              )}
            </Pressable>
            <Pressable
              style={[styles.declineButton, { backgroundColor: colors.surfaceTertiary }]}
              onPress={() => handleDecline(item.id)}
            >
              <Text style={[styles.declineText, { color: colors.textPrimary }]}>Decline</Text>
            </Pressable>
          </View>
        </View>
      );
    },
    [colors, handleAccept, handleDecline, acceptingIds]
  );

  const renderOutgoing = useCallback(
    ({ item }: { item: FriendRequest }) => {
      const recipientName =
        item.recipient.displayName || item.recipient.username;
      return (
        <View style={[styles.requestRow, { borderBottomColor: colors.border }]}>
          <View style={[styles.avatar, { backgroundColor: colors.surfaceTertiary }]}>
            <Text style={[styles.avatarText, { color: colors.textSecondary }]}>
              {recipientName.charAt(0).toUpperCase()}
            </Text>
          </View>
          <View style={styles.requestInfo}>
            <Text
              style={[styles.requestName, { color: colors.textPrimary }]}
              numberOfLines={1}
            >
              {recipientName}
            </Text>
            <Text
              style={[styles.requestUsername, { color: colors.textSecondary }]}
              numberOfLines={1}
            >
              @{item.recipient.username}
            </Text>
          </View>
          <View style={[styles.pendingBadge, { backgroundColor: colors.accentYellow }]}>
            <Text style={styles.pendingText}>Pending</Text>
          </View>
        </View>
      );
    },
    [colors]
  );

  if (loading) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfaceSecondary }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (error) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfaceSecondary }]}>
        <Text style={[styles.errorText, { color: colors.accentRed }]}>{error}</Text>
        <Pressable
          style={[styles.retryButton, { backgroundColor: colors.primary }]}
          onPress={loadRequests}
        >
          <Text style={[styles.retryText, { color: colors.textInverse }]}>Retry</Text>
        </Pressable>
      </View>
    );
  }

  if (sections.length === 0) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfaceSecondary }]}>
        <Ionicons name="checkmark-circle-outline" size={48} color={colors.textSecondary} />
        <Text style={[styles.emptyTitle, { color: colors.textSecondary }]}>
          No pending requests
        </Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}>
      {sections.map((section) => (
        <View key={section.type}>
          <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>
            {section.title}
          </Text>
          <FlashList
            data={section.data}
            keyExtractor={(item) => item.id.toString()}
            renderItem={section.type === 'incoming' ? renderIncoming : renderOutgoing}
            scrollEnabled={false}
          />
        </View>
      ))}
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
  sectionTitle: {
    fontSize: SlackTypography.caption.fontSize,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    paddingHorizontal: SlackSpacing.lg,
    paddingTop: SlackSpacing.lg,
    paddingBottom: SlackSpacing.sm,
  },
  requestRow: {
    flexDirection: 'row',
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.md,
    alignItems: 'center',
    borderBottomWidth: 1,
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: SlackSpacing.md,
  },
  avatarText: {
    fontSize: 16,
    fontWeight: '600',
  },
  requestInfo: {
    flex: 1,
    marginRight: SlackSpacing.md,
  },
  requestName: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
  },
  requestUsername: {
    fontSize: SlackTypography.bodySm.fontSize,
  },
  requestActions: {
    flexDirection: 'row',
    gap: SlackSpacing.sm,
  },
  acceptButton: {
    borderRadius: SlackBorderRadius.pill,
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.sm,
    minWidth: 72,
    alignItems: 'center',
  },
  acceptText: {
    color: '#fff',
    fontSize: SlackTypography.bodySm.fontSize,
    fontWeight: '600',
  },
  declineButton: {
    borderRadius: SlackBorderRadius.pill,
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.sm,
    minWidth: 72,
    alignItems: 'center',
  },
  declineText: {
    fontSize: SlackTypography.bodySm.fontSize,
    fontWeight: '600',
  },
  pendingBadge: {
    borderRadius: SlackBorderRadius.pill,
    paddingHorizontal: SlackSpacing.md,
    paddingVertical: SlackSpacing.xs,
  },
  pendingText: {
    color: '#fff',
    fontSize: SlackTypography.caption.fontSize,
    fontWeight: '600',
  },
  emptyTitle: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
    marginTop: SlackSpacing.lg,
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
