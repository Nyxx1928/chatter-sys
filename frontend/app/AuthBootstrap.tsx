'use client';

import { useEffect, useState } from 'react';
import { useAuthStore } from '@/lib/store/authStore';

export default function AuthBootstrap() {
  const isInitialized = useAuthStore((state) => state.isInitialized);
  const isChecking = useAuthStore((state) => state.isChecking);
  const validateSession = useAuthStore((state) => state.validateSession);
  const [showWakeUp, setShowWakeUp] = useState(false);

  useEffect(() => {
    if (!isInitialized) {
      void validateSession();
    }
  }, [isInitialized, validateSession]);

  // If the session check is still running after 3s, the backend is likely
  // cold-starting on Render's free tier. Show a subtle notice.
  useEffect(() => {
    if (!isChecking) {
      // Reset via a microtask so setState is not called synchronously in the effect body
      const id = setTimeout(() => setShowWakeUp(false), 0);
      return () => clearTimeout(id);
    }
    const id = setTimeout(() => setShowWakeUp(true), 3000);
    return () => clearTimeout(id);
  }, [isChecking]);

  if (!showWakeUp) return null;

  return (
    <div className="fixed bottom-4 left-1/2 -translate-x-1/2 z-[100] flex items-center gap-2 rounded-full bg-slack-surface-primary border border-slack-border px-4 py-2 text-xs text-slack-text-secondary shadow-slack-lg">
      <span className="inline-block w-2 h-2 rounded-full bg-yellow-400 animate-pulse" />
      Waking up the server… this takes ~30s on first load
    </div>
  );
}
