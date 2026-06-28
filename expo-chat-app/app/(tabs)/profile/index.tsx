import { Text } from '@/components/Themed';
import { View, StyleSheet } from 'react-native';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';
import SlackSpacing from '@/constants/Spacing';
import { SlackBorderRadius } from '@/constants/BorderRadius';
import { useColorScheme } from '@/components/useColorScheme';

export default function ProfileScreen() {
  const colorScheme = useColorScheme();
  const colors = colorScheme === 'dark' ? SlackColors.dark : SlackColors.light;

  return (
    <View style={[styles.container, { backgroundColor: colors.surfaceSecondary }]}>
      <View style={[styles.card, { backgroundColor: colors.surfacePrimary, borderColor: colors.border }]}>
        <Text style={[styles.text, { color: colors.textPrimary }]}>Profile (Coming Soon)</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: SlackSpacing['2xl'],
  },
  card: {
    width: '100%',
    borderRadius: SlackBorderRadius.lg,
    padding: SlackSpacing['2xl'],
    alignItems: 'center',
    borderWidth: 1,
  },
  text: {
    fontSize: SlackTypography.bodyLg.fontSize,
  },
});
