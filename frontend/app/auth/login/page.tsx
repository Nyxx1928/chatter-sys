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
    <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-slack-surface-secondary to-slack-surface-primary p-4">
      <div className="w-full max-w-md space-y-6">
        {/* Header */}
        <div className="text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg" />
            <span className="text-lg font-bold text-slack-text-primary">Chatter</span>
          </div>
          <h1 className="text-3xl font-bold text-slack-text-primary">Welcome Back</h1>
          <p className="mt-2 text-sm text-slack-text-secondary">
            Sign in to your account to continue
          </p>
        </div>

        {/* Login Form Card */}
        <div className="rounded-2xl border border-slack-border bg-slack-surface-primary p-6 shadow-slack-lg sm:p-8">
          {isRegistered && (
            <div className="mb-4 rounded-lg border border-slack-accent-green/30 bg-slack-accent-green/10 p-3 text-sm text-slack-accent-green">
              {emailSent
                ? 'Account created. Check your email for the verification link before logging in.'
                : 'Account created. Email delivery could not be confirmed, so use the verification link prompt from signup or request another verification email.'}
            </div>
          )}
          <LoginForm />
        </div>

        {/* Registration Link */}
        <div className="text-center text-sm">
          <span className="text-slack-text-secondary">Don&apos;t have an account? </span>
          <Link
            href="/auth/register"
            className="font-medium text-slack-accent-blue hover:text-slack-accent-blue/80 focus:outline-none focus:underline"
          >
            Create one now
          </Link>
        </div>
      </div>
    </main>
  );
}
