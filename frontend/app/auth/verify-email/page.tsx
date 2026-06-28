import Image from 'next/image';
import Link from 'next/link';

type VerificationPageProps = {
  searchParams?: Promise<{
    status?: string;
    message?: string;
  }>;
};

const FALLBACK_MESSAGES = {
  success: 'Your email has been verified. You can log in now.',
  error: 'We could not verify your email. Please request a new verification email and try again.',
} as const;

export default async function VerifyEmailPage({ searchParams }: VerificationPageProps) {
  const params = await searchParams;
  const isSuccess = params?.status === 'success';
  const message = params?.message?.trim()
    || (isSuccess ? FALLBACK_MESSAGES.success : FALLBACK_MESSAGES.error);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-b from-slack-surface-secondary to-slack-surface-primary p-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="mb-4 flex items-center justify-center gap-2">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg" />
            <span className="text-lg font-bold text-slack-text-primary">Chatter</span>
          </div>
          <h1 className="text-3xl font-bold text-slack-text-primary">
            {isSuccess ? 'Email Verified' : 'Verification Failed'}
          </h1>
          <p className="mt-2 text-sm text-slack-text-secondary">
            {isSuccess
              ? 'Your account is ready to use.'
              : 'Let\'s get you back on track.'}
          </p>
        </div>

        <div className={`rounded-2xl border p-6 sm:p-8 ${
          isSuccess
            ? 'border-slack-accent-green/30 bg-slack-accent-green/10'
            : 'border-slack-accent-red/30 bg-slack-accent-red/10'
        }`}>
          <p className={`text-sm ${
            isSuccess ? 'text-slack-accent-green' : 'text-slack-accent-red'
          }`}>
            {message}
          </p>

          <div className="mt-6 flex flex-col gap-3 sm:flex-row">
            <Link
              href={isSuccess ? '/auth/login' : '/auth/register'}
              className="inline-flex min-h-[44px] items-center justify-center rounded-pill bg-slack-primary px-6 py-3 text-base font-medium text-slack-text-inverse transition-all duration-100 hover:bg-slack-primary-light focus:outline-none focus:ring-2 focus:ring-slack-primary focus:ring-offset-2"
            >
              {isSuccess ? 'Go to Login' : 'Create Account Again'}
            </Link>
            {!isSuccess && (
              <Link
                href="/auth/login"
                className="inline-flex min-h-[44px] items-center justify-center rounded-pill border border-slack-accent-blue px-6 py-3 text-base font-medium text-slack-accent-blue transition-all duration-100 hover:bg-slack-accent-blue hover:text-slack-text-inverse focus:outline-none focus:ring-2 focus:ring-slack-accent-blue focus:ring-offset-2"
              >
                Back to Login
              </Link>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}
