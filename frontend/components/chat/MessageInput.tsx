'use client';

import { useState, useRef, KeyboardEvent } from 'react';

export interface MessageInputProps {
  onSend: (content: string) => void;
  disabled?: boolean;
  placeholder?: string;
  maxLength?: number;
  className?: string;
}

/**
 * MessageInput component with a styled input bar matching the CollabChat design.
 * Features attachment, emoji, GIF, and formatting icon buttons plus a circular send button.
 * Handles Enter to send (Shift+Enter for new line).
 *
 * Requirements: 15.2, 15.5, 17.3
 */
export function MessageInput({
  onSend,
  disabled = false,
  placeholder = 'Type a message…',
  maxLength = 2000,
  className = '',
}: MessageInputProps) {
  const [content, setContent] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSend = () => {
    const trimmed = content.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setContent('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const textarea = e.target;
    setContent(textarea.value);
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 150)}px`;
  };

  const remainingChars = maxLength - content.length;
  const isOverLimit = remainingChars < 0;
  const isNearLimit = remainingChars < 100;
  const canSend = content.trim().length > 0 && !disabled && !isOverLimit;

  return (
    <div className={`flex flex-col gap-1 ${className}`}>
      {/* Character count */}
      {isNearLimit && (
        <div className="flex justify-end px-1">
          <span
            className={`text-xs ${isOverLimit ? 'text-red-400 font-medium' : 'text-kiro-slate-500'}`}
            role="status"
            aria-live="polite"
          >
            {remainingChars} remaining
          </span>
        </div>
      )}

      {/* Input row */}
      <div className="flex items-center gap-2 bg-[#1e1e30] rounded-2xl px-3 py-2 border border-white/5 focus-within:border-kiro-purple-600/50 transition-colors">
        {/* Attachment icon — hidden below 480px */}
        <span className="[@media(max-width:479px)]:hidden contents">
          <InputIconBtn label="Attach file" disabled={disabled}>
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
              <path strokeLinecap="round" strokeLinejoin="round" d="m18.375 12.739-7.693 7.693a4.5 4.5 0 0 1-6.364-6.364l10.94-10.94A3 3 0 1 1 19.5 7.372L8.552 18.32m.009-.01-.01.01m5.699-9.941-7.81 7.81a1.5 1.5 0 0 0 2.112 2.13" />
            </svg>
          </InputIconBtn>
        </span>

        {/* Textarea */}
        <textarea
          ref={textareaRef}
          value={content}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled}
          maxLength={maxLength}
          rows={1}
          className="flex-1 bg-transparent text-sm text-kiro-slate-100 placeholder-kiro-slate-500 resize-none focus:outline-none min-h-[44px] max-h-[150px] leading-6 py-0.5"
          aria-label="Message input"
          aria-describedby="msg-input-hint"
        />
        <span id="msg-input-hint" className="sr-only">
          Press Enter to send, Shift+Enter for new line
        </span>

        {/* Emoji icon */}
        <InputIconBtn label="Add emoji" disabled={disabled}>
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M15.182 15.182a4.5 4.5 0 0 1-6.364 0M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0ZM9.75 9.75c0 .414-.168.75-.375.75S9 10.164 9 9.75 9.168 9 9.375 9s.375.336.375.75Zm-.375 0h.008v.015h-.008V9.75Zm5.625 0c0 .414-.168.75-.375.75s-.375-.336-.375-.75.168-.75.375-.75.375.336.375.75Zm-.375 0h.008v.015h-.008V9.75Z" />
          </svg>
        </InputIconBtn>

        {/* GIF icon — hidden below 480px */}
        <span className="[@media(max-width:479px)]:hidden contents">
          <InputIconBtn label="Send GIF" disabled={disabled}>
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
              <path strokeLinecap="round" strokeLinejoin="round" d="m2.25 15.75 5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5 1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
            </svg>
          </InputIconBtn>
        </span>

        {/* Format icon — hidden below 480px */}
        <span className="[@media(max-width:479px)]:hidden contents">
          <InputIconBtn label="Format text" disabled={disabled}>
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
              <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25H12" />
            </svg>
          </InputIconBtn>
        </span>

        {/* Send button — circular purple */}
        <button
          type="button"
          onClick={handleSend}
          disabled={!canSend}
          className="shrink-0 w-9 h-9 flex items-center justify-center rounded-full bg-kiro-purple-600 text-white hover:bg-kiro-purple-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors focus:outline-none focus:ring-2 focus:ring-kiro-purple-400 focus:ring-offset-2 focus:ring-offset-[#1e1e30]"
          aria-label="Send message"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 translate-x-0.5" aria-hidden="true">
            <path d="M3.478 2.405a.75.75 0 0 0-.926.94l2.432 7.905H13.5a.75.75 0 0 1 0 1.5H4.984l-2.432 7.905a.75.75 0 0 0 .926.94 60.519 60.519 0 0 0 18.445-8.986.75.75 0 0 0 0-1.218A60.517 60.517 0 0 0 3.478 2.405Z" />
          </svg>
        </button>
      </div>

      {/* Disconnected warning */}
      {disabled && (
        <p className="text-xs text-red-400 px-1" role="alert">
          Cannot send messages while disconnected
        </p>
      )}
    </div>
  );
}

/** Small icon button inside the input bar */
function InputIconBtn({
  label,
  disabled,
  children,
}: {
  label: string;
  disabled?: boolean;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      disabled={disabled}
      className="shrink-0 p-1 text-kiro-slate-500 hover:text-kiro-slate-300 disabled:opacity-40 disabled:cursor-not-allowed transition-colors focus:outline-none focus:ring-1 focus:ring-kiro-purple-400 rounded"
    >
      {children}
    </button>
  );
}
