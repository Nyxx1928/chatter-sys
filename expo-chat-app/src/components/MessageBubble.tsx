import { memo } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { Text } from '@/components/Themed';
import { MessageType } from '@/src/types/domain';
import { MessageWithStatus } from '@/src/stores/chatStore';
import { formatMessageTime } from '@/src/utils/date';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

type Props = {
  message: MessageWithStatus;
  isOwn: boolean;
  onRetry?: () => void;
};

const styles = StyleSheet.create({
  systemContainer: {
    alignItems: 'center',
    paddingVertical: SlackSpacing.sm,
    paddingHorizontal: SlackSpacing.lg,
  },
  systemText: {
    fontSize: SlackTypography.caption.fontSize,
    fontStyle: 'italic',
    textAlign: 'center',
  },
  bubbleContainer: {
    marginHorizontal: SlackSpacing.lg,
    marginVertical: 2,
    maxWidth: '80%',
  },
  ownBubbleContainer: {
    alignSelf: 'flex-end',
  },
  otherBubbleContainer: {
    alignSelf: 'flex-start',
  },
  sending: {
    opacity: 0.6,
  },
  bubble: {
    borderRadius: SlackBorderRadius.lg,
    paddingHorizontal: 14,
    paddingVertical: SlackSpacing.sm,
  },
  ownBubble: {
    borderBottomRightRadius: SlackBorderRadius.sm,
  },
  otherBubble: {
    borderBottomLeftRadius: SlackBorderRadius.sm,
  },
  messageText: {
    fontSize: SlackTypography.bodyLg.fontSize,
    lineHeight: SlackTypography.bodyLg.lineHeight,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    marginTop: 2,
    gap: 4,
  },
  time: {
    fontSize: SlackTypography.caption.fontSize,
  },
  failedIcon: {
    fontSize: 14,
    fontWeight: 'bold',
  },
});

const MessageBubble = memo<Props>(function MessageBubble({ message, isOwn, onRetry }) {
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  if (message.messageType === MessageType.SYSTEM || message.messageType === MessageType.JOIN || message.messageType === MessageType.LEAVE) {
    return (
      <View style={styles.systemContainer}>
        <Text style={[styles.systemText, { color: colors.textSecondary }]}>{message.content}</Text>
      </View>
    );
  }

  const isFailed = message._status === 'failed';
  const isSending = message._status === 'sending';

  const retryLabel = isFailed ? 'Tap to retry sending message' : undefined;

  return (
    <Pressable
      style={[
        styles.bubbleContainer,
        isOwn ? styles.ownBubbleContainer : styles.otherBubbleContainer,
        isSending && styles.sending,
      ]}
      onPress={isFailed ? onRetry : undefined}
      accessibilityLabel={retryLabel}
      accessibilityRole={isFailed ? 'button' : 'text'}
    >
      <View
        style={[
          styles.bubble,
          isOwn ? [styles.ownBubble, { backgroundColor: colors.primary }] : [styles.otherBubble, { backgroundColor: colors.surfaceTertiary }],
        ]}
      >
        <Text style={[styles.messageText, { color: isOwn ? colors.textInverse : colors.textPrimary }]}>
          {message.content}
        </Text>
        <View style={styles.metaRow}>
          {isFailed && <Text style={[styles.failedIcon, { color: colors.accentRed }]}>!</Text>}
          <Text style={[styles.time, { color: isOwn ? 'rgba(255,255,255,0.7)' : colors.textSecondary }]}>
            {formatMessageTime(message.timestamp)}
          </Text>
        </View>
      </View>
    </Pressable>
  );
});
export default MessageBubble;
