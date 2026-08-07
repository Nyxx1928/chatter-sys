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
import Lightfall from '@/components/Lightfall';

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
    <div className="min-h-screen text-slack-text-primary relative">
      {/* Full-viewport Lightfall background — canvas covers entire page */}
      <div className="fixed inset-0 z-0 blur-sm">
        <Lightfall
          colors={['#4A154B', '#7C2382', '#36C5F0']}
          backgroundColor="#0A0A14"
          speed={0.3}
          streakCount={2}
          streakWidth={1}
          streakLength={1.5}
          glow={1}
          density={0.5}
          twinkle={0.8}
          zoom={3}
          backgroundGlow={0.4}
          opacity={0.65}
          mouseInteraction
          mouseStrength={0.25}
          mouseRadius={0.8}
        />
      </div>

      {/* Splash Screen */}
      {showSplash && <SplashScreen onComplete={handleSplashComplete} />}

      {/* Landing Page Content */}
      {!showSplash && (
        <div
          className={`relative z-10 transition-opacity duration-500 ${
            showLanding ? 'animate-fade-in opacity-100' : 'opacity-0'
          }`}
        >
          <NavigationHeader />

          <main>
            <HeroSection />
            <FeaturesSection />
          </main>

          {/* Footer */}
          <FooterSection />
        </div>
      )}
    </div>
  );
}
