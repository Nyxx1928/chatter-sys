import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Text } from '@/components/Themed';
import MessageList from '@/src/components/MessageList';
import MessageInput from '@/src/components/MessageInput';
import ConnectionBanner from '@/src/components/ConnectionBanner';
import { useChatStore, MessageWithStatus } from '@/src/stores/chatStore';
import { useAuthStore } from '@/src/stores/authStore';
import { useConnectionStore } from '@/src/stores/connectionStore';
import { useStompSubscription } from '@/src/stomp/hooks';
import { getMessageHistory } from '@/src/api/messages';
import { getRoomDetails, getRoomMembers } from '@/src/api/rooms';
import { ChatRoom, Message, MessageType, User } from '@/src/types/domain';
import { RoomMessagePayload, PresencePayload } from '@/src/types/stomp';

export default function ChannelChatScreen() {
  const { roomId } = useLocalSearchParams<{ roomId: string }>();
  const router = useRouter();
  const roomIdNum = parseInt(roomId!, 10);
  const token = useAuthStore((s) => s.token);
  const currentUser = useAuthStore((s) => s.user);
  const messages = useChatStore((s) => s.messages.get(roomIdNum.toString()) ?? []);
  const addMessage = useChatStore((s) => s.addMessage);
  const prependMessages = useChatStore((s) => s.prependMessages);
  const loadMessages = useChatStore((s) => s.loadMessages);
  const pagination = useChatStore((s) => s.pagination[roomIdNum.toString()]);
  const confirmMessage = useChatStore((s) => s.confirmMessage);
  const updateMessageStatus = useChatStore((s) => s.updateMessageStatus);
  const sendMsg = useConnectionStore((s) => s.sendMessage);
  const connected = useConnectionStore((s) => s.connected);
  const [room, setRoom] = useState<ChatRoom | null>(null);
  const [loading, setLoading] = useState(true);
  const [sendingHistory, setSendingHistory] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const tempIdCounter = useRef(0);

  const loadInitialData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const [roomData, history] = await Promise.all([
        getRoomDetails(token, roomIdNum),
        getMessageHistory(token, roomIdNum, { page: 0, size: 50 }),
      ]);
      setRoom(roomData);
      loadMessages(roomIdNum, history.content as MessageWithStatus[]);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to load messages');
    } finally {
      setLoading(false);
    }
  }, [token, roomIdNum, loadMessages]);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  const handleLoadMore = useCallback(async () => {
    if (!token) return;
    const page = pagination?.currentPage ?? 0;
    const hasMore = pagination?.hasMore ?? true;
    if (!hasMore || sendingHistory) return;
    setSendingHistory(true);
    try {
      const history = await getMessageHistory(token, roomIdNum, { page: page + 1, size: 50 });
      prependMessages(roomIdNum, history.content as MessageWithStatus[], page + 1, !history.last);
    } catch {
      // Silently fail pagination
    } finally {
      setSendingHistory(false);
    }
  }, [token, roomIdNum, pagination, sendingHistory, prependMessages]);

  useStompSubscription<RoomMessagePayload>(`/topic/room/${roomIdNum}`, (payload) => {
    const msg = payload as unknown as Message;
    addMessage(roomIdNum, { ...msg, _status: 'sent' });
  });

  useStompSubscription<PresencePayload>(`/topic/presence/${roomIdNum}`, () => {
    // Presence updates handled by presenceStore
  });

  const handleSend = useCallback(
    (content: string) => {
      if (!currentUser || !room) return;
      const tempId = --tempIdCounter.current;
      const optimistic: MessageWithStatus = {
        id: tempId,
        senderId: currentUser.id,
        senderUsername: currentUser.username,
        senderDisplayName: currentUser.displayName,
        chatRoomId: roomIdNum,
        content,
        timestamp: new Date().toISOString(),
        messageType: MessageType.TEXT,
        _status: 'sending',
      };
      addMessage(roomIdNum, optimistic);
      sendMsg(`/app/chat.send/${roomIdNum}`, { content });
    },
    [currentUser, room, roomIdNum, addMessage, sendMsg]
  );

  const handleRetry = useCallback(
    (msg: MessageWithStatus) => {
      if (msg._status !== 'failed') return;
      updateMessageStatus(roomIdNum, msg.id, 'sending');
      sendMsg(`/app/chat.send/${roomIdNum}`, { content: msg.content });
    },
    [roomIdNum, updateMessageStatus, sendMsg]
  );

  const handleInfo = () => {
    router.push(`/(tabs)/channels/members?roomId=${roomIdNum}`);
  };

  if (loading && room === null) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#2f95dc" />
      </View>
    );
  }

  if (error && room === null) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error}</Text>
        <Pressable style={styles.retryButton} onPress={loadInitialData}>
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
        <View style={styles.headerCenter}>
          <Text style={styles.headerTitle} numberOfLines={1}>{room?.name ?? 'Channel'}</Text>
          {room && (
            <Text style={styles.headerSubtitle}>{room.description}</Text>
          )}
        </View>
        <Pressable onPress={handleInfo} style={styles.infoButton}>
          <Ionicons name="information-circle-outline" size={24} color="#2f95dc" />
        </Pressable>
      </View>
      <ConnectionBanner />
      <View style={styles.chatArea}>
        <MessageList
          messages={messages}
          onEndReached={handleLoadMore}
          onRetry={handleRetry}
          ListEmptyComponent={
            <View style={styles.emptyMessages}>
              <Text style={styles.emptyText}>No messages yet. Say hello!</Text>
            </View>
          }
        />
      </View>
      <MessageInput onSend={handleSend} disabled={!connected} />
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
    paddingRight: 8,
  },
  backText: {
    color: '#2f95dc',
    fontSize: 17,
  },
  headerCenter: {
    flex: 1,
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 17,
    fontWeight: '600',
  },
  headerSubtitle: {
    fontSize: 12,
    color: '#9ca3af',
    marginTop: 1,
  },
  infoButton: {
    paddingLeft: 8,
  },
  chatArea: {
    flex: 1,
  },
  emptyMessages: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
  },
  emptyText: {
    fontSize: 16,
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
