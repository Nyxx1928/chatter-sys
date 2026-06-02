'use client';

import Link from 'next/link';

const HERO_STATS = [
  { value: '2500+', label: 'Launch concepts shaped' },
  { value: '15x', label: 'Growth measured over time' },
  { value: '98%', label: 'Confident launch readiness' },
  { value: '6M', label: 'Signals aligned as one' },
];

const PANEL_METRICS = [
  { value: '78%', label: 'Market Fit' },
  { value: '4.9x', label: 'Team Velocity' },
  { value: '94%', label: 'Stakeholder Buy-In' },
];

export interface HeroSectionProps {
  className?: string;
}

export function HeroSection({ className = '' }: HeroSectionProps) {
  return (
    <section
      id="home"
      className={`relative overflow-hidden px-4 pb-16 pt-20 sm:px-6 sm:pb-20 sm:pt-32 lg:pt-36 ${className}`.trim()}
      aria-labelledby="hero-heading"
    >
      <div aria-hidden className="absolute inset-0">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,#1d1036_0%,transparent_55%)]" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_25%,rgba(111,66,193,0.35),transparent_55%)]" />
        <div className="absolute right-[-120px] top-[-140px] h-[320px] w-[320px] rounded-full bg-kiro-purple-600/30 blur-[110px] animate-[glowPulse_8s_ease-in-out_infinite]" />
        <div className="absolute bottom-[-140px] left-[-120px] h-[320px] w-[320px] rounded-full bg-kiro-purple-500/25 blur-[120px] animate-[float_16s_ease-in-out_infinite]" />
        <div className="absolute inset-0 opacity-[0.08] [background-image:linear-gradient(to_right,rgba(255,255,255,0.18)_1px,transparent_1px),linear-gradient(to_bottom,rgba(255,255,255,0.18)_1px,transparent_1px)] [background-size:96px_96px]" />
      </div>

      <div className="relative z-10 mx-auto grid w-full max-w-6xl gap-12 lg:grid-cols-[1.05fr_0.95fr]">
        <div className="text-left">
          <span className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-[11px] font-semibold uppercase tracking-[0.3em] text-white/70">
            Idea Lab Launch
          </span>
          <h1
            id="hero-heading"
            className="mt-6 text-3xl font-semibold leading-tight text-white sm:text-5xl lg:text-6xl"
          >
            Growing ideas,
            <span className="block bg-gradient-to-r from-kiro-purple-400 via-kiro-purple-500 to-kiro-purple-400 bg-clip-text text-transparent">
              inspiring brilliance
            </span>
          </h1>
          <p className="mt-5 max-w-xl text-sm text-white/70 sm:mt-6 sm:text-lg">
            Turn ambitious concepts into launch-ready momentum with a workspace that blends strategy, clarity, and community feedback in one place.
          </p>
          <div className="mt-8 flex flex-col items-stretch gap-3 sm:mt-10 sm:flex-row sm:items-center sm:gap-4">
            <Link
              href="/auth/register"
              className="w-full rounded-full bg-gradient-to-r from-kiro-purple-600 via-kiro-purple-500 to-kiro-purple-400 px-7 py-3 text-center text-xs font-semibold uppercase tracking-[0.25em] text-white shadow-[0_14px_36px_rgba(88,61,196,0.35)] transition-transform hover:-translate-y-0.5 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0a0a0f] sm:w-auto"
            >
              Start Building
            </Link>
            <Link
              href="#stories"
              className="w-full rounded-full border border-white/15 px-7 py-3 text-center text-xs font-semibold uppercase tracking-[0.25em] text-white/80 transition-colors hover:border-kiro-purple-400/60 hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0a0a0f] sm:w-auto"
            >
              Success Stories
            </Link>
          </div>

          <div className="mt-10 grid gap-3 sm:mt-12 sm:grid-cols-2 sm:gap-4 lg:grid-cols-4">
            {HERO_STATS.map((stat) => (
              <div
                key={stat.value}
                className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 backdrop-blur sm:py-4"
              >
                <p className="text-2xl font-semibold text-white">{stat.value}</p>
                <p className="mt-2 text-xs uppercase tracking-[0.2em] text-white/60">
                  {stat.label}
                </p>
              </div>
            ))}
          </div>
        </div>

        <div className="relative">
          <div className="absolute -right-10 top-10 h-24 w-24 rounded-full border border-white/15 bg-white/5 shadow-[0_20px_60px_rgba(111,66,193,0.2)] animate-[float_12s_ease-in-out_infinite]" />
          <div className="relative rounded-[32px] border border-white/10 bg-[#0f0f1a]/90 p-5 shadow-[0_30px_80px_rgba(7,4,18,0.75)] backdrop-blur sm:p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.3em] text-white/50">
                  Insight Deck
                </p>
                <p className="mt-2 text-base font-semibold text-white sm:text-lg">Launch momentum</p>
              </div>
              <span className="rounded-full border border-white/15 bg-white/5 px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.25em] text-white/70">
                Live
              </span>
            </div>

            <div className="mt-5 rounded-2xl border border-white/10 bg-[#0a0a12] p-5 sm:mt-6 sm:p-6">
              <svg
                className="h-36 w-full sm:h-44"
                viewBox="0 0 320 180"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                aria-hidden="true"
              >
                <defs>
                  <linearGradient id="grid" x1="0" y1="0" x2="1" y2="1">
                    <stop offset="0" stopColor="#6f42c1" stopOpacity="0.15" />
                    <stop offset="1" stopColor="#9b7cff" stopOpacity="0.3" />
                  </linearGradient>
                  <linearGradient id="ridge" x1="0" y1="0" x2="1" y2="0">
                    <stop offset="0" stopColor="#6f42c1" stopOpacity="0.35" />
                    <stop offset="0.5" stopColor="#9b7cff" stopOpacity="0.7" />
                    <stop offset="1" stopColor="#6f42c1" stopOpacity="0.35" />
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
                  className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3"
                >
                  <p className="text-base font-semibold text-white sm:text-lg">{metric.value}</p>
                  <p className="mt-1 text-[11px] uppercase tracking-[0.2em] text-white/60">
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
