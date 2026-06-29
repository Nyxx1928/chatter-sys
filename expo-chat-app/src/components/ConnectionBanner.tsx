import { memo, useEffect, useRef } from 'react';
import { Animated, StyleSheet } from 'react-native';
import { Text } from '@/components/Themed';
import { useConnectionStore } from '@/src/stores/connectionStore';
import { SlackColors } from '@/constants/Colors';
import SlackTypography from '@/constants/Typography';

const ConnectionBanner = memo(function ConnectionBanner() {
  const connected = useConnectionStore((s) => s.connected);
  const connecting = useConnectionStore((s) => s.connecting);
  const error = useConnectionStore((s) => s.error);
  const translateY = useRef(new Animated.Value(-60)).current;

  const show = !connected || connecting;

  useEffect(() => {
    Animated.spring(translateY, {
      toValue: show ? 0 : -60,
      useNativeDriver: true,
      tension: 80,
      friction: 12,
    }).start();
  }, [show, translateY]);

  let bgColor = SlackColors.light.accentRed;
  let message = error || 'No Connection';

  if (connecting) {
    bgColor = SlackColors.light.accentYellow;
    message = 'Reconnecting...';
  }

  return (
    <Animated.View
      style={[styles.banner, { backgroundColor: bgColor, transform: [{ translateY }] }]}
      accessibilityLabel={message}
      accessibilityRole="alert"
    >
      <Text style={styles.text}>{message}</Text>
    </Animated.View>
  );
});

const styles = StyleSheet.create({
  banner: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 100,
    paddingVertical: 8,
    paddingHorizontal: 16,
    alignItems: 'center',
  },
  text: {
    color: '#fff',
    fontSize: SlackTypography.bodySm.fontSize,
    fontWeight: '600',
  },
});

export default ConnectionBanner;
