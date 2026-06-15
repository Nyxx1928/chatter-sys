import { Pressable, StyleSheet, View } from 'react-native';
import { Text } from '@/components/Themed';
import PresenceDot from './PresenceDot';
import { ChatRoom } from '@/src/types/domain';
import { usePresenceStore } from '@/src/stores/presenceStore';
import { formatRelativeTime } from '@/src/utils/date';

type Props = {
  room: ChatRoom;
  latestMessage?: string;
  latestMessageTimestamp?: string;
  onPress: () => void;
};

export default function RoomListItem({ room, latestMessage, latestMessageTimestamp, onPress }: Props) {
  const isOnline = usePresenceStore((s) =>
    room.otherParticipant ? s.isOnline(room.otherParticipant.id) : undefined
  );
  const displayName = room.otherParticipant?.displayName ?? room.name;
  const initial = displayName.charAt(0).toUpperCase();

  return (
    <Pressable style={styles.container} onPress={onPress}>
      <View style={styles.avatarContainer}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>{initial}</Text>
        </View>
        {room.otherParticipant && (
          <View style={styles.presenceContainer}>
            <PresenceDot online={isOnline ?? false} size={10} />
          </View>
        )}
      </View>
      <View style={styles.content}>
        <View style={styles.topRow}>
          <Text style={styles.name} numberOfLines={1}>
            {displayName}
          </Text>
          {latestMessageTimestamp && (
            <Text style={styles.timestamp}>{formatRelativeTime(latestMessageTimestamp)}</Text>
          )}
        </View>
        {latestMessage && (
          <Text style={styles.preview} numberOfLines={1}>
            {latestMessage}
          </Text>
        )}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 12,
    alignItems: 'center',
  },
  avatarContainer: {
    position: 'relative',
    marginRight: 12,
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#2f95dc',
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    color: '#fff',
    fontSize: 20,
    fontWeight: '600',
  },
  presenceContainer: {
    position: 'absolute',
    bottom: 0,
    right: 0,
  },
  content: {
    flex: 1,
  },
  topRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  name: {
    fontSize: 16,
    fontWeight: '600',
    flex: 1,
    marginRight: 8,
  },
  timestamp: {
    fontSize: 12,
    color: '#9ca3af',
  },
  preview: {
    fontSize: 14,
    color: '#6b7280',
  },
});
