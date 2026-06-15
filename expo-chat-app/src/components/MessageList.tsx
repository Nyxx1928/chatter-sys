import { useCallback, useRef } from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import { Text } from '@/components/Themed';
import MessageBubble from './MessageBubble';
import { MessageWithStatus } from '@/src/stores/chatStore';
import { formatDateSeparator, isSameDay } from '@/src/utils/date';
import { useAuthStore } from '@/src/stores/authStore';

type Props = {
  messages: MessageWithStatus[];
  onEndReached?: () => void;
  onRetry?: (message: MessageWithStatus) => void;
  ListEmptyComponent?: React.ReactElement;
};

type Section = {
  date: string;
  data: MessageWithStatus[];
};

const groupMessagesByDate = (messages: MessageWithStatus[]): Section[] => {
  const sections: Section[] = [];
  let currentDate = '';
  let currentSection: MessageWithStatus[] = [];

  for (const msg of messages) {
    const dateLabel = formatDateSeparator(msg.timestamp);
    if (dateLabel !== currentDate && currentSection.length > 0) {
      sections.push({ date: currentDate, data: currentSection });
      currentSection = [];
    }
    currentDate = dateLabel;
    currentSection.push(msg);
  }
  if (currentSection.length > 0) {
    sections.push({ date: currentDate, data: currentSection });
  }
  return sections;
};

export default function MessageList({ messages, onEndReached, onRetry, ListEmptyComponent }: Props) {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const listRef = useRef<FlatList>(null);
  const sections = groupMessagesByDate(messages);

  const renderMessage = useCallback(
    ({ item }: { item: MessageWithStatus }) => (
      <MessageBubble
        message={item}
        isOwn={item.senderId === currentUserId}
        onRetry={() => onRetry?.(item)}
      />
    ),
    [currentUserId, onRetry]
  );

  const renderSectionHeader = useCallback(
    ({ section }: { section: Section }) => (
      <View style={styles.dateSeparator}>
        <Text style={styles.dateText}>{section.date}</Text>
      </View>
    ),
    []
  );

  if (messages.length === 0 && ListEmptyComponent) {
    return ListEmptyComponent;
  }

  return (
    <FlatList
      ref={listRef}
      data={messages}
      keyExtractor={(item, index) => `${item.id}-${index}`}
      renderItem={renderMessage}
      onEndReached={onEndReached}
      onEndReachedThreshold={0.3}
      inverted
      contentContainerStyle={styles.listContent}
      ListFooterComponent={
        messages.length > 0 ? (
          <View style={styles.footer}>
            <Text style={styles.footerText}>
              {formatDateSeparator(messages[messages.length - 1].timestamp)}
            </Text>
          </View>
        ) : null
      }
    />
  );
}

const styles = StyleSheet.create({
  listContent: {
    paddingVertical: 8,
  },
  dateSeparator: {
    alignItems: 'center',
    paddingVertical: 12,
  },
  dateText: {
    fontSize: 12,
    color: '#9ca3af',
    backgroundColor: '#f3f4f6',
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 10,
    overflow: 'hidden',
  },
  footer: {
    alignItems: 'center',
    paddingVertical: 12,
  },
  footerText: {
    fontSize: 12,
    color: '#9ca3af',
  },
});
