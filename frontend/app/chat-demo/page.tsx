'use client';

import { useState } from 'react';
import { MessageList, MessageInput, UserList, RoomSelector } from '@/components/chat';
import { Message, MessageType, User, ChatRoom } from '@/types/domain';

/**
 * Demo page for chat UI components.
 * This page demonstrates the MessageList, MessageInput, UserList, and RoomSelector components.
 */
export default function ChatDemoPage() {
  // Mock data
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 1,
      senderId: 1,
      senderUsername: 'alice',
      senderDisplayName: 'Alice Johnson',
      chatRoomId: 1,
      content: 'Hey everyone! Welcome to the chat demo.',
      timestamp: new Date(Date.now() - 3600000).toISOString(),
      messageType: MessageType.TEXT
    },
    {
      id: 2,
      senderId: 2,
      senderUsername: 'bob',
      senderDisplayName: 'Bob Smith',
      chatRoomId: 1,
      content: 'Thanks Alice! This looks great.',
      timestamp: new Date(Date.now() - 3000000).toISOString(),
      messageType: MessageType.TEXT
    },
    {
      id: 3,
      senderId: 3,
      senderUsername: 'charlie',
      senderDisplayName: 'Charlie Brown',
      chatRoomId: 1,
      content: 'Charlie Brown joined the room',
      timestamp: new Date(Date.now() - 1800000).toISOString(),
      messageType: MessageType.JOIN
    },
    {
      id: 4,
      senderId: 3,
      senderUsername: 'charlie',
      senderDisplayName: 'Charlie Brown',
      chatRoomId: 1,
      content: 'Hi everyone! Excited to be here.',
      timestamp: new Date(Date.now() - 1200000).toISOString(),
      messageType: MessageType.TEXT
    }
  ]);

  const users: User[] = [
    {
      id: 1,
      username: 'alice',
      email: 'alice@example.com',
      displayName: 'Alice Johnson',
      createdAt: new Date(Date.now() - 86400000 * 30).toISOString(),
      lastSeen: new Date().toISOString(),
      online: true
    },
    {
      id: 2,
      username: 'bob',
      email: 'bob@example.com',
      displayName: 'Bob Smith',
      createdAt: new Date(Date.now() - 86400000 * 20).toISOString(),
      lastSeen: new Date().toISOString(),
      online: true
    },
    {
      id: 3,
      username: 'charlie',
      email: 'charlie@example.com',
      displayName: 'Charlie Brown',
      createdAt: new Date(Date.now() - 86400000 * 10).toISOString(),
      lastSeen: new Date().toISOString(),
      online: true
    },
    {
      id: 4,
      username: 'diana',
      email: 'diana@example.com',
      displayName: 'Diana Prince',
      createdAt: new Date(Date.now() - 86400000 * 5).toISOString(),
      lastSeen: new Date(Date.now() - 3600000).toISOString(),
      online: false
    }
  ];

  const rooms: ChatRoom[] = [
    {
      id: 1,
      name: 'General',
      description: 'General discussion for everyone',
      createdAt: new Date(Date.now() - 86400000 * 30).toISOString(),
      createdBy: users[0]
    },
    {
      id: 2,
      name: 'Random',
      description: 'Random thoughts and off-topic conversations',
      createdAt: new Date(Date.now() - 86400000 * 20).toISOString(),
      createdBy: users[1]
    },
    {
      id: 3,
      name: 'Tech Talk',
      description: 'Discuss technology, programming, and development',
      createdAt: new Date(Date.now() - 86400000 * 10).toISOString(),
      createdBy: users[0]
    }
  ];

  const [currentRoomId, setCurrentRoomId] = useState<number>(1);
  const currentUserId = 1; // Simulating logged-in user as Alice

  const handleSendMessage = (content: string) => {
    const newMessage: Message = {
      id: messages.length + 1,
      senderId: currentUserId,
      senderUsername: 'alice',
      senderDisplayName: 'Alice Johnson',
      chatRoomId: currentRoomId,
      content,
      timestamp: new Date().toISOString(),
      messageType: MessageType.TEXT
    };
    setMessages([...messages, newMessage]);
  };

  const handleRoomSelect = (room: ChatRoom) => {
    setCurrentRoomId(room.id);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 px-4 py-3">
        <h1 className="text-xl font-bold text-gray-900">Chat Components Demo</h1>
        <p className="text-sm text-gray-600 mt-1">
          Demonstrating MessageList, MessageInput, UserList, and RoomSelector
        </p>
      </header>

      {/* Main content */}
      <div className="container mx-auto p-4">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-4 h-[calc(100vh-120px)]">
          {/* Room Selector - Left sidebar on desktop, top on mobile */}
          <div className="lg:col-span-1 bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
            <RoomSelector
              rooms={rooms}
              currentRoomId={currentRoomId}
              onRoomSelect={handleRoomSelect}
            />
          </div>

          {/* Chat Area - Center */}
          <div className="lg:col-span-2 bg-white rounded-lg shadow-sm border border-gray-200 flex flex-col overflow-hidden">
            {/* Messages */}
            <div className="flex-1 overflow-hidden">
              <MessageList messages={messages} currentUserId={currentUserId} />
            </div>

            {/* Input */}
            <div className="border-t border-gray-200 p-4">
              <MessageInput onSend={handleSendMessage} />
            </div>
          </div>

          {/* User List - Right sidebar on desktop, bottom on mobile */}
          <div className="lg:col-span-1 bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
            <UserList users={users} currentUserId={currentUserId} />
          </div>
        </div>
      </div>
    </div>
  );
}
