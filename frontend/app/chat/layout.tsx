'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { useConnectionStore } from '@/lib/store/connectionStore';

/**
 * Chat layout that wraps all chat pages.
 * Provides authentication protection for chat routes.
 * Establishes STOMP connection when user is authenticated.
 * 
 * Requirements: 5.1, 14.1, 14.2, 14.3, 14.4, 15.1, 16.1
 */
export default function ChatLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const { isAuthenticated, user, token } = useAuthStore();
  const { connected, connecting, error, connect, disconnect } = useConnectionStore();
  const [isClient, setIsClient] = useState(false);

  // Set client-side flag to prevent hydration mismatch
  useEffect(() => {
    setIsClient(true);
  }, []);

  // Protect chat pages - redirect to login if not authenticated
  useEffect(() => {
    if (isClient && (!isAuthenticated || !user)) {
      router.push('/auth/login');
    }
  }, [isClient, isAuthenticated, user, router]);

  // Connect to STOMP server when user is authenticated
  useEffect(() => {
    if (isClient && isAuthenticated && token && !connected && !connecting) {
      connect(token);
    }

    // Cleanup: disconnect when leaving chat area
    return () => {
      if (connected) {
        disconnect();
      }
    };
  }, [isClient, isAuthenticated, token, connected, connecting, connect, disconnect]);

  // Show loading state while checking authentication or during SSR
  if (!isClient || !isAuthenticated || !user) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4" />
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 px-4 py-3 sm:px-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-bold text-gray-900">Real-Time Chat</h1>
            
            {/* Connection status indicator */}
            <div
              className={`flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium ${
                connected
                  ? 'bg-green-100 text-green-800'
                  : connecting
                  ? 'bg-yellow-100 text-yellow-800'
                  : 'bg-red-100 text-red-800'
              }`}
              role="status"
              aria-live="polite"
            >
              <div
                className={`w-2 h-2 rounded-full ${
                  connected
                    ? 'bg-green-500'
                    : connecting
                    ? 'bg-yellow-500 animate-pulse'
                    : 'bg-red-500'
                }`}
                aria-hidden="true"
              />
              <span>
                {connected ? 'Connected' : connecting ? 'Connecting...' : 'Disconnected'}
              </span>
            </div>
          </div>
          
          <div className="flex items-center gap-3">
            {/* User info */}
            <div className="hidden sm:flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white font-medium text-sm">
                {user.displayName.charAt(0).toUpperCase()}
              </div>
              <div className="text-sm">
                <p className="font-medium text-gray-900">{user.displayName}</p>
                <p className="text-gray-500">@{user.username}</p>
              </div>
            </div>

            {/* Logout button */}
            <button
              onClick={() => {
                useAuthStore.getState().logout();
                router.push('/');
              }}
              className="px-3 py-2 text-sm font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500"
              aria-label="Logout"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 flex flex-col min-h-0">
        {/* Connection error banner */}
        {error && (
          <div
            className="bg-red-50 border-l-4 border-red-500 p-4 mx-4 mt-4"
            role="alert"
          >
            <div className="flex items-center">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.5}
                stroke="currentColor"
                className="w-5 h-5 text-red-500 mr-3 shrink-0"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"
                />
              </svg>
              <div className="flex-1">
                <p className="text-sm font-medium text-red-800">
                  Connection Error
                </p>
                <p className="text-sm text-red-700 mt-1">
                  {error}
                </p>
              </div>
              <button
                onClick={() => connect(token)}
                className="ml-4 px-3 py-1 text-sm font-medium text-red-800 hover:text-red-900 hover:bg-red-100 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-red-500"
              >
                Retry
              </button>
            </div>
          </div>
        )}
        
        <div className="flex-1 min-h-0 overflow-hidden">
          {children}
        </div>
      </main>
    </div>
  );
}
