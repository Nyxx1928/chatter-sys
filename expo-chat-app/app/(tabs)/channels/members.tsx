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

export default function MemberListScreen() {
  const { roomId } = useLocalSearchParams<{ roomId: string }>();
  const router = useRouter();
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
        <View style={styles.memberRow}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>{initial}</Text>
          </View>
          <View style={styles.memberInfo}>
            <Text style={styles.memberName}>{item.displayName}</Text>
            <Text style={styles.memberUsername}>@{item.username}</Text>
          </View>
          <PresenceDot online={online ?? false} size={10} />
        </View>
      );
    },
    []
  );

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#2f95dc" />
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error}</Text>
        <Pressable style={styles.retryButton} onPress={loadMembers}>
          <Text style={styles.retryText}>Retry</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={styles.backButton}>
          <Text style={styles.backText}>Back</Text>
        </Pressable>
        <Text style={styles.headerTitle}>Members ({members.length})</Text>
        <View style={styles.headerRight} />
      </View>

      <FlashList
        data={members}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderMember}
        contentContainerStyle={styles.listContent}
      />

      <View style={styles.actions}>
        <Pressable style={styles.inviteButton}>
          <Text style={styles.inviteText}>Invite</Text>
        </Pressable>
        <Pressable
          style={[styles.deleteButton, deleting && styles.buttonDisabled]}
          onPress={handleDeleteChannel}
          disabled={deleting}
        >
          {deleting ? (
            <ActivityIndicator color="#fff" size="small" />
          ) : (
            <Text style={styles.deleteText}>Delete Channel</Text>
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
    padding: 24,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingTop: 56,
    paddingBottom: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backButton: {
    paddingRight: 12,
  },
  backText: {
    color: '#2f95dc',
    fontSize: 17,
  },
  headerTitle: {
    flex: 1,
    fontSize: 17,
    fontWeight: '600',
    textAlign: 'center',
  },
  headerRight: {
    width: 50,
  },
  listContent: {
    paddingVertical: 8,
  },
  memberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#2f95dc',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  avatarText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  memberInfo: {
    flex: 1,
  },
  memberName: {
    fontSize: 16,
    fontWeight: '500',
  },
  memberUsername: {
    fontSize: 13,
    color: '#9ca3af',
    marginTop: 1,
  },
  actions: {
    padding: 16,
    gap: 12,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
  },
  inviteButton: {
    borderWidth: 1,
    borderColor: '#2f95dc',
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  inviteText: {
    color: '#2f95dc',
    fontSize: 16,
    fontWeight: '600',
  },
  deleteButton: {
    backgroundColor: '#ef4444',
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  deleteText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
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
