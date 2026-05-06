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
    <main className="flex min-h-screen flex-col items-center justify-center p-4 bg-kiro-ink-950">
      <div className="w-full max-w-md space-y-6">
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
        <div className="bg-kiro-ink-900 rounded-xl border border-kiro-ink-900/80 p-6 sm:p-8">
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
