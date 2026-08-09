'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useCallback, useEffect, useRef, useState } from 'react';
import { cn } from '@/lib/utils';
import {
  NavigationMenu,
  NavigationMenuList,
  NavigationMenuItem,
  NavigationMenuTrigger,
  NavigationMenuLink,
} from '@/components/ui/navigation-menu';
import { Sheet, SheetContent, SheetTrigger, SheetClose } from '@/components/ui/sheet';
import { Accordion, AccordionItem, AccordionTrigger, AccordionContent } from '@/components/ui/accordion';
import { Button } from '@/components/ui/shadcn-button';
import { MENU_ITEMS, SYSTEM_MODULES } from './navigationData';
import { SystemMegaMenu } from './SystemMegaMenu';
import { Menu } from 'lucide-react';

export interface NavigationHeaderProps {
  className?: string;
}

export function NavigationHeader({ className = '' }: NavigationHeaderProps) {
  const [activeSection, setActiveSection] = useState('home');
  const [isSheetOpen, setIsSheetOpen] = useState(false);

  const menuButtonRef = useRef<HTMLButtonElement>(null);

  const handleItemClick = useCallback((id: string) => {
    const target = document.getElementById(id);
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
    setIsSheetOpen(false);
  }, []);

  const handleSheetOpenChange = useCallback((open: boolean) => {
    setIsSheetOpen(open);
    if (!open) {
      setTimeout(() => menuButtonRef.current?.focus(), 100);
    }
  }, []);

  useEffect(() => {
    const handleScroll = () => {
      const offset = 140;
      let currentSection = 'home';
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
    <header className={`sticky top-0 z-40 w-full ${className}`.trim()}>
      <div className="mx-auto flex max-w-7xl items-center justify-center px-6 py-2">
        <div className="flex h-14 w-full items-center justify-between gap-2 rounded-full border border-white/10 bg-[#0A0A14]/20 backdrop-blur-md shadow-slack-md pr-3 sm:h-16 md:w-5xl">
          {/* Logo */}
          <button
            type="button"
            onClick={() => handleItemClick('home')}
            className="flex items-center gap-2 pl-4 pr-6 cursor-pointer"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-slack-primary sm:h-9 sm:w-9">
              <Image
                src="/logo1.png"
                alt="Chatter logo"
                width={22}
                height={22}
                className="brightness-0 invert"
              />
            </div>
            <span className="text-base font-bold tracking-tight text-slack-text-primary sm:text-lg">
              Chatter
            </span>
          </button>

          {/* Desktop Navigation */}
          <div className="hidden lg:block">
            <NavigationMenu
              className={cn(
                'static',
                '[&>div:last-child]:inset-x-0 [&>div:last-child]:top-full [&>div:last-child]:w-full',
                '[&_[data-slot=navigation-menu-viewport]]:mx-auto [&_[data-slot=navigation-menu-viewport]]:-mt-4 [&_[data-slot=navigation-menu-viewport]]:max-w-7xl [&_[data-slot=navigation-menu-viewport]]:ring-0',
                '[&_[data-slot=navigation-menu-viewport]]:rounded-[2rem]',
                '[&_[data-slot=navigation-menu-viewport]]:transition-all [&_[data-slot=navigation-menu-viewport]]:duration-300 [&_[data-slot=navigation-menu-viewport]]:ease-in-out',
                '[&_[data-slot=navigation-menu-viewport]]:data-open:fade-in-0 [&_[data-slot=navigation-menu-viewport]]:data-closed:fade-out-0',
                '[&_[data-slot=navigation-menu-viewport]]:data-open:zoom-in-100 [&_[data-slot=navigation-menu-viewport]]:data-closed:zoom-out-100',
              )}
            >
              <NavigationMenuList className="gap-1">
                {MENU_ITEMS.map((item) => {
                  if (item.id === 'system') {
                    return (
                      <NavigationMenuItem key={item.id}>
                        <NavigationMenuTrigger
                          className={cn(
                            'h-auto rounded-full bg-transparent px-4 py-2 text-sm font-medium transition-all hover:bg-slack-surface-tertiary hover:text-slack-text-primary focus:bg-transparent data-[state=open]:bg-slack-surface-tertiary',
                            activeSection === item.id
                              ? 'text-slack-text-primary'
                              : 'text-slack-text-secondary'
                          )}
                        >
                          {item.label}
                        </NavigationMenuTrigger>
                        <SystemMegaMenu />
                      </NavigationMenuItem>
                    );
                  }
                  return (
                    <NavigationMenuItem key={item.id}>
                      <NavigationMenuLink
                        asChild
                        className={cn(
                          'cursor-pointer rounded-full bg-transparent px-4 py-2 text-sm font-medium transition-colors hover:text-slack-text-primary',
                          activeSection === item.id
                            ? 'bg-slack-surface-tertiary text-slack-text-primary'
                            : 'text-slack-text-secondary'
                        )}
                        onClick={() => handleItemClick(item.id)}
                      >
                        <button type="button">{item.label}</button>
                      </NavigationMenuLink>
                    </NavigationMenuItem>
                  );
                })}
              </NavigationMenuList>
            </NavigationMenu>
          </div>

          {/* Desktop actions */}
          <div className="flex items-center gap-2">
            <Link href="/auth/login" className="hidden md:block">
              <Button variant="outline" size="sm" className="rounded-full border-slack-border">
                Sign In
              </Button>
            </Link>
            <Link href="/auth/register" className="hidden md:block">
              <Button size="sm" className="rounded-full bg-slack-primary text-slack-text-primary hover:bg-slack-primary-light">
                Get Started
              </Button>
            </Link>

            {/* Mobile trigger */}
            <div className="lg:hidden">
              <Sheet open={isSheetOpen} onOpenChange={handleSheetOpenChange}>
                <SheetTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon-lg"
                    className="rounded-full text-slack-text-secondary"
                    aria-label={isSheetOpen ? 'Close menu' : 'Open menu'}
                  >
                    <Menu className="size-5" />
                  </Button>
                </SheetTrigger>
                <SheetContent
                  side="right"
                  className="flex w-[280px] flex-col gap-6 bg-slack-surface-primary p-6 sm:w-[350px]"
                >
                  {/* Sheet header */}
                  <div className="flex items-center gap-2.5 mt-6">
                    <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-slack-primary">
                      <Image
                        src="/logo1.png"
                        alt="Chatter logo"
                        width={22}
                        height={22}
                        className="brightness-0 invert"
                      />
                    </div>
                    <span className="text-lg font-bold text-slack-text-primary">Chatter</span>
                  </div>

                  {/* Nav links */}
                  <div className="flex flex-col gap-4">
                    {MENU_ITEMS.map((item) => {
                      if (item.id === 'system') {
                        return (
                          <Accordion key={item.id} type="single" collapsible className="w-full">
                            <AccordionItem value="system" className="border-none">
                              <AccordionTrigger className="justify-between py-0 text-base font-medium text-slack-text-primary hover:no-underline">
                                {item.label}
                              </AccordionTrigger>
                              <AccordionContent className="mt-1 ml-2 flex !h-auto flex-col gap-3 border-l border-slack-border pb-0 pl-4 text-base font-medium [&_a]:no-underline">
                                <div className="flex flex-col gap-2 pt-4">
                                  <span className="text-[10px] font-semibold uppercase tracking-[0.2em] text-slack-text-secondary">
                                    Modules
                                  </span>
                                  {SYSTEM_MODULES.map((mod) => (
                                    <SheetClose key={mod.title} asChild>
                                      <button
                                        type="button"
                                        onClick={() => handleItemClick(item.id)}
                                        className="flex items-center gap-3 rounded-xl px-3 py-2 text-left text-sm font-medium tracking-tight text-slack-text-secondary transition-colors hover:bg-slack-surface-tertiary hover:text-slack-primary"
                                      >
                                        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-slack-primary/20">
                                          <mod.icon className="h-3.5 w-3.5 text-slack-primary" />
                                        </div>
                                        <div>
                                          <p className="text-sm font-medium text-slack-text-primary">{mod.title}</p>
                                          <p className="text-[10px] uppercase tracking-[0.15em] text-slack-text-secondary">{mod.role}</p>
                                        </div>
                                      </button>
                                    </SheetClose>
                                  ))}
                                </div>
                              </AccordionContent>
                            </AccordionItem>
                          </Accordion>
                        );
                      }
                      return (
                        <SheetClose key={item.id} asChild>
                          <button
                            type="button"
                            onClick={() => handleItemClick(item.id)}
                            className={`block w-full rounded-xl px-3 py-2 text-left text-base font-medium transition-colors hover:bg-slack-surface-tertiary ${
                              activeSection === item.id
                                ? 'bg-slack-surface-tertiary text-slack-text-primary'
                                : 'text-slack-text-secondary'
                            }`}
                          >
                            {item.label}
                          </button>
                        </SheetClose>
                      );
                    })}
                  </div>

                  {/* Auth buttons */}
                  <div className="mt-auto flex flex-col gap-3">
                    <SheetClose asChild>
                      <Link href="/auth/login" className="block w-full">
                        <Button variant="outline" className="w-full rounded-full border-slack-border">
                          Sign In
                        </Button>
                      </Link>
                    </SheetClose>
                    <SheetClose asChild>
                      <Link href="/auth/register" className="block w-full">
                        <Button className="w-full rounded-full bg-slack-primary text-slack-text-inverse hover:bg-slack-primary-light">
                          Get Started
                        </Button>
                      </Link>
                    </SheetClose>
                  </div>
                </SheetContent>
              </Sheet>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
