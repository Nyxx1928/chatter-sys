'use client';

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '../ui/Button';
import { verifyOtp, resendOtp } from '../../lib/api/auth';
import { ApiError } from '../../lib/api/client';

interface OtpVerificationFormProps {
  email: string;
}

export function OtpVerificationForm({ email }: OtpVerificationFormProps) {
  const router = useRouter();
  const [otpDigits, setOtpDigits] = useState<string[]>(Array(6).fill(''));
  const [error, setError] = useState<string | null>(null);
  const [submitState, setSubmitState] = useState<'idle' | 'verifying' | 'success' | 'error'>('idle');
  const [resendCooldown, setResendCooldown] = useState(60);
  const [timerExpiry, setTimerExpiry] = useState<number>(() => Date.now() + 10 * 60 * 1000);
  const [timeRemaining, setTimeRemaining] = useState(600);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    const interval = setInterval(() => {
      const remaining = Math.max(0, Math.floor((timerExpiry - Date.now()) / 1000));
      setTimeRemaining(remaining);
    }, 1000);
    return () => clearInterval(interval);
  }, [timerExpiry]);

  useEffect(() => {
    if (resendCooldown > 0) {
      const timer = setTimeout(() => setResendCooldown(resendCooldown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [resendCooldown]);

  const handleDigitChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return;

    const newDigits = [...otpDigits];
    newDigits[index] = value.slice(-1);
    setOtpDigits(newDigits);
    setError(null);

    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !otpDigits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = useCallback((e: React.ClipboardEvent) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (pasted.length === 6) {
      const newDigits = pasted.split('');
      setOtpDigits(newDigits);
      inputRefs.current[5]?.focus();
      setError(null);
    }
  }, []);

  const handleVerify = async () => {
    const otp = otpDigits.join('');
    if (otp.length !== 6) {
      setError('Please enter all 6 digits');
      return;
    }

    setSubmitState('verifying');
    setError(null);

    try {
      const response = await verifyOtp({ email, otp });
      if (response.success) {
        setSubmitState('success');
        setTimeout(() => {
          router.push('/auth/login');
        }, 1500);
      } else {
        setSubmitState('error');
        setError(response.message);
      }
    } catch (err) {
      setSubmitState('error');
      if (err instanceof ApiError) {
        setError(err.message || 'Verification failed');
      } else {
        setError('An unexpected error occurred');
      }
    }
  };

  const handleResend = async () => {
    setResendCooldown(60);
    try {
      await resendOtp({ email });
      setTimerExpiry(Date.now() + 10 * 60 * 1000);
      setError(null);
    } catch (err) {
      setResendCooldown(0);
      if (err instanceof ApiError) {
        setError(err.message || 'Failed to resend code');
      } else {
        setError('Failed to resend code');
      }
    }
  };

  const formatTime = (seconds: number): string => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  if (submitState === 'success') {
    return (
      <div className="text-center py-8">
        <div className="text-4xl mb-4">✓</div>
        <h2 className="text-xl font-bold text-slack-text-primary mb-2">Email Verified!</h2>
        <p className="text-sm text-slack-text-secondary">Redirecting to login...</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center gap-6 w-full max-w-md">
      <div className="text-center">
        <p className="text-sm text-slack-text-secondary mb-1">
          Enter the 6-digit code sent to
        </p>
        <p className="text-sm font-medium text-slack-text-primary">{email}</p>
      </div>

      <div className="flex gap-2 justify-center" onPaste={handlePaste} role="group" aria-label="Verification code input">
        {otpDigits.map((digit, index) => (
          <input
            key={index}
            ref={(el) => { inputRefs.current[index] = el; }}
            type="text"
            inputMode="numeric"
            maxLength={1}
            value={digit}
            onChange={(e) => handleDigitChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            aria-label={`Digit ${index + 1}`}
            disabled={submitState === 'verifying'}
            className={`w-12 h-14 text-center text-2xl font-bold rounded-lg border-2 bg-slack-surface-primary text-slack-text-primary transition-colors
              ${submitState === 'verifying' ? 'opacity-50 cursor-not-allowed' : ''}
              ${error ? 'border-slack-accent-red' : 'border-slack-border focus:border-slack-primary'}
              focus:outline-none focus:ring-2 focus:ring-slack-primary/30`}
          />
        ))}
      </div>

      {error && (
        <div role="alert" className="w-full p-3 text-sm text-slack-accent-red bg-slack-accent-red/10 border border-slack-accent-red/30 rounded-lg text-center">
          {error}
        </div>
      )}

      <Button
        variant="primary"
        fullWidth
        onClick={handleVerify}
        disabled={submitState === 'verifying' || otpDigits.join('').length !== 6}
        aria-label={submitState === 'verifying' ? 'Verifying...' : 'Verify code'}
      >
        {submitState === 'verifying' ? 'Verifying...' : 'Verify Code'}
      </Button>

      <div className="text-center">
        <p className="text-xs text-slack-text-secondary mb-2" aria-live="polite">
          Code expires in {formatTime(timeRemaining)}
        </p>
        <button
          onClick={handleResend}
          disabled={resendCooldown > 0 || submitState === 'verifying'}
          className="text-sm text-slack-accent-blue hover:text-slack-accent-blue/80 disabled:text-slack-text-tertiary disabled:cursor-not-allowed focus:outline-none focus:underline transition-colors"
          aria-label={resendCooldown > 0 ? `Resend code in ${resendCooldown}s` : 'Resend code'}
        >
          {resendCooldown > 0
            ? `Resend code in ${resendCooldown}s`
            : 'Resend code'}
        </button>
      </div>
    </div>
  );
}
