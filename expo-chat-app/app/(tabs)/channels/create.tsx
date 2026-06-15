import { useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useRouter } from 'expo-router';
import { Text } from '@/components/Themed';
import { useAuthStore } from '@/src/stores/authStore';
import { createRoom } from '@/src/api/rooms';

export default function CreateChannelScreen() {
  const router = useRouter();
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
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.inner}>
        <Text style={styles.title}>Create Channel</Text>

        {error && <Text style={styles.error}>{error}</Text>}

        <TextInput
          style={styles.input}
          placeholder="Channel name"
          placeholderTextColor="#9ca3af"
          value={name}
          onChangeText={setName}
          editable={!loading}
          autoCapitalize="none"
        />

        <TextInput
          style={[styles.input, styles.descriptionInput]}
          placeholder="Description (optional)"
          placeholderTextColor="#9ca3af"
          value={description}
          onChangeText={setDescription}
          editable={!loading}
          multiline
          numberOfLines={3}
        />

        <Pressable
          style={[styles.button, loading && styles.buttonDisabled]}
          onPress={handleCreate}
          disabled={loading || !name.trim()}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.buttonText}>Create</Text>
          )}
        </Pressable>

        <Pressable onPress={() => router.back()} disabled={loading}>
          <Text style={styles.cancel}>Cancel</Text>
        </Pressable>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.4)',
  },
  inner: {
    backgroundColor: '#fff',
    marginHorizontal: 24,
    borderRadius: 16,
    padding: 24,
    gap: 16,
  },
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
    color: '#1f2937',
  },
  descriptionInput: {
    minHeight: 80,
    textAlignVertical: 'top',
  },
  button: {
    backgroundColor: '#2f95dc',
    borderRadius: 8,
    paddingVertical: 14,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  cancel: {
    color: '#6b7280',
    textAlign: 'center',
    fontSize: 14,
  },
  error: {
    color: '#dc2626',
    textAlign: 'center',
    fontSize: 14,
  },
});
