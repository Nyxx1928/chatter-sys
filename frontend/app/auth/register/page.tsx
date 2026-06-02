import { RegisterForm } from '@/components/auth/RegisterForm';
import Image from 'next/image';
import Link from 'next/link';

/**
 * Registration page that wraps the RegisterForm component.
 * Provides navigation to login page.
 * 
 * Requirements: 1.1, 1.2, 15.1, 15.2
 */
export default function RegisterPage() {
  return (
    <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-kiro-ink-950 p-4">
      <div aria-hidden className="pointer-events-none absolute inset-0">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(111,66,193,0.25),transparent_55%)]" />
        <div className="absolute -right-24 top-10 h-64 w-64 rounded-full bg-kiro-purple-600/25 blur-[120px]" />
        <div className="absolute -left-24 bottom-0 h-64 w-64 rounded-full bg-kiro-purple-500/20 blur-[120px]" />
      </div>
      <div className="relative z-10 w-full max-w-md space-y-6">
        {/* Header */}
        <div className="text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg brightness-0 invert" />
            <span className="text-lg font-bold text-kiro-slate-100">Chatter</span>
          </div>
          <h1 className="text-3xl font-bold text-kiro-slate-100">Create Account</h1>
          <p className="mt-2 text-sm text-kiro-slate-500">
            Join our chat community today
          </p>
        </div>

        {/* Registration Form Card */}
        <div className="rounded-2xl border border-white/10 bg-white/5 p-6 shadow-[0_20px_60px_rgba(8,8,20,0.45)] backdrop-blur-xl sm:p-8">
          <RegisterForm />
        </div>

        {/* Login Link */}
        <div className="text-center text-sm">
          <span className="text-kiro-slate-500">Already have an account? </span>
          <Link
            href="/auth/login"
            className="font-medium text-kiro-purple-400 hover:text-kiro-purple-400/80 focus:outline-none focus:underline"
          >
            Sign in
          </Link>
        </div>
      </div>
    </main>
  );
}
