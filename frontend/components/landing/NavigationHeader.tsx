'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useCallback, useEffect, useRef, useState } from 'react';

interface MenuItem {
  label: string;
  id: string;
}

const MENU_ITEMS: MenuItem[] = [
  { label: 'Features', id: 'features' },
  { label: 'About', id: 'about' },
  { label: 'How It Works', id: 'how-it-works' },
  { label: 'Contact', id: 'contact' },
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
      className={`sticky top-0 z-40 w-full border-b border-white/5 bg-[#0a0a0f]/90 backdrop-blur-md ${className}`.trim()}
    >
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-4">
        {/* Logo */}
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-kiro-purple-600">
            <Image
              src="/logo1.png"
              alt="Chatter logo"
              width={22}
              height={22}
              className="brightness-0 invert"
            />
          </div>
          <span className="text-lg font-bold text-white">Chatter</span>
        </div>

        {/* Desktop nav */}
        <nav
          className="hidden items-center gap-1 lg:flex"
          aria-label="Main navigation"
        >
          {MENU_ITEMS.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => handleItemClick(item.id)}
              className={`rounded-lg px-4 py-2 text-sm transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0a0a0f] ${
                activeSection === item.id
                  ? 'text-white'
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
            className="rounded-lg border border-kiro-purple-500/60 px-5 py-2 text-sm font-medium text-white transition-colors hover:border-kiro-purple-400 hover:bg-kiro-purple-500/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0a0a0f]"
          >
            Log In
          </Link>
          <Link
            href="/auth/register"
            className="rounded-lg bg-kiro-purple-600 px-5 py-2 text-sm font-medium text-white transition-colors hover:bg-kiro-purple-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0a0a0f]"
          >
            Sign Up Free
          </Link>
        </div>

        {/* Mobile hamburger */}
        <button
          ref={menuButtonRef}
          type="button"
          onClick={handleMenuToggle}
          className="flex h-10 w-10 items-center justify-center rounded-lg border border-white/10 text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 lg:hidden"
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
                className={`block w-full rounded-lg px-4 py-3 text-left text-sm transition-colors hover:bg-white/5 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-inset ${
                  activeSection === item.id ? 'text-white' : 'text-white/60 hover:text-white'
                }`}
              >
                {item.label}
              </button>
            ))}
            <div className="flex flex-col gap-2 pt-3">
              <Link
                href="/auth/login"
                className="block rounded-lg border border-kiro-purple-500/60 px-4 py-2.5 text-center text-sm font-medium text-white hover:bg-kiro-purple-500/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-inset"
              >
                Log In
              </Link>
              <Link
                href="/auth/register"
                className="block rounded-lg bg-kiro-purple-600 px-4 py-2.5 text-center text-sm font-medium text-white hover:bg-kiro-purple-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-inset"
              >
                Sign Up Free
              </Link>
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
