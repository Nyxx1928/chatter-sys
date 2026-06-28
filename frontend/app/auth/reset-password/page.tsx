import { ResetPasswordForm } from '@/components/auth/ResetPasswordForm';
import Image from 'next/image';
import Link from 'next/link';

export default async function ResetPasswordPage({
  searchParams,
}: {
  searchParams?: Promise<{ token?: string }>;
}) {
  const params = await searchParams;
  const token = params?.token;

  if (!token) {
    return (
      <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-slack-surface-secondary to-slack-surface-primary p-4">
        <div className="relative z-10 w-full max-w-md space-y-6 text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg" />
            <span className="text-lg font-bold text-slack-text-primary">Chatter</span>
          </div>
          <div className="rounded-2xl border border-slack-border bg-slack-surface-primary p-8 shadow-slack-lg">
            <h1 className="text-2xl font-bold text-slack-text-primary mb-4">Invalid Reset Link</h1>
            <p className="text-sm text-slack-text-secondary mb-6">
              This reset link is missing the required token. Please check your email for the full link.
            </p>
            <Link
              href="/auth/forgot-password"
              className="inline-flex items-center justify-center font-medium rounded-lg min-h-[44px] px-6 py-3 text-base bg-slack-primary text-slack-text-inverse hover:bg-slack-primary-light"
            >
              Request a new reset link
            </Link>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-slack-surface-secondary to-slack-surface-primary p-4">
      <div className="relative z-10 w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg" />
            <span className="text-lg font-bold text-slack-text-primary">Chatter</span>
          </div>
          <h1 className="text-3xl font-bold text-slack-text-primary">Reset Password</h1>
          <p className="mt-2 text-sm text-slack-text-secondary">
            Choose a new password for your account
          </p>
        </div>

        <div className="rounded-2xl border border-slack-border bg-slack-surface-primary p-6 shadow-slack-lg sm:p-8">
          <ResetPasswordForm token={token} />
        </div>

        <div className="text-center text-sm">
          <span className="text-slack-text-secondary">Remember your password? </span>
          <Link
            href="/auth/login"
            className="font-medium text-slack-accent-blue hover:text-slack-accent-blue/80 focus:outline-none focus:underline"
          >
            Sign in
          </Link>
        </div>
      </div>
    </main>
  );
}
