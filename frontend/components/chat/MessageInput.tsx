'use client';

import { useState, useRef, KeyboardEvent } from 'react';
import { Button } from '../ui/Button';

export interface MessageInputProps {
  onSend: (content: string) => void;
  disabled?: boolean;
  placeholder?: string;
  maxLength?: number;
  className?: string;
}

/**
 * MessageInput component with text input and send button.
 * Handles Enter key to send message (Shift+Enter for new line).
 * Follows mobile-first design principles with Tailwind CSS.
 * 
 * Requirements: 15.2, 15.5, 17.3
 */
export function MessageInput({
  onSend,
  disabled = false,
  placeholder = 'Type a message...',
  maxLength = 2000,
  className = ''
}: MessageInputProps) {
  const [content, setContent] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Handle send message
  const handleSend = () => {
    const trimmedContent = content.trim();
    
    if (!trimmedContent || disabled) {
      return;
    }

    onSend(trimmedContent);
    setContent('');
    
    // Reset textarea height
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  // Handle Enter key press
  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    // Send on Enter (without Shift)
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
    // Allow Shift+Enter for new line (default behavior)
  };

  // Auto-resize textarea based on content
  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const textarea = e.target;
    setContent(textarea.value);

    // Reset height to auto to get the correct scrollHeight
    textarea.style.height = 'auto';
    
    // Set height to scrollHeight (max 150px)
    const newHeight = Math.min(textarea.scrollHeight, 150);
    textarea.style.height = `${newHeight}px`;
  };

  const remainingChars = maxLength - content.length;
  const isNearLimit = remainingChars < 100;
  const isOverLimit = remainingChars < 0;

  return (
    <div className={`flex flex-col gap-2 ${className}`}>
      {/* Character count (shown when near limit) */}
      {isNearLimit && (
        <div className="flex justify-end px-2">
          <span
            className={`text-xs ${
              isOverLimit ? 'text-red-600 font-medium' : 'text-gray-500'
            }`}
            role="status"
            aria-live="polite"
          >
            {remainingChars} characters remaining
          </span>
        </div>
      )}

      {/* Input container */}
      <div className="flex items-end gap-2">
        {/* Textarea */}
        <div className="flex-1 relative">
          <textarea
            ref={textareaRef}
            value={content}
            onChange={handleInput}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            disabled={disabled}
            maxLength={maxLength}
            rows={1}
            className="w-full min-h-[44px] max-h-[150px] px-4 py-3 text-base rounded-lg border border-gray-300 resize-none transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100"
            aria-label="Message input"
            aria-describedby="message-input-hint"
          />
          
          {/* Hidden hint for screen readers */}
          <span id="message-input-hint" className="sr-only">
            Press Enter to send, Shift+Enter for new line
          </span>
        </div>

        {/* Send button */}
        <Button
          onClick={handleSend}
          disabled={disabled || !content.trim() || isOverLimit}
          variant="primary"
          size="md"
          className="shrink-0"
          aria-label="Send message"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="currentColor"
            className="w-5 h-5"
            aria-hidden="true"
          >
            <path d="M3.478 2.405a.75.75 0 00-.926.94l2.432 7.905H13.5a.75.75 0 010 1.5H4.984l-2.432 7.905a.75.75 0 00.926.94 60.519 60.519 0 0018.445-8.986.75.75 0 000-1.218A60.517 60.517 0 003.478 2.405z" />
          </svg>
        </Button>
      </div>

      {/* Helper text */}
      {!disabled && (
        <p className="text-xs text-gray-500 px-2">
          Press <kbd className="px-1 py-0.5 bg-gray-100 border border-gray-300 rounded text-xs">Enter</kbd> to send, <kbd className="px-1 py-0.5 bg-gray-100 border border-gray-300 rounded text-xs">Shift+Enter</kbd> for new line
        </p>
      )}

      {disabled && (
        <p className="text-xs text-red-600 px-2" role="alert">
          Cannot send messages while disconnected
        </p>
      )}
    </div>
  );
}
