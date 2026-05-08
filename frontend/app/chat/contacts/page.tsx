'use client';

import { FriendsPanel } from '@/components/chat/FriendsPanel';

export default function ContactsPage() {
  return (
    <div className="flex flex-col h-full bg-[#13131f] min-w-0">
      <header className="px-4 py-4 border-b border-white/5 shrink-0">
        <h1 className="text-lg font-semibold text-white">Contacts</h1>
      </header>
      <div className="flex-1 overflow-y-auto px-4 py-4">
        <FriendsPanel />
      </div>
    </div>
  );
}
