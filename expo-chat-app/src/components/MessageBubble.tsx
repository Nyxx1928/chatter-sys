import { Pressable, StyleSheet, View } from 'react-native';
import { Text } from '@/components/Themed';
import { MessageType } from '@/src/types/domain';
import { MessageWithStatus } from '@/src/stores/chatStore';
import { formatMessageTime } from '@/src/utils/date';

type Props = {
  message: MessageWithStatus;
  isOwn: boolean;
  onRetry?: () => void;
};

export default function MessageBubble({ message, isOwn, onRetry }: Props) {
  if (message.messageType === MessageType.SYSTEM || message.messageType === MessageType.JOIN || message.messageType === MessageType.LEAVE) {
    return (
      <View style={styles.systemContainer}>
        <Text style={styles.systemText}>{message.content}</Text>
      </View>
    );
  }

  const isFailed = message._status === 'failed';
  const isSending = message._status === 'sending';

  return (
    <Pressable
      style={[
        styles.bubbleContainer,
        isOwn ? styles.ownBubbleContainer : styles.otherBubbleContainer,
        isSending && styles.sending,
      ]}
      onPress={isFailed ? onRetry : undefined}
    >
      <View
        style={[
          styles.bubble,
          isOwn ? styles.ownBubble : styles.otherBubble,
        ]}
      >
        <Text style={[styles.messageText, isOwn ? styles.ownText : styles.otherText]}>
          {message.content}
        </Text>
        <View style={styles.metaRow}>
          {isFailed && <Text style={styles.failedIcon}>!</Text>}
          <Text style={[styles.time, isOwn ? styles.ownTime : styles.otherTime]}>
            {formatMessageTime(message.timestamp)}
          </Text>
        </View>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  systemContainer: {
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 16,
  },
  systemText: {
    fontSize: 12,
    fontStyle: 'italic',
    color: '#9ca3af',
    textAlign: 'center',
  },
  bubbleContainer: {
    marginHorizontal: 16,
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
    borderRadius: 16,
    paddingHorizontal: 14,
    paddingVertical: 8,
  },
  ownBubble: {
    backgroundColor: '#2f95dc',
    borderBottomRightRadius: 4,
  },
  otherBubble: {
    backgroundColor: '#e5e7eb',
    borderBottomLeftRadius: 4,
  },
  messageText: {
    fontSize: 16,
    lineHeight: 22,
  },
  ownText: {
    color: '#fff',
  },
  otherText: {
    color: '#1f2937',
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    marginTop: 2,
    gap: 4,
  },
  time: {
    fontSize: 11,
  },
  ownTime: {
    color: 'rgba(255,255,255,0.7)',
  },
  otherTime: {
    color: '#9ca3af',
  },
  failedIcon: {
    color: '#ef4444',
    fontSize: 14,
    fontWeight: 'bold',
  },
});
