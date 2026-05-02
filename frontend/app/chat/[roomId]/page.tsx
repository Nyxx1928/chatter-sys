'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { Message, User, ChatRoom } from '@/types/domain';
import { MessageList } from '@/components/chat/MessageList';
import { MessageInput } from '@/components/chat/MessageInput';
import { UserList } from '@/components/chat/UserList';
import { Button, Modal } from '@/components/ui';
import { useAuthStore } from '@/lib/store/authStore';
import { useConnectionStore } from '@/lib/store/connectionStore';
import { useStompSubscription } from '@/lib/stomp/hooks';
import { getMessageHistory } from '@/lib/api/messages';
import { deleteRoom, getRoomDetails, getRoomMembers } from '@/lib/api/rooms';

/**
 * Individual chat room page.
 * Displays MessageList, MessageInput, and UserList for a specific room.
 * Handles real-time message subscriptions and presence updates.
 * 
 * Requirements: 5.1, 14.2, 15.1, 15.2, 15.3, 15.4
 */
export default function ChatRoomPage() {
  const router = useRouter();
  const params = useParams();
  const roomId = params.roomId as string;
  const { user, token } = useAuthStore();
  const { connected, sendMessage } = useConnectionStore();

  const [room, setRoom] = useState<ChatRoom | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [members, setMembers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showUserList, setShowUserList] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Load room data on mount and send join message
  useEffect(() => {
    loadRoomData();
    
    // Send room join message when connected
    if (connected && user) {
      sendMessage(`/app/room.join/${roomId}`, {});
    }

    // Send room leave message on unmount
    return () => {
      if (connected && user) {
        sendMessage(`/app/room.leave/${roomId}`, {});
      }
    };
  }, [roomId, token, connected, user, sendMessage]);

  const loadRoomData = async () => {
    if (!token) return;

    try {
      setLoading(true);
      setError(null);

      // Load room details, messages, and members in parallel
      const [roomDetails, messageHistory, roomMembers] = await Promise.all([
        getRoomDetails(token, parseInt(roomId)),
        getMessageHistory(token, parseInt(roomId), { page: 0, size: 50 }),
        getRoomMembers(token, parseInt(roomId))
      ]);

      setRoom(roomDetails);
      setMessages(messageHistory?.content ?? []);
      setMembers(roomMembers);
    } catch (err) {
      console.error('Failed to load room data:', err);
      setError('Failed to load chat room. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Subscribe to room messages
  const handleNewMessage = useCallback((message: Message) => {
    setMessages((prev) => [...prev, message]);
  }, []);

  useStompSubscription<Message>(
    connected ? `/topic/room/${roomId}` : null,
    handleNewMessage
  );

  // Subscribe to presence updates
  const handlePresenceUpdate = useCallback((update: { userId: number; online: boolean }) => {
    setMembers((prev) =>
      prev.map((member) =>
        member.id === update.userId
          ? { ...member, online: update.online }
          : member
      )
    );
  }, []);

  useStompSubscription<{ userId: number; online: boolean }>(
    connected ? `/topic/presence/${roomId}` : null,
    handlePresenceUpdate
  );

  // Send message handler
  const handleSendMessage = (content: string) => {
    if (!connected || !user) {
      return;
    }

    sendMessage(`/app/chat.send/${roomId}`, {
      content,
      senderId: user.id,
      senderUsername: user.username,
      senderDisplayName: user.displayName,
      chatRoomId: parseInt(roomId),
      timestamp: new Date().toISOString()
    });
  };

  const handleDeleteRoom = async () => {
    if (!token) return;

    try {
      setIsDeleting(true);
      setDeleteError(null);
      await deleteRoom(token, parseInt(roomId));
      setShowDeleteModal(false);
      router.push('/chat');
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
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4" />
          <p className="text-gray-600">Loading chat room...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error || !room) {
    return (
      <div className="flex items-center justify-center h-full p-4">
        <div className="text-center max-w-md">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
            className="w-16 h-16 mx-auto text-red-500 mb-4"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"
            />
          </svg>
          <p className="text-red-600 font-medium mb-4">
            {error || 'Room not found'}
          </p>
          <Link href="/chat">
            <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
              Back to Rooms
            </button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col lg:flex-row">
      {/* Main chat area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Room header */}
        <div className="bg-white border-b border-gray-200 px-4 py-3 sm:px-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3 min-w-0">
              {/* Back button (mobile) */}
              <Link
                href="/chat"
                className="lg:hidden shrink-0 p-2 -ml-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                aria-label="Back to rooms"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  className="w-6 h-6"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M15.75 19.5L8.25 12l7.5-7.5"
                  />
                </svg>
              </Link>

              <div className="min-w-0">
                <h2 className="text-lg font-semibold text-gray-900 truncate">
                  {room.name}
                </h2>
                {room.description && (
                  <p className="text-sm text-gray-600 truncate">
                    {room.description}
                  </p>
                )}
              </div>
            </div>

            <div className="flex items-center gap-2">
              {user?.id === room.createdBy?.id && (
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => setShowDeleteModal(true)}
                >
                  Delete room
                </Button>
              )}
              {/* Toggle user list (mobile/tablet) */}
              <button
                onClick={() => setShowUserList(!showUserList)}
                className="lg:hidden p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                aria-label="Toggle user list"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  className="w-6 h-6"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z"
                  />
                </svg>
              </button>
            </div>
          </div>
        </div>

        {/* Messages area */}
        <div className="flex-1 overflow-hidden bg-white">
          <MessageList
            messages={messages}
            currentUserId={user?.id}
            className="h-full"
          />
        </div>

        {/* Message input */}
        <div className="bg-white border-t border-gray-200 p-4">
          <MessageInput
            onSend={handleSendMessage}
            disabled={!connected}
            placeholder={
              connected
                ? 'Type a message...'
                : 'Connecting to chat server...'
            }
          />
        </div>
      </div>

      {/* User list sidebar (desktop) */}
      <aside className="hidden lg:block w-80 border-l border-gray-200 bg-white">
        <UserList users={members} currentUserId={user?.id} />
      </aside>

      {/* User list modal (mobile/tablet) */}
      {showUserList && (
        <div
          className="lg:hidden fixed inset-0 bg-black bg-opacity-50 z-50"
          onClick={() => setShowUserList(false)}
        >
          <div
            className="absolute right-0 top-0 bottom-0 w-80 max-w-full bg-white shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">Members</h3>
              <button
                onClick={() => setShowUserList(false)}
                className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                aria-label="Close user list"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  className="w-6 h-6"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
            <UserList users={members} currentUserId={user?.id} />
          </div>
        </div>
      )}

      <Modal
        open={showDeleteModal}
        title="Delete room"
        onClose={() => setShowDeleteModal(false)}
        footer={
          <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
            <Button variant="secondary" onClick={() => setShowDeleteModal(false)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={handleDeleteRoom} disabled={isDeleting}>
              {isDeleting ? 'Deleting...' : 'Delete room'}
            </Button>
          </div>
        }
      >
        <p className="text-sm text-gray-600">
          Deleting <span className="font-semibold text-gray-900">{room.name}</span> will remove all
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
