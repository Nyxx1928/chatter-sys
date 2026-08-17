'use client';

import { useCallback, useEffect, useState } from 'react';
import { ChatRoom, Message, User } from '@/types/domain';
import { RoomSelector } from '@/components/chat/RoomSelector';
import { RoomCreateModal } from '@/components/chat/RoomCreateModal';
import { MessageList } from '@/components/chat/MessageList';
import { MessageInput } from '@/components/chat/MessageInput';
import { UserList } from '@/components/chat/UserList';
import { Button, Input, Modal } from '@/components/ui';
import {
  createRoom,
  deleteRoom,
  getRoomDetails,
  getRoomMembers,
  inviteToRoom,
  listRooms,
} from '@/lib/api/rooms';
import { searchUsers } from '@/lib/api/users';
import { getMessageHistory } from '@/lib/api/messages';
import { useAuthStore } from '@/lib/store/authStore';
import { useConnectionStore } from '@/lib/store/connectionStore';
import { usePresenceStore } from '@/lib/store/presenceStore';
import { useStompSubscription } from '@/lib/stomp/hooks';

/**
 * Channels page — self-contained GROUP room browser with inline chat.
 * Clicking a room opens the chat view on this page (no redirect to /chat).
 * Includes invite-member and delete-room controls for GROUP rooms.
 */
export default function ChannelsPage() {
  const { token, user } = useAuthStore();
  const { connected, sendMessage } = useConnectionStore();
  const { updatePresence } = usePresenceStore();

  // ── Room list state ──────────────────────────────────────────────────────────
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [latestMessages, setLatestMessages] = useState<Record<number, Message>>({});

  // ── Create room ──────────────────────────────────────────────────────────────
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  // ── Delete room ──────────────────────────────────────────────────────────────
  const [deleteTarget, setDeleteTarget] = useState<ChatRoom | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // ── Invite member ────────────────────────────────────────────────────────────
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteSearchQuery, setInviteSearchQuery] = useState('');
  const [inviteSearchResults, setInviteSearchResults] = useState<import('@/types/domain').UserSearchResult[]>([]);
  const [inviteSearchLoading, setInviteSearchLoading] = useState(false);
  const [invitingUserId, setInvitingUserId] = useState<number | null>(null);
  const [inviteSuccess, setInviteSuccess] = useState<Record<number, boolean>>({});

  // ── Selected room / chat state ───────────────────────────────────────────────
  const [selectedRoom, setSelectedRoom] = useState<ChatRoom | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [members, setMembers] = useState<User[]>([]);
  const [roomLoading, setRoomLoading] = useState(false);
  const [showMembersOnMobile, setShowMembersOnMobile] = useState(false);

  // ── Data fetching ────────────────────────────────────────────────────────────

  const fetchRooms = useCallback(async () => {
    if (!token) return [] as ChatRoom[];
    const all = await listRooms(token);
    return all.filter((r) => r.roomType === 'GROUP');
  }, [token]);

  const loadRooms = useCallback(async () => {
    if (!token) return;
    try {
      setLoading(true);
      setError(null);
      setRooms(await fetchRooms());
    } catch {
      setError('Failed to load channels. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [token, fetchRooms]);

  useEffect(() => {
    if (!token) return;
    let active = true;
    fetchRooms()
      .then((list) => { if (active) setRooms(list); })
      .catch(() => { if (active) setError('Failed to load channels. Please try again.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [token, fetchRooms]);

  // ── Room selection ───────────────────────────────────────────────────────────

  const handleRoomSelect = async (room: ChatRoom) => {
    if (!token) return;
    if (selectedRoom && connected && user) {
      sendMessage(`/app/room.leave/${selectedRoom.id}`, {});
    }
    setSelectedRoom(room);
    setMessages([]);
    setMembers([]);
    setRoomLoading(true);
    setShowMembersOnMobile(false);
    try {
      const [details, history, roomMembers] = await Promise.all([
        getRoomDetails(token, room.id),
        getMessageHistory(token, room.id, { page: 0, size: 50 }),
        getRoomMembers(token, room.id),
      ]);
      setSelectedRoom(details);
      setMessages([...(history?.content ?? [])].reverse());
      setMembers(roomMembers);
    } catch {
      // non-fatal — room header still shows
    } finally {
      setRoomLoading(false);
    }
    if (connected && user) sendMessage(`/app/room.join/${room.id}`, {});
  };

  // Leave room on unmount
  useEffect(() => {
    return () => {
      if (selectedRoom && connected && user) {
        sendMessage(`/app/room.leave/${selectedRoom.id}`, {});
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedRoom?.id, connected]);

  // ── STOMP subscriptions ──────────────────────────────────────────────────────

  const handleNewMessage = useCallback((msg: Message) => {
    setMessages((prev) => [...prev, msg]);
    setLatestMessages((prev) => ({ ...prev, [msg.chatRoomId]: msg }));
  }, []);
  useStompSubscription<Message>(
    connected && selectedRoom ? `/topic/room/${selectedRoom.id}` : null,
    handleNewMessage,
  );

  const handlePresenceUpdate = useCallback(
    (update: { userId: number; online: boolean }) => {
      setMembers((prev) =>
        prev.map((m) => (m.id === update.userId ? { ...m, online: update.online } : m)),
      );
      updatePresence(update.userId, update.online);
    },
    [updatePresence],
  );
  useStompSubscription<{ userId: number; online: boolean }>(
    connected && selectedRoom ? `/topic/presence/${selectedRoom.id}` : null,
    handlePresenceUpdate,
  );

  const handleGlobalMessage = useCallback((msg: Message) => {
    setLatestMessages((prev) => ({ ...prev, [msg.chatRoomId]: msg }));
  }, []);
  useStompSubscription<Message>(
    connected && rooms.length > 0 ? `/topic/rooms` : null,
    handleGlobalMessage,
  );

  // ── Messaging ────────────────────────────────────────────────────────────────

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

  // ── Create room ──────────────────────────────────────────────────────────────

  const handleCreateRoom = async (name: string, description: string) => {
    if (!token) return;
    try {
      setIsCreating(true);
      setCreateError(null);
      const newRoom = await createRoom(token, { name, description: description || undefined });
      await loadRooms();
      setShowCreateModal(false);
      void handleRoomSelect(newRoom);
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create room.');
    } finally {
      setIsCreating(false);
    }
  };

  // ── Delete room ──────────────────────────────────────────────────────────────

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
      setDeleteError(err instanceof Error ? err.message : 'Failed to delete room.');
    } finally {
      setIsDeleting(false);
    }
  };

  // ── Invite member ────────────────────────────────────────────────────────────

  const handleOpenInvite = () => {
    setInviteSearchQuery('');
    setInviteSearchResults([]);
    setInviteSuccess({});
    setShowInviteModal(true);
  };

  const handleInviteSearch = useCallback(
    async (query: string) => {
      setInviteSearchQuery(query);
      if (!token || query.trim().length < 2) { setInviteSearchResults([]); return; }
      setInviteSearchLoading(true);
      try {
        const results = await searchUsers(token, query.trim());
        const memberIds = new Set(members.map((m) => m.id));
        setInviteSearchResults(results.filter((r) => !memberIds.has(r.user.id)));
      } catch { /* non-fatal */ }
      finally { setInviteSearchLoading(false); }
    },
    [token, members],
  );

  const handleInviteUser = async (inviteeId: number) => {
    if (!token || !selectedRoom) return;
    setInvitingUserId(inviteeId);
    try {
      await inviteToRoom(token, selectedRoom.id, inviteeId);
      setInviteSuccess((prev) => ({ ...prev, [inviteeId]: true }));
      const updated = await getRoomMembers(token, selectedRoom.id);
      setMembers(updated);
    } catch { /* non-fatal */ }
    finally { setInvitingUserId(null); }
  };

  // ── Derived state ────────────────────────────────────────────────────────────

  const filteredRooms = rooms.filter((room) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return room.name.toLowerCase().includes(q) || (room.description ?? '').toLowerCase().includes(q);
  });

  const onlineCount = members.filter((m) => m.online).length;
  const mobileShowChat = selectedRoom !== null;
  const isOwner = selectedRoom && user && selectedRoom.createdBy?.id === user.id;

  // ── Loading / error states ───────────────────────────────────────────────────

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full bg-slack-surface-secondary" role="status" aria-live="polite">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-slack-primary mb-4" aria-hidden="true" />
          <p className="text-slack-text-secondary">Loading channels…</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-full p-4 bg-slack-surface-secondary">
        <div className="text-center max-w-md" role="alert">
          <p className="text-slack-accent-red font-medium mb-4">{error}</p>
          <Button onClick={loadRooms} variant="primary">Try Again</Button>
        </div>
      </div>
    );
  }

  // ── Main render ──────────────────────────────────────────────────────────────

  return (
    <div className="h-full flex min-w-0">

      {/* ── Channel list panel ──
          Mobile: full-screen, hidden when a room is open.
          Desktop (md+): fixed-width sidebar, always visible. ── */}
      <section
        className={`
          flex flex-col bg-slack-surface-secondary border-r border-slack-border
          ${mobileShowChat ? 'hidden md:flex' : 'flex w-full'}
          md:w-80 xl:w-72 md:shrink-0
        `}
        aria-label="Channel list"
      >
        {/* Panel header */}
        <header className="px-4 py-4 border-b border-slack-border">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-base font-semibold text-slack-text-primary">Channels</h2>
            <div className="flex items-center gap-1">
              <Button
                onClick={() => setShowCreateModal(true)}
                variant="ghost"
                size="sm"
                aria-label="Create new channel"
                title="Create channel"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
              </Button>
              <Button onClick={loadRooms} variant="ghost" size="sm" aria-label="Refresh channels">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" />
                </svg>
              </Button>
            </div>
          </div>
          <Input
            label="Search channels"
            placeholder="Search channels…"
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
            canDeleteRoom={(room) => Boolean(room.createdBy?.id === user?.id)}
            emptyStateTitle={searchQuery.trim() ? 'No channels match your search' : 'No channels yet'}
            emptyStateDescription={searchQuery.trim() ? 'Try a different keyword.' : 'Create a channel to get started.'}
            className="h-full"
          />
        </main>
      </section>

      {/* ── Main content area ── */}
      {selectedRoom ? (
        <>
          {/* ── Chat column ── */}
          <div
            className={`
              flex-1 flex flex-col min-w-0 bg-slack-surface-secondary
              ${mobileShowChat ? 'flex' : 'hidden md:flex'}
            `}
          >
            {/* Room header */}
            <div className="flex items-center justify-between px-4 py-3 bg-slack-surface-primary border-b border-slack-border shrink-0">
              <div className="flex items-center gap-3 min-w-0">
                {/* Back button — mobile only */}
                <button
                  onClick={() => setSelectedRoom(null)}
                  className="md:hidden shrink-0 p-1.5 -ml-1 text-slack-text-secondary hover:text-slack-text-primary hover:bg-slack-surface-tertiary rounded-lg transition-colors"
                  aria-label="Back to channels"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
                  </svg>
                </button>

                {/* Room avatar */}
                <div className="shrink-0 w-9 h-9 rounded-full bg-slack-primary flex items-center justify-center text-slack-text-inverse font-semibold text-sm">
                  {selectedRoom.name.charAt(0).toUpperCase()}
                </div>

                <div className="min-w-0">
                  <h2 className="text-sm font-semibold text-slack-text-primary truncate leading-tight">
                    {selectedRoom.name}
                  </h2>
                  <p className="text-xs text-slack-accent-green leading-tight">
                    {onlineCount > 0 ? `${onlineCount} online` : `${members.length} members`}
                  </p>
                </div>
              </div>

              {/* Header actions */}
              <div className="flex items-center gap-1 shrink-0">
                {/* Invite button — visible to room owner */}
                {isOwner && (
                  <button
                    onClick={handleOpenInvite}
                    className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slack-primary hover:text-slack-primary/80 hover:bg-slack-primary/20 rounded-lg transition-colors border border-slack-primary/30 hover:border-slack-primary/50"
                    aria-label="Invite members"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M18 7.5v3m0 0v3m0-3h3m-3 0h-3m-2.25-4.125a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0ZM3 19.235v-.11a6.375 6.375 0 0 1 12.75 0v.109A12.318 12.318 0 0 1 9.374 21c-2.331 0-4.512-.645-6.374-1.766Z" />
                    </svg>
                    <span className="hidden sm:inline">Invite</span>
                  </button>
                )}

                {/* Members toggle */}
                <button
                  onClick={() => setShowMembersOnMobile(!showMembersOnMobile)}
                  className="xl:hidden p-2 text-slack-text-secondary hover:text-slack-text-primary hover:bg-slack-surface-tertiary rounded-lg transition-colors"
                  aria-label="Toggle member list"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
                  </svg>
                </button>
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-hidden">
              {roomLoading ? (
                <ChannelMessagesSkeleton />
              ) : (
                <MessageList messages={messages} currentUserId={user?.id} className="h-full" />
              )}
            </div>

            {/* Message input */}
            <div className="px-3 py-3 bg-slack-surface-primary border-t border-slack-border shrink-0">
              <MessageInput
                onSend={handleSendMessage}
                disabled={!connected || roomLoading}
                placeholder={connected ? `Message #${selectedRoom.name}…` : 'Connecting…'}
              />
            </div>
          </div>

          {/* ── Members sidebar (desktop xl+) ── */}
          <aside className="hidden xl:flex flex-col w-60 shrink-0 bg-slack-surface-secondary border-l border-slack-border overflow-y-auto">
            {/* Invite button in sidebar for owner */}
            {isOwner && (
              <div className="px-3 pt-3">
                <button
                  onClick={handleOpenInvite}
                  className="w-full flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium text-slack-primary hover:text-slack-primary/80 bg-slack-primary/10 hover:bg-slack-primary/20 rounded-xl transition-colors border border-slack-primary/20 hover:border-slack-primary/40"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M18 7.5v3m0 0v3m0-3h3m-3 0h-3m-2.25-4.125a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0ZM3 19.235v-.11a6.375 6.375 0 0 1 12.75 0v.109A12.318 12.318 0 0 1 9.374 21c-2.331 0-4.512-.645-6.374-1.766Z" />
                  </svg>
                  Invite Members
                </button>
              </div>
            )}
            <UserList users={members} currentUserId={user?.id} />
          </aside>

          {/* ── Members drawer (mobile overlay) ── */}
          {showMembersOnMobile && (
            <div
              className="xl:hidden fixed inset-0 bg-black/70 z-50"
              onClick={() => setShowMembersOnMobile(false)}
            >
              <div
                className="absolute right-0 top-0 bottom-0 w-72 max-w-[85vw] bg-slack-surface-secondary shadow-xl flex flex-col"
                onClick={(e) => e.stopPropagation()}
              >
                <div className="flex items-center justify-between px-4 py-3 border-b border-slack-border">
                  <h3 className="text-base font-semibold text-slack-text-primary">Members</h3>
                  <button
                    onClick={() => setShowMembersOnMobile(false)}
                    className="p-1.5 text-slack-text-secondary hover:text-slack-text-primary hover:bg-slack-surface-tertiary rounded-lg transition-colors"
                    aria-label="Close member list"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
                {isOwner && (
                  <div className="px-3 pt-3">
                    <button
                      onClick={() => { setShowMembersOnMobile(false); handleOpenInvite(); }}
                      className="w-full flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium text-slack-primary hover:text-slack-primary/80 bg-slack-primary/10 hover:bg-slack-primary/20 rounded-xl transition-colors border border-slack-primary/20"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4" aria-hidden="true">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M18 7.5v3m0 0v3m0-3h3m-3 0h-3m-2.25-4.125a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0ZM3 19.235v-.11a6.375 6.375 0 0 1 12.75 0v.109A12.318 12.318 0 0 1 9.374 21c-2.331 0-4.512-.645-6.374-1.766Z" />
                      </svg>
                      Invite Members
                    </button>
                  </div>
                )}
                <UserList users={members} currentUserId={user?.id} />
              </div>
            </div>
          )}
        </>
      ) : (
        /* ── Select-a-channel placeholder (desktop only) ── */
        <div className="hidden md:flex flex-1 items-center justify-center bg-slack-surface-secondary">
          <div className="text-center px-6">
            <div className="w-16 h-16 rounded-2xl bg-slack-surface-tertiary flex items-center justify-center mx-auto mb-4">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-8 h-8 text-slack-text-secondary">
                <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z" />
              </svg>
            </div>
            <p className="text-slack-text-primary font-semibold text-lg mb-1">Pick a channel</p>
            <p className="text-slack-text-secondary text-sm">Select a channel from the list to start chatting</p>
          </div>
        </div>
      )}

      {/* ── Create room modal ── */}
      <RoomCreateModal
        open={showCreateModal}
        onClose={() => { setShowCreateModal(false); setCreateError(null); }}
        onCreate={handleCreateRoom}
        isSubmitting={isCreating}
        errorMessage={createError}
      />

      {/* ── Delete room confirmation modal ── */}
      <Modal
        open={Boolean(deleteTarget)}
        title="Delete channel"
        onClose={() => setDeleteTarget(null)}
        footer={
          <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
            <Button variant="secondary" onClick={() => setDeleteTarget(null)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={handleDeleteRoom} disabled={isDeleting}>
              {isDeleting ? 'Deleting…' : 'Delete channel'}
            </Button>
          </div>
        }
      >
        <p className="text-sm text-slack-text-primary">
          Deleting <span className="font-semibold text-slack-text-primary">{deleteTarget?.name}</span> will
          remove all messages and memberships. This cannot be undone.
        </p>
        {deleteError && (
          <p className="mt-3 text-sm text-slack-accent-red" role="alert">{deleteError}</p>
        )}
      </Modal>

      {/* ── Invite member modal ── */}
      <Modal
        open={showInviteModal}
        title={`Invite to #${selectedRoom?.name ?? 'channel'}`}
        onClose={() => setShowInviteModal(false)}
      >
        <div className="flex flex-col gap-4">
          <Input
            label="Search users"
            placeholder="Search by username or display name…"
            value={inviteSearchQuery}
            onChange={(e) => handleInviteSearch(e.target.value)}
            fullWidth
            autoFocus
          />

          {inviteSearchLoading && (
            <p className="text-sm text-slack-text-secondary text-center py-2">Searching…</p>
          )}

          {!inviteSearchLoading && inviteSearchResults.length === 0 && inviteSearchQuery.trim().length >= 2 && (
            <p className="text-sm text-slack-text-secondary text-center py-2">No users found.</p>
          )}

          {inviteSearchResults.length > 0 && (
            <ul className="flex flex-col gap-1 max-h-64 overflow-y-auto" role="list">
              {inviteSearchResults.map((result) => {
                const alreadyInvited = inviteSuccess[result.user.id];
                const isInviting = invitingUserId === result.user.id;
                return (
                  <li
                    key={result.user.id}
                    className="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-slack-surface-tertiary transition-colors"
                  >
                    <div className="w-9 h-9 rounded-full bg-slack-primary flex items-center justify-center text-slack-text-inverse font-semibold text-sm shrink-0">
                      {result.user.displayName.charAt(0).toUpperCase()}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-slack-text-primary truncate">{result.user.displayName}</p>
                      <p className="text-xs text-slack-text-secondary truncate">@{result.user.username}</p>
                    </div>
                    <Button
                      variant={alreadyInvited ? 'secondary' : 'primary'}
                      size="sm"
                      disabled={alreadyInvited || isInviting}
                      onClick={() => handleInviteUser(result.user.id)}
                    >
                      {alreadyInvited ? 'Invited ✓' : isInviting ? 'Inviting…' : 'Invite'}
                    </Button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </Modal>
    </div>
  );
}

// ── Skeleton loader ────────────────────────────────────────────────────────────

function ChannelMessagesSkeleton() {
  const rows: Array<{ self: boolean; lines: number[] }> = [
    { self: false, lines: [140, 90] },
    { self: true,  lines: [200] },
    { self: false, lines: [80, 160, 60] },
    { self: true,  lines: [120, 80] },
    { self: false, lines: [100] },
    { self: true,  lines: [180, 110, 70] },
    { self: false, lines: [130, 50] },
    { self: true,  lines: [90] },
  ];

  return (
    <div
      className="h-full overflow-hidden flex flex-col-reverse px-4 py-4 gap-5 bg-slack-surface-primary"
      role="status"
      aria-label="Loading messages"
      aria-live="polite"
    >
      {rows.map((row, i) => (
        <div key={i} className={`flex items-end gap-3 ${row.self ? 'flex-row-reverse' : 'flex-row'}`}>
          <div className="shrink-0 w-10 h-10 rounded-full bg-white/[0.06] animate-pulse" />
          <div className={`flex flex-col gap-1.5 max-w-[60%] ${row.self ? 'items-end' : 'items-start'}`}>
            {!row.self && <div className="h-3 w-20 rounded-full bg-white/[0.06] animate-pulse" />}
            {row.lines.map((width, j) => (
              <div
                key={j}
                className={`h-9 rounded-2xl bg-white/[0.06] animate-pulse ${row.self ? 'rounded-br-sm' : 'rounded-bl-sm'}`}
                style={{ width }}
              />
            ))}
            <div className="h-2.5 w-12 rounded-full bg-white/[0.04] animate-pulse" />
          </div>
        </div>
      ))}
    </div>
  );
}
