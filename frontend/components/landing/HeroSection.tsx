'use client';

import Link from 'next/link';

export interface HeroSectionProps {
  className?: string;
}

export function HeroSection({ className = '' }: HeroSectionProps) {
  return (
    <section
      id="home"
      className={`relative overflow-hidden px-6 py-20 sm:py-28 lg:py-36 ${className}`.trim()}
      aria-labelledby="hero-heading"
    >
      {/* Animated network background */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-transparent via-kiro-purple-600/5 to-transparent" />
        {/* Network nodes and lines */}
        <svg
          className="absolute inset-0 h-full w-full"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <defs>
            <radialGradient id="node-glow" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stopColor="#9b7cff" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#9b7cff" stopOpacity="0" />
            </radialGradient>
            <linearGradient id="line-gradient" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#6f42c1" stopOpacity="0.3" />
              <stop offset="50%" stopColor="#9b7cff" stopOpacity="0.5" />
              <stop offset="100%" stopColor="#6f42c1" stopOpacity="0.3" />
            </linearGradient>
          </defs>
          
          {/* Connection lines */}
          <line x1="15%" y1="30%" x2="35%" y2="60%" stroke="url(#line-gradient)" strokeWidth="2" />
          <line x1="35%" y1="60%" x2="50%" y2="70%" stroke="url(#line-gradient)" strokeWidth="2" />
          <line x1="50%" y1="70%" x2="70%" y2="55%" stroke="url(#line-gradient)" strokeWidth="2" />
          <line x1="70%" y1="55%" x2="85%" y2="35%" stroke="url(#line-gradient)" strokeWidth="2" />
          <line x1="35%" y1="60%" x2="70%" y2="55%" stroke="url(#line-gradient)" strokeWidth="2" />
          <line x1="15%" y1="30%" x2="50%" y2="20%" stroke="url(#line-gradient)" strokeWidth="2" />
          <line x1="50%" y1="20%" x2="85%" y2="35%" stroke="url(#line-gradient)" strokeWidth="2" />
          
          {/* Glowing nodes */}
          <circle cx="15%" cy="30%" r="8" fill="url(#node-glow)" />
          <circle cx="15%" cy="30%" r="4" fill="#9b7cff" />
          
          <circle cx="35%" cy="60%" r="10" fill="url(#node-glow)" />
          <circle cx="35%" cy="60%" r="5" fill="#9b7cff" />
          
          <circle cx="50%" cy="70%" r="8" fill="url(#node-glow)" />
          <circle cx="50%" cy="70%" r="4" fill="#9b7cff" />
          
          <circle cx="70%" cy="55%" r="10" fill="url(#node-glow)" />
          <circle cx="70%" cy="55%" r="5" fill="#9b7cff" />
          
          <circle cx="85%" cy="35%" r="12" fill="url(#node-glow)" />
          <circle cx="85%" cy="35%" r="6" fill="#9b7cff" />
          
          <circle cx="50%" cy="20%" r="6" fill="url(#node-glow)" />
          <circle cx="50%" cy="20%" r="3" fill="#9b7cff" />
          
          {/* Additional decorative stars */}
          <circle cx="25%" cy="15%" r="2" fill="#9b7cff" opacity="0.6" />
          <circle cx="60%" cy="40%" r="2" fill="#9b7cff" opacity="0.6" />
          <circle cx="80%" cy="65%" r="2" fill="#9b7cff" opacity="0.6" />
          <circle cx="40%" cy="85%" r="2" fill="#9b7cff" opacity="0.6" />
        </svg>
      </div>

      {/* Content */}
      <div className="relative z-10 mx-auto max-w-4xl text-center">
        <h1
          id="hero-heading"
          className="animate-fade-in text-4xl font-bold leading-tight text-white sm:text-5xl lg:text-6xl"
        >
          Connect. Chat. Collaborate.
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-base text-white/70 sm:text-lg lg:text-xl">
          Seamless communication, instant messaging, and team collaboration, all in one powerful platform.
        </p>
        <div className="mt-10 flex justify-center">
          <Link
            href="/auth/register"
            className="rounded-lg bg-kiro-purple-600 px-8 py-3.5 text-base font-medium text-white transition-colors hover:bg-kiro-purple-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0a0a0f]"
          >
            Start Chatting Now
          </Link>
        </div>
      </div>
    </section>
  );
}
