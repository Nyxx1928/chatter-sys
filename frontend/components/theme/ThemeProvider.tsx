'use client';

import { useEffect } from 'react';
import { useThemeStore } from '@/lib/store/themeStore';

/**
 * Applies the .dark class to <html> based on the theme store.
 * Must be mounted once inside the root layout.
 */
export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const theme = useThemeStore((s) => s.theme);

  useEffect(() => {
    const root = document.documentElement;

    if (theme === 'dark') {
      root.classList.add('dark');
      root.classList.remove('light');
    } else {
      root.classList.add('light');
      root.classList.remove('dark');
    }
  }, [theme]);

  return <>{children}</>;
}
