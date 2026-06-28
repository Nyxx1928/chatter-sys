import { Text } from '@/components/Themed';
import { View, StyleSheet } from 'react-native';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import { useColorScheme } from '@/components/useColorScheme';

export default function FriendRequestsScreen() {
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  return (
    <View style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}>
      <Text style={[styles.text, { color: colors.textSecondary }]}>Friend Requests (Coming Soon)</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  text: { fontSize: SlackTypography.bodyLg.fontSize },
});
