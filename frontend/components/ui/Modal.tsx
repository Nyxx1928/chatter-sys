'use client';

import React, { useEffect, useRef } from 'react';

export interface ModalProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: React.ReactNode;
  footer?: React.ReactNode;
}

/**
 * Accessible modal dialog with focus management and Escape handling.
 */
export function Modal({ open, title, onClose, children, footer }: ModalProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }

    previousFocusRef.current = document.activeElement as HTMLElement | null;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    const focusTimer = window.setTimeout(() => {
      const focusable = containerRef.current?.querySelector<HTMLElement>(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );
      focusable?.focus();
    }, 0);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      window.clearTimeout(focusTimer);
      previousFocusRef.current?.focus();
    };
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="presentation"
      onClick={onClose}
    >
      <div
        ref={containerRef}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="w-full max-w-lg rounded-2xl bg-kiro-ink-900 shadow-xl border border-kiro-ink-900/80"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4 border-b border-kiro-ink-900/80 px-6 py-4">
          <h2 className="text-lg font-semibold text-kiro-slate-100">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-2 text-kiro-slate-500 hover:bg-kiro-ink-950/60 hover:text-kiro-slate-100 focus:outline-none focus:ring-2 focus:ring-kiro-purple-400"
            aria-label="Close dialog"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.5}
              stroke="currentColor"
              className="h-5 w-5"
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="px-6 py-4">{children}</div>
        {footer && <div className="border-t border-kiro-ink-900/80 px-6 py-4">{footer}</div>}
      </div>
    </div>
  );
}
