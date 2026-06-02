'use client';

import Image from 'next/image';
import Link from 'next/link';

const READINESS_STEPS = [
  'Validate the core idea and market signal',
  'Map the collaboration structure',
  'Align launch messaging with the community',
  'Set measurable growth milestones',
];

const SYSTEM_MODULES = [
  {
    title: 'Pulse',
    role: 'AI business planner',
    stats: [
      { value: '2500+', label: 'Launch labs' },
      { value: '7+ regions', label: 'Market scans' },
      { value: '92%', label: 'Signal clarity' },
    ],
  },
  {
    title: 'Relay',
    role: 'Community dashboard',
    stats: [
      { value: '3400+', label: 'Signal loops' },
      { value: '100+', label: 'Founder circles' },
      { value: '94%', label: 'Feedback lift' },
    ],
  },
  {
    title: 'Aurora',
    role: 'Smart founder hub',
    stats: [
      { value: '120+', label: 'Mentor tracks' },
      { value: '90%', label: 'Milestones hit' },
      { value: '8x', label: 'Momentum' },
    ],
  },
];

const INTEGRATION_CARDS = [
  {
    title: '10x Rapid Planning',
    description: 'Align roadmap decisions, launch notes, and team updates in a single workspace.',
  },
  {
    title: 'Compliance-Ready',
    description: 'Secure workflows with shared audit trails and permissioned collaboration.',
  },
  {
    title: 'Engaged Community',
    description: 'Host feedback sessions, gather insights, and shape the story with founders.',
  },
];

const STORY_CARDS = [
  {
    name: 'Fatima Al-Zahra',
    role: 'Growth Director, NewEra',
    quote:
      'Chatter turned scattered signals into a focused roadmap. We launched with confidence and clarity.',
    metric: '4.9x',
    metricLabel: 'Launch Velocity',
  },
  {
    name: 'Rafael Santos',
    role: 'Founder, Arbor Labs',
    quote:
      'The readiness sprint helped us align every team before day one. The community feedback was gold.',
    metric: '92%',
    metricLabel: 'Stakeholder Alignment',
  },
  {
    name: 'Mei Tanaka',
    role: 'Head of Product, SoraFlow',
    quote:
      'We built momentum week after week. The integration layer kept everyone in sync across time zones.',
    metric: '6M',
    metricLabel: 'Signals Mapped',
  },
];

const TRUSTED_PARTNERS = ['NovaLabs', 'Beacon', 'Sage', 'Orbit'];

export interface FeaturesSectionProps {
  className?: string;
}

export function FeaturesSection({ className = '' }: FeaturesSectionProps) {
  return (
    <div className={`px-4 sm:px-6 ${className}`.trim()}>
      <section id="readiness" className="mx-auto w-full max-w-6xl py-14 sm:py-20">
        <div className="grid gap-10 lg:grid-cols-[1.05fr_0.95fr]">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-kiro-purple-400">
              Discover your readiness
            </p>
            <h2 className="mt-4 text-2xl font-semibold text-white sm:text-3xl lg:text-4xl">
              Discover your startup readiness
            </h2>
            <p className="mt-4 max-w-xl text-sm text-white/70 sm:text-base">
              Complete a quick assessment to surface the signals your launch needs. The result is a tailored roadmap and the right community connection points.
            </p>
            <div className="mt-6 grid gap-3 sm:mt-8 sm:grid-cols-2 sm:gap-4">
              {READINESS_STEPS.map((step) => (
                <div
                  key={step}
                  className="rounded-2xl border border-white/10 bg-white/5 px-4 py-4 text-sm text-white/70"
                >
                  {step}
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-3xl border border-white/10 bg-[#0f0f1a]/90 p-6 shadow-[0_30px_80px_rgba(7,4,18,0.6)] sm:p-8">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.3em] text-white/50">
                  Let&apos;s get to know you
                </p>
                <p className="mt-2 text-lg font-semibold text-white">Readiness checkpoint</p>
              </div>
              <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.25em] text-white/70">
                Step 2/4
              </span>
            </div>
            <div className="mt-5 space-y-3 sm:mt-6">
              {[
                'Define the launch goal',
                'Map your collaboration phase',
                'List your most urgent blockers',
                'Outline your launch audience',
              ].map((item, index) => (
                <div
                  key={item}
                  className="flex items-center gap-3 rounded-xl border border-white/10 bg-[#0b0b12] px-4 py-3 text-sm text-white/70"
                >
                  <span className="flex h-6 w-6 items-center justify-center rounded-full border border-white/20 text-[11px] text-white/60">
                    {index + 1}
                  </span>
                  <span>{item}</span>
                </div>
              ))}
            </div>
            <div className="mt-5 h-2 w-full overflow-hidden rounded-full bg-white/10 sm:mt-6">
              <div className="h-full w-2/3 rounded-full bg-gradient-to-r from-kiro-purple-600 via-kiro-purple-500 to-kiro-purple-400" />
            </div>
            <div className="mt-5 flex items-center justify-between text-xs uppercase tracking-[0.2em] text-white/50 sm:mt-6">
              <span>Progress</span>
              <span>66%</span>
            </div>
            <button
              type="button"
              className="mt-6 w-full rounded-full bg-white/10 py-3 text-xs font-semibold uppercase tracking-[0.25em] text-white/80 transition-colors hover:bg-white/20"
            >
              Continue
            </button>
          </div>
        </div>
      </section>

      <section id="system" className="mx-auto w-full max-w-6xl py-14 sm:py-20">
        <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-[linear-gradient(140deg,#0d0b1a_0%,#140f26_40%,#120b1f_100%)] p-6 sm:p-10">
          <div className="absolute -right-20 -top-32 h-64 w-64 rounded-full bg-kiro-purple-600/20 blur-[120px]" aria-hidden />
          <div className="absolute -bottom-24 left-10 h-56 w-56 rounded-full bg-kiro-purple-500/20 blur-[110px]" aria-hidden />
          <div className="relative grid gap-10 lg:grid-cols-[1.05fr_0.95fr]">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-kiro-purple-400">
                Holistic system
              </p>
              <h2 className="mt-4 text-2xl font-semibold text-white sm:text-3xl lg:text-4xl">
                Holistic product development system
              </h2>
              <p className="mt-4 text-sm text-white/70 sm:text-base">
                Three powerful tracks work together to turn vision into measurable growth. Each module reinforces the next for a seamless launch path.
              </p>
              <Link
                href="#integration"
                className="mt-6 inline-flex rounded-full border border-white/15 px-6 py-3 text-xs font-semibold uppercase tracking-[0.25em] text-white/80 transition-colors hover:border-kiro-purple-400/60 hover:text-white sm:mt-8"
              >
                Explore more
              </Link>
            </div>
            <div className="grid gap-4">
              {SYSTEM_MODULES.map((module) => (
                <div
                  key={module.title}
                  className="rounded-2xl border border-white/10 bg-white/5 p-6"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-lg font-semibold text-white">{module.title}</p>
                      <p className="text-xs uppercase tracking-[0.2em] text-white/50">
                        {module.role}
                      </p>
                    </div>
                    <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-[10px] uppercase tracking-[0.25em] text-white/60">
                      Active
                    </span>
                  </div>
                  <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
                    {module.stats.map((stat) => (
                      <div
                        key={stat.label}
                        className="rounded-xl bg-[#0b0b12] px-3 py-2"
                      >
                        <p className="text-sm font-semibold text-white sm:text-base">
                          {stat.value}
                        </p>
                        <p className="mt-1 text-[9px] uppercase tracking-[0.16em] text-white/50 leading-snug break-words sm:text-[10px] sm:tracking-[0.2em]">
                          {stat.label}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section id="integration" className="mx-auto w-full max-w-6xl py-14 sm:py-20">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.3em] text-kiro-purple-400">
            Smooth integration
          </p>
          <h2 className="mt-4 text-2xl font-semibold text-white sm:text-3xl lg:text-4xl">
            Advantages of smooth and effortless integration
          </h2>
          <p className="mx-auto mt-4 max-w-2xl text-sm text-white/70 sm:text-base">
            Connect product, people, and community without friction. The system adapts to the rhythm of your team and keeps every touchpoint aligned.
          </p>
        </div>
        <div className="mt-8 grid gap-4 sm:mt-12 sm:gap-6 md:grid-cols-3">
          {INTEGRATION_CARDS.map((card) => (
            <div
              key={card.title}
              className="rounded-2xl border border-white/10 bg-white/5 p-5 sm:p-6"
            >
              <div className="mb-4 inline-flex h-10 w-10 items-center justify-center rounded-xl bg-kiro-purple-600/40">
                <span className="h-2 w-2 rounded-full bg-kiro-purple-400" />
              </div>
              <h3 className="text-lg font-semibold text-white">{card.title}</h3>
              <p className="mt-3 text-sm text-white/60">{card.description}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="community" className="mx-auto w-full max-w-6xl py-14 sm:py-20">
        <div className="grid gap-10 lg:grid-cols-[1.05fr_0.95fr]">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-kiro-purple-400">
              Community space
            </p>
            <h2 className="mt-4 text-2xl font-semibold text-white sm:text-3xl lg:text-4xl">
              Live interactions within the community space
            </h2>
            <p className="mt-4 max-w-xl text-sm text-white/70 sm:text-base">
              Host live sessions, share updates, and build momentum with founders who know the journey. Every interaction becomes a source of confidence.
            </p>
            <div className="mt-5 space-y-3 text-sm text-white/70 sm:mt-6">
              <p>Weekly feedback rooms with curated mentors.</p>
              <p>Real-time collaboration with multi-team visibility.</p>
              <p>Signals dashboard that highlights what&apos;s next.</p>
            </div>
            <div className="mt-6 rounded-2xl border border-white/10 bg-white/5 p-5 sm:mt-8 sm:p-6">
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-white/50">
                Trusted by
              </p>
              <div className="mt-4 flex flex-wrap items-center gap-3">
                {TRUSTED_PARTNERS.map((partner) => (
                  <span
                    key={partner}
                    className="rounded-full border border-white/15 bg-white/5 px-4 py-2 text-[11px] font-semibold uppercase tracking-[0.2em] text-white/70"
                  >
                    {partner}
                  </span>
                ))}
              </div>
            </div>
          </div>
          <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-[#0f0f1a]/90 aspect-[4/3] sm:aspect-[16/10]">
            <Image
              src="/image.png"
              alt="Community collaboration session"
              fill
              className="object-cover"
              sizes="(min-width: 1024px) 480px, (min-width: 640px) 60vw, 100vw"
              priority
            />
            <div
              aria-hidden
              className="absolute inset-0 bg-gradient-to-t from-[#0b0b12]/70 via-transparent to-transparent"
            />
            <div className="absolute bottom-4 left-4 right-4 rounded-2xl border border-white/10 bg-[#0b0b12]/70 px-4 py-3 text-[11px] font-semibold uppercase tracking-[0.2em] text-white/80 backdrop-blur">
              Community sessions in action
            </div>
          </div>
        </div>
      </section>

      <section id="stories" className="mx-auto w-full max-w-6xl py-14 sm:py-20">
        <div className="flex flex-wrap items-center justify-between gap-6">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-kiro-purple-400">
              Success stories
            </p>
            <h2 className="mt-4 text-2xl font-semibold text-white sm:text-3xl lg:text-4xl">
              Founders who saw beyond ordinary
            </h2>
          </div>
          <Link
            href="/auth/register"
            className="rounded-full border border-white/15 px-6 py-3 text-xs font-semibold uppercase tracking-[0.25em] text-white/80 transition-colors hover:border-kiro-purple-400/60 hover:text-white"
          >
            Join them
          </Link>
        </div>
        <div className="mt-8 grid gap-4 sm:mt-10 sm:gap-6 lg:grid-cols-3">
          {STORY_CARDS.map((story) => (
            <div
              key={story.name}
              className="rounded-2xl border border-white/10 bg-white/5 p-5 sm:p-6"
            >
              <p className="text-sm text-white/70">{story.quote}</p>
              <div className="mt-6 flex items-center justify-between border-t border-white/10 pt-4">
                <div>
                  <p className="text-sm font-semibold text-white">{story.name}</p>
                  <p className="text-[11px] uppercase tracking-[0.2em] text-white/50">
                    {story.role}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-lg font-semibold text-white">{story.metric}</p>
                  <p className="text-[10px] uppercase tracking-[0.2em] text-white/50">
                    {story.metricLabel}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section id="contact" className="mx-auto w-full max-w-6xl pb-20 pt-8 sm:pb-28 sm:pt-10">
        <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-[linear-gradient(120deg,#1a1230_0%,#1c0f2a_45%,#140b22_100%)] p-6 text-center sm:p-10">
          <div className="absolute -left-20 top-10 h-52 w-52 rounded-full bg-kiro-purple-600/30 blur-[110px]" aria-hidden />
          <div className="absolute -right-16 bottom-0 h-52 w-52 rounded-full bg-kiro-purple-500/25 blur-[120px]" aria-hidden />
          <div className="relative">
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-white/60">
              Ready to begin
            </p>
            <h2 className="mt-4 text-2xl font-semibold text-white sm:text-3xl lg:text-4xl">
              Ready to craft your ultimate success journey?
            </h2>
            <p className="mx-auto mt-4 max-w-2xl text-sm text-white/70 sm:text-base">
              Join Chatter to align your launch signals, connect with founders, and move from idea to impact with confidence.
            </p>
            <div className="mt-6 flex flex-col items-stretch justify-center gap-3 sm:mt-8 sm:flex-row sm:items-center sm:gap-4">
              <Link
                href="/auth/register"
                className="w-full rounded-full bg-gradient-to-r from-kiro-purple-600 via-kiro-purple-500 to-kiro-purple-400 px-7 py-3 text-center text-xs font-semibold uppercase tracking-[0.25em] text-white shadow-[0_14px_36px_rgba(88,61,196,0.35)] transition-transform hover:-translate-y-0.5 sm:w-auto"
              >
                Start Journey
              </Link>
              <a
                href="mailto:hello@chatterchat.io"
                className="w-full rounded-full border border-white/15 px-7 py-3 text-center text-xs font-semibold uppercase tracking-[0.25em] text-white/80 transition-colors hover:border-kiro-purple-400/60 hover:text-white sm:w-auto"
              >
                Talk to us
              </a>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
