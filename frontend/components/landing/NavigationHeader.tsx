'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useCallback, useEffect, useRef, useState } from 'react';

interface MenuItem {
  label: string;
  id: string;
}

const MENU_ITEMS: MenuItem[] = [
  { label: 'Home', id: 'home' },
  { label: 'Readiness', id: 'readiness' },
  { label: 'System', id: 'system' },
  { label: 'Integration', id: 'integration' },
  { label: 'Community', id: 'community' },
  { label: 'Stories', id: 'stories' },
];

export interface NavigationHeaderProps {
  className?: string;
}

export function NavigationHeader({ className = '' }: NavigationHeaderProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [activeSection, setActiveSection] = useState(MENU_ITEMS[0]?.id ?? 'features');

  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const mobileMenuRef = useRef<HTMLDivElement>(null);
  const firstFocusableRef = useRef<HTMLButtonElement>(null);

  const handleMenuToggle = () => setIsMenuOpen((prev) => !prev);

  const handleMenuClose = useCallback(() => {
    setIsMenuOpen(false);
    menuButtonRef.current?.focus();
  }, []);

  const handleItemClick = useCallback(
    (id: string) => {
      const target = document.getElementById(id);
      if (target) {
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
      handleMenuClose();
    },
    [handleMenuClose]
  );

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isMenuOpen) {
        handleMenuClose();
      }
    };
    if (isMenuOpen) {
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isMenuOpen, handleMenuClose]);

  useEffect(() => {
    if (!isMenuOpen) return;
    const timer = setTimeout(() => firstFocusableRef.current?.focus(), 100);
    const handleTabKey = (event: KeyboardEvent) => {
      if (event.key !== 'Tab') return;
      const focusableElements = mobileMenuRef.current?.querySelectorAll<HTMLElement>(
        'button, a[href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );
      if (!focusableElements || focusableElements.length === 0) return;
      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];
      if (event.shiftKey) {
        if (document.activeElement === firstElement) {
          event.preventDefault();
          lastElement?.focus();
        }
      } else {
        if (document.activeElement === lastElement) {
          event.preventDefault();
          firstElement?.focus();
        }
      }
    };
    document.addEventListener('keydown', handleTabKey);
    return () => {
      clearTimeout(timer);
      document.removeEventListener('keydown', handleTabKey);
    };
  }, [isMenuOpen]);

  useEffect(() => {
    const handleScroll = () => {
      const offset = 140;
      let currentSection = MENU_ITEMS[0]?.id ?? 'features';
      MENU_ITEMS.forEach(({ id }) => {
        const section = document.getElementById(id);
        if (!section) return;
        const { top } = section.getBoundingClientRect();
        if (top <= offset) currentSection = id;
      });
      setActiveSection(currentSection);
    };
    handleScroll();
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <header
      className={`sticky top-0 z-40 w-full border-b border-white/5 bg-[#0a0a0f]/80 backdrop-blur-xl ${className}`.trim()}
    >
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between px-4 py-3 sm:px-6 sm:py-4">
        {/* Logo */}
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-kiro-purple-600 sm:h-9 sm:w-9">
            <Image
              src="/logo1.png"
              alt="Chatter logo"
              width={22}
              height={22}
              className="brightness-0 invert"
            />
          </div>
          <span className="text-base font-bold text-white sm:text-lg">Chatter</span>
        </div>

        {/* Desktop nav */}
        <nav className="hidden items-center gap-1 lg:flex" aria-label="Main navigation">
          {MENU_ITEMS.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => handleItemClick(item.id)}
              className={`rounded-full px-4 py-2 text-xs uppercase tracking-[0.18em] transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0a0a0f] ${
                activeSection === item.id
                  ? 'bg-white/10 text-white'
                  : 'text-white/60 hover:text-white'
              }`}
            >
              {item.label}
            </button>
          ))}
        </nav>

        {/* Desktop CTA buttons */}
        <div className="hidden items-center gap-3 lg:flex">
          <Link
            href="/auth/login"
            className="rounded-full border border-white/15 px-5 py-2 text-xs font-semibold uppercase tracking-[0.2em] text-white transition-colors hover:border-kiro-purple-400/60 hover:bg-white/5 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0a0a0f]"
          >
            Sign In
          </Link>
          <Link
            href="/auth/register"
            className="rounded-full bg-gradient-to-r from-kiro-purple-600 via-kiro-purple-500 to-kiro-purple-400 px-6 py-2 text-xs font-semibold uppercase tracking-[0.2em] text-white shadow-[0_12px_30px_rgba(88,61,196,0.35)] transition-transform hover:-translate-y-0.5 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0a0a0f]"
          >
            Start Journey
          </Link>
        </div>

        {/* Mobile hamburger */}
        <button
          ref={menuButtonRef}
          type="button"
          onClick={handleMenuToggle}
          className="flex h-10 w-10 items-center justify-center rounded-full border border-white/10 text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 lg:hidden"
          aria-label={isMenuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={isMenuOpen}
        >
          <span className="sr-only">Toggle menu</span>
          <div className="flex flex-col gap-1.5">
            <span className="h-0.5 w-5 rounded-full bg-white" />
            <span className="h-0.5 w-5 rounded-full bg-white" />
            <span className="h-0.5 w-5 rounded-full bg-white" />
          </div>
        </button>
      </div>

      {/* Mobile menu */}
      {isMenuOpen && (
        <div
          ref={mobileMenuRef}
          className="border-t border-white/5 bg-[#0a0a0f] px-6 py-4 lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-label="Mobile navigation menu"
        >
          <div className="animate-slide-down space-y-1">
            {MENU_ITEMS.map((item, index) => (
              <button
                key={item.id}
                ref={index === 0 ? firstFocusableRef : null}
                type="button"
                onClick={() => handleItemClick(item.id)}
                className={`block w-full rounded-xl px-4 py-3 text-left text-xs uppercase tracking-[0.18em] transition-colors hover:bg-white/5 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-inset ${
                  activeSection === item.id ? 'text-white' : 'text-white/60 hover:text-white'
                }`}
              >
                {item.label}
              </button>
            ))}
            <div className="flex flex-col gap-2 pt-3">
              <Link
                href="/auth/login"
                className="block rounded-full border border-white/15 px-4 py-2.5 text-center text-xs font-semibold uppercase tracking-[0.2em] text-white hover:bg-white/5 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-inset"
              >
                Sign In
              </Link>
              <Link
                href="/auth/register"
                className="block rounded-full bg-gradient-to-r from-kiro-purple-600 via-kiro-purple-500 to-kiro-purple-400 px-4 py-2.5 text-center text-xs font-semibold uppercase tracking-[0.2em] text-white hover:opacity-90 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-inset"
              >
                Start Journey
              </Link>
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
