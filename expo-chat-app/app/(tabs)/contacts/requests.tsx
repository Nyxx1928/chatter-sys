import { Text } from '@/components/Themed';
import { View, StyleSheet } from 'react-native';

export default function FriendRequestsScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.text}>Friend Requests (Coming Soon)</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  text: { fontSize: 18, color: '#9ca3af' },
});
