'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useEffect, useSyncExternalStore } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { useConnectionStore } from '@/lib/store/connectionStore';

/**
 * Chat layout that wraps all chat pages.
 * Provides authentication protection for chat routes.
 * Establishes STOMP connection when user is authenticated.
 *
 * Desktop: slim icon-only left sidebar nav.
 * Mobile:  bottom tab bar (Chats / Contacts / Profile).
 *
 * Requirements: 5.1, 14.1, 14.2, 14.3, 14.4, 15.1, 16.1
 */
export default function ChatLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { isAuthenticated, isInitialized, isChecking, user, token } = useAuthStore();
  const { connected, connecting, error, connect, disconnect } = useConnectionStore();
  const isClient = useSyncExternalStore(
    () => () => {},
    () => true,
    () => false
  );

  useEffect(() => {
    if (isClient && isInitialized && (!isAuthenticated || !user)) {
      router.push('/auth/login');
    }
  }, [isClient, isInitialized, isAuthenticated, user, router]);

  useEffect(() => {
    if (isClient && isAuthenticated && token && !connected && !connecting) {
      connect(token);
    }
    return () => {
      if (connected) disconnect();
    };
  }, [isClient, isAuthenticated, token, connected, connecting, connect, disconnect]);

  if (!isClient || isChecking || !isInitialized || !isAuthenticated || !user) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-kiro-ink-950">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-kiro-purple-400 mb-4" />
          <p className="text-kiro-slate-400">Loading...</p>
        </div>
      </div>
    );
  }

  const isChatsActive    = pathname === '/chat';
  const isChannelsActive = pathname.startsWith('/chat/channels');
  const isContactsActive = pathname.startsWith('/chat/contacts');
  const isProfileActive  = pathname.startsWith('/chat/profile');

  return (
    <div className="flex h-[100dvh] bg-[#13131f] overflow-hidden">

      {/* ── Desktop: slim icon-only left nav (hidden on mobile) ── */}
      <nav
        className="hidden md:flex flex-col items-center w-16 shrink-0 bg-[#0e0e1a] border-r border-white/5 py-4 gap-1"
        aria-label="Main navigation"
      >
        {/* Logo */}
        <div className="mb-4 flex items-center justify-center w-10 h-10">
          <Image src="/logo1.png" alt="Chatter" width={32} height={32} className="brightness-0 invert" />
        </div>

        <NavIcon href="/chat" label="Home" active={isChatsActive}>
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
            <path strokeLinecap="round" strokeLinejoin="round" d="m2.25 12 8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
          </svg>
        </NavIcon>

        <NavIcon href="/chat/channels" label="Channels" active={isChannelsActive}>
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z" />
          </svg>
        </NavIcon>

        <NavIcon href="/chat/contacts" label="Contacts" active={isContactsActive}>
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
          </svg>
        </NavIcon>

        <NavIcon href="/chat/profile" label="Profile" active={isProfileActive}>
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.325.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 0 1 1.37.49l1.296 2.247a1.125 1.125 0 0 1-.26 1.431l-1.003.827c-.293.241-.438.613-.43.992a7.723 7.723 0 0 1 0 .255c-.008.378.137.75.43.991l1.004.827c.424.35.534.955.26 1.43l-1.298 2.247a1.125 1.125 0 0 1-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.47 6.47 0 0 1-.22.128c-.331.183-.581.495-.644.869l-.213 1.281c-.09.543-.56.94-1.11.94h-2.594c-.55 0-1.019-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 0 1-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 0 1-1.369-.49l-1.297-2.247a1.125 1.125 0 0 1 .26-1.431l1.004-.827c.292-.24.437-.613.43-.991a6.932 6.932 0 0 1 0-.255c.007-.38-.138-.751-.43-.992l-1.004-.827a1.125 1.125 0 0 1-.26-1.43l1.297-2.247a1.125 1.125 0 0 1 1.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.086.22-.128.332-.183.582-.495.644-.869l.214-1.28Z" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
          </svg>
        </NavIcon>

        <div className="flex-1" />

        {/* Connection dot */}
        <div
          className={`w-2.5 h-2.5 rounded-full mb-2 ${connected ? 'bg-green-500' : connecting ? 'bg-yellow-500 animate-pulse' : 'bg-red-500'}`}
          title={connected ? 'Connected' : connecting ? 'Connecting…' : 'Disconnected'}
          aria-label={connected ? 'Connected' : connecting ? 'Connecting' : 'Disconnected'}
        />

        <button
          onClick={() => { useAuthStore.getState().logout(); router.push('/'); }}
          className="mt-3 w-9 h-9 rounded-full bg-gradient-to-br from-kiro-purple-500 to-kiro-purple-700 flex items-center justify-center text-white font-semibold text-sm hover:ring-2 hover:ring-kiro-purple-400 transition-all focus:outline-none focus:ring-2 focus:ring-kiro-purple-400"
          title={`Logout (${user.displayName})`}
          aria-label="Logout"
        >
          {user.displayName.charAt(0).toUpperCase()}
        </button>
      </nav>

      {/* ── Page content ── */}
      <div className="flex flex-col flex-1 min-w-0 min-h-0">
        {/* Connection error banner */}
        {error && (
          <div className="bg-red-950/60 border-b border-red-800/50 px-4 py-2" role="alert">
            <div className="flex items-center gap-3">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4 text-red-400 shrink-0">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 3.75h.008v.008H12v-.008Z" />
              </svg>
              <p className="text-sm text-red-300 flex-1">Connection error: {error}</p>
              <button onClick={() => connect(token)} className="text-xs text-red-300 hover:text-red-100 underline focus:outline-none">
                Retry
              </button>
            </div>
          </div>
        )}

        {/* Main content — takes all space above the mobile tab bar */}
        <div className="flex-1 min-h-0 overflow-hidden pb-0 md:pb-0">
          {children}
        </div>

        {/* ── Mobile bottom tab bar (hidden on md+) ── */}
        <nav
          className="md:hidden flex items-center justify-around bg-[#0e0e1a] border-t border-white/5 shrink-0 safe-area-bottom"
          aria-label="Mobile navigation"
          style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
        >
          <MobileTab href="/chat" label="Chats" active={isChatsActive}>
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
              <path strokeLinecap="round" strokeLinejoin="round" d="M8.625 12a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H8.25m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H12m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 0 1-2.555-.337A5.972 5.972 0 0 1 5.41 20.97a5.969 5.969 0 0 1-.474-.065 4.48 4.48 0 0 0 .978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25Z" />
            </svg>
          </MobileTab>

          <MobileTab href="/chat/channels" label="Channels" active={isChannelsActive}>
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
              <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z" />
            </svg>
          </MobileTab>

          <MobileTab href="/chat/contacts" label="Contacts" active={isContactsActive}>
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6">
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
            </svg>
          </MobileTab>

          <MobileTab href="/chat/profile" label="Profile" active={isProfileActive}>
            <div className="w-6 h-6 rounded-full bg-gradient-to-br from-kiro-purple-500 to-kiro-purple-700 flex items-center justify-center text-white font-semibold text-xs">
              {user.displayName.charAt(0).toUpperCase()}
            </div>
          </MobileTab>
        </nav>
      </div>
    </div>
  );
}

/** Desktop left-nav link icon */
function NavIcon({ href, label, active, children }: { href: string; label: string; active: boolean; children: React.ReactNode }) {
  return (
    <Link
      href={href}
      aria-label={label}
      title={label}
      className={`relative flex items-center justify-center w-10 h-10 rounded-xl transition-colors focus:outline-none focus:ring-2 focus:ring-kiro-purple-400 ${
        active ? 'bg-kiro-purple-600/30 text-kiro-purple-400' : 'text-kiro-slate-500 hover:bg-white/5 hover:text-kiro-slate-200'
      }`}
    >
      {active && <span className="absolute left-0 top-1/2 -translate-y-1/2 w-0.5 h-5 bg-kiro-purple-500 rounded-r-full" aria-hidden="true" />}
      {children}
    </Link>
  );
}

/** Mobile bottom tab — link variant */
function MobileTab({ href, label, active, children }: { href: string; label: string; active: boolean; children: React.ReactNode }) {
  return (
    <Link
      href={href}
      aria-label={label}
      aria-current={active ? 'page' : undefined}
      className={`flex flex-col items-center gap-1 py-2 px-4 min-w-[64px] min-h-[44px] justify-center transition-colors focus:outline-none focus:ring-2 focus:ring-kiro-purple-400 rounded-lg ${
        active ? 'text-kiro-purple-400' : 'text-kiro-slate-500'
      }`}
    >
      {children}
      <span className="text-[10px] font-medium">{label}</span>
    </Link>
  );
}

