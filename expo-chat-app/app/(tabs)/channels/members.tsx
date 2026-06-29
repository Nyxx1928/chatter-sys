import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, StyleSheet, View } from 'react-native';
import { FlashList } from '@shopify/flash-list';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Text } from '@/components/Themed';
import PresenceDot from '@/src/components/PresenceDot';
import { useAuthStore } from '@/src/stores/authStore';
import { usePresenceStore } from '@/src/stores/presenceStore';
import { getRoomMembers, deleteRoom } from '@/src/api/rooms';
import { User, MemberRole } from '@/src/types/domain';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function MemberListScreen() {
  const { roomId } = useLocalSearchParams<{ roomId: string }>();
  const router = useRouter();
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
  const roomIdNum = parseInt(roomId!, 10);
  const token = useAuthStore((s) => s.token);
  const currentUser = useAuthStore((s) => s.user);
  const [members, setMembers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const loadMembers = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const data = await getRoomMembers(token, roomIdNum);
      setMembers(data);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to load members');
    } finally {
      setLoading(false);
    }
  }, [token, roomIdNum]);

  useEffect(() => {
    loadMembers();
  }, [loadMembers]);

  const handleDeleteChannel = () => {
    Alert.alert(
      'Delete Channel',
      'Are you sure you want to delete this channel? This cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            if (!token) return;
            setDeleting(true);
            try {
              await deleteRoom(token, roomIdNum);
              router.replace('/(tabs)/channels');
            } catch (err: unknown) {
              const apiErr = err as { message?: string };
              Alert.alert('Error', apiErr.message || 'Failed to delete channel');
            } finally {
              setDeleting(false);
            }
          },
        },
      ]
    );
  };

  const isOwner = currentUser && members.find(
    (m) => m.id === currentUser.id
  );

  const renderMember = useCallback(
    ({ item }: { item: User }) => {
      const online = usePresenceStore.getState().isOnline(item.id);
      const initial = item.displayName.charAt(0).toUpperCase();
      return (
        <View style={[styles.memberRow, { borderBottomColor: colors.border }]}>
          <View style={[styles.avatar, { backgroundColor: colors.primary }]}>
            <Text style={[styles.avatarText, { color: colors.textInverse }]}>{initial}</Text>
          </View>
          <View style={styles.memberInfo}>
            <Text style={[styles.memberName, { color: colors.textPrimary }]}>{item.displayName}</Text>
            <Text style={[styles.memberUsername, { color: colors.textSecondary }]}>@{item.username}</Text>
          </View>
          <PresenceDot online={online ?? false} size={10} />
        </View>
      );
    },
    [colors]
  );

  if (loading) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfacePrimary }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  if (error) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfacePrimary }]}>
        <Text style={[styles.errorText, { color: colors.accentRed }]}>{error}</Text>
        <Pressable style={[styles.retryButton, { backgroundColor: colors.primary }]} onPress={loadMembers}>
          <Text style={[styles.retryText, { color: colors.textInverse }]}>Retry</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.surfacePrimary }]}>
      <View style={[styles.header, { backgroundColor: colors.surfacePrimary, borderBottomColor: colors.border }]}>
        <Pressable onPress={() => router.back()} style={styles.backButton}>
          <Text style={[styles.backText, { color: colors.primary }]}>Back</Text>
        </Pressable>
        <Text style={[styles.headerTitle, { color: colors.textPrimary }]}>Members ({members.length})</Text>
        <View style={styles.headerRight} />
      </View>

      <FlashList
        data={members}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderMember}
        contentContainerStyle={styles.listContent}
      />

      <View style={[styles.actions, { borderTopColor: colors.border }]}>
        <Pressable style={[styles.inviteButton, { borderColor: colors.primary }]}>
          <Text style={[styles.inviteText, { color: colors.primary }]}>Invite</Text>
        </Pressable>
        <Pressable
          style={[styles.deleteButton, { backgroundColor: colors.accentRed }, deleting && styles.buttonDisabled]}
          onPress={handleDeleteChannel}
          disabled={deleting}
        >
          {deleting ? (
            <ActivityIndicator color="#fff" size="small" />
          ) : (
            <Text style={[styles.deleteText, { color: colors.textInverse }]}>Delete Channel</Text>
          )}
        </Pressable>
      </View>
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
    alignItems: 'center',
    paddingHorizontal: SlackSpacing.lg,
    paddingTop: 56,
    paddingBottom: SlackSpacing.md,
    borderBottomWidth: 1,
  },
  backButton: {
    paddingRight: SlackSpacing.md,
  },
  backText: {
    fontSize: SlackTypography.bodyLg.fontSize,
  },
  headerTitle: {
    flex: 1,
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
    textAlign: 'center',
  },
  headerRight: {
    width: 50,
  },
  listContent: {
    paddingVertical: SlackSpacing.sm,
  },
  memberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.sm,
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
  memberInfo: {
    flex: 1,
  },
  memberName: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '500',
  },
  memberUsername: {
    fontSize: SlackTypography.bodySm.fontSize,
    marginTop: 1,
  },
  actions: {
    padding: SlackSpacing.lg,
    gap: SlackSpacing.md,
    borderTopWidth: 1,
  },
  inviteButton: {
    borderWidth: 1,
    borderRadius: SlackBorderRadius.pill,
    paddingVertical: SlackSpacing.md,
    alignItems: 'center',
  },
  inviteText: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
  },
  deleteButton: {
    borderRadius: SlackBorderRadius.pill,
    paddingVertical: SlackSpacing.md,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  deleteText: {
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
