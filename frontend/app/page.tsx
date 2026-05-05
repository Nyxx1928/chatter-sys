'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  HeroSection,
  NavigationHeader,
  SplashScreen,
  UserAvatarDisplay,
} from '@/components/landing';
import { useAuthStore } from '@/lib/store/authStore';

export default function Home() {
  const router = useRouter();
  const { isAuthenticated, isInitialized } = useAuthStore();
  const [showSplash, setShowSplash] = useState(true);
  const [showLanding, setShowLanding] = useState(false);

  // Redirect authenticated users to chat after session validation
  useEffect(() => {
    if (isInitialized && isAuthenticated) {
      router.push('/chat');
    }
  }, [isInitialized, isAuthenticated, router]);

  // Handle splash completion with fade transition
  const handleSplashComplete = () => {
    setShowSplash(false);
    // Small delay to allow fade-out before showing landing
    setTimeout(() => setShowLanding(true), 50);
  };

  // Keep splash/landing visible for unauthenticated users after init
  if (isInitialized && isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-kiro-ink-950 text-kiro-slate-100">
      {/* Splash Screen */}
      {showSplash && (
        <SplashScreen onComplete={handleSplashComplete} />
      )}

      {/* Landing Page Content */}
      {!showSplash && (
        <div
          className={`transition-opacity duration-500 ${
            showLanding ? 'animate-fade-in opacity-100' : 'opacity-0'
          }`}
        >
          <NavigationHeader />

          <main className="relative min-h-[calc(100vh-80px)]">
            {/* Hero Section */}
            <HeroSection />

      {/* User Avatar Display - decorative network visualization */}
      <div className="absolute inset-0 top-32 pointer-events-none lg:pointer-events-auto">
        <UserAvatarDisplay className="h-full w-full opacity-30 sm:opacity-40 lg:opacity-60" />
      </div>
          </main>

          {/* Additional sections for navigation scroll targets */}
          <div className="relative z-10 bg-kiro-ink-950">
            <section id="about" className="mx-auto w-full max-w-6xl px-4 py-16 sm:px-6">
              <div className="grid gap-6 lg:grid-cols-2">
                <div className="rounded-2xl border border-kiro-ink-900/70 bg-kiro-ink-900/60 p-6">
                  <h2 className="text-2xl font-semibold">About Kiro</h2>
                  <p className="mt-3 text-sm text-kiro-slate-200">
                    A chat layer that stays aligned with how your team plans, ships, and celebrates.
                  </p>
                </div>
                <div className="rounded-2xl border border-kiro-ink-900/70 bg-kiro-ink-900/60 p-6">
                  <h2 className="text-2xl font-semibold">What you get</h2>
                  <p className="mt-3 text-sm text-kiro-slate-200">
                    Structured rooms, focused threads, and a presence signal that keeps context crisp.
                  </p>
                </div>
              </div>
            </section>

            <section id="how-it-works" className="mx-auto w-full max-w-6xl px-4 py-16 sm:px-6">
              <div className="rounded-2xl border border-kiro-ink-900/70 bg-kiro-ink-900/60 p-6">
                <h2 className="text-2xl font-semibold">How it works</h2>
                <div className="mt-4 grid gap-4 sm:grid-cols-3">
                  <div className="rounded-xl bg-kiro-ink-950/60 p-4">
                    <p className="text-sm text-kiro-slate-500">Step 01</p>
                    <p className="mt-2 font-medium">Create a room</p>
                  </div>
                  <div className="rounded-xl bg-kiro-ink-950/60 p-4">
                    <p className="text-sm text-kiro-slate-500">Step 02</p>
                    <p className="mt-2 font-medium">Sync the team</p>
                  </div>
                  <div className="rounded-xl bg-kiro-ink-950/60 p-4">
                    <p className="text-sm text-kiro-slate-500">Step 03</p>
                    <p className="mt-2 font-medium">Ship faster</p>
                  </div>
                </div>
              </div>
            </section>

            <section id="pricing" className="mx-auto w-full max-w-6xl px-4 py-16 sm:px-6">
              <div className="rounded-2xl border border-kiro-ink-900/70 bg-kiro-ink-900/60 p-6">
                <h2 className="text-2xl font-semibold">Pricing</h2>
                <p className="mt-3 text-sm text-kiro-slate-200">
                  Launch-tier access is free during beta. Invite your team and lock in early access.
                </p>
              </div>
            </section>

            <section id="contact" className="mx-auto w-full max-w-6xl px-4 py-16 pb-20 sm:px-6">
              <div className="rounded-2xl border border-kiro-ink-900/70 bg-kiro-ink-900/60 p-6">
                <h2 className="text-2xl font-semibold">Contact</h2>
                <p className="mt-3 text-sm text-kiro-slate-200">
                  Reach out at hello@kirochat.io for launch support and partnerships.
                </p>
              </div>
            </section>
          </div>
        </div>
      )}
    </div>
  );
}
