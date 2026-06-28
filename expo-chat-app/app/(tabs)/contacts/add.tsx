import { Text } from '@/components/Themed';
import { View, StyleSheet } from 'react-native';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import { useColorScheme } from '@/components/useColorScheme';

export default function AddFriendScreen() {
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  return (
    <View style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}>
      <Text style={[styles.text, { color: colors.textSecondary }]}>Add Friend (Coming Soon)</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  text: { fontSize: SlackTypography.bodyLg.fontSize },
});
