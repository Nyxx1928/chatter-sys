import Image from 'next/image';
import Link from 'next/link';
import { OtpVerificationForm } from '@/components/auth/OtpVerificationForm';

type OtpPageProps = {
  searchParams?: Promise<{ email?: string }>;
};

export default async function VerifyOtpPage({ searchParams }: OtpPageProps) {
  const params = await searchParams;
  const email = params?.email;

  if (!email) {
    return (
      <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-slack-surface-secondary to-slack-surface-primary p-4">
        <div className="relative z-10 w-full max-w-md space-y-6 text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg" />
            <span className="text-lg font-bold text-slack-text-primary">Chatter</span>
          </div>
          <div className="rounded-2xl border border-slack-border bg-slack-surface-primary p-8 shadow-slack-lg">
            <h1 className="text-2xl font-bold text-slack-text-primary mb-4">Missing Email</h1>
            <p className="text-sm text-slack-text-secondary mb-6">
              Please register first to receive a verification code.
            </p>
            <Link
              href="/auth/register"
              className="inline-flex items-center justify-center font-medium rounded-lg min-h-[44px] px-6 py-3 text-base bg-slack-primary text-slack-text-inverse hover:bg-slack-primary-light"
            >
              Create Account
            </Link>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-slack-surface-secondary to-slack-surface-primary p-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg" />
            <span className="text-lg font-bold text-slack-text-primary">Chatter</span>
          </div>
          <h1 className="text-3xl font-bold text-slack-text-primary">Verify Your Email</h1>
          <p className="mt-2 text-sm text-slack-text-secondary">
            Check your email for the verification code
          </p>
        </div>

        <div className="rounded-2xl border border-slack-border bg-slack-surface-primary p-6 shadow-slack-lg sm:p-8">
          <OtpVerificationForm email={email} />
        </div>

        <div className="text-center text-sm">
          <span className="text-slack-text-secondary">Already verified? </span>
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
