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
  const baseStyles = 'inline-flex items-center justify-center font-medium rounded-pill transition-all duration-100 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed';
  
  // Size variants - all meet 44x44px minimum for mobile
  const sizeStyles = {
    sm: 'min-h-[44px] px-4 py-2 text-sm',
    md: 'min-h-[44px] px-6 py-3 text-base',
    lg: 'min-h-[48px] px-8 py-4 text-lg',
  };
  
  // Color variants with proper contrast ratios (WCAG AA)
  const variantStyles = {
    primary: 'bg-slack-primary text-slack-text-inverse hover:bg-slack-primary-light focus:ring-slack-accent-blue active:bg-slack-primary-light',
    secondary: 'bg-transparent text-slack-accent-blue border border-slack-accent-blue hover:bg-slack-accent-blue hover:text-white focus:ring-slack-accent-blue active:bg-slack-accent-blue',
    danger: 'bg-slack-accent-red text-white hover:bg-slack-accent-red/80 focus:ring-slack-accent-red active:bg-slack-accent-red',
    ghost: 'bg-transparent text-slack-text-secondary hover:bg-slack-surface-tertiary hover:text-slack-text-primary focus:ring-slack-accent-blue',
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
