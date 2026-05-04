'use client';

import React from 'react';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  fullWidth?: boolean;
}

/**
 * Mobile-optimized Input component with proper touch targets and responsive design
 * Follows mobile-first design principles with Tailwind CSS
 * 
 * Requirements: 13.2, 13.3, 13.4, 13.5, 15.5
 */
export function Input({
  label,
  error,
  helperText,
  fullWidth = false,
  className = '',
  id,
  disabled,
  required,
  ...props
}: InputProps) {
  // Generate unique ID if not provided
  const generatedInputId = React.useId();
  const inputId = id || `input-${generatedInputId}`;
  
  // Base input styles - mobile-first with minimum 44px height for touch targets
  const baseInputStyles = 'min-h-[44px] px-4 py-3 text-base rounded-lg border transition-colors focus:outline-none focus:ring-2 focus:ring-offset-1 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100';
  
  // State-dependent styles with proper contrast (WCAG AA)
  const stateStyles = error
    ? 'border-red-500 focus:border-red-500 focus:ring-red-500'
    : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500';
  
  // Width styles
  const widthStyles = fullWidth ? 'w-full' : '';
  
  const inputClassName = `${baseInputStyles} ${stateStyles} ${widthStyles} ${className}`.trim();
  
  return (
    <div className={`flex flex-col gap-1 ${fullWidth ? 'w-full' : ''}`}>
      {label && (
        <label
          htmlFor={inputId}
          className="text-sm font-medium text-gray-700"
        >
          {label}
          {required && <span className="text-red-500 ml-1" aria-label="required">*</span>}
        </label>
      )}
      
      <input
        id={inputId}
        className={inputClassName}
        disabled={disabled}
        required={required}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={
          error ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined
        }
        {...props}
      />
      
      {error && (
        <p
          id={`${inputId}-error`}
          className="text-sm text-red-600"
          role="alert"
        >
          {error}
        </p>
      )}
      
      {helperText && !error && (
        <p
          id={`${inputId}-helper`}
          className="text-sm text-gray-600"
        >
          {helperText}
        </p>
      )}
    </div>
  );
}

export interface TextAreaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  helperText?: string;
  fullWidth?: boolean;
}

/**
 * Mobile-optimized TextArea component with proper touch targets and responsive design
 * Follows mobile-first design principles with Tailwind CSS
 * 
 * Requirements: 13.2, 13.3, 13.4, 13.5, 15.5
 */
export function TextArea({
  label,
  error,
  helperText,
  fullWidth = false,
  className = '',
  id,
  disabled,
  required,
  rows = 3,
  ...props
}: TextAreaProps) {
  // Generate unique ID if not provided
  const generatedTextareaId = React.useId();
  const textareaId = id || `textarea-${generatedTextareaId}`;
  
  // Base textarea styles - mobile-first with proper touch targets
  const baseTextareaStyles = 'min-h-[88px] px-4 py-3 text-base rounded-lg border transition-colors focus:outline-none focus:ring-2 focus:ring-offset-1 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 resize-vertical';
  
  // State-dependent styles with proper contrast (WCAG AA)
  const stateStyles = error
    ? 'border-red-500 focus:border-red-500 focus:ring-red-500'
    : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500';
  
  // Width styles
  const widthStyles = fullWidth ? 'w-full' : '';
  
  const textareaClassName = `${baseTextareaStyles} ${stateStyles} ${widthStyles} ${className}`.trim();
  
  return (
    <div className={`flex flex-col gap-1 ${fullWidth ? 'w-full' : ''}`}>
      {label && (
        <label
          htmlFor={textareaId}
          className="text-sm font-medium text-gray-700"
        >
          {label}
          {required && <span className="text-red-500 ml-1" aria-label="required">*</span>}
        </label>
      )}
      
      <textarea
        id={textareaId}
        className={textareaClassName}
        disabled={disabled}
        required={required}
        rows={rows}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={
          error ? `${textareaId}-error` : helperText ? `${textareaId}-helper` : undefined
        }
        {...props}
      />
      
      {error && (
        <p
          id={`${textareaId}-error`}
          className="text-sm text-red-600"
          role="alert"
        >
          {error}
        </p>
      )}
      
      {helperText && !error && (
        <p
          id={`${textareaId}-helper`}
          className="text-sm text-gray-600"
        >
          {helperText}
        </p>
      )}
    </div>
  );
}
