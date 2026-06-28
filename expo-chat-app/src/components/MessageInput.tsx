import { memo, useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, StyleSheet, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

type Props = {
  onSend: (content: string) => void;
  disabled?: boolean;
};

const MessageInput = memo(function MessageInput({ onSend, disabled }: Props) {
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
  const [text, setText] = useState('');

  const handleSend = () => {
    const trimmed = text.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setText('');
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
    >
      <View style={[styles.container, { borderTopColor: colors.border, backgroundColor: colors.surfacePrimary }]}>
        <TextInput
          style={[styles.input, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
          placeholder="Message..."
          placeholderTextColor={colors.textSecondary}
          value={text}
          onChangeText={setText}
          multiline
          maxLength={2000}
          editable={!disabled}
          accessibilityLabel="Message input"
          accessibilityRole="text"
        />
        <Pressable
          style={[styles.sendButton, { backgroundColor: (!text.trim() || disabled) ? colors.surfaceTertiary : colors.primary }]}
          onPress={handleSend}
          disabled={!text.trim() || disabled}
          accessibilityLabel="Send message"
          accessibilityRole="button"
        >
          <Ionicons
            name="send"
            size={20}
            color={!text.trim() || disabled ? colors.textSecondary : colors.textInverse}
          />
        </Pressable>
      </View>
    </KeyboardAvoidingView>
  );
});

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: SlackSpacing.md,
    paddingVertical: SlackSpacing.sm,
    borderTopWidth: 1,
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderRadius: SlackBorderRadius.pill,
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.sm,
    fontSize: SlackTypography.bodyLg.fontSize,
    maxHeight: 120,
    marginRight: SlackSpacing.sm,
  },
  sendButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
});

export default MessageInput;
