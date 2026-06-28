'use client';

import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'outlined' | 'elevated';
  padding?: 'none' | 'sm' | 'md' | 'lg';
  children: React.ReactNode;
}

/**
 * Responsive Card component with mobile-first padding and shadows
 * Follows mobile-first design principles with Tailwind CSS
 * 
 * Requirements: 13.2, 13.3, 13.4, 13.5, 15.5
 */
export function Card({
  variant = 'default',
  padding = 'md',
  className = '',
  children,
  ...props
}: CardProps) {
  // Base styles
  const baseStyles = 'rounded-lg transition-shadow';
  
  // Variant styles with proper shadows and borders
  const variantStyles = {
    default: 'bg-slack-surface-primary border border-slack-border',
    outlined: 'bg-slack-surface-primary border-2 border-slack-border',
    elevated: 'bg-slack-surface-primary shadow-slack-md hover:shadow-slack-lg',
  };
  
  // Responsive padding - mobile-first approach
  // Smaller padding on mobile, larger on desktop
  const paddingStyles = {
    none: '',
    sm: 'p-3 sm:p-4',
    md: 'p-4 sm:p-6',
    lg: 'p-6 sm:p-8',
  };
  
  const combinedClassName = `${baseStyles} ${variantStyles[variant]} ${paddingStyles[padding]} ${className}`.trim();
  
  return (
    <div className={combinedClassName} {...props}>
      {children}
    </div>
  );
}

export interface CardHeaderProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
}

/**
 * Card header component with consistent spacing
 */
export function CardHeader({
  className = '',
  children,
  ...props
}: CardHeaderProps) {
  return (
    <div
      className={`mb-4 ${className}`.trim()}
      {...props}
    >
      {children}
    </div>
  );
}

export interface CardTitleProps extends React.HTMLAttributes<HTMLHeadingElement> {
  as?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
  children: React.ReactNode;
}

/**
 * Card title component with responsive typography
 */
export function CardTitle({
  as: Component = 'h3',
  className = '',
  children,
  ...props
}: CardTitleProps) {
  // Responsive font sizes - mobile-first
  const baseStyles = 'font-semibold text-slack-text-primary text-lg sm:text-xl';
  
  return (
    <Component
      className={`${baseStyles} ${className}`.trim()}
      {...props}
    >
      {children}
    </Component>
  );
}

export interface CardContentProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
}

/**
 * Card content component with consistent spacing
 */
export function CardContent({
  className = '',
  children,
  ...props
}: CardContentProps) {
  return (
    <div
      className={`text-slack-text-secondary ${className}`.trim()}
      {...props}
    >
      {children}
    </div>
  );
}

export interface CardFooterProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
}

/**
 * Card footer component with consistent spacing
 */
export function CardFooter({
  className = '',
  children,
  ...props
}: CardFooterProps) {
  return (
    <div
      className={`mt-4 pt-4 border-t border-slack-border ${className}`.trim()}
      {...props}
    >
      {children}
    </div>
  );
}
