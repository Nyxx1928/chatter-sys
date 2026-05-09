'use client';

import { useState } from 'react';
import { MessageList, MessageInput, UserList, RoomSelector } from '@/components/chat';
import { Message, MessageType, User, ChatRoom } from '@/types/domain';

const DEMO_NOW = new Date('2026-05-03T12:00:00Z').getTime();
const ONE_HOUR = 3600000;
const ONE_DAY = 86400000;

const demoTimestamp = (offsetMs: number) => new Date(DEMO_NOW - offsetMs).toISOString();

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
      timestamp: demoTimestamp(ONE_HOUR),
      messageType: MessageType.TEXT
    },
    {
      id: 2,
      senderId: 2,
      senderUsername: 'bob',
      senderDisplayName: 'Bob Smith',
      chatRoomId: 1,
      content: 'Thanks Alice! This looks great.',
      timestamp: demoTimestamp(3000000),
      messageType: MessageType.TEXT
    },
    {
      id: 3,
      senderId: 3,
      senderUsername: 'charlie',
      senderDisplayName: 'Charlie Brown',
      chatRoomId: 1,
      content: 'Charlie Brown joined the room',
      timestamp: demoTimestamp(1800000),
      messageType: MessageType.JOIN
    },
    {
      id: 4,
      senderId: 3,
      senderUsername: 'charlie',
      senderDisplayName: 'Charlie Brown',
      chatRoomId: 1,
      content: 'Hi everyone! Excited to be here.',
      timestamp: demoTimestamp(1200000),
      messageType: MessageType.TEXT
    }
  ]);

  const users: User[] = [
    {
      id: 1,
      username: 'alice',
      email: 'alice@example.com',
      displayName: 'Alice Johnson',
      createdAt: demoTimestamp(ONE_DAY * 30),
      lastSeen: new Date().toISOString(),
      online: true
    },
    {
      id: 2,
      username: 'bob',
      email: 'bob@example.com',
      displayName: 'Bob Smith',
      createdAt: demoTimestamp(ONE_DAY * 20),
      lastSeen: new Date().toISOString(),
      online: true
    },
    {
      id: 3,
      username: 'charlie',
      email: 'charlie@example.com',
      displayName: 'Charlie Brown',
      createdAt: demoTimestamp(ONE_DAY * 10),
      lastSeen: new Date().toISOString(),
      online: true
    },
    {
      id: 4,
      username: 'diana',
      email: 'diana@example.com',
      displayName: 'Diana Prince',
      createdAt: demoTimestamp(ONE_DAY * 5),
      lastSeen: demoTimestamp(ONE_HOUR),
      online: false
    }
  ];

  const rooms: ChatRoom[] = [
    {
      id: 1,
      name: 'General',
      description: 'General discussion for everyone',
      createdAt: demoTimestamp(ONE_DAY * 30),
      createdBy: users[0],
      roomType: 'GROUP'
    },
    {
      id: 2,
      name: 'Random',
      description: 'Random thoughts and off-topic conversations',
      createdAt: demoTimestamp(ONE_DAY * 20),
      createdBy: users[1],
      roomType: 'GROUP'
    },
    {
      id: 3,
      name: 'Tech Talk',
      description: 'Discuss technology, programming, and development',
      createdAt: demoTimestamp(ONE_DAY * 10),
      createdBy: users[0],
      roomType: 'GROUP'
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
