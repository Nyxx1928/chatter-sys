'use client';

import { useEffect, useRef } from 'react';
import { Message, MessageType } from '../../types/domain';

export interface MessageListProps {
  messages?: Message[];
  currentUserId?: number;
  className?: string;
}

/**
 * MessageList component displays chat messages with sender, timestamp, and content.
 * Implements auto-scroll to bottom on new messages.
 * Follows mobile-first design principles with Tailwind CSS.
 * 
 * Requirements: 14.2, 15.1, 17.3
 */
export function MessageList({ messages = [], currentUserId, className = '' }: MessageListProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const prevMessageCountRef = useRef(messages.length);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (messages.length > prevMessageCountRef.current) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
    prevMessageCountRef.current = messages.length;
  }, [messages]);

  // Format timestamp to readable format
  const formatTimestamp = (timestamp: string): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();

    if (isToday) {
      return date.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });
    }

    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  };

  // Render system messages (JOIN, LEAVE, SYSTEM)
  const getMessageKey = (message: Message): string => {
    if (message.id !== undefined && message.id !== null) {
      return String(message.id);
    }

    return `${message.messageType}-${message.timestamp}-${message.senderId ?? 'system'}`;
  };

  const renderSystemMessage = (message: Message) => (
    <div
      key={getMessageKey(message)}
      className="flex justify-center py-2"
      role="status"
      aria-live="polite"
    >
      <p className="text-sm text-gray-500 italic">
        {message.content}
      </p>
    </div>
  );

  // Render regular text messages
  const renderTextMessage = (message: Message) => {
    const isOwnMessage = currentUserId === message.senderId;

    return (
      <div
        key={getMessageKey(message)}
        className={`flex flex-col gap-1 py-2 px-3 ${
          isOwnMessage ? 'items-end' : 'items-start'
        }`}
      >
        {/* Sender name and timestamp */}
        <div className="flex items-center gap-2 text-xs text-gray-600">
          <span className="font-medium">{message.senderDisplayName}</span>
          <span className="text-gray-400">•</span>
          <time dateTime={message.timestamp}>
            {formatTimestamp(message.timestamp)}
          </time>
        </div>

        {/* Message content */}
        <div
          className={`max-w-[85%] sm:max-w-[75%] md:max-w-[65%] rounded-lg px-4 py-2 break-words ${
            isOwnMessage
              ? 'bg-blue-600 text-white rounded-br-none'
              : 'bg-gray-200 text-gray-900 rounded-bl-none'
          }`}
        >
          <p className="text-base leading-relaxed whitespace-pre-wrap">
            {message.content}
          </p>
        </div>
      </div>
    );
  };

  // Empty state
  if (messages.length === 0) {
    return (
      <div
        className={`flex items-center justify-center h-full ${className}`}
        role="status"
      >
        <p className="text-gray-500 text-center px-4">
          No messages yet. Start the conversation!
        </p>
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      className={`flex flex-col overflow-y-auto h-full ${className}`}
      role="log"
      aria-live="polite"
      aria-label="Chat messages"
    >
      {/* Messages list */}
      <div className="flex-1 space-y-1">
        {messages.map((message) => {
          if (
            message.messageType === MessageType.SYSTEM ||
            message.messageType === MessageType.JOIN ||
            message.messageType === MessageType.LEAVE
          ) {
            return renderSystemMessage(message);
          }
          return renderTextMessage(message);
        })}
      </div>

      {/* Scroll anchor */}
      <div ref={messagesEndRef} />
    </div>
  );
}
