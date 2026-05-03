'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChatRoom } from '@/types/domain';
import { FriendsPanel } from '@/components/chat/FriendsPanel';
import { RoomCreateModal } from '@/components/chat/RoomCreateModal';
import { RoomSelector } from '@/components/chat/RoomSelector';
import { Button, Input, Modal } from '@/components/ui';
import { createRoom, deleteRoom, listRooms } from '@/lib/api/rooms';
import { useAuthStore } from '@/lib/store/authStore';

/**
 * Chat rooms list page.
 * Displays available chat rooms and allows navigation to specific rooms.
 * 
 * Requirements: 5.1, 14.2, 15.4
 */
export default function ChatRoomsPage() {
  const router = useRouter();
  const { token, user } = useAuthStore();
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ChatRoom | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchRooms = useCallback(async () => {
    if (!token) {
      return [] as ChatRoom[];
    }

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
      setError('Failed to load chat rooms. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [token, fetchRooms]);

  // Load available rooms on mount
  useEffect(() => {
    if (!token) {
      return;
    }

    let isActive = true;

    const loadInitialRooms = async () => {
      try {
        const roomsList = await fetchRooms();

        if (!isActive) {
          return;
        }

        setRooms(roomsList);
      } catch (err) {
        if (isActive) {
          console.error('Failed to load rooms:', err);
          setError('Failed to load chat rooms. Please try again.');
        }
      } finally {
        if (isActive) {
          setLoading(false);
        }
      }
    };

    void loadInitialRooms();

    return () => {
      isActive = false;
    };
  }, [token, fetchRooms]);

  const handleRoomSelect = (room: ChatRoom) => {
    router.push(`/chat/${room.id}`);
  };

  const filteredRooms = rooms.filter((room) => {
    if (!searchQuery.trim()) {
      return true;
    }

    const query = searchQuery.toLowerCase();
    return (
      room.name.toLowerCase().includes(query) ||
      (room.description ?? '').toLowerCase().includes(query)
    );
  });

  const handleCreateRoom = async (name: string, description: string) => {
    if (!token) return;

    try {
      setIsCreating(true);
      setCreateError(null);
      const newRoom = await createRoom(token, { name, description: description || undefined });
      await loadRooms();
      setShowCreateModal(false);
      router.push(`/chat/${newRoom.id}`);
    } catch (err) {
      console.error('Failed to create room:', err);
      setCreateError('Failed to create room. Please try again.');
    } finally {
      setIsCreating(false);
    }
  };

  const handleDeleteRoom = async () => {
    if (!token || !deleteTarget) return;

    try {
      setIsDeleting(true);
      setDeleteError(null);
      await deleteRoom(token, deleteTarget.id);
      setDeleteTarget(null);
      await loadRooms();
    } catch (err) {
      console.error('Failed to delete room:', err);
      setDeleteError('Failed to delete room. Please try again.');
    } finally {
      setIsDeleting(false);
    }
  };

  // Loading state
  if (loading) {
    return (
      <div className="flex items-center justify-center h-full" role="status" aria-live="polite">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4" aria-hidden="true" />
          <p className="text-gray-600">Loading rooms...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="flex items-center justify-center h-full p-4">
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
          <p className="text-red-600 font-medium mb-4">{error}</p>
          <Button onClick={loadRooms} variant="primary">
            Try Again
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col lg:flex-row">
      <section className="flex flex-1 flex-col">
        {/* Page header */}
        <header className="bg-white border-b border-gray-200 px-4 py-4 sm:px-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">Chat Rooms</h2>
              <p className="text-sm text-gray-600 mt-1">
                Select a room to start chatting
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <Button
                onClick={() => {
                  setCreateError(null);
                  setShowCreateModal(true);
                }}
                variant="primary"
                size="sm"
              >
                Create room
              </Button>
              <Button
                onClick={loadRooms}
                variant="secondary"
                size="sm"
                aria-label="Refresh rooms"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  className="w-5 h-5"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
                  />
                </svg>
              </Button>
            </div>
          </div>
          <div className="mt-4 max-w-xl">
            <Input
              label="Search rooms"
              placeholder="Filter by room name or description"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              fullWidth
            />
          </div>
        </header>

        {/* Room list */}
        <main className="flex-1 overflow-hidden">
          <RoomSelector
            rooms={filteredRooms}
            onRoomSelect={handleRoomSelect}
            onRoomDelete={(room) => {
              setDeleteError(null);
              setDeleteTarget(room);
            }}
            canDeleteRoom={(room) => room.createdBy?.id === user?.id}
            emptyStateTitle={
              searchQuery.trim().length > 0 ? 'No rooms match your search' : undefined
            }
            emptyStateDescription={
              searchQuery.trim().length > 0
                ? 'Try a different keyword or clear the filter.'
                : undefined
            }
            className="h-full"
          />
        </main>
      </section>

      <aside className="border-t border-gray-200 bg-gray-50 px-4 py-6 lg:w-96 lg:border-l lg:border-t-0 lg:overflow-y-auto">
        <FriendsPanel />
      </aside>

      <RoomCreateModal
        key={showCreateModal ? 'room-create-open' : 'room-create-closed'}
        open={showCreateModal}
        isSubmitting={isCreating}
        errorMessage={createError}
        onClose={() => setShowCreateModal(false)}
        onCreate={handleCreateRoom}
      />

      <Modal
        open={Boolean(deleteTarget)}
        title="Delete room"
        onClose={() => setDeleteTarget(null)}
        footer={
          <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
            <Button variant="secondary" onClick={() => setDeleteTarget(null)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={handleDeleteRoom} disabled={isDeleting}>
              {isDeleting ? 'Deleting...' : 'Delete room'}
            </Button>
          </div>
        }
      >
        <p className="text-sm text-gray-600">
          Deleting <span className="font-semibold text-gray-900">{deleteTarget?.name}</span> will remove all
          messages and memberships. This action cannot be undone.
        </p>
        {deleteError && (
          <p className="mt-3 text-sm text-red-600" role="alert">
            {deleteError}
          </p>
        )}
      </Modal>
    </div>
  );
}
