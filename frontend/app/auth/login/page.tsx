import { LoginForm } from '@/components/auth/LoginForm';
import Link from 'next/link';

/**
 * Login page that wraps the LoginForm component.
 * Provides navigation to registration page.
 * 
 * Requirements: 1.1, 1.2, 15.1, 15.2
 */
export default function LoginPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-4 bg-gray-50">
      <div className="w-full max-w-md space-y-6">
        {/* Header */}
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">Welcome Back</h1>
          <p className="mt-2 text-sm text-gray-600">
            Sign in to your account to continue
          </p>
        </div>

        {/* Login Form Card */}
        <div className="bg-white rounded-lg shadow-md p-6 sm:p-8">
          <LoginForm />
        </div>

        {/* Registration Link */}
        <div className="text-center text-sm">
          <span className="text-gray-600">Don&apos;t have an account? </span>
          <Link
            href="/auth/register"
            className="font-medium text-blue-600 hover:text-blue-500 focus:outline-none focus:underline"
          >
            Create one now
          </Link>
        </div>
      </div>
    </main>
  );
}
