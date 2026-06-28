import { Pressable, StyleSheet, View } from 'react-native';
import { Text } from '@/components/Themed';
import PresenceDot from './PresenceDot';
import { ChatRoom } from '@/src/types/domain';
import { usePresenceStore } from '@/src/stores/presenceStore';
import { formatRelativeTime } from '@/src/utils/date';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

type Props = {
  room: ChatRoom;
  latestMessage?: string;
  latestMessageTimestamp?: string;
  onPress: () => void;
};

export default function RoomListItem({ room, latestMessage, latestMessageTimestamp, onPress }: Props) {
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
  const isOnline = usePresenceStore((s) =>
    room.otherParticipant ? s.isOnline(room.otherParticipant.id) : undefined
  );
  const displayName = room.otherParticipant?.displayName ?? room.name;
  const initial = displayName.charAt(0).toUpperCase();

  return (
    <Pressable style={[styles.container, { borderBottomColor: colors.border }]} onPress={onPress}>
      <View style={styles.avatarContainer}>
        <View style={[styles.avatar, { backgroundColor: colors.primary }]}>
          <Text style={[styles.avatarText, { color: colors.textInverse }]}>{initial}</Text>
        </View>
        {room.otherParticipant && (
          <View style={styles.presenceContainer}>
            <PresenceDot online={isOnline ?? false} size={10} />
          </View>
        )}
      </View>
      <View style={styles.content}>
        <View style={styles.topRow}>
          <Text style={[styles.name, { color: colors.textPrimary }]} numberOfLines={1}>
            {displayName}
          </Text>
          {latestMessageTimestamp && (
            <Text style={[styles.timestamp, { color: colors.textSecondary }]}>{formatRelativeTime(latestMessageTimestamp)}</Text>
          )}
        </View>
        {latestMessage && (
          <Text style={[styles.preview, { color: colors.textSecondary }]} numberOfLines={1}>
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
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
    flex: 1,
    marginRight: SlackSpacing.sm,
  },
  timestamp: {
    fontSize: SlackTypography.caption.fontSize,
  },
  preview: {
    fontSize: SlackTypography.bodySm.fontSize,
  },
});
