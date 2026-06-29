'use client';

import React, { useState } from 'react';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { forgotPassword } from '../../lib/api/auth';

export function ForgotPasswordForm() {
  const [email, setEmail] = useState('');
  const [errors, setErrors] = useState<{ email?: string; general?: string }>({});
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const validateForm = (): boolean => {
    const newErrors: { email?: string } = {};

    if (!email.trim()) {
      newErrors.email = 'Email is required';
    } else if (email.length > 100) {
      newErrors.email = 'Email must not exceed 100 characters';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Please enter a valid email address';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});

    if (!validateForm()) {
      return;
    }

    setIsLoading(true);

    try {
      await forgotPassword({ email: email.trim() });
      setIsSuccess(true);
    } catch (error) {
      console.error('Forgot password request failed:', error);

      if (error instanceof Error) {
        setErrors({ general: error.message });
      } else {
        setErrors({ general: 'An unexpected error occurred. Please try again.' });
      }
    } finally {
      setIsLoading(false);
    }
  };

  if (isSuccess) {
    return (
      <div className="text-center">
        <div className="mb-4 rounded-lg border border-slack-accent-green/30 bg-slack-accent-green/10 p-4 text-sm text-slack-accent-green">
          If an account with that email exists, we&apos;ve sent a password reset link.
          Please check your email and follow the instructions.
        </div>
        <p className="text-sm text-slack-text-secondary">
          The link will expire in 15 minutes.
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4 w-full max-w-md" noValidate>
      <p className="text-sm text-slack-text-secondary">
        Enter your email address and we&apos;ll send you a link to reset your password.
      </p>

      <Input
        type="email"
        label="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        error={errors.email}
        placeholder="Enter your email"
        autoComplete="email"
        required
        fullWidth
        disabled={isLoading}
        aria-label="Email"
      />

      {errors.general && (
        <div
          role="alert"
          className="p-3 text-sm text-slack-accent-red bg-slack-accent-red/10 border border-slack-accent-red/30 rounded-lg"
        >
          {errors.general}
        </div>
      )}

      <Button
        type="submit"
        variant="primary"
        fullWidth
        disabled={isLoading}
        aria-label={isLoading ? 'Sending...' : 'Send Reset Link'}
      >
        {isLoading ? 'Sending...' : 'Send Reset Link'}
      </Button>
    </form>
  );
}
