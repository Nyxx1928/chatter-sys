'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { useAuthStore } from '../../lib/store/authStore';

/**
 * Registration form component with username, email, password, and displayName inputs.
 * Integrates with auth store for user registration.
 * 
 * Requirements: 1.1, 1.2, 15.1, 15.2, 17.3
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
  
  const [isLoading, setIsLoading] = useState(false);

  const promptToOpenVerificationLink = (
    verificationUrl: string,
    verificationEmailSent?: boolean
  ) => {
    const promptMessage = verificationEmailSent === false
      ? 'Your account was created, but email delivery could not be confirmed. Open the verification link now?'
      : 'Your account was created. Open the verification link now in a new tab?';

    if (window.confirm(promptMessage)) {
      window.open(verificationUrl, '_blank', 'noopener,noreferrer');
    }
  };

  const handleChange = (field: keyof typeof formData) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const validateForm = (): boolean => {
    const newErrors: typeof errors = {};

    // Username validation
    if (!formData.username.trim()) {
      newErrors.username = 'Username is required';
    } else if (formData.username.length < 3) {
      newErrors.username = 'Username must be at least 3 characters';
    } else if (formData.username.length > 50) {
      newErrors.username = 'Username must not exceed 50 characters';
    } else if (!/^[a-zA-Z0-9_-]+$/.test(formData.username)) {
      newErrors.username = 'Username can only contain letters, numbers, hyphens, and underscores';
    }

    // Email validation
    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (formData.email.length > 100) {
      newErrors.email = 'Email must not exceed 100 characters';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }

    // Password validation
    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    } else if (formData.password.length > 100) {
      newErrors.password = 'Password must not exceed 100 characters';
    }

    // Confirm password validation
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password';
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    // Display name validation
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
    
    // Clear previous errors
    setErrors({});
    
    // Validate form
    if (!validateForm()) {
      return;
    }

    setIsLoading(true);

    try {
      const user = await register({
        username: formData.username.trim(),
        email: formData.email.trim(),
        password: formData.password,
        displayName: formData.displayName.trim(),
      });

      if (user.verificationUrl) {
        promptToOpenVerificationLink(user.verificationUrl, user.verificationEmailSent);
      }
      
      // Redirect to login page on successful registration
      router.push(`/auth/login?registered=true&emailSent=${user.verificationEmailSent !== false}`);
    } catch (error) {
      console.error('Registration failed:', error);
      
      // Display user-friendly error message
      if (error instanceof Error) {
        const message = error.message || 'Registration failed';
        
        // Check for specific error messages from backend
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
    } finally {
      setIsLoading(false);
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
        disabled={isLoading}
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
        disabled={isLoading}
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
        disabled={isLoading}
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
        disabled={isLoading}
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
        disabled={isLoading}
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
        disabled={isLoading}
        aria-label={isLoading ? 'Creating account...' : 'Create account'}
      >
        {isLoading ? 'Creating Account...' : 'Create Account'}
      </Button>
    </form>
  );
}
