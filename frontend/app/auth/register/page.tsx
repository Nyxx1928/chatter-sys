import { RegisterForm } from '@/components/auth/RegisterForm';
import Link from 'next/link';

/**
 * Registration page that wraps the RegisterForm component.
 * Provides navigation to login page.
 * 
 * Requirements: 1.1, 1.2, 15.1, 15.2
 */
export default function RegisterPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-4 bg-gray-50">
      <div className="w-full max-w-md space-y-6">
        {/* Header */}
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">Create Account</h1>
          <p className="mt-2 text-sm text-gray-600">
            Join our chat community today
          </p>
        </div>

        {/* Registration Form Card */}
        <div className="bg-white rounded-lg shadow-md p-6 sm:p-8">
          <RegisterForm />
        </div>

        {/* Login Link */}
        <div className="text-center text-sm">
          <span className="text-gray-600">Already have an account? </span>
          <Link
            href="/auth/login"
            className="font-medium text-blue-600 hover:text-blue-500 focus:outline-none focus:underline"
          >
            Sign in
          </Link>
        </div>
      </div>
    </main>
  );
}
