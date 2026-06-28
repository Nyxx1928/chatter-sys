import { ForgotPasswordForm } from '@/components/auth/ForgotPasswordForm';
import Image from 'next/image';
import Link from 'next/link';

export default function ForgotPasswordPage() {
  return (
    <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-slack-surface-secondary to-slack-surface-primary p-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg" />
            <span className="text-lg font-bold text-slack-text-primary">Chatter</span>
          </div>
          <h1 className="text-3xl font-bold text-slack-text-primary">Forgot Password</h1>
          <p className="mt-2 text-sm text-slack-text-secondary">
            Reset your password to regain access
          </p>
        </div>

        <div className="rounded-2xl border border-slack-border bg-slack-surface-primary p-6 shadow-slack-lg sm:p-8">
          <ForgotPasswordForm />
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
