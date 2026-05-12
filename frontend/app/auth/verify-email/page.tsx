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
    <main className="flex min-h-screen flex-col items-center justify-center bg-kiro-ink-950 p-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="mb-4 flex items-center justify-center gap-2">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg brightness-0 invert" />
            <span className="text-lg font-bold text-kiro-slate-100">Chatter</span>
          </div>
          <h1 className="text-3xl font-bold text-kiro-slate-100">
            {isSuccess ? 'Email Verified' : 'Verification Failed'}
          </h1>
          <p className="mt-2 text-sm text-kiro-slate-500">
            {isSuccess
              ? 'Your account is ready to use.'
              : 'Let’s get you back on track.'}
          </p>
        </div>

        <div className={`rounded-xl border p-6 sm:p-8 ${
          isSuccess
            ? 'border-emerald-900/60 bg-emerald-950/40'
            : 'border-amber-900/60 bg-amber-950/30'
        }`}>
          <p className={`text-sm ${
            isSuccess ? 'text-emerald-200' : 'text-amber-100'
          }`}>
            {message}
          </p>

          <div className="mt-6 flex flex-col gap-3 sm:flex-row">
            <Link
              href={isSuccess ? '/auth/login' : '/auth/register'}
              className="inline-flex min-h-[44px] items-center justify-center rounded-lg bg-kiro-purple-600 px-6 py-3 text-base font-medium text-white transition-all duration-100 hover:bg-kiro-purple-700 focus:outline-none focus:ring-2 focus:ring-kiro-purple-400 focus:ring-offset-2"
            >
              {isSuccess ? 'Go to Login' : 'Create Account Again'}
            </Link>
            {!isSuccess && (
              <Link
                href="/auth/login"
                className="inline-flex min-h-[44px] items-center justify-center rounded-lg border border-kiro-purple-500 px-6 py-3 text-base font-medium text-kiro-purple-400 transition-all duration-100 hover:bg-kiro-purple-600 hover:text-white focus:outline-none focus:ring-2 focus:ring-kiro-purple-400 focus:ring-offset-2"
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
