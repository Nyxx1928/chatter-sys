import { useRouter, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, StyleSheet, TextInput, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';

import { Text } from '@/components/Themed';
import { useAuthStore } from '@/src/stores/authStore';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function LoginScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const { registered, emailSent } = useLocalSearchParams<{ registered?: string; emailSent?: string }>();
  const login = useAuthStore((s) => s.login);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  const handleLogin = async () => {
    if (!username.trim() || !password.trim()) {
      setError('Please enter username and password');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await login({ username: username.trim(), password });
      router.replace('/(tabs)/chats');
    } catch (err: unknown) {
      const apiErr = err as { status?: number; message?: string };
      if (apiErr.status === 401) {
        setError('Invalid username or password');
      } else {
        setError(apiErr.message || 'Login failed');
      }
    } finally {
      setLoading(false);
    }
  };

  const gradientColors: readonly [string, string] = colorScheme === 'dark'
    ? [colors.surfacePrimary, colors.surfacePrimary]
    : ['#F4EDE4', '#FFFFFF'];

  return (
    <LinearGradient colors={gradientColors} style={styles.container}>
      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <View style={styles.inner}>
          <Text style={[styles.title, { color: colors.textPrimary }]}>Chatter</Text>

          {registered === 'true' && (
            <Text style={[styles.successMessage, { color: colors.accentGreen }]}>Account created successfully. Please log in.</Text>
          )}
          {emailSent === 'true' && (
            <Text style={[styles.successMessage, { color: colors.accentGreen }]}>Password reset successfully. Please log in.</Text>
          )}

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
            placeholder="Password"
            placeholderTextColor={colors.textSecondary}
            secureTextEntry
            value={password}
            onChangeText={setPassword}
            editable={!loading}
          />

          <Pressable
            style={[styles.button, { backgroundColor: colors.primary }, loading && styles.buttonDisabled]}
            onPress={handleLogin}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color={colors.textInverse} />
            ) : (
              <Text style={[styles.buttonText, { color: colors.textInverse }]}>Log In</Text>
            )}
          </Pressable>

          <Pressable onPress={() => router.push('/(auth)/register')}>
            <Text style={[styles.link, { color: colors.accentBlue }]}>Create account</Text>
          </Pressable>

          <Pressable onPress={() => router.push('/(auth)/forgot-password')}>
            <Text style={[styles.link, { color: colors.accentBlue }]}>Forgot password?</Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </LinearGradient>
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
  successMessage: {
    textAlign: 'center',
    fontSize: SlackTypography.bodySm.fontSize,
  },
});
