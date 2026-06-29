import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, StyleSheet, TextInput, View } from 'react-native';
import { FlashList } from '@shopify/flash-list';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Text } from '@/components/Themed';
import PresenceDot from '@/src/components/PresenceDot';
import { useAuthStore } from '@/src/stores/authStore';
import { usePresenceStore } from '@/src/stores/presenceStore';
import { searchUsers } from '@/src/api/users';
import { sendFriendRequest } from '@/src/api/friends';
import { RelationshipStatus, UserSearchResult } from '@/src/types/domain';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function AddFriendScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
  const token = useAuthStore((s) => s.token);
  const presenceMap = usePresenceStore((s) => s.presenceMap);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<UserSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingIds, setPendingIds] = useState<Set<number>>(new Set());
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const doSearch = useCallback(async (q: string) => {
    if (!token || !q.trim()) {
      setResults([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await searchUsers(token, q.trim());
      setResults(data);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Search failed');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }
    debounceRef.current = setTimeout(() => {
      doSearch(query);
    }, 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query, doSearch]);

  const handleSendRequest = useCallback(async (recipientId: number) => {
    if (!token) return;
    setPendingIds((prev) => new Set(prev).add(recipientId));
    try {
      await sendFriendRequest(token, recipientId);
      setResults((prev) =>
        prev.map((r) =>
          r.user.id === recipientId
            ? { ...r, relationshipStatus: RelationshipStatus.PENDING_OUTGOING }
            : r
        )
      );
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      Alert.alert('Error', apiErr.message || 'Failed to send request');
    } finally {
      setPendingIds((prev) => {
        const next = new Set(prev);
        next.delete(recipientId);
        return next;
      });
    }
  }, [token]);

  const renderItem = useCallback(
    ({ item }: { item: UserSearchResult }) => {
      const isOnline = presenceMap[item.user.id] ?? false;
      const isPending = pendingIds.has(item.user.id);
      let actionLabel = '';
      let actionDisabled = false;
      let onAction: (() => void) | undefined;
      let badgeColor: string | undefined;
      let badgeTextColor = colors.textInverse;
      let isBadge = false;

      switch (item.relationshipStatus) {
        case RelationshipStatus.NONE:
          actionLabel = 'Add';
          badgeColor = colors.primary;
          onAction = () => handleSendRequest(item.user.id);
          break;
        case RelationshipStatus.PENDING_OUTGOING:
          actionLabel = 'Pending';
          badgeColor = colors.accentYellow;
          badgeTextColor = '#fff';
          actionDisabled = true;
          isBadge = true;
          break;
        case RelationshipStatus.PENDING_INCOMING:
          actionLabel = 'Accept';
          badgeColor = colors.accentGreen;
          onAction = () => router.push('/(tabs)/contacts/requests');
          break;
        case RelationshipStatus.FRIENDS:
          actionLabel = 'Friend';
          badgeColor = colors.accentGreen;
          badgeTextColor = '#fff';
          actionDisabled = true;
          isBadge = true;
          break;
      }

      return (
        <View style={[styles.resultRow, { borderBottomColor: colors.border }]}>
          <View style={styles.avatarContainer}>
            <View style={[styles.avatar, { backgroundColor: colors.primary }]}>
              <Text style={[styles.avatarText, { color: colors.textInverse }]}>
                {item.user.displayName.charAt(0).toUpperCase()}
              </Text>
            </View>
            <View style={styles.presenceContainer}>
              <PresenceDot online={isOnline} size={8} />
            </View>
          </View>
          <View style={styles.resultInfo}>
            <Text
              style={[styles.resultName, { color: colors.textPrimary }]}
              numberOfLines={1}
            >
              {item.user.displayName}
            </Text>
            <Text
              style={[styles.resultUsername, { color: colors.textSecondary }]}
              numberOfLines={1}
            >
              @{item.user.username}
            </Text>
          </View>
          <Pressable
            style={[
              styles.actionButton,
              { backgroundColor: badgeColor ?? (actionDisabled ? colors.surfaceTertiary : colors.primary) },
            ]}
            onPress={onAction}
            disabled={actionDisabled || isPending}
          >
            {isPending ? (
              <ActivityIndicator size="small" color={colors.textInverse} />
            ) : (
              <Text
                style={[
                  styles.actionText,
                  { color: isBadge ? badgeTextColor : colors.textInverse },
                ]}
              >
                {actionLabel}
              </Text>
            )}
          </Pressable>
        </View>
      );
    },
    [colors, handleSendRequest, pendingIds, presenceMap, router]
  );

  return (
    <View style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}>
      <View style={styles.searchContainer}>
        <View
          style={[
            styles.searchInputWrapper,
            { backgroundColor: colors.surfaceTertiary, borderColor: colors.border },
          ]}
        >
          <Ionicons name="search" size={18} color={colors.textSecondary} />
          <TextInput
            style={[styles.searchInput, { color: colors.textPrimary }]}
            placeholder="Search by name or username..."
            placeholderTextColor={colors.textSecondary}
            value={query}
            onChangeText={setQuery}
            autoCapitalize="none"
            autoCorrect={false}
            returnKeyType="search"
          />
        </View>
      </View>

      {loading && (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      )}

      {error && (
        <View style={styles.center}>
          <Text style={[styles.errorText, { color: colors.accentRed }]}>{error}</Text>
        </View>
      )}

      {!loading && query.trim().length > 0 && results.length === 0 && (
        <View style={styles.emptyState}>
          <Ionicons name="search-outline" size={48} color={colors.textSecondary} />
          <Text style={[styles.emptyTitle, { color: colors.textSecondary }]}>No users found</Text>
        </View>
      )}

      {!loading && query.trim().length === 0 && (
        <View style={styles.emptyState}>
          <Ionicons name="people-outline" size={48} color={colors.textSecondary} />
          <Text style={[styles.emptyTitle, { color: colors.textSecondary }]}>
            Search for users by name or username
          </Text>
        </View>
      )}

      <FlashList
        data={results}
        keyExtractor={(item) => item.user.id.toString()}
        renderItem={renderItem}
        contentContainerStyle={styles.listContent}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  searchContainer: {
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.md,
  },
  searchInputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: SlackBorderRadius.pill,
    paddingHorizontal: SlackSpacing.lg,
    gap: SlackSpacing.sm,
  },
  searchInput: {
    flex: 1,
    paddingVertical: SlackSpacing.md,
    fontSize: SlackTypography.bodyLg.fontSize,
  },
  center: {
    padding: SlackSpacing['2xl'],
    alignItems: 'center',
  },
  listContent: {
    flexGrow: 1,
  },
  resultRow: {
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
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    fontSize: 16,
    fontWeight: '600',
  },
  presenceContainer: {
    position: 'absolute',
    bottom: 0,
    right: 0,
  },
  resultInfo: {
    flex: 1,
    marginRight: SlackSpacing.md,
  },
  resultName: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
  },
  resultUsername: {
    fontSize: SlackTypography.bodySm.fontSize,
  },
  actionButton: {
    borderRadius: SlackBorderRadius.pill,
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.sm,
    minWidth: 72,
    alignItems: 'center',
  },
  actionText: {
    fontSize: SlackTypography.bodySm.fontSize,
    fontWeight: '600',
  },
  emptyState: {
    padding: 40,
    alignItems: 'center',
  },
  emptyTitle: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
    marginTop: SlackSpacing.lg,
    textAlign: 'center',
  },
  errorText: {
    fontSize: SlackTypography.bodyLg.fontSize,
    textAlign: 'center',
  },
});
