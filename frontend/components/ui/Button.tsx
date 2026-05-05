'use client';

import React from 'react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  fullWidth?: boolean;
  children: React.ReactNode;
}

/**
 * Mobile-friendly Button component with minimum 44x44px touch targets
 * Follows mobile-first design principles with Tailwind CSS
 * 
 * Requirements: 13.2, 13.3, 13.4, 13.5, 15.5
 */
export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  className = '',
  children,
  disabled,
  type = 'button',
  ...props
}: ButtonProps) {
  // Base styles - mobile-first with 44x44px minimum touch target
  const baseStyles = 'inline-flex items-center justify-center font-medium rounded-lg transition-all duration-100 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed';
  
  // Size variants - all meet 44x44px minimum for mobile
  const sizeStyles = {
    sm: 'min-h-[44px] px-4 py-2 text-sm',
    md: 'min-h-[44px] px-6 py-3 text-base',
    lg: 'min-h-[48px] px-8 py-4 text-lg',
  };
  
  // Color variants with proper contrast ratios (WCAG AA)
  const variantStyles = {
    primary: 'bg-kiro-purple-500 text-white hover:bg-kiro-purple-600 focus:ring-kiro-purple-400 active:bg-kiro-purple-700',
    secondary: 'bg-kiro-orange-500 text-white hover:bg-kiro-orange-600 focus:ring-kiro-orange-500 active:bg-kiro-orange-600',
    danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500 active:bg-red-800',
    ghost: 'bg-transparent text-kiro-slate-500 hover:bg-kiro-slate-100 focus:ring-kiro-slate-200 active:bg-kiro-slate-200',
  };
  
  // Width styles
  const widthStyles = fullWidth ? 'w-full' : '';
  
  const combinedClassName = `${baseStyles} ${sizeStyles[size]} ${variantStyles[variant]} ${widthStyles} ${className}`.trim();
  
  return (
    <button
      type={type}
      className={combinedClassName}
      disabled={disabled}
      {...props}
    >
      {children}
    </button>
  );
}
