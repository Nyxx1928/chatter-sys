import { useCallback, useRef } from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import { Text } from '@/components/Themed';
import MessageBubble from './MessageBubble';
import { MessageWithStatus } from '@/src/stores/chatStore';
import { formatDateSeparator, isSameDay } from '@/src/utils/date';
import { useAuthStore } from '@/src/stores/authStore';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

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
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
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
        <Text style={[styles.dateText, { color: colors.textSecondary, backgroundColor: colors.surfaceTertiary }]}>{section.date}</Text>
      </View>
    ),
    [colors]
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
            <Text style={[styles.footerText, { color: colors.textSecondary }]}>
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
    paddingVertical: SlackSpacing.sm,
  },
  dateSeparator: {
    alignItems: 'center',
    paddingVertical: SlackSpacing.md,
  },
  dateText: {
    fontSize: SlackTypography.caption.fontSize,
    paddingHorizontal: SlackSpacing.md,
    paddingVertical: SlackSpacing.xs,
    borderRadius: SlackBorderRadius.pill,
    overflow: 'hidden',
  },
  footer: {
    alignItems: 'center',
    paddingVertical: SlackSpacing.md,
  },
  footerText: {
    fontSize: SlackTypography.caption.fontSize,
  },
});
