'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { resetPassword } from '../../lib/api/auth';

interface ResetPasswordFormProps {
  token: string;
}

export function ResetPasswordForm({ token }: ResetPasswordFormProps) {
  const router = useRouter();

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<{
    newPassword?: string;
    confirmPassword?: string;
    general?: string;
  }>({});
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const validateForm = (): boolean => {
    const newErrors: { newPassword?: string; confirmPassword?: string } = {};

    if (!newPassword) {
      newErrors.newPassword = 'New password is required';
    } else if (newPassword.length < 8) {
      newErrors.newPassword = 'Password must be at least 8 characters';
    } else if (newPassword.length > 100) {
      newErrors.newPassword = 'Password must not exceed 100 characters';
    }

    if (!confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your new password';
    } else if (newPassword !== confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
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
      await resetPassword({ token, newPassword });
      setIsSuccess(true);
      setTimeout(() => {
        router.push('/auth/login');
      }, 3000);
    } catch (error) {
      console.error('Password reset failed:', error);

      if (error instanceof Error) {
        const message = error.message;
        if (message.toLowerCase().includes('expired')) {
          setErrors({ general: 'This reset link has expired. Please request a new one.' });
        } else if (message.toLowerCase().includes('used') || message.toLowerCase().includes('invalid')) {
          setErrors({ general: 'This reset link is invalid or has already been used.' });
        } else {
          setErrors({ general: message });
        }
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
          Your password has been reset successfully!
        </div>
        <p className="text-sm text-kiro-slate-500">
          Redirecting you to the login page...
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4 w-full max-w-md" noValidate>
      <p className="text-sm text-kiro-slate-400">
        Enter your new password below.
      </p>

      <Input
        type="password"
        label="New Password"
        value={newPassword}
        onChange={(e) => setNewPassword(e.target.value)}
        error={errors.newPassword}
        placeholder="Enter new password"
        autoComplete="new-password"
        required
        fullWidth
        disabled={isLoading}
        aria-label="New Password"
        showPasswordToggle
      />

      <Input
        type="password"
        label="Confirm New Password"
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
        error={errors.confirmPassword}
        placeholder="Confirm new password"
        autoComplete="new-password"
        required
        fullWidth
        disabled={isLoading}
        aria-label="Confirm New Password"
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
        aria-label={isLoading ? 'Resetting...' : 'Reset Password'}
      >
        {isLoading ? 'Resetting...' : 'Reset Password'}
      </Button>
    </form>
  );
}
