import { useRouter, useLocalSearchParams } from 'expo-router';
import { Pressable, StyleSheet, View } from 'react-native';

import { Text } from '@/components/Themed';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function VerifyEmailScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const { status, message } = useLocalSearchParams<{ status: string; message: string }>();
  const isSuccess = status === 'success';

  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  return (
    <View style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}>
      <View style={styles.inner}>
        <Text style={[styles.icon, { color: isSuccess ? colors.accentGreen : colors.accentRed }]}>
          {isSuccess ? '✓' : '✗'}
        </Text>
        <Text style={[styles.title, { color: colors.textPrimary }]}>
          {isSuccess ? 'Email Verified' : 'Verification Failed'}
        </Text>
        {message && <Text style={[styles.message, { color: colors.textSecondary }]}>{message}</Text>}
        <Pressable
          style={[styles.button, { backgroundColor: colors.primary }]}
          onPress={() => router.replace('/(auth)/login')}
        >
          <Text style={[styles.buttonText, { color: colors.textInverse }]}>Continue to Login</Text>
        </Pressable>
      </View>
    </View>
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
    alignItems: 'center',
  },
  icon: {
    fontSize: 64,
    marginBottom: SlackSpacing.xl,
  },
  title: {
    fontSize: SlackTypography.displayLg.fontSize,
    fontWeight: SlackTypography.displayLg.fontWeight,
    lineHeight: SlackTypography.displayLg.lineHeight,
    fontFamily: SlackTypography.displayLg.fontFamily,
    textAlign: 'center',
  },
  message: {
    fontSize: SlackTypography.bodySm.fontSize,
    textAlign: 'center',
  },
  button: {
    backgroundColor: SlackColors.light.primary,
    borderRadius: SlackBorderRadius.pill,
    paddingVertical: SlackSpacing.lg,
    paddingHorizontal: SlackSpacing['2xl'],
    alignItems: 'center',
    marginTop: SlackSpacing.xl,
  },
  buttonText: {
    fontSize: SlackTypography.bodyMd.fontSize,
    fontWeight: '600',
  },
});
