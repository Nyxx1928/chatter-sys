import { useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, StyleSheet, TextInput, View } from 'react-native';

import { Text } from '@/components/Themed';
import { useAuthStore } from '@/src/stores/authStore';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function ForgotPasswordScreen() {
  const colorScheme = useColorScheme();
  const forgotPassword = useAuthStore((s) => s.forgotPassword);
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  const handleSubmit = async () => {
    if (!email.trim()) {
      setError('Please enter your email');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await forgotPassword({ email: email.trim() });
      setSent(true);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr.message || 'Failed to send reset email');
    } finally {
      setLoading(false);
    }
  };

  if (sent) {
    return (
      <View style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}>
        <View style={styles.inner}>
          <Text style={[styles.title, { color: colors.textPrimary }]}>Check Your Email</Text>
          <Text style={[styles.message, { color: colors.textSecondary }]}>Check your email for the password reset link.</Text>
        </View>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.inner}>
        <Text style={[styles.title, { color: colors.textPrimary }]}>Forgot Password</Text>
        <Text style={[styles.message, { color: colors.textSecondary }]}>Enter your email address and we'll send you a reset link.</Text>

        {error && <Text style={[styles.error, { color: colors.accentRed }]}>{error}</Text>}

        <TextInput
          style={[styles.input, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
          placeholder="Email"
          placeholderTextColor={colors.textSecondary}
          autoCapitalize="none"
          keyboardType="email-address"
          value={email}
          onChangeText={setEmail}
          editable={!loading}
        />

        <Pressable
          style={[styles.button, { backgroundColor: colors.primary }, loading && styles.buttonDisabled]}
          onPress={handleSubmit}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator color={colors.textInverse} />
          ) : (
            <Text style={[styles.buttonText, { color: colors.textInverse }]}>Send Reset Link</Text>
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
  message: {
    fontSize: SlackTypography.bodySm.fontSize,
    textAlign: 'center',
    marginBottom: SlackSpacing.sm,
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
