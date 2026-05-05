'use client';

import { useEffect } from 'react';
import { useAuthStore } from '@/lib/store/authStore';

export default function AuthBootstrap() {
  const isInitialized = useAuthStore((state) => state.isInitialized);
  const validateSession = useAuthStore((state) => state.validateSession);

  useEffect(() => {
    if (!isInitialized) {
      void validateSession();
    }
  }, [isInitialized, validateSession]);

  return null;
}
