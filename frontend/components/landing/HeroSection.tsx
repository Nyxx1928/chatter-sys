'use client';

import Link from 'next/link';
import { AnimatedStatCounter } from '@/components/ui/AnimatedStatCounter';

const HERO_STATS = [
  { value: 2500, suffix: '+', label: 'Launch concepts shaped' },
  { value: 15, suffix: 'x', label: 'Growth measured over time' },
  { value: 98, suffix: '%', label: 'Confident launch readiness' },
  { value: 6, suffix: 'M', label: 'Signals aligned as one' },
];

const PANEL_METRICS = [
  { value: 78, suffix: '%', label: 'Market Fit' },
  { value: 4.9, suffix: 'x', label: 'Team Velocity', decimals: 1 },
  { value: 94, suffix: '%', label: 'Stakeholder Buy-In' },
];

export interface HeroSectionProps {
  className?: string;
}

export function HeroSection({ className = '' }: HeroSectionProps) {
  return (
    <section
      id="home"
      className={`sticky top-0 z-0 px-4 pb-16 pt-20 sm:px-6 sm:pb-20 sm:pt-24 lg:pt-24 relative overflow-hidden ${className}`.trim()}
      aria-labelledby="hero-heading"
    >
      {/* Bottom Border Effect */}
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-slack-border to-transparent" />

      <div className="relative mx-auto grid w-full max-w-6xl gap-12 lg:grid-cols-[1.05fr_0.95fr]">
        <div className="text-left">
          <span className="inline-flex items-center gap-2 rounded-pill border border-slack-border bg-slack-surface-tertiary px-4 py-2 text-[11px] font-semibold uppercase tracking-[0.3em] text-slack-text-secondary">
            Idea Lab Launch
          </span>
          <h1
            id="hero-heading"
            className="mt-6 text-3xl font-semibold leading-tight text-slack-text-primary sm:text-5xl lg:text-6xl"
          >
            Growing ideas,
            <span className="text-slack-primary">
              inspiring brilliance
            </span>
          </h1>
          <p className="mt-5 max-w-xl text-sm text-slack-text-secondary sm:mt-6 sm:text-lg">
            Turn ambitious concepts into launch-ready momentum with a workspace that blends strategy, clarity, and community feedback in one place.
          </p>
          <div className="mt-8 flex flex-col items-stretch gap-3 sm:mt-10 sm:flex-row sm:items-center sm:gap-4">
            <Link
              href="/auth/register"
              className="w-full rounded-pill bg-slack-primary px-7 py-3 text-center text-xs font-semibold uppercase tracking-[0.25em] text-slack-text-primary shadow-slack-lg transition-transform hover:-translate-y-0.5 focus:outline-none focus-visible:ring-2 focus-visible:ring-slack-primary focus-visible:ring-offset-2 focus-visible:ring-offset-slack-surface-primary sm:w-auto"
            >
              Start Building
            </Link>
            <Link
              href="#stories"
              className="w-full rounded-pill border border-slack-border px-7 py-3 text-center text-xs font-semibold uppercase tracking-[0.25em] text-slack-text-secondary transition-colors hover:bg-slack-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-slack-primary focus-visible:ring-offset-2 focus-visible:ring-offset-slack-surface-primary sm:w-auto"
            >
              Success Stories
            </Link>
          </div>

          <div className="mt-10 grid gap-3 sm:mt-12 sm:grid-cols-2 sm:gap-4 lg:grid-cols-4">
            {HERO_STATS.map((stat) => (
              <div
                key={`${stat.value}${stat.suffix}`}
                className="rounded-2xl border border-slack-border bg-slack-surface-secondary px-4 py-3 sm:py-4"
              >
                <AnimatedStatCounter
                  value={stat.value}
                  suffix={stat.suffix}
                  className="text-2xl font-semibold text-slack-text-primary"
                />
                <p className="mt-2 text-xs uppercase tracking-[0.2em] text-slack-text-secondary">
                  {stat.label}
                </p>
              </div>
            ))}
          </div>
        </div>

        <div className="relative">
          <div className="relative rounded-[32px] border border-slack-border bg-slack-surface-primary p-5 shadow-slack-lg sm:p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.3em] text-slack-text-secondary">
                  Insight Deck
                </p>
                <p className="mt-2 text-base font-semibold text-slack-text-primary sm:text-lg">Launch momentum</p>
              </div>
              <span className="rounded-pill border border-slack-border bg-slack-surface-tertiary px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.25em] text-slack-text-secondary">
                Live
              </span>
            </div>

            <div className="mt-5 rounded-2xl border border-slack-border bg-slack-surface-secondary p-5 sm:mt-6 sm:p-6">
              <svg
                className="h-36 w-full sm:h-44"
                viewBox="0 0 320 180"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                aria-hidden="true"
              >
                <defs>
                  <linearGradient id="grid" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0" stopColor="#4A154B" stopOpacity="0.15" />
                    <stop offset="1" stopColor="#7C2382" stopOpacity="0.3" />
                  </linearGradient>
                  <linearGradient id="ridge" x1="0" y1="0" x2="1" y2="0">
                    <stop offset="0" stopColor="#4A154B" stopOpacity="0.35" />
                    <stop offset="0.5" stopColor="#7C2382" stopOpacity="0.7" />
                    <stop offset="1" stopColor="#4A154B" stopOpacity="0.35" />
                  </linearGradient>
                </defs>
                {Array.from({ length: 6 }).map((_, index) => (
                  <line
                    key={`h-${index}`}
                    x1="20"
                    x2="300"
                    y1={30 + index * 25}
                    y2={30 + index * 25}
                    stroke="url(#grid)"
                    strokeWidth="1"
                  />
                ))}
                {Array.from({ length: 7 }).map((_, index) => (
                  <line
                    key={`v-${index}`}
                    x1={30 + index * 40}
                    x2={30 + index * 40}
                    y1="20"
                    y2="160"
                    stroke="url(#grid)"
                    strokeWidth="1"
                  />
                ))}
                <path
                  d="M20 140 C 70 110 120 160 170 120 C 210 90 260 120 300 80"
                  stroke="url(#ridge)"
                  strokeWidth="3"
                  fill="none"
                />
                <path
                  d="M20 120 C 60 95 120 130 170 90 C 220 60 270 95 300 60"
                  stroke="url(#ridge)"
                  strokeWidth="2"
                  fill="none"
                />
              </svg>
            </div>

            <div className="mt-5 grid gap-3 sm:mt-6 sm:grid-cols-3">
              {PANEL_METRICS.map((metric) => (
                <div
                  key={metric.label}
                  className="rounded-2xl border border-slack-border bg-slack-surface-secondary px-4 py-3"
                >
                  <AnimatedStatCounter
                    value={metric.value}
                    suffix={metric.suffix}
                    decimals={'decimals' in metric ? metric.decimals : undefined}
                    className="text-base font-semibold text-slack-text-primary sm:text-lg"
                  />
                  <p className="mt-1 text-[11px] uppercase tracking-[0.2em] text-slack-text-secondary">
                    {metric.label}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
