import { useRouter, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, StyleSheet, TextInput, View } from 'react-native';

import { Text } from '@/components/Themed';
import { useAuthStore } from '@/src/stores/authStore';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function ResetPasswordScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const { token } = useLocalSearchParams<{ token: string }>();
  const resetPassword = useAuthStore((s) => s.resetPassword);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  const handleReset = async () => {
    if (!newPassword || !confirmPassword) {
      setError('Please fill in both fields');
      return;
    }
    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    if (!token) {
      setError('Invalid reset link');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await resetPassword({ token, newPassword });
      router.replace('/(auth)/login?emailSent=true');
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to reset password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.inner}>
        <Text style={[styles.title, { color: colors.textPrimary }]}>Reset Password</Text>

        {error && <Text style={[styles.error, { color: colors.accentRed }]}>{error}</Text>}

        <TextInput
          style={[styles.input, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
          placeholder="New Password"
          placeholderTextColor={colors.textSecondary}
          secureTextEntry
          value={newPassword}
          onChangeText={setNewPassword}
          editable={!loading}
        />

        <TextInput
          style={[styles.input, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
          placeholder="Confirm New Password"
          placeholderTextColor={colors.textSecondary}
          secureTextEntry
          value={confirmPassword}
          onChangeText={setConfirmPassword}
          editable={!loading}
        />

        <Pressable
          style={[styles.button, { backgroundColor: colors.primary }, loading && styles.buttonDisabled]}
          onPress={handleReset}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator color={colors.textInverse} />
          ) : (
            <Text style={[styles.buttonText, { color: colors.textInverse }]}>Reset Password</Text>
          )}
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
    paddingHorizontal: SlackSpacing['2xl'],
    gap: SlackSpacing.lg,
  },
  title: {
    fontSize: SlackTypography.displayXl.fontSize,
    fontWeight: SlackTypography.displayXl.fontWeight,
    lineHeight: SlackTypography.displayXl.lineHeight,
    fontFamily: SlackTypography.displayXl.fontFamily,
    textAlign: 'center',
    marginBottom: SlackSpacing.xl,
  },
  input: {
    borderWidth: 1,
    borderRadius: SlackBorderRadius.md,
    paddingHorizontal: SlackSpacing.lg,
    paddingVertical: SlackSpacing.md,
    fontSize: SlackTypography.bodyMd.fontSize,
  },
  button: {
    backgroundColor: SlackColors.light.primary,
    borderRadius: SlackBorderRadius.pill,
    paddingVertical: SlackSpacing.lg,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    fontSize: SlackTypography.bodyMd.fontSize,
    fontWeight: '600',
  },
  error: {
    textAlign: 'center',
    fontSize: SlackTypography.bodySm.fontSize,
  },
});
