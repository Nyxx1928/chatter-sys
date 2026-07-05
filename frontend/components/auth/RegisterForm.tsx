'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { useAuthStore } from '../../lib/store/authStore';
import { ApiError } from '../../lib/api/client';

/**
 * Registration form component with username, email, password, and displayName inputs.
 * Integrates with auth store for user registration.
 * On success, redirects to the OTP verification page.
 * 
 * Requirements: 1.1, 1.2, 1.4
 */
export function RegisterForm() {
  const router = useRouter();
  const register = useAuthStore((state) => state.register);
  
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    displayName: '',
  });
  
  const [errors, setErrors] = useState<{
    username?: string;
    email?: string;
    password?: string;
    confirmPassword?: string;
    displayName?: string;
    general?: string;
  }>({});
  
  const [submitState, setSubmitState] = useState<
    'idle' | 'submitting' | 'success' | 'error'
  >('idle');

  const handleChange = (field: keyof typeof formData) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const validateForm = (): boolean => {
    const newErrors: typeof errors = {};

    if (!formData.username.trim()) {
      newErrors.username = 'Username is required';
    } else if (formData.username.length < 3) {
      newErrors.username = 'Username must be at least 3 characters';
    } else if (formData.username.length > 50) {
      newErrors.username = 'Username must not exceed 50 characters';
    } else if (!/^[a-zA-Z0-9_-]+$/.test(formData.username)) {
      newErrors.username = 'Username can only contain letters, numbers, hyphens, and underscores';
    }

    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (formData.email.length > 100) {
      newErrors.email = 'Email must not exceed 100 characters';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    } else if (formData.password.length > 100) {
      newErrors.password = 'Password must not exceed 100 characters';
    } else if (!/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*#?&]).{8,}$/.test(formData.password)) {
      newErrors.password = 'Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character (@$!%*#?&)';
    }

    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password';
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    if (!formData.displayName.trim()) {
      newErrors.displayName = 'Display name is required';
    } else if (formData.displayName.length < 2) {
      newErrors.displayName = 'Display name must be at least 2 characters';
    } else if (formData.displayName.length > 100) {
      newErrors.displayName = 'Display name must not exceed 100 characters';
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

    setSubmitState('submitting');

    try {
      await register({
        username: formData.username.trim(),
        email: formData.email.trim(),
        password: formData.password,
        displayName: formData.displayName.trim(),
      });

      setSubmitState('success');

      setTimeout(() => {
        router.push(`/auth/verify-otp?email=${encodeURIComponent(formData.email.trim())}`);
      }, 800);
    } catch (error) {
      console.error('Registration failed:', error);
      setSubmitState('error');

      if (error instanceof ApiError) {
        const details = error.details as Record<string, unknown> | undefined;
        if (details && typeof details === 'object' && 'errors' in details) {
          const fieldErrors = details.errors as Record<string, string>;
          const mapped: typeof errors = {};
          if (fieldErrors.username) mapped.username = fieldErrors.username;
          if (fieldErrors.email) mapped.email = fieldErrors.email;
          if (fieldErrors.password) mapped.password = fieldErrors.password;
          if (fieldErrors.displayName) mapped.displayName = fieldErrors.displayName;
          if (Object.keys(mapped).length > 0) {
            setErrors(mapped);
            return;
          }
        }
      }

      if (error instanceof Error) {
        const message = error.message || 'Registration failed';
        if (message.toLowerCase().includes('username')) {
          setErrors({ username: 'Username is already taken' });
        } else if (message.toLowerCase().includes('email')) {
          setErrors({ email: 'Email is already registered' });
        } else {
          setErrors({ general: message });
        }
      } else {
        setErrors({ general: 'An unexpected error occurred. Please try again.' });
      }
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4 w-full max-w-md" noValidate>
      <Input
        type="text"
        label="Username"
        value={formData.username}
        onChange={handleChange('username')}
        error={errors.username}
        placeholder="Choose a username"
        autoComplete="username"
        required
        fullWidth
        disabled={submitState === 'submitting' || submitState === 'success'}
        aria-label="Username"
      />

      <Input
        type="email"
        label="Email"
        value={formData.email}
        onChange={handleChange('email')}
        error={errors.email}
        placeholder="Enter your email"
        autoComplete="email"
        required
        fullWidth
        disabled={submitState === 'submitting' || submitState === 'success'}
        aria-label="Email"
      />

      <Input
        type="text"
        label="Display Name"
        value={formData.displayName}
        onChange={handleChange('displayName')}
        error={errors.displayName}
        placeholder="Enter your display name"
        autoComplete="name"
        required
        fullWidth
        disabled={submitState === 'submitting' || submitState === 'success'}
        aria-label="Display Name"
        helperText="This is how other users will see you"
      />

      <Input
        type="password"
        label="Password"
        value={formData.password}
        onChange={handleChange('password')}
        error={errors.password}
        placeholder="Create a password"
        autoComplete="new-password"
        required
        fullWidth
        disabled={submitState === 'submitting' || submitState === 'success'}
        aria-label="Password"
        showPasswordToggle
      />

      <Input
        type="password"
        label="Confirm Password"
        value={formData.confirmPassword}
        onChange={handleChange('confirmPassword')}
        error={errors.confirmPassword}
        placeholder="Confirm your password"
        autoComplete="new-password"
        required
        fullWidth
        disabled={submitState === 'submitting' || submitState === 'success'}
        aria-label="Confirm Password"
        showPasswordToggle
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
        disabled={submitState === 'submitting' || submitState === 'success'}
        aria-label={
          submitState === 'submitting' ? 'Creating account...' :
          submitState === 'success' ? 'Account created' :
          'Create account'
        }
      >
        {submitState === 'submitting' && 'Creating Account...'}
        {submitState === 'success' && '✓ Account Created!'}
        {(submitState === 'idle' || submitState === 'error') && 'Create Account'}
      </Button>
    </form>
  );
}
