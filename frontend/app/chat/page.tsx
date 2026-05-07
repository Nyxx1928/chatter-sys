'use client';

import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { ChatRoom, Message, User } from '@/types/domain';
import { RoomCreateModal } from '@/components/chat/RoomCreateModal';
import { RoomSelector } from '@/components/chat/RoomSelector';
import { MessageList } from '@/components/chat/MessageList';
import { MessageInput } from '@/components/chat/MessageInput';
import { UserList } from '@/components/chat/UserList';
import { Button, Input, Modal } from '@/components/ui';
import { createRoom, deleteRoom, listRooms, inviteToRoom, getRoomDetails, getRoomMembers } from '@/lib/api/rooms';
import { searchUsers } from '@/lib/api/users';
import { getMessageHistory } from '@/lib/api/messages';
import { useAuthStore } from '@/lib/store/authStore';
import { useConnectionStore } from '@/lib/store/connectionStore';
import { usePresenceStore } from '@/lib/store/presenceStore';
import { useStompSubscription } from '@/lib/stomp/hooks';

/**
 * Unified chat page with room list, messages, and members.
 * Displays available chat rooms and shows messages inline when a room is selected.
 * 
 * Requirements: 5.1, 14.2, 15.1, 15.2, 15.3, 15.4
 */
export default function ChatRoomsPage() {
  const { token, user } = useAuthStore();
  const { connected, sendMessage } = useConnectionStore();
  const { updatePresence } = usePresenceStore();
  const searchParams = useSearchParams();
  const preselectedRoomId = searchParams.get('room');
  
  // Room list state
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
  const [latestMessages, setLatestMessages] = useState<Record<number, Message>>({});

  // Invite member state
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteRoomTarget, setInviteRoomTarget] = useState<ChatRoom | null>(null);
  const [inviteSearchQuery, setInviteSearchQuery] = useState('');
  const [inviteSearchResults, setInviteSearchResults] = useState<import('@/types/domain').UserSearchResult[]>([]);
  const [inviteSearchLoading, setInviteSearchLoading] = useState(false);
  const [invitingUserId, setInvitingUserId] = useState<number | null>(null);
  const [inviteSuccess, setInviteSuccess] = useState<Record<number, boolean>>({});
  
  // Selected room state
  const [selectedRoom, setSelectedRoom] = useState<ChatRoom | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [members, setMembers] = useState<User[]>([]);
  const [roomLoading, setRoomLoading] = useState(false);
  const [showMembersOnMobile, setShowMembersOnMobile] = useState(false);

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
      setError('Failed to load chat rooms. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [token, fetchRooms]);

  // Load available rooms on mount
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
          setError('Failed to load chat rooms. Please try again.');
        }
      } finally {
        if (isActive) setLoading(false);
      }
    };
    void loadInitialRooms();
    return () => { isActive = false; };
  }, [token, fetchRooms]);

  // Auto-select a room from the URL search param ?room={id}
  useEffect(() => {
    if (!preselectedRoomId || rooms.length === 0) return;
    const room = rooms.find((r) => r.id === Number(preselectedRoomId));
    if (room && room.id !== selectedRoom?.id) void handleRoomSelect(room);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preselectedRoomId, rooms]);

  // Load room data when a room is selected
  const handleRoomSelect = useCallback(async (room: ChatRoom) => {
    if (!token) return;

    // Leave previous room
    if (selectedRoom && connected && user) {
      sendMessage(`/app/room.leave/${selectedRoom.id}`, {});
    }

    setSelectedRoom(room);
    setMessages([]);
    setMembers([]);
    setRoomLoading(true);
    setShowMembersOnMobile(false);

    try {
      const [roomDetails, messageHistory, roomMembers] = await Promise.all([
        getRoomDetails(token, room.id),
        getMessageHistory(token, room.id, { page: 0, size: 50 }),
        getRoomMembers(token, room.id),
      ]);
      setSelectedRoom(roomDetails);
      setMessages(messageHistory?.content ?? []);
      setMembers(roomMembers);
    } catch (err) {
      console.error('Failed to load room data:', err);
    } finally {
      setRoomLoading(false);
    }

    if (connected && user) {
      sendMessage(`/app/room.join/${room.id}`, {});
    }
  }, [token, selectedRoom, connected, user, sendMessage]);

  // Leave room on unmount
  useEffect(() => {
    return () => {
      if (selectedRoom && connected && user) {
        sendMessage(`/app/room.leave/${selectedRoom.id}`, {});
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedRoom?.id, connected]);

  // Subscribe to selected room messages
  const handleNewMessage = useCallback((message: Message) => {
    setMessages((prev) => [...prev, message]);
    setLatestMessages((prev) => ({ ...prev, [message.chatRoomId]: message }));
  }, []);

  useStompSubscription<Message>(
    connected && selectedRoom ? `/topic/room/${selectedRoom.id}` : null,
    handleNewMessage
  );

  // Subscribe to presence updates for selected room
  const handlePresenceUpdate = useCallback(
    (update: { userId: number; online: boolean }) => {
      setMembers((prev) =>
        prev.map((member) =>
          member.id === update.userId ? { ...member, online: update.online } : member
        )
      );
      updatePresence(update.userId, update.online);
    },
    [updatePresence]
  );

  useStompSubscription<{ userId: number; online: boolean }>(
    connected && selectedRoom ? `/topic/presence/${selectedRoom.id}` : null,
    handlePresenceUpdate
  );

  // Subscribe to global room topic for latest message previews
  const handleGlobalMessage = useCallback((message: Message) => {
    setLatestMessages((prev) => ({ ...prev, [message.chatRoomId]: message }));
  }, []);

  useStompSubscription<Message>(
    connected && rooms.length > 0 ? `/topic/rooms` : null,
    handleGlobalMessage
  );

  const handleSendMessage = (content: string) => {
    if (!connected || !user || !selectedRoom) return;
    sendMessage(`/app/chat.send/${selectedRoom.id}`, {
      content,
      senderId: user.id,
      senderUsername: user.username,
      senderDisplayName: user.displayName,
      chatRoomId: selectedRoom.id,
      timestamp: new Date().toISOString(),
    });
  };

  const handleOpenInvite = (room: ChatRoom) => {
    setInviteRoomTarget(room);
    setInviteSearchQuery('');
    setInviteSearchResults([]);
    setInviteSuccess({});
    setShowInviteModal(true);
  };

  const handleInviteSearch = useCallback(async (query: string) => {
    setInviteSearchQuery(query);
    if (!token || query.trim().length < 2) {
      setInviteSearchResults([]);
      return;
    }
    setInviteSearchLoading(true);
    try {
      const results = await searchUsers(token, query.trim());
      // Filter out users already in the room
      const memberIds = new Set(members.map((m) => m.id));
      setInviteSearchResults(results.filter((r) => !memberIds.has(r.user.id)));
    } catch (err) {
      console.error('User search failed:', err);
    } finally {
      setInviteSearchLoading(false);
    }
  }, [token, members]);

  const handleInviteUser = async (inviteeId: number) => {
    if (!token || !inviteRoomTarget) return;
    setInvitingUserId(inviteeId);
    try {
      await inviteToRoom(token, inviteRoomTarget.id, inviteeId);
      setInviteSuccess((prev) => ({ ...prev, [inviteeId]: true }));
      // Refresh member list if we're currently in that room
      if (selectedRoom?.id === inviteRoomTarget.id) {
        const updated = await getRoomMembers(token, inviteRoomTarget.id);
        setMembers(updated);
      }
    } catch (err) {
      const apiErr = err as { status?: number; details?: unknown; message?: string };
      console.error('Failed to invite user — status:', apiErr.status, 'details:', apiErr.details, 'message:', apiErr.message);
    } finally {
      setInvitingUserId(null);
    }
  };

  const filteredRooms = rooms.filter((room) => {
    if (!searchQuery.trim()) return true;
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
      // Auto-select the newly created room
      void handleRoomSelect(newRoom);
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
      if (selectedRoom?.id === deleteTarget.id) {
        setSelectedRoom(null);
        setMessages([]);
        setMembers([]);
      }
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
      <div className="flex items-center justify-center h-full bg-kiro-ink-950" role="status" aria-live="polite">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-kiro-purple-400 mb-4" aria-hidden="true" />
          <p className="text-kiro-slate-400">Loading rooms...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="flex items-center justify-center h-full p-4 bg-kiro-ink-950">
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

  const onlineCount = members.filter((m) => m.online).length;

  // On mobile: show room list full-screen until a room is picked,
  // then show the chat full-screen with a back button.
  // On md+ both panels are visible side by side.
  const mobileShowChat = selectedRoom !== null;

  return (
    <div className="h-full flex min-w-0">

      {/* ── Room list panel ──
          Mobile: full-screen, hidden when a room is open.
          Desktop (lg+): fixed-width sidebar, always visible. ── */}
      <section
        className={`
          flex flex-col bg-[#16162a] border-r border-white/5
          ${mobileShowChat ? 'hidden md:flex' : 'flex w-full'}
          md:w-80 xl:w-72 md:shrink-0
        `}
      >
        {/* Panel header */}
        <header className="px-4 py-4 border-b border-white/5">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-base font-semibold text-kiro-slate-100">Chats</h2>
            <div className="flex items-center gap-1">
              <Button
                onClick={() => { setCreateError(null); setShowCreateModal(true); }}
                variant="primary"
                size="sm"
                aria-label="Create new room"
              >
                +
              </Button>
              <Button onClick={loadRooms} variant="ghost" size="sm" aria-label="Refresh rooms">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" />
                </svg>
              </Button>
            </div>
          </div>
          <Input
            label="Search rooms"
            placeholder="Search…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            fullWidth
          />
        </header>

        <main className="flex-1 overflow-hidden">
          <RoomSelector
            rooms={filteredRooms}
            currentRoomId={selectedRoom?.id}
            latestMessages={latestMessages}
            onRoomSelect={handleRoomSelect}
            onRoomDelete={(room) => { setDeleteError(null); setDeleteTarget(room); }}
            canDeleteRoom={(room) => room.createdBy?.id === user?.id}
            emptyStateTitle={searchQuery.trim() ? 'No rooms match your search' : undefined}
            emptyStateDescription={searchQuery.trim() ? 'Try a different keyword or clear the filter.' : undefined}
            className="h-full"
          />
        </main>
      </section>

      {/* ── Main content area ── */}
      {selectedRoom ? (
        <>
          {/* ── Chat column ──
              Mobile: full-screen (visible when room selected).
              Desktop: flex-1 beside the room list. ── */}
          <div
            className={`
              flex-1 flex flex-col min-w-0 bg-[#13131f]
              ${mobileShowChat ? 'flex' : 'hidden md:flex'}
            `}
          >
            {/* Room header */}
            <div className="flex items-center justify-between px-4 py-3 bg-[#16162a] border-b border-white/5 shrink-0">
              <div className="flex items-center gap-3 min-w-0">
                {/* Back button — mobile only */}
                <button
                  onClick={() => setSelectedRoom(null)}
                  className="md:hidden shrink-0 p-1.5 -ml-1 text-kiro-slate-400 hover:text-kiro-slate-100 hover:bg-white/5 rounded-lg transition-colors"
                  aria-label="Back to rooms"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
                  </svg>
                </button>

                {/* Room avatar */}
                <div className="shrink-0 w-9 h-9 rounded-full bg-gradient-to-br from-kiro-purple-500 to-kiro-purple-700 flex items-center justify-center text-white font-semibold text-sm relative">
                  {selectedRoom.name.charAt(0).toUpperCase()}
                  <span className="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full bg-green-500 border-2 border-[#16162a]" aria-hidden="true" />
                </div>

                <div className="min-w-0">
                  <h2 className="text-sm font-semibold text-kiro-slate-100 truncate leading-tight">
                    {selectedRoom.name}
                  </h2>
                  <p className="text-xs text-green-400 leading-tight">
                    {onlineCount > 0 ? `${onlineCount} online` : 'No one online'}
                  </p>
                </div>
              </div>

              {/* Header actions */}
              <div className="flex items-center gap-1 shrink-0">
                {/* Members toggle */}
                <button
                  onClick={() => setShowMembersOnMobile(!showMembersOnMobile)}
                  className="xl:hidden p-2 text-kiro-slate-400 hover:text-kiro-slate-100 hover:bg-white/5 rounded-lg transition-colors"
                  aria-label="Toggle member list"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
                  </svg>
                </button>

                {/* Invite member */}
                <button
                  onClick={() => handleOpenInvite(selectedRoom)}
                  className="p-2 text-kiro-slate-400 hover:text-kiro-slate-100 hover:bg-white/5 rounded-lg transition-colors"
                  aria-label="Invite member"
                  title="Invite member"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M18 7.5v3m0 0v3m0-3h3m-3 0h-3m-2.25-4.125a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0ZM3 19.235v-.11a6.375 6.375 0 0 1 12.75 0v.109A12.318 12.318 0 0 1 9.374 21c-2.331 0-4.512-.645-6.374-1.766Z" />
                  </svg>
                </button>

                {/* Delete room (owner only) */}
                {user?.id === selectedRoom.createdBy?.id && (
                  <button
                    onClick={() => { setDeleteError(null); setDeleteTarget(selectedRoom); }}
                    className="p-2 text-kiro-slate-400 hover:text-red-400 hover:bg-red-900/20 rounded-lg transition-colors"
                    aria-label="Delete room"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 12a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0ZM12.75 12a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0ZM18.75 12a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" />
                    </svg>
                  </button>
                )}
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-hidden">
              {roomLoading ? (
                <ChatMessagesSkeleton />
              ) : (
                <MessageList messages={messages} currentUserId={user?.id} className="h-full" />
              )}
            </div>

            {/* Message input */}
            <div className="px-3 py-3 bg-[#16162a] border-t border-white/5 shrink-0">
              <MessageInput
                onSend={handleSendMessage}
                disabled={!connected || roomLoading}
                placeholder={connected ? `Message #${selectedRoom.name}…` : 'Connecting…'}
              />
            </div>
          </div>

          {/* ── Members sidebar (desktop xl+) ── */}
          <aside className="hidden xl:flex flex-col w-60 shrink-0 bg-[#16162a] border-l border-white/5 overflow-y-auto">
            <UserList users={members} currentUserId={user?.id} />
          </aside>

          {/* ── Members / Friends drawer (mobile overlay) ── */}
          {showMembersOnMobile && (
            <div className="xl:hidden fixed inset-0 bg-black/70 z-50" onClick={() => setShowMembersOnMobile(false)}>
              <div className="absolute right-0 top-0 bottom-0 w-72 max-w-[85vw] bg-[#16162a] shadow-xl" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center justify-between px-4 py-3 border-b border-white/5">
                  <h3 className="text-base font-semibold text-kiro-slate-100">Members</h3>
                  <button
                    onClick={() => setShowMembersOnMobile(false)}
                    className="p-1.5 text-kiro-slate-400 hover:text-kiro-slate-100 hover:bg-white/5 rounded-lg transition-colors"
                    aria-label="Close member list"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
                <UserList users={members} currentUserId={user?.id} />
              </div>
            </div>
          )}
        </>
      ) : (
        <>
          {/* ── Select-a-room placeholder (desktop only) ── */}
          <div className="hidden md:flex flex-1 items-center justify-center bg-[#13131f]">
            <div className="text-center px-6">
              <div className="w-16 h-16 rounded-2xl bg-white/5 flex items-center justify-center mx-auto mb-4">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-8 h-8 text-kiro-slate-500">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M8.625 12a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H8.25m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H12m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 0 1-2.555-.337A5.972 5.972 0 0 1 5.41 20.97a5.969 5.969 0 0 1-.474-.065 4.48 4.48 0 0 0 .978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25Z" />
                </svg>
              </div>
              <p className="text-kiro-slate-300 font-semibold text-lg mb-1">Select a room</p>
              <p className="text-kiro-slate-500 text-sm">Choose a room from the list to start chatting</p>
            </div>
          </div>
        </>
      )}

      {/* ── Invite Member modal ── */}
      <Modal
        open={showInviteModal}
        title={`Invite to ${inviteRoomTarget?.name ?? 'room'}`}
        onClose={() => setShowInviteModal(false)}
        footer={
          <Button variant="secondary" onClick={() => setShowInviteModal(false)}>
            Close
          </Button>
        }
      >
        <div className="space-y-4">
          <Input
            label="Search users"
            placeholder="Type username or email…"
            value={inviteSearchQuery}
            onChange={(e) => handleInviteSearch(e.target.value)}
            fullWidth
          />

          {inviteSearchLoading ? (
            <div className="flex items-center justify-center py-6" role="status" aria-live="polite">
              <div className="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-kiro-purple-400" aria-hidden="true" />
            </div>
          ) : inviteSearchQuery.trim().length < 2 ? (
            <p className="text-sm text-kiro-slate-400 text-center py-4">
              Type at least 2 characters to search
            </p>
          ) : inviteSearchResults.length === 0 ? (
            <p className="text-sm text-kiro-slate-400 text-center py-4">
              No users found
            </p>
          ) : (
            <ul className="divide-y divide-white/5 -mx-4 max-h-64 overflow-y-auto">
              {inviteSearchResults.map((result) => (
                <li key={result.user.id} className="flex items-center justify-between gap-3 px-4 py-3 hover:bg-white/5 transition-colors">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-kiro-slate-100 truncate">{result.user.username}</p>
                    {result.user.displayName && (
                      <p className="text-xs text-kiro-slate-400 truncate">{result.user.displayName}</p>
                    )}
                  </div>
                  {inviteSuccess[result.user.id] ? (
                    <span className="text-xs text-green-400 font-medium">Invited ✓</span>
                  ) : (
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => handleInviteUser(result.user.id)}
                      disabled={invitingUserId === result.user.id}
                      aria-label={`Invite ${result.user.username}`}
                    >
                      {invitingUserId === result.user.id ? 'Inviting…' : 'Invite'}
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </Modal>

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
        <p className="text-sm text-kiro-slate-200">
          Deleting <span className="font-semibold text-kiro-slate-100">{deleteTarget?.name}</span> will remove all
          messages and memberships. This action cannot be undone.
        </p>
        {deleteError && (
          <p className="mt-3 text-sm text-red-400" role="alert">
            {deleteError}
          </p>
        )}
      </Modal>
    </div>
  );
}

/**
 * Skeleton loader for the chat messages area.
 * Mimics the real message layout with shimmer animation while room data loads.
 */
function ChatMessagesSkeleton() {
  // A mix of "other" and "self" bubble shapes to feel natural
  const rows: Array<{ self: boolean; lines: number[]; avatarWidth: number }> = [
    { self: false, lines: [140, 90],        avatarWidth: 40 },
    { self: true,  lines: [200],            avatarWidth: 40 },
    { self: false, lines: [80, 160, 60],    avatarWidth: 40 },
    { self: true,  lines: [120, 80],        avatarWidth: 40 },
    { self: false, lines: [100],            avatarWidth: 40 },
    { self: true,  lines: [180, 110, 70],   avatarWidth: 40 },
    { self: false, lines: [130, 50],        avatarWidth: 40 },
    { self: true,  lines: [90],             avatarWidth: 40 },
  ];

  return (
    <div
      className="h-full overflow-hidden flex flex-col-reverse px-4 py-4 gap-5 bg-[#13131f]"
      role="status"
      aria-label="Loading messages"
      aria-live="polite"
    >
      {rows.map((row, i) => (
        <div
          key={i}
          className={`flex items-end gap-3 ${row.self ? 'flex-row-reverse' : 'flex-row'}`}
        >
          {/* Avatar bone */}
          <div
            className="shrink-0 rounded-full bg-white/[0.06] animate-pulse"
            style={{ width: row.avatarWidth, height: row.avatarWidth }}
          />

          {/* Bubble group */}
          <div className={`flex flex-col gap-1.5 max-w-[60%] ${row.self ? 'items-end' : 'items-start'}`}>
            {/* Sender name bone (only for others) */}
            {!row.self && (
              <div className="h-3 w-20 rounded-full bg-white/[0.06] animate-pulse" />
            )}

            {/* Message bubble bones */}
            {row.lines.map((width, j) => (
              <div
                key={j}
                className={`h-9 rounded-2xl bg-white/[0.06] animate-pulse ${
                  row.self ? 'rounded-br-sm' : 'rounded-bl-sm'
                }`}
                style={{ width }}
              />
            ))}

            {/* Timestamp bone */}
            <div className="h-2.5 w-12 rounded-full bg-white/[0.04] animate-pulse" />
          </div>
        </div>
      ))}
    </div>
  );
}
