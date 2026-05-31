import { LoginForm } from '@/components/auth/LoginForm';
import Image from 'next/image';
import Link from 'next/link';

/**
 * Login page that wraps the LoginForm component.
 * Provides navigation to registration page.
 * 
 * Requirements: 1.1, 1.2, 15.1, 15.2
 */
export default async function LoginPage({
  searchParams,
}: {
  searchParams?: Promise<{ registered?: string; emailSent?: string }>;
}) {
  const params = await searchParams;
  const isRegistered = params?.registered === 'true';
  const emailSent = params?.emailSent !== 'false';

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
          <h1 className="text-3xl font-bold text-kiro-slate-100">Welcome Back</h1>
          <p className="mt-2 text-sm text-kiro-slate-500">
            Sign in to your account to continue
          </p>
        </div>

        {/* Login Form Card */}
        <div className="rounded-2xl border border-white/10 bg-white/5 p-6 shadow-[0_20px_60px_rgba(8,8,20,0.45)] backdrop-blur-xl sm:p-8">
          {isRegistered && (
            <div className="mb-4 rounded-lg border border-emerald-900/60 bg-emerald-950/40 p-3 text-sm text-emerald-200">
              {emailSent
                ? 'Account created. Check your email for the verification link before logging in.'
                : 'Account created. Email delivery could not be confirmed, so use the verification link prompt from signup or request another verification email.'}
            </div>
          )}
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
