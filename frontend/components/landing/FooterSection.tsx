'use client';

import Image from 'next/image';
import Link from 'next/link';

const NAV_LINKS = [
  { href: '#home', label: 'Home' },
  { href: '#readiness', label: 'Readiness' },
  { href: '#system', label: 'System' },
  { href: '#integration', label: 'Integration' },
  { href: '#community', label: 'Community' },
  { href: '#stories', label: 'Stories' },
  { href: '#contact', label: 'Contact' },
];

const SERVICE_LINKS = [
  { href: '#readiness', label: 'Launch Readiness' },
  { href: '#system', label: 'Product Systems' },
  { href: '#integration', label: 'Compliance-Ready' },
  { href: '#integration', label: 'Workflow Automation' },
  { href: '#community', label: 'Community Rooms' },
  { href: '#stories', label: 'Success Playbooks' },
];

const SOCIAL_LINKS = [
  { href: 'https://facebook.com', label: 'Facebook' },
  { href: 'https://twitter.com', label: 'Twitter' },
  { href: 'https://linkedin.com', label: 'LinkedIn' },
  { href: 'https://instagram.com', label: 'Instagram' },
];

export interface FooterSectionProps {
  className?: string;
}

export function FooterSection({ className = '' }: FooterSectionProps) {
  return (
    <footer
      className={`relative overflow-hidden border-t border-slack-border bg-slack-surface-secondary text-slack-text-primary ${className}`.trim()}
      aria-label="Site footer"
    >


      <div className="relative mx-auto w-full max-w-[1180px] px-4 pb-8 pt-10 sm:px-6 sm:pt-14 md:px-8 md:pt-16">
        <div className="grid gap-8 border-b border-slack-border pb-12 sm:gap-10 md:grid-cols-[1.5fr_1fr_1fr_1fr]">
          <div className="relative z-10 max-w-sm">
            <Link
              href="#home"
              className="inline-flex items-center gap-3 text-slack-text-primary no-underline hover:no-underline"
            >
              <Image
                src="/logo1.png"
                alt="Chatter logo"
                width={44}
                height={44}
                className="h-11 w-11 rounded-xl object-contain"
              />
              <span className="text-lg font-semibold tracking-tight sm:text-xl">Chatter</span>
            </Link>
            <p className="mt-4 text-sm leading-relaxed text-slack-text-secondary">
              Shape bold ideas into confident launches with a workspace built for clarity.
            </p>
          </div>

          <div className="relative z-10">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slack-text-primary">
              Navigation
            </p>
            <ul className="mt-4 space-y-2.5">
              {NAV_LINKS.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-slack-text-secondary transition-colors hover:text-slack-text-primary"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div className="relative z-10">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slack-text-primary">
              Services
            </p>
            <ul className="mt-4 space-y-2.5">
              {SERVICE_LINKS.map((service) => (
                <li key={service.label}>
                  <Link
                    href={service.href}
                    className="text-sm text-slack-text-secondary transition-colors hover:text-slack-text-primary"
                  >
                    {service.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div className="relative z-10">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slack-text-primary">
              Follow Us
            </p>
            <ul className="mt-4 space-y-2.5">
              {SOCIAL_LINKS.map((link) => (
                <li key={link.label}>
                  <a
                    href={link.href}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-sm text-slack-text-secondary transition-colors hover:text-slack-text-primary"
                  >
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="relative z-10 mt-6 flex flex-col items-start justify-between gap-4 text-xs text-slack-text-secondary md:flex-row md:items-center">
          <p>(c) 2026 Chatter Inc. All rights reserved.</p>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
            <a href="#" className="transition-colors hover:text-slack-text-primary">
              Terms of Service
            </a>
            <a href="#" className="transition-colors hover:text-slack-text-primary">
              Privacy Policy
            </a>
            <a href="#" className="transition-colors hover:text-slack-text-primary">
              Accessibility Statement
            </a>
          </div>
        </div>


      </div>
    </footer>
  );
}
