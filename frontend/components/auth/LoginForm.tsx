'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { useAuthStore } from '../../lib/store/authStore';

/**
 * Login form component with username and password inputs.
 * Integrates with auth store for authentication.
 * 
 * Requirements: 1.1, 1.2, 15.1, 15.2, 17.3
 */
export function LoginForm() {
  const router = useRouter();
  const login = useAuthStore((state) => state.login);
  
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ username?: string; password?: string; general?: string }>({});
  const [isLoading, setIsLoading] = useState(false);

  const validateForm = (): boolean => {
    const newErrors: { username?: string; password?: string } = {};

    if (!username.trim()) {
      newErrors.username = 'Username is required';
    } else if (username.length < 3) {
      newErrors.username = 'Username must be at least 3 characters';
    } else if (username.length > 50) {
      newErrors.username = 'Username must not exceed 50 characters';
    }

    if (!password) {
      newErrors.password = 'Password is required';
    } else if (password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Clear previous errors
    setErrors({});
    
    // Validate form
    if (!validateForm()) {
      return;
    }

    setIsLoading(true);

    try {
      await login({ username: username.trim(), password });
      
      // Redirect to chat on successful login
      router.push('/chat');
    } catch (error) {
      console.error('Login failed:', error);
      
      // Display user-friendly error message
      if (error instanceof Error) {
        setErrors({ general: error.message || 'Invalid username or password' });
      } else {
        setErrors({ general: 'An unexpected error occurred. Please try again.' });
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4 w-full max-w-md" noValidate>
      <Input
        type="text"
        label="Username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        error={errors.username}
        placeholder="Enter your username"
        autoComplete="username"
        required
        fullWidth
        disabled={isLoading}
        aria-label="Username"
      />

      <Input
        type="password"
        label="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        error={errors.password}
        placeholder="Enter your password"
        autoComplete="current-password"
        required
        fullWidth
        disabled={isLoading}
        aria-label="Password"
        showPasswordToggle
      />

      {errors.general && (
        <div
          role="alert"
          className="p-3 text-sm text-red-400 bg-red-950/40 border border-red-900/50 rounded-lg"
        >
          {errors.general}
        </div>
      )}

      <Button
        type="submit"
        variant="primary"
        fullWidth
        disabled={isLoading}
        aria-label={isLoading ? 'Logging in...' : 'Log in'}
      >
        {isLoading ? 'Logging in...' : 'Log In'}
      </Button>

      <div className="text-center text-xs">
        <Link
          href="/auth/forgot-password"
          className="text-kiro-slate-500 hover:text-kiro-purple-400 focus:outline-none focus:underline"
        >
          Forgot Password?
        </Link>
      </div>
    </form>
  );
}
