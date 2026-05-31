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
      className={`relative overflow-hidden border-t border-white/10 bg-[linear-gradient(180deg,#09060f_0%,#120b1a_55%,#09050e_100%)] text-white ${className}`.trim()}
      aria-label="Site footer"
    >
      <div aria-hidden className="pointer-events-none absolute inset-0">
        <div className="absolute inset-0 opacity-[0.08] [background-image:linear-gradient(to_right,rgba(255,255,255,0.12)_1px,transparent_1px),linear-gradient(to_bottom,rgba(255,255,255,0.12)_1px,transparent_1px)] [background-size:64px_64px]" />
        <div className="absolute bottom-[-140px] right-[-80px] h-[340px] w-[340px] rounded-full bg-kiro-purple-600/25 blur-[95px]" />
      </div>

      <div className="relative mx-auto w-full max-w-[1180px] px-4 pb-8 pt-10 sm:px-6 sm:pt-14 md:px-8 md:pt-16">
        <div className="grid gap-8 border-b border-white/10 pb-12 sm:gap-10 md:grid-cols-[1.5fr_1fr_1fr_1fr]">
          <div className="relative z-10 max-w-sm">
            <Link
              href="#home"
              className="inline-flex items-center gap-3 text-white no-underline hover:no-underline"
            >
              <Image
                src="/logo1.png"
                alt="Chatter logo"
                width={44}
                height={44}
                className="h-11 w-11 rounded-xl object-contain brightness-0 invert"
              />
              <span className="text-lg font-semibold tracking-tight sm:text-xl">Chatter</span>
            </Link>
            <p className="mt-4 text-sm leading-relaxed text-[#c7a3ae]">
              Shape bold ideas into confident launches with a workspace built for clarity.
            </p>
          </div>

          <div className="relative z-10">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/80">
              Navigation
            </p>
            <ul className="mt-4 space-y-2.5">
              {NAV_LINKS.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-[#c7a3ae] transition-colors hover:text-white"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div className="relative z-10">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/80">
              Services
            </p>
            <ul className="mt-4 space-y-2.5">
              {SERVICE_LINKS.map((service) => (
                <li key={service.label}>
                  <Link
                    href={service.href}
                    className="text-sm text-[#c7a3ae] transition-colors hover:text-white"
                  >
                    {service.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div className="relative z-10">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/80">
              Follow Us
            </p>
            <ul className="mt-4 space-y-2.5">
              {SOCIAL_LINKS.map((link) => (
                <li key={link.label}>
                  <a
                    href={link.href}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-sm text-[#c7a3ae] transition-colors hover:text-white"
                  >
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="relative z-10 mt-6 flex flex-col items-start justify-between gap-4 text-xs text-[#b88f9b] md:flex-row md:items-center">
          <p>(c) 2026 Chatter Inc. All rights reserved.</p>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
            <a href="#" className="transition-colors hover:text-white">
              Terms of Service
            </a>
            <a href="#" className="transition-colors hover:text-white">
              Privacy Policy
            </a>
            <a href="#" className="transition-colors hover:text-white">
              Accessibility Statement
            </a>
          </div>
        </div>

        <p
          aria-hidden
          className="pointer-events-none absolute bottom-2 left-0 select-none text-[56px] font-semibold leading-none tracking-tight text-[rgba(227,157,171,0.12)] md:text-[160px]"
        >
          Chatter
        </p>
      </div>
    </footer>
  );
}
