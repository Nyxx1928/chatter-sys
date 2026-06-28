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
    <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-slack-surface-secondary to-slack-surface-primary p-4">
      <div className="w-full max-w-md space-y-6">
        {/* Header */}
        <div className="text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg" />
            <span className="text-lg font-bold text-slack-text-primary">Chatter</span>
          </div>
          <h1 className="text-3xl font-bold text-slack-text-primary">Create Account</h1>
          <p className="mt-2 text-sm text-slack-text-secondary">
            Join our chat community today
          </p>
        </div>

        {/* Registration Form Card */}
        <div className="rounded-2xl border border-slack-border bg-slack-surface-primary p-6 shadow-slack-lg sm:p-8">
          <RegisterForm />
        </div>

        {/* Login Link */}
        <div className="text-center text-sm">
          <span className="text-slack-text-secondary">Already have an account? </span>
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
