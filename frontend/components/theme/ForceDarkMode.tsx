'use client';

import { useEffect, useRef } from 'react';
import { useThemeStore } from '@/lib/store/themeStore';

export function ForceDarkMode() {
  const themeRef = useRef(useThemeStore.getState().theme);

  useEffect(() => {
    const root = document.documentElement;

    root.classList.add('dark');
    root.classList.remove('light');

    const currentTheme = themeRef.current;
    return () => {
      if (currentTheme === 'light') {
        root.classList.add('light');
        root.classList.remove('dark');
      }
    };
  }, []);

  return null;
}
