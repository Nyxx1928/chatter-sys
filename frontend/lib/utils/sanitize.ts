/**
 * HTML sanitization utilities for frontend XSS protection.
 * Provides functions to escape HTML content and detect dangerous patterns.
 */

/**
 * Escapes HTML entities in a string to prevent XSS attacks.
 * Uses the browser's built-in HTML parsing to safely escape content.
 *
 * @param content the content to sanitize
 * @returns the sanitized content with HTML entities escaped
 */
export function sanitizeHtml(content: string): string {
  if (!content) return content;

  // Create a temporary element to leverage browser's HTML parsing
  const temp = document.createElement('div');
  temp.textContent = content; // textContent automatically escapes HTML
  return temp.innerHTML;
}

/**
 * Detects dangerous HTML patterns that could indicate XSS attempts.
 * Checks for script tags, event handlers, and other malicious patterns.
 *
 * @param content the content to check
 * @returns true if dangerous patterns are detected, false otherwise
 */
export function isSafeContent(content: string): boolean {
  if (!content) return true;

  // Pattern to detect dangerous HTML/JavaScript patterns
  const dangerousPatterns = /<script|<iframe|<object|<embed|on\w+\s*=/gi;
  return !dangerousPatterns.test(content);
}

/**
 * Checks if content contains any dangerous patterns.
 * This is the inverse of isSafeContent for convenience.
 *
 * @param content the content to check
 * @returns true if dangerous patterns are found, false otherwise
 */
export function containsDangerousPatterns(content: string): boolean {
  return !isSafeContent(content);
}
