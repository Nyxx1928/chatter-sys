import { useRouter, useLocalSearchParams } from 'expo-router';
import { Pressable, StyleSheet, View } from 'react-native';

import { Text } from '@/components/Themed';

export default function VerifyEmailScreen() {
  const router = useRouter();
  const { status, message } = useLocalSearchParams<{ status: string; message: string }>();
  const isSuccess = status === 'success';

  return (
    <View style={styles.container}>
      <View style={styles.inner}>
        <Text style={[styles.icon, isSuccess ? styles.successIcon : styles.errorIcon]}>
          {isSuccess ? '✓' : '✗'}
        </Text>
        <Text style={styles.title}>
          {isSuccess ? 'Email Verified' : 'Verification Failed'}
        </Text>
        {message && <Text style={styles.message}>{message}</Text>}
        <Pressable
          style={styles.button}
          onPress={() => router.replace('/(auth)/login')}
        >
          <Text style={styles.buttonText}>Continue to Login</Text>
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
    paddingHorizontal: 24,
    gap: 16,
    alignItems: 'center',
  },
  icon: {
    fontSize: 64,
    marginBottom: 16,
  },
  successIcon: {
    color: '#16a34a',
  },
  errorIcon: {
    color: '#dc2626',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  message: {
    fontSize: 14,
    textAlign: 'center',
    color: '#666',
  },
  button: {
    backgroundColor: '#2f95dc',
    borderRadius: 8,
    paddingVertical: 14,
    paddingHorizontal: 32,
    alignItems: 'center',
    marginTop: 16,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
