import { LoginForm, RegisterForm } from '@/components/auth';

/**
 * Demo page for authentication form components.
 * This page showcases the LoginForm and RegisterForm components.
 */
export default function AuthDemoPage() {
  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8 text-center">
          Authentication Forms Demo
        </h1>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Login Form Demo */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-2xl font-semibold text-gray-800 mb-4">
              Login Form
            </h2>
            <p className="text-gray-600 mb-6">
              Username and password authentication form with validation.
            </p>
            <LoginForm />
          </div>
          
          {/* Register Form Demo */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-2xl font-semibold text-gray-800 mb-4">
              Register Form
            </h2>
            <p className="text-gray-600 mb-6">
              User registration form with username, email, password, and display name.
            </p>
            <RegisterForm />
          </div>
        </div>
        
        <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-blue-900 mb-2">
            Features
          </h3>
          <ul className="list-disc list-inside text-blue-800 space-y-1">
            <li>Client-side form validation with error messages</li>
            <li>Mobile-first responsive design with 44x44px touch targets</li>
            <li>Integration with Zustand auth store</li>
            <li>Loading states during submission</li>
            <li>Accessible form controls with ARIA labels</li>
            <li>Password confirmation validation (register form)</li>
            <li>Email format validation (register form)</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
