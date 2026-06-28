import NetInfo from '@react-native-community/netinfo';
import { useEffect, useState } from 'react';
import { useConnectionStore } from '../stores/connectionStore';

type NetworkStatus = {
  isConnected: boolean;
  connectionType: string | null;
};

export function useNetworkStatus(): NetworkStatus {
  const [status, setStatus] = useState<NetworkStatus>({
    isConnected: true,
    connectionType: null,
  });
  const connect = useConnectionStore((s) => s.connect);

  useEffect(() => {
    const unsubscribe = NetInfo.addEventListener((state) => {
      const isConnected = state.isConnected ?? true;
      setStatus({
        isConnected,
        connectionType: state.type,
      });

      if (isConnected) {
        connect();
      }
    });

    return () => {
      unsubscribe();
    };
  }, [connect]);

  return status;
}
