import { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  TextInput,
  View,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Text } from '@/components/Themed';
import { useAuthStore } from '@/src/stores/authStore';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

const APP_VERSION = '1.0.0';

export default function ProfileScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;
  const user = useAuthStore((s) => s.user);
  const updateProfile = useAuthStore((s) => s.updateProfile);
  const logout = useAuthStore((s) => s.logout);
  const deleteAccount = useAuthStore((s) => s.deleteAccount);

  const [displayName, setDisplayName] = useState(user?.displayName ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [deleteStep, setDeleteStep] = useState<'initial' | 'confirm' | 'typing'>('initial');
  const [deleteConfirmUsername, setDeleteConfirmUsername] = useState('');
  const [deleting, setDeleting] = useState(false);

  const handleSave = async () => {
    if (!displayName.trim()) {
      setSaveError('Display name cannot be empty');
      return;
    }
    setSaving(true);
    setSaveError(null);
    try {
      await updateProfile({ displayName: displayName.trim(), email: email.trim() || undefined });
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setSaveError(apiErr.message || 'Failed to save changes');
    } finally {
      setSaving(false);
    }
  };

  const handleLogout = () => {
    Alert.alert('Logout', 'Are you sure you want to log out?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Logout',
        style: 'destructive',
        onPress: async () => {
          await logout();
          router.replace('/(auth)/login');
        },
      },
    ]);
  };

  const handleDeleteStart = () => {
    Alert.alert(
      'Delete Account',
      'Are you sure? This cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Continue',
          style: 'destructive',
          onPress: () => setDeleteStep('typing'),
        },
      ]
    );
  };

  const handleDeleteConfirm = async () => {
    if (deleteConfirmUsername !== user?.username) return;
    setDeleting(true);
    try {
      await deleteAccount();
      router.replace('/(auth)/login');
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      Alert.alert('Error', apiErr.message || 'Failed to delete account');
    } finally {
      setDeleting(false);
    }
  };

  if (!user) {
    return (
      <View style={[styles.center, { backgroundColor: colors.surfaceSecondary }]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.avatarSection}>
          <View style={[styles.avatar, { backgroundColor: colors.primary }]}>
            <Text style={[styles.avatarText, { color: colors.textInverse }]}>
              {user.displayName.charAt(0).toUpperCase()}
            </Text>
          </View>
          <Text style={[styles.displayName, { color: colors.textPrimary }]}>
            {user.displayName}
          </Text>
          <Text style={[styles.username, { color: colors.textSecondary }]}>
            @{user.username}
          </Text>
        </View>

        <View style={[styles.card, { backgroundColor: colors.surfacePrimary, borderColor: colors.border }]}>
          <Text style={[styles.fieldLabel, { color: colors.textSecondary }]}>Display Name</Text>
          <TextInput
            style={[styles.textInput, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
            value={displayName}
            onChangeText={setDisplayName}
            placeholder="Your display name"
            placeholderTextColor={colors.textSecondary}
            autoCapitalize="words"
          />

          <Text style={[styles.fieldLabel, { color: colors.textSecondary }]}>Email</Text>
          <TextInput
            style={[styles.textInput, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
            value={email}
            onChangeText={setEmail}
            placeholder="your@email.com"
            placeholderTextColor={colors.textSecondary}
            keyboardType="email-address"
            autoCapitalize="none"
          />

          {saveError && (
            <Text style={[styles.errorText, { color: colors.accentRed }]}>{saveError}</Text>
          )}

          <Pressable
            style={[styles.saveButton, { backgroundColor: colors.primary }]}
            onPress={handleSave}
            disabled={saving}
          >
            {saving ? (
              <ActivityIndicator size="small" color={colors.textInverse} />
            ) : (
              <Text style={[styles.saveButtonText, { color: colors.textInverse }]}>Save Changes</Text>
            )}
          </Pressable>
        </View>

        <View style={[styles.card, { backgroundColor: colors.surfacePrimary, borderColor: colors.border }]}>
          <Pressable
            style={[styles.logoutButton, { backgroundColor: colors.accentRed }]}
            onPress={handleLogout}
          >
            <Text style={styles.logoutButtonText}>Logout</Text>
          </Pressable>

          <View style={[styles.divider, { backgroundColor: colors.border }]} />

          {deleteStep === 'initial' && (
            <Pressable style={styles.actionRow} onPress={handleDeleteStart}>
              <Text style={[styles.actionText, { color: colors.accentRed }]}>Delete Account</Text>
            </Pressable>
          )}

          {deleteStep === 'typing' && (
            <View style={styles.deleteConfirmSection}>
              <Text style={[styles.deleteLabel, { color: colors.textSecondary }]}>
                Type your username to confirm: <Text style={{ fontWeight: '700', color: colors.textPrimary }}>{user.username}</Text>
              </Text>
              <TextInput
                style={[styles.textInput, { backgroundColor: colors.surfaceTertiary, color: colors.textPrimary, borderColor: colors.border }]}
                value={deleteConfirmUsername}
                onChangeText={setDeleteConfirmUsername}
                placeholder={user.username}
                placeholderTextColor={colors.textSecondary}
                autoCapitalize="none"
                autoCorrect={false}
              />
              <Pressable
                style={[
                  styles.deleteButton,
                  { backgroundColor: deleteConfirmUsername === user.username ? colors.accentRed : colors.surfaceTertiary },
                ]}
                onPress={handleDeleteConfirm}
                disabled={deleteConfirmUsername !== user.username || deleting}
              >
                {deleting ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text
                    style={[
                      styles.deleteButtonText,
                      { color: deleteConfirmUsername === user.username ? '#fff' : colors.textSecondary },
                    ]}
                  >
                    Delete My Account
                  </Text>
                )}
              </Pressable>
              <Pressable
                style={styles.cancelDelete}
                onPress={() => {
                  setDeleteStep('initial');
                  setDeleteConfirmUsername('');
                }}
              >
                <Text style={[styles.cancelDeleteText, { color: colors.textSecondary }]}>Cancel</Text>
              </Pressable>
            </View>
          )}
        </View>

        <Text style={[styles.versionText, { color: colors.textSecondary }]}>
          Chatter v{APP_VERSION}
        </Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: SlackSpacing['2xl'],
  },
  scrollContent: {
    padding: SlackSpacing.lg,
    paddingBottom: SlackSpacing['3xl'],
  },
  avatarSection: {
    alignItems: 'center',
    paddingVertical: SlackSpacing['2xl'],
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: SlackSpacing.md,
  },
  avatarText: {
    fontSize: 32,
    fontWeight: '700',
  },
  displayName: {
    fontSize: SlackTypography.displayMd.fontSize,
    fontWeight: '600',
    marginBottom: SlackSpacing.xs,
  },
  username: {
    fontSize: SlackTypography.bodyLg.fontSize,
  },
  card: {
    borderRadius: SlackBorderRadius.lg,
    padding: SlackSpacing.lg,
    borderWidth: 1,
    marginBottom: SlackSpacing.lg,
  },
  fieldLabel: {
    fontSize: SlackTypography.caption.fontSize,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: SlackSpacing.sm,
    marginTop: SlackSpacing.md,
  },
  textInput: {
    borderWidth: 1,
    borderRadius: SlackBorderRadius.md,
    paddingHorizontal: SlackSpacing.md,
    paddingVertical: SlackSpacing.md,
    fontSize: SlackTypography.bodyLg.fontSize,
    marginBottom: SlackSpacing.sm,
  },
  errorText: {
    fontSize: SlackTypography.bodySm.fontSize,
    marginBottom: SlackSpacing.sm,
  },
  saveButton: {
    borderRadius: SlackBorderRadius.pill,
    paddingVertical: SlackSpacing.md,
    alignItems: 'center',
    marginTop: SlackSpacing.sm,
  },
  saveButtonText: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
  },
  logoutButton: {
    borderRadius: SlackBorderRadius.pill,
    paddingVertical: SlackSpacing.md,
    alignItems: 'center',
    marginBottom: SlackSpacing.md,
  },
  logoutButtonText: {
    color: '#fff',
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
  },
  actionRow: {
    paddingVertical: SlackSpacing.md,
  },
  actionText: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
  },
  divider: {
    height: 1,
  },
  deleteConfirmSection: {
    paddingTop: SlackSpacing.md,
  },
  deleteLabel: {
    fontSize: SlackTypography.bodySm.fontSize,
    marginBottom: SlackSpacing.sm,
  },
  deleteButton: {
    borderRadius: SlackBorderRadius.pill,
    paddingVertical: SlackSpacing.md,
    alignItems: 'center',
    marginTop: SlackSpacing.sm,
  },
  deleteButtonText: {
    fontSize: SlackTypography.bodyLg.fontSize,
    fontWeight: '600',
  },
  cancelDelete: {
    alignItems: 'center',
    paddingVertical: SlackSpacing.md,
  },
  cancelDeleteText: {
    fontSize: SlackTypography.bodyLg.fontSize,
  },
  versionText: {
    textAlign: 'center',
    fontSize: SlackTypography.caption.fontSize,
    marginTop: SlackSpacing.lg,
  },
});
