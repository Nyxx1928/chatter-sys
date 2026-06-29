import { useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useRouter } from 'expo-router';
import { Text } from '@/components/Themed';
import { useAuthStore } from '@/src/stores/authStore';
import { createRoom } from '@/src/api/rooms';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function CreateChannelScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
  const token = useAuthStore((s) => s.token);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleCreate = async () => {
    if (!name.trim()) {
      setError('Channel name is required');
      return;
    }
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const room = await createRoom(token, { name: name.trim(), description: description.trim() || undefined });
      router.replace(`/(tabs)/channels/${room.id}`);
    } catch (err: unknown) {
      const apiErr = err as { message?: string; status?: number };
      if (apiErr.status === 409) {
        setError('Channel name already exists');
      } else {
        setError(apiErr.message || 'Failed to create channel');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={[styles.container, { backgroundColor: 'rgba(0,0,0,0.4)' }]}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={[styles.inner, { backgroundColor: colors.surfacePrimary }]}>
        <Text style={[styles.title, { color: colors.textPrimary }]}>Create Channel</Text>

        {error && <Text style={[styles.error, { color: colors.accentRed }]}>{error}</Text>}

        <TextInput
          style={[styles.input, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
          placeholder="Channel name"
          placeholderTextColor={colors.textSecondary}
          value={name}
          onChangeText={setName}
          editable={!loading}
          autoCapitalize="none"
        />

        <TextInput
          style={[styles.input, styles.descriptionInput, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
          placeholder="Description (optional)"
          placeholderTextColor={colors.textSecondary}
          value={description}
          onChangeText={setDescription}
          editable={!loading}
          multiline
          numberOfLines={3}
        />

        <Pressable
          style={[styles.button, { backgroundColor: colors.primary }, loading && styles.buttonDisabled]}
          onPress={handleCreate}
          disabled={loading || !name.trim()}
        >
          {loading ? (
            <ActivityIndicator color={colors.textInverse} />
          ) : (
            <Text style={[styles.buttonText, { color: colors.textInverse }]}>Create</Text>
          )}
        </Pressable>

        <Pressable onPress={() => router.back()} disabled={loading}>
          <Text style={[styles.cancel, { color: colors.textSecondary }]}>Cancel</Text>
        </Pressable>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
  },
  inner: {
    marginHorizontal: SlackSpacing['2xl'],
    borderRadius: SlackBorderRadius.lg,
    padding: SlackSpacing['2xl'],
    gap: SlackSpacing.lg,
  },
  title: {
    fontSize: SlackTypography.displayMd.fontSize,
    fontWeight: SlackTypography.displayMd.fontWeight,
    fontFamily: SlackTypography.displayMd.fontFamily,
    textAlign: 'center',
    marginBottom: SlackSpacing.sm,
  },
  input: {
    borderWidth: 1,
    borderRadius: SlackBorderRadius.md,
    paddingHorizontal: SlackSpacing.md,
    paddingVertical: SlackSpacing.md,
    fontSize: SlackTypography.bodyLg.fontSize,
  },
  descriptionInput: {
    minHeight: 80,
    textAlignVertical: 'top',
  },
  button: {
    borderRadius: SlackBorderRadius.pill,
    paddingVertical: SlackSpacing.lg,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
  },
  cancel: {
    textAlign: 'center',
    fontSize: SlackTypography.bodySm.fontSize,
  },
  error: {
    textAlign: 'center',
    fontSize: SlackTypography.bodySm.fontSize,
  },
});
