'use client';

import Link from 'next/link';

import { Button } from '@/components/ui/Button';

interface HeroContent {
  headline: string;
  highlightWord: string;
  subheading: string;
  ctaText: string;
  ctaHref: string;
}

const HERO_CONTENT: HeroContent = {
  headline: 'Your new way for communication',
  highlightWord: 'communication',
  subheading:
    'Stay connected with friends, family, and colleagues through real-time messaging',
  ctaText: 'Register Now',
  ctaHref: '/auth/register',
};

export interface HeroSectionProps {
  className?: string;
}

export function HeroSection({ className = '' }: HeroSectionProps) {
  const parts = HERO_CONTENT.headline.split(HERO_CONTENT.highlightWord);

  return (
    <section
      id="home"
      className={`animate-fade-in px-4 py-16 sm:px-6 lg:py-24 ${className}`.trim()}
      aria-labelledby="hero-heading"
    >
      <div className="mx-auto max-w-6xl">
        <div className="max-w-3xl">
          <h1 id="hero-heading" className="text-center text-3xl font-bold leading-tight text-kiro-slate-100 sm:text-left sm:text-5xl">
            {parts[0]}
            <span className="text-kiro-orange-400">
              {HERO_CONTENT.highlightWord}
            </span>
            {parts[1]}
          </h1>
          <p className="mt-6 text-center text-lg text-kiro-slate-500 sm:text-left sm:text-xl">
            {HERO_CONTENT.subheading}
          </p>
          <div className="mt-8 flex justify-center sm:justify-start">
            <Link 
              href={HERO_CONTENT.ctaHref}
              className="focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-kiro-ink-950 rounded-lg"
            >
              <Button variant="secondary" size="lg">
                {HERO_CONTENT.ctaText}
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
