'use client';

import { useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { FriendsPanel } from '@/components/chat/FriendsPanel';
import { listRooms } from '@/lib/api/rooms';
import { useAuthStore } from '@/lib/store/authStore';

export default function ContactsPage() {
  const router = useRouter();
  const { token } = useAuthStore();

  /** Called when a friend request is accepted — navigate straight to the new DM room. */
  const handleDmRoomCreated = (dmRoomId: number) => {
    router.push(`/chat?room=${dmRoomId}`);
  };

  /**
   * Called when the user clicks "Message" on an existing friend.
   * Looks up the DM room from the rooms list and navigates to it.
   */
  const handleOpenDm = useCallback(
    async (friendId: number) => {
      if (!token) return;
      try {
        const rooms = await listRooms(token);
        // DM room names follow the pattern dm__{minId}__{maxId}
        const allDmRooms = rooms.filter((r) => r.roomType === 'DIRECT');
        // Find the specific DM room for this friend by checking if the name contains the friendId
        const targetRoom = allDmRooms.find((r) => {
          const parts = r.name.split('__');
          return parts.includes(String(friendId));
        });
        if (targetRoom) {
          router.push(`/chat?room=${targetRoom.id}`);
        }
      } catch (err) {
        console.error('Failed to find DM room:', err);
      }
    },
    [token, router]
  );

  return (
    <div className="flex flex-col h-full bg-slack-surface-secondary min-w-0">
      <header className="px-4 py-4 border-b border-slack-border shrink-0">
        <h1 className="text-lg font-semibold text-slack-text-primary">Contacts</h1>
      </header>
      <div className="flex-1 overflow-y-auto px-4 py-4">
        <FriendsPanel
          onDmRoomCreated={handleDmRoomCreated}
          onOpenDm={handleOpenDm}
        />
      </div>
    </div>
  );
}
