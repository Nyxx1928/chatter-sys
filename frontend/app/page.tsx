'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  HeroSection,
  NavigationHeader,
  SplashScreen,
  FeaturesSection,
  FooterSection,
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
    setTimeout(() => setShowLanding(true), 50);
  };

  // Keep splash/landing visible for unauthenticated users after init
  if (isInitialized && isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-[#0a0a0f] text-white">
      {/* Splash Screen */}
      {showSplash && <SplashScreen onComplete={handleSplashComplete} />}

      {/* Landing Page Content */}
      {!showSplash && (
        <div
          className={`transition-opacity duration-500 ${
            showLanding ? 'animate-fade-in opacity-100' : 'opacity-0'
          }`}
        >
          <NavigationHeader />

          <main>
            {/* Hero Section */}
            <HeroSection />

            {/* Features Section */}
            <FeaturesSection />

            {/* Additional sections */}
            <div className="bg-[#0a0a0f]">
              <section
                id="about"
                className="mx-auto w-full max-w-6xl px-6 py-16 sm:py-20"
              >
                <div className="grid gap-6 lg:grid-cols-2">
                  <div className="rounded-2xl border border-white/5 bg-[#111118] p-8">
                    <h2 className="text-2xl font-semibold text-white">
                      About Chatter
                    </h2>
                    <p className="mt-4 text-sm leading-relaxed text-white/60">
                      A chat layer that stays aligned with how your team plans,
                      ships, and celebrates.
                    </p>
                  </div>
                  <div className="rounded-2xl border border-white/5 bg-[#111118] p-8">
                    <h2 className="text-2xl font-semibold text-white">
                      What you get
                    </h2>
                    <p className="mt-4 text-sm leading-relaxed text-white/60">
                      Structured rooms, focused threads, and a presence signal
                      that keeps context crisp.
                    </p>
                  </div>
                </div>
              </section>

              <section
                id="how-it-works"
                className="mx-auto w-full max-w-6xl px-6 py-16 sm:py-20"
              >
                <div className="rounded-2xl border border-white/5 bg-[#111118] p-8">
                  <h2 className="text-2xl font-semibold text-white">
                    How it works
                  </h2>
                  <div className="mt-6 grid gap-4 sm:grid-cols-3">
                    <div className="rounded-xl bg-[#0a0a0f] p-5">
                      <p className="text-sm text-white/40">Step 01</p>
                      <p className="mt-2 font-medium text-white">
                        Create a room
                      </p>
                    </div>
                    <div className="rounded-xl bg-[#0a0a0f] p-5">
                      <p className="text-sm text-white/40">Step 02</p>
                      <p className="mt-2 font-medium text-white">
                        Sync the team
                      </p>
                    </div>
                    <div className="rounded-xl bg-[#0a0a0f] p-5">
                      <p className="text-sm text-white/40">Step 03</p>
                      <p className="mt-2 font-medium text-white">
                        Ship faster
                      </p>
                    </div>
                  </div>
                </div>
              </section>

              <section
                id="contact"
                className="mx-auto w-full max-w-6xl px-6 py-16 pb-20 sm:py-20"
              >
                <div className="rounded-2xl border border-white/5 bg-[#111118] p-8">
                  <h2 className="text-2xl font-semibold text-white">Contact</h2>
                  <p className="mt-4 text-sm leading-relaxed text-white/60">
                    Reach out at hello@chatterchat.io for launch support and
                    partnerships.
                  </p>
                </div>
              </section>
            </div>
          </main>

          {/* Footer */}
          <FooterSection />
        </div>
      )}
    </div>
  );
}
