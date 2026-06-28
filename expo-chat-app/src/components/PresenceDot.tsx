import { View, StyleSheet } from 'react-native';
import { SlackColors } from '@/constants/Colors';

type Props = {
  online: boolean;
  size?: number;
};

export default function PresenceDot({ online, size = 10 }: Props) {
  return (
    <View
      style={[
        styles.dot,
        { width: size, height: size, borderRadius: size / 2 },
        online ? styles.online : styles.offline,
      ]}
    />
  );
}

const styles = StyleSheet.create({
  dot: {
    borderWidth: 1.5,
    borderColor: '#fff',
  },
  online: {
    backgroundColor: SlackColors.light.accentGreen,
  },
  offline: {
    backgroundColor: SlackColors.light.textSecondary,
  },
});
