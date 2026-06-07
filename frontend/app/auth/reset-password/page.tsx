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
      <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-kiro-ink-950 p-4">
        <div className="relative z-10 w-full max-w-md space-y-6 text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg brightness-0 invert" />
            <span className="text-lg font-bold text-kiro-slate-100">Chatter</span>
          </div>
          <div className="rounded-2xl border border-white/10 bg-white/5 p-8 shadow-[0_20px_60px_rgba(8,8,20,0.45)] backdrop-blur-xl">
            <h1 className="text-2xl font-bold text-kiro-slate-100 mb-4">Invalid Reset Link</h1>
            <p className="text-sm text-kiro-slate-400 mb-6">
              This reset link is missing the required token. Please check your email for the full link.
            </p>
            <Link
              href="/auth/forgot-password"
              className="inline-flex items-center justify-center font-medium rounded-lg min-h-[44px] px-6 py-3 text-base bg-kiro-purple-600 text-white hover:bg-kiro-purple-700"
            >
              Request a new reset link
            </Link>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-kiro-ink-950 p-4">
      <div aria-hidden className="pointer-events-none absolute inset-0">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(111,66,193,0.25),transparent_55%)]" />
        <div className="absolute -right-24 top-10 h-64 w-64 rounded-full bg-kiro-purple-600/25 blur-[120px]" />
        <div className="absolute -left-24 bottom-0 h-64 w-64 rounded-full bg-kiro-purple-500/20 blur-[120px]" />
      </div>
      <div className="relative z-10 w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Image src="/logo1.png" alt="Chatter logo" width={32} height={32} className="rounded-lg brightness-0 invert" />
            <span className="text-lg font-bold text-kiro-slate-100">Chatter</span>
          </div>
          <h1 className="text-3xl font-bold text-kiro-slate-100">Reset Password</h1>
          <p className="mt-2 text-sm text-kiro-slate-500">
            Choose a new password for your account
          </p>
        </div>

        <div className="rounded-2xl border border-white/10 bg-white/5 p-6 shadow-[0_20px_60px_rgba(8,8,20,0.45)] backdrop-blur-xl sm:p-8">
          <ResetPasswordForm token={token} />
        </div>

        <div className="text-center text-sm">
          <span className="text-kiro-slate-500">Remember your password? </span>
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
