import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, StyleSheet, TextInput, View } from 'react-native';

import { Text } from '@/components/Themed';
import { useAuthStore } from '@/src/stores/authStore';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function RegisterScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const register = useAuthStore((s) => s.register);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  const handleRegister = async () => {
    if (!username.trim() || !email.trim() || !displayName.trim() || !password.trim()) {
      setError('All fields are required');
      return;
    }
    if (password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await register({
        username: username.trim(),
        email: email.trim(),
        displayName: displayName.trim(),
        password,
      });
      router.replace('/(auth)/login?registered=true');
    } catch (err: unknown) {
      const apiErr = err as { status?: number; message?: string };
      if (apiErr.status === 409) {
        setError('Username or email already exists');
      } else {
        setError(apiErr.message || 'Registration failed');
      }
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
        <Text style={[styles.title, { color: colors.textPrimary }]}>Create Account</Text>

        {error && <Text style={[styles.error, { color: colors.accentRed }]}>{error}</Text>}

        <TextInput
          style={[styles.input, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
          placeholder="Username"
          placeholderTextColor={colors.textSecondary}
          autoCapitalize="none"
          autoCorrect={false}
          value={username}
          onChangeText={setUsername}
          editable={!loading}
        />

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

        <TextInput
          style={[styles.input, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
          placeholder="Display Name"
          placeholderTextColor={colors.textSecondary}
          value={displayName}
          onChangeText={setDisplayName}
          editable={!loading}
        />

        <TextInput
          style={[styles.input, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
          placeholder="Password"
          placeholderTextColor={colors.textSecondary}
          secureTextEntry
          value={password}
          onChangeText={setPassword}
          editable={!loading}
        />

        <Pressable
          style={[styles.button, { backgroundColor: colors.primary }, loading && styles.buttonDisabled]}
          onPress={handleRegister}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator color={colors.textInverse} />
          ) : (
            <Text style={[styles.buttonText, { color: colors.textInverse }]}>Create Account</Text>
          )}
        </Pressable>

        <Pressable onPress={() => router.back()}>
          <Text style={[styles.link, { color: colors.accentBlue }]}>Already have an account? Log in</Text>
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
  link: {
    textAlign: 'center',
    fontSize: SlackTypography.bodySm.fontSize,
    marginTop: SlackSpacing.xs,
  },
  error: {
    textAlign: 'center',
    fontSize: SlackTypography.bodySm.fontSize,
  },
});
