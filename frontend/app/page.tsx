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
    <div className="min-h-screen bg-[#07070d] text-white">
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
