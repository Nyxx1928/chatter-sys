'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

/**
 * Redirects legacy /chat/[roomId] URLs to the unified /chat page.
 * Room selection is now handled inline on the main chat page.
 */
export default function ChatRoomRedirect() {
  const router = useRouter();

  useEffect(() => {
    router.replace('/chat');
  }, [router]);

  return (
    <div className="flex items-center justify-center h-full bg-[#13131f]">
      <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-kiro-purple-400" />
    </div>
  );
}
