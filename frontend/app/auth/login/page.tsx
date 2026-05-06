import { LoginForm } from '@/components/auth/LoginForm';
import Image from 'next/image';
import Link from 'next/link';

/**
 * Login page that wraps the LoginForm component.
 * Provides navigation to registration page.
 * 
 * Requirements: 1.1, 1.2, 15.1, 15.2
 */
export default function LoginPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-4 bg-kiro-ink-950">
      <div className="w-full max-w-md space-y-6">
        {/* Header */}
        <div className="text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg brightness-0 invert" />
            <span className="text-lg font-bold text-kiro-slate-100">Chatter</span>
          </div>
          <h1 className="text-3xl font-bold text-kiro-slate-100">Welcome Back</h1>
          <p className="mt-2 text-sm text-kiro-slate-500">
            Sign in to your account to continue
          </p>
        </div>

        {/* Login Form Card */}
        <div className="bg-kiro-ink-900 rounded-xl border border-kiro-ink-900/80 p-6 sm:p-8">
          <LoginForm />
        </div>

        {/* Registration Link */}
        <div className="text-center text-sm">
          <span className="text-kiro-slate-500">Don&apos;t have an account? </span>
          <Link
            href="/auth/register"
            className="font-medium text-kiro-purple-400 hover:text-kiro-purple-400/80 focus:outline-none focus:underline"
          >
            Create one now
          </Link>
        </div>
      </div>
    </main>
  );
}
