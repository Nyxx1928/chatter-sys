'use client';

import Link from 'next/link';
import { useCallback, useEffect, useRef, useState } from 'react';

import { Button } from '@/components/ui/Button';

interface MenuItem {
  label: string;
  id: string;
}

const MENU_ITEMS: MenuItem[] = [
  { label: 'Home', id: 'home' },
  { label: 'About', id: 'about' },
  { label: 'How It Works', id: 'how-it-works' },
  { label: 'Pricing', id: 'pricing' },
  { label: 'Contact', id: 'contact' },
];

export interface NavigationHeaderProps {
  className?: string;
}

export function NavigationHeader({ className = '' }: NavigationHeaderProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [activeSection, setActiveSection] = useState(MENU_ITEMS[0]?.id ?? 'home');
  
  // Refs for focus management
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const mobileMenuRef = useRef<HTMLDivElement>(null);
  const firstFocusableRef = useRef<HTMLButtonElement>(null);
  const lastFocusableRef = useRef<HTMLAnchorElement>(null);

  const handleMenuToggle = () => setIsMenuOpen((prev) => !prev);

  const handleMenuClose = useCallback(() => {
    setIsMenuOpen(false);
    // Return focus to hamburger button when menu closes
    menuButtonRef.current?.focus();
  }, []);

  const handleItemClick = useCallback((id: string) => {
    const target = document.getElementById(id);
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
    handleMenuClose();
  }, [handleMenuClose]);

  // Handle Escape key to close mobile menu
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isMenuOpen) {
        handleMenuClose();
      }
    };

    if (isMenuOpen) {
      document.addEventListener('keydown', handleKeyDown);
    }

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isMenuOpen, handleMenuClose]);

  // Focus trap for mobile menu
  useEffect(() => {
    if (!isMenuOpen) return;

    // Focus the first focusable element when menu opens
    const timer = setTimeout(() => {
      firstFocusableRef.current?.focus();
    }, 100);

    const handleTabKey = (event: KeyboardEvent) => {
      if (event.key !== 'Tab') return;

      const focusableElements = mobileMenuRef.current?.querySelectorAll(
        'button, a[href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );

      if (!focusableElements || focusableElements.length === 0) return;

      const firstElement = focusableElements[0] as HTMLElement;
      const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;

      if (event.shiftKey) {
        // Shift + Tab: if on first element, move to last
        if (document.activeElement === firstElement) {
          event.preventDefault();
          lastElement.focus();
        }
      } else {
        // Tab: if on last element, move to first
        if (document.activeElement === lastElement) {
          event.preventDefault();
          firstElement.focus();
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
      let currentSection = MENU_ITEMS[0]?.id ?? 'home';

      MENU_ITEMS.forEach(({ id }) => {
        const section = document.getElementById(id);
        if (!section) {
          return;
        }

        const { top } = section.getBoundingClientRect();
        if (top <= offset) {
          currentSection = id;
        }
      });

      setActiveSection(currentSection);
    };

    handleScroll();
    window.addEventListener('scroll', handleScroll, { passive: true });

    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <header
      className={`sticky top-0 z-40 w-full border-b border-kiro-ink-900/60 bg-kiro-ink-950/90 backdrop-blur ${className}`.trim()}
    >
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between px-4 py-4 sm:px-6">
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-xl bg-kiro-purple-600" />
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.3em] text-kiro-slate-500">Kiro</p>
            <p className="text-lg font-semibold text-kiro-slate-100">Chat Studio</p>
          </div>
        </div>

        {/* Desktop navigation - visible on lg (1024px+) and above */}
        <nav className="hidden items-center gap-6 text-sm text-kiro-slate-200 lg:flex" aria-label="Main navigation">
          {MENU_ITEMS.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => handleItemClick(item.id)}
              className={`min-h-[44px] min-w-[44px] px-2 transition-all duration-100 hover:text-kiro-purple-400 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-kiro-ink-950 ${
                activeSection === item.id ? 'text-kiro-purple-400' : ''
              }`}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="hidden lg:block">
          <Link 
            href="/auth/register"
            className="focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-kiro-ink-950 rounded-lg"
          >
            <Button variant="secondary" size="sm">
              Sign Up
            </Button>
          </Link>
        </div>

        {/* Mobile/tablet hamburger menu - visible below lg (1024px) */}
        <button
          ref={menuButtonRef}
          type="button"
          onClick={handleMenuToggle}
          className="flex h-11 w-11 min-h-[44px] min-w-[44px] items-center justify-center rounded-lg border border-kiro-ink-900/70 text-kiro-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-kiro-ink-950 lg:hidden"
          aria-label={isMenuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={isMenuOpen}
        >
          <span className="sr-only">Toggle menu</span>
          <div className="flex flex-col gap-1">
            <span className="h-0.5 w-5 rounded-full bg-kiro-slate-100" />
            <span className="h-0.5 w-5 rounded-full bg-kiro-slate-100" />
            <span className="h-0.5 w-5 rounded-full bg-kiro-slate-100" />
          </div>
        </button>
      </div>

      {isMenuOpen ? (
        <div 
          ref={mobileMenuRef}
          className="border-t border-kiro-ink-900/70 bg-kiro-ink-950 px-4 py-4 lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-label="Mobile navigation menu"
        >
          <div className="animate-slide-down space-y-2 text-sm text-kiro-slate-200">
            {MENU_ITEMS.map((item, index) => (
              <button
                key={item.id}
                ref={index === 0 ? firstFocusableRef : null}
                type="button"
                onClick={() => handleItemClick(item.id)}
                className={`block w-full min-h-[44px] rounded-lg px-4 py-3 text-left transition-all duration-100 hover:bg-kiro-ink-900/60 hover:text-kiro-purple-400 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-inset ${
                  activeSection === item.id ? 'text-kiro-purple-400' : ''
                }`}
              >
                {item.label}
              </button>
            ))}
            <div className="pt-2">
              <Link 
                ref={lastFocusableRef}
                href="/auth/register" 
                className="block focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-inset rounded-lg"
              >
                <Button variant="secondary" size="sm" fullWidth>
                  Sign Up
                </Button>
              </Link>
            </div>
          </div>
        </div>
      ) : null}
    </header>
  );
}
