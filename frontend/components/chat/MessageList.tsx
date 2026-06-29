'use client';

import { useEffect, useRef } from 'react';
import { Message, MessageType } from '../../types/domain';

export interface MessageListProps {
  messages?: Message[];
  currentUserId?: number;
  className?: string;
}

/**
 * MessageList component displays chat messages with sender avatars, timestamps,
 * and content. Own messages are right-aligned in purple; others are left-aligned
 * with an avatar. Implements auto-scroll to bottom on new messages.
 *
 * Requirements: 14.2, 15.1, 17.3
 */
export function MessageList({ messages = [], currentUserId, className = '' }: MessageListProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const prevMessageCountRef = useRef(messages.length);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const isNearBottom =
      container.scrollHeight - container.scrollTop - container.clientHeight < 100;
    if (messages.length > prevMessageCountRef.current && isNearBottom) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
    prevMessageCountRef.current = messages.length;
  }, [messages]);

  const formatTimestamp = (timestamp: string): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();

    if (isToday) {
      return date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
    }
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  };

  /**
   * Returns true when a timestamp should be shown above this message.
   * Rules: always show for the first message, then only when 5+ minutes
   * have passed since the previous message.
   */
  const shouldShowTimestamp = (index: number, allMessages: Message[]): boolean => {
    if (index === 0) return true;
    const prev = new Date(allMessages[index - 1].timestamp).getTime();
    const curr = new Date(allMessages[index].timestamp).getTime();
    return curr - prev >= 5 * 60 * 1000; // 5 minutes
  };

  const getMessageKey = (message: Message): string => {
    if (message.id !== undefined && message.id !== null) return String(message.id);
    return `${message.messageType}-${message.timestamp}-${message.senderId ?? 'system'}`;
  };

  /** Initials avatar for a display name */
  const Avatar = ({ name, size = 9 }: { name: string; size?: number }) => (
    <div
      className={`shrink-0 w-${size} h-${size} rounded-full bg-slack-primary flex items-center justify-center text-slack-text-inverse font-semibold text-sm select-none`}
      aria-hidden="true"
    >
      {name.charAt(0).toUpperCase()}
    </div>
  );

  const renderSystemMessage = (message: Message) => (
    <div key={getMessageKey(message)} className="flex justify-center py-2" role="status" aria-live="polite">
      <p className="text-xs text-slack-text-secondary italic bg-slack-surface-tertiary px-3 py-1 rounded-pill">
        {message.content}
      </p>
    </div>
  );

  const renderTextMessage = (message: Message, index: number, allMessages: Message[]) => {
    const isOwn = currentUserId === message.senderId;
    const showTimestamp = shouldShowTimestamp(index, allMessages);

    return (
      <div key={getMessageKey(message)}>
        {/* Timestamp divider — only when enough time has passed */}
          {showTimestamp && (
          <div className="flex justify-center py-3">
            <time
              dateTime={message.timestamp}
              className="text-xs text-slack-text-secondary bg-slack-surface-tertiary px-3 py-1 rounded-pill select-none"
            >
              {formatTimestamp(message.timestamp)}
            </time>
          </div>
        )}

        <div
          className={`flex items-end gap-3 px-4 py-1 ${isOwn ? 'flex-row-reverse' : 'flex-row'}`}
        >
          {/* Avatar — only for others */}
          {!isOwn && (
            <Avatar name={message.senderDisplayName ?? '?'} />
          )}

          <div className={`flex flex-col gap-1 max-w-[85%] md:max-w-[70%] ${isOwn ? 'items-end' : 'items-start'}`}>
            {/* Sender name — nudged right to align with the bubble, not the avatar */}
            <span
              className={`text-xs font-semibold text-slack-text-primary ${!isOwn ? 'pl-1' : 'pr-1'}`}
            >
              {isOwn ? 'Me' : message.senderDisplayName}
            </span>

            {/* Bubble */}
            <div
              className={`px-4 py-2.5 rounded-2xl break-words text-sm leading-relaxed ${
                isOwn
                  ? 'bg-slack-primary text-slack-text-inverse rounded-br-sm'
                  : 'bg-slack-surface-tertiary text-slack-text-primary rounded-bl-sm'
              }`}
            >
              <p className="whitespace-pre-wrap break-words [overflow-wrap:anywhere]">{message.content}</p>
            </div>
          </div>
        </div>
      </div>
    );
  };

  if (messages.length === 0) {
    return (
      <div className={`flex items-center justify-center h-full bg-slack-surface-secondary ${className}`} role="status">
        <div className="text-center px-4">
          <div className="w-14 h-14 rounded-full bg-slack-surface-tertiary flex items-center justify-center mx-auto mb-3">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-7 h-7 text-slack-text-secondary">
              <path strokeLinecap="round" strokeLinejoin="round" d="M8.625 12a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H8.25m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H12m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 0 1-2.555-.337A5.972 5.972 0 0 1 5.41 20.97a5.969 5.969 0 0 1-.474-.065 4.48 4.48 0 0 0 .978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25Z" />
            </svg>
          </div>
          <p className="text-slack-text-secondary font-medium text-sm">No messages yet</p>
          <p className="text-slack-text-secondary text-xs mt-1">Be the first to say something!</p>
        </div>
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      className={`flex flex-col overflow-y-auto h-full bg-slack-surface-secondary py-4 ${className}`}
      role="log"
      aria-live="polite"
      aria-label="Chat messages"
    >
      <div className="flex flex-col gap-0.5">
        {messages.map((message, index) => {
          if (
            message.messageType === MessageType.SYSTEM ||
            message.messageType === MessageType.JOIN ||
            message.messageType === MessageType.LEAVE
          ) {
            return renderSystemMessage(message);
          }
          return renderTextMessage(message, index, messages);
        })}
      </div>
      <div ref={messagesEndRef} />
    </div>
  );
}
