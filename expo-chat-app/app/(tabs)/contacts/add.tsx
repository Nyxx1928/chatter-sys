import { Text } from '@/components/Themed';
import { View, StyleSheet } from 'react-native';

export default function AddFriendScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.text}>Add Friend (Coming Soon)</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  text: { fontSize: 18, color: '#9ca3af' },
});
