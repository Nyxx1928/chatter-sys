'use client';

import Image from 'next/image';
import { useEffect, useState, useSyncExternalStore } from 'react';

export interface SplashScreenProps {
  onComplete?: () => void;
  duration?: number;
  className?: string;
}

// Subscribe to reduced motion preference changes
function subscribe(callback: () => void) {
  const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
  if (mediaQuery.addEventListener) {
    mediaQuery.addEventListener('change', callback);
    return () => mediaQuery.removeEventListener('change', callback);
  }
  mediaQuery.addListener(callback);
  return () => mediaQuery.removeListener(callback);
}

// Get current reduced motion preference
function getSnapshot(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function getServerSnapshot(): boolean {
  return false;
}

export function SplashScreen({
  onComplete,
  duration = 2200,
  className = '',
}: SplashScreenProps) {
  const [progress, setProgress] = useState(0);
  const [isComplete, setIsComplete] = useState(false);
  
  // Use useSyncExternalStore for media query subscription
  const prefersReducedMotion = useSyncExternalStore(
    subscribe,
    getSnapshot,
    getServerSnapshot
  );

  useEffect(() => {
    let frameId = 0;
    let timeoutId: ReturnType<typeof setTimeout> | null = null;

    if (prefersReducedMotion) {
      // Use microtask to avoid synchronous setState in effect
      queueMicrotask(() => {
        setProgress(100);
        setIsComplete(true);
        timeoutId = setTimeout(() => onComplete?.(), 200);
      });
      return () => {
        if (timeoutId) {
          clearTimeout(timeoutId);
        }
      };
    }

    let startTime: number | null = null;

    const tick = (timestamp: number) => {
      if (!startTime) {
        startTime = timestamp;
      }

      const elapsed = timestamp - startTime;
      const nextProgress = Math.min(100, Math.round((elapsed / duration) * 100));
      setProgress(nextProgress);

      if (elapsed < duration) {
        frameId = window.requestAnimationFrame(tick);
      } else {
        setIsComplete(true);
        timeoutId = setTimeout(() => onComplete?.(), 350);
      }
    };

    frameId = window.requestAnimationFrame(tick);

    return () => {
      window.cancelAnimationFrame(frameId);
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    };
  }, [duration, onComplete, prefersReducedMotion]);

  return (
    <div
      className={`fixed inset-0 z-50 flex items-center justify-center bg-kiro-ink-950 text-kiro-slate-100 ${
        isComplete ? 'animate-fade-out' : 'animate-fade-in'
      } ${className}`.trim()}
    >
      <div className="w-full max-w-md px-6 text-center">
        <div className="flex items-center justify-center gap-3 mb-2">
          <Image src="/logo1.png" alt="Chatter logo" width={40} height={40} className="rounded-xl brightness-0 invert" />
          <p className="text-xl font-bold text-kiro-slate-100">Chatter</p>
        </div>
        <h1 className="mt-3 text-3xl font-semibold sm:text-4xl">Connecting bright teams</h1>
        <p className="mt-4 text-sm text-kiro-slate-500">Initializing workspace</p>

        <div
          className="mt-8 rounded-full bg-kiro-ink-900/80 p-1"
          role="progressbar"
          aria-valuenow={progress}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label="Loading progress"
          aria-live="polite"
          aria-busy={!isComplete}
        >
          <div
            className="h-3 origin-left rounded-full bg-gradient-to-r from-kiro-purple-700 via-kiro-purple-500 to-kiro-purple-400 transition-transform duration-150 will-change-transform"
            style={{ transform: `scaleX(${progress / 100})` }}
          />
        </div>
        <p className="mt-3 text-sm text-kiro-slate-200" aria-hidden="true">{progress}%</p>
      </div>
    </div>
  );
}
