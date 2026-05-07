'use client';

import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';

/**
 * Profile page — displays user info and provides a logout action.
 *
 * This page is a child of ChatLayout, so auth is already handled upstream.
 * No additional auth guard is needed here.
 *
 * Requirements: 6.1, 6.2, 6.3, 6.4
 */
export default function ProfilePage() {
  const router = useRouter();
  const { user, logout } = useAuthStore();

  // Guard: if user is somehow null, render nothing (ChatLayout handles redirect)
  if (!user) return null;

  const handleLogOut = () => {
    logout();
    router.push('/');
  };

  const initial = user.displayName?.charAt(0).toUpperCase() ?? '?';

  return (
    <div className="flex flex-col h-full bg-[#13131f] min-w-0">
      {/* ── Content area — centred on mobile, left-aligned on md+ ── */}
      <div className="flex-1 flex flex-col items-center md:items-start justify-center px-8 py-12 gap-6">

        {/* Avatar */}
        <div
          className="w-24 h-24 rounded-full bg-gradient-to-br from-kiro-purple-500 to-kiro-purple-700
                     flex items-center justify-center shrink-0"
          aria-hidden="true"
        >
          <span className="text-4xl font-bold text-white select-none">
            {initial}
          </span>
        </div>

        {/* User info */}
        <div className="flex flex-col items-center md:items-start gap-1 min-w-0">
          <h1 className="text-xl font-semibold text-white truncate max-w-xs">
            {user.displayName}
          </h1>
          <p className="text-sm text-kiro-slate-400 truncate max-w-xs">
            @{user.username}
          </p>
        </div>

        {/* Log Out button */}
        <button
          onClick={handleLogOut}
          aria-label="Log Out"
          className="mt-2 px-5 py-2.5 rounded-lg border border-red-500/40 text-red-400
                     hover:bg-red-500/10 active:bg-red-500/20 transition-colors
                     text-sm font-medium min-h-[44px] min-w-[44px]"
        >
          Log Out
        </button>
      </div>
    </div>
  );
}
