'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChatRoom } from '@/types/domain';
import { RoomSelector } from '@/components/chat/RoomSelector';
import { RoomCreateModal } from '@/components/chat/RoomCreateModal';
import { Button, Input } from '@/components/ui';
import { createRoom, listRooms } from '@/lib/api/rooms';
import { useAuthStore } from '@/lib/store/authStore';

/**
 * Channels page — browse and create chat rooms.
 * Selecting a room navigates to /chat?room={id} so ChatRoomsPage auto-selects it.
 *
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
export default function ChannelsPage() {
  const router = useRouter();
  const { token, user } = useAuthStore();

  // Room list state
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Create room modal state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  // ── Fetch rooms ──────────────────────────────────────────────────────────────

  const fetchRooms = useCallback(async () => {
    if (!token) return [] as ChatRoom[];
    return listRooms(token);
  }, [token]);

  const loadRooms = useCallback(async () => {
    if (!token) return;
    try {
      setLoading(true);
      setError(null);
      const roomsList = await fetchRooms();
      setRooms(roomsList);
    } catch (err) {
      console.error('Failed to load rooms:', err);
      setError('Failed to load channels. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [token, fetchRooms]);

  // Load rooms on mount
  useEffect(() => {
    if (!token) return;
    let isActive = true;
    const loadInitialRooms = async () => {
      try {
        const roomsList = await fetchRooms();
        if (!isActive) return;
        setRooms(roomsList);
      } catch (err) {
        if (isActive) {
          console.error('Failed to load rooms:', err);
          setError('Failed to load channels. Please try again.');
        }
      } finally {
        if (isActive) setLoading(false);
      }
    };
    void loadInitialRooms();
    return () => {
      isActive = false;
    };
  }, [token, fetchRooms]);

  // ── Handlers ─────────────────────────────────────────────────────────────────

  /** Navigate to the Chats page with the selected room pre-selected. */
  const handleRoomSelect = useCallback(
    (room: ChatRoom) => {
      router.push('/chat?room=' + room.id);
    },
    [router]
  );

  const handleCreateRoom = async (name: string, description: string) => {
    if (!token) return;
    try {
      setIsCreating(true);
      setCreateError(null);
      const newRoom = await createRoom(token, {
        name,
        description: description || undefined,
      });
      setShowCreateModal(false);
      // Navigate to the newly created room
      router.push('/chat?room=' + newRoom.id);
    } catch (err) {
      console.error('Failed to create room:', err);
      setCreateError('Failed to create room. Please try again.');
    } finally {
      setIsCreating(false);
    }
  };

  // ── Derived state ─────────────────────────────────────────────────────────────

  const filteredRooms = rooms.filter((room) => {
    if (!searchQuery.trim()) return true;
    const query = searchQuery.toLowerCase();
    return (
      room.name.toLowerCase().includes(query) ||
      (room.description ?? '').toLowerCase().includes(query)
    );
  });

  // ── Render ────────────────────────────────────────────────────────────────────

  if (loading) {
    return (
      <div
        className="flex items-center justify-center h-full bg-[#13131f]"
        role="status"
        aria-live="polite"
      >
        <div className="text-center">
          <div
            className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-kiro-purple-400 mb-4"
            aria-hidden="true"
          />
          <p className="text-kiro-slate-400">Loading channels…</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-full p-4 bg-[#13131f]">
        <div className="text-center max-w-md" role="alert">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
            className="w-16 h-16 mx-auto text-red-500 mb-4"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"
            />
          </svg>
          <p className="text-red-400 font-medium mb-4">{error}</p>
          <Button onClick={loadRooms} variant="primary">
            Try Again
          </Button>
        </div>
      </div>
    );
  }

  return (
    // Task 2.4 — mobile-safe root container: flex column, full height, no fixed widths
    <div className="flex flex-col h-full bg-[#13131f] min-w-0">

      {/* ── Header ── */}
      <header className="px-4 py-4 border-b border-white/5 shrink-0">
        <div className="flex items-center justify-between mb-3">
          <h1 className="text-base font-semibold text-kiro-slate-100">Channels</h1>
          {/* Task 2.3 — Create Room button */}
          <Button
            onClick={() => {
              setCreateError(null);
              setShowCreateModal(true);
            }}
            variant="primary"
            size="sm"
            aria-label="Create new room"
          >
            + Create Room
          </Button>
        </div>

        {/* Search */}
        <Input
          label="Search channels"
          placeholder="Search…"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          fullWidth
        />
      </header>

      {/* ── Room list — fills remaining height ── */}
      {/* Task 2.4 — flex-1 overflow-hidden so RoomSelector can scroll internally */}
      <div className="flex-1 overflow-hidden">
        <RoomSelector
          rooms={filteredRooms}
          latestMessages={{}}
          onRoomSelect={handleRoomSelect}
          canDeleteRoom={(room) => room.createdBy?.id === user?.id}
          emptyStateTitle={
            searchQuery.trim() ? 'No channels match your search' : 'No channels yet'
          }
          emptyStateDescription={
            searchQuery.trim()
              ? 'Try a different keyword or clear the filter.'
              : 'Create a channel to start chatting.'
          }
          className="h-full"
        />
      </div>

      {/* ── Create Room modal ── */}
      <RoomCreateModal
        key={showCreateModal ? 'channels-create-open' : 'channels-create-closed'}
        open={showCreateModal}
        isSubmitting={isCreating}
        errorMessage={createError}
        onClose={() => setShowCreateModal(false)}
        onCreate={handleCreateRoom}
      />
    </div>
  );
}
