package org.example.chat.util;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

/**
 * Component for sanitizing HTML content to prevent XSS (Cross-Site Scripting) attacks.
 * 
 * Escapes HTML entities and removes dangerous patterns from message content
 * before persistence and display.
 */
@Component
public class HtmlSanitizer {

    // Pattern to detect dangerous HTML/JavaScript patterns
    private static final Pattern DANGEROUS_PATTERNS = Pattern.compile(
        "<script|<iframe|<object|<embed|on\\w+\\s*=|javascript:",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Sanitizes content by escaping HTML entities and removing dangerous patterns.
     * 
     * This method:
     * 1. Escapes HTML entities (e.g., < becomes &lt;)
     * 2. Removes dangerous patterns (script tags, event handlers, etc.)
     * 3. Preserves legitimate content (special characters, unicode, emoji)
     *
     * @param content the content to sanitize
     * @return the sanitized content
     */
    public String sanitize(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // Escape HTML entities to prevent script injection
        String escaped = HtmlUtils.htmlEscape(content);

        // Remove dangerous patterns (additional safety layer)
        String cleaned = DANGEROUS_PATTERNS.matcher(escaped)
            .replaceAll("");

        return cleaned;
    }

    /**
     * Checks if content contains dangerous HTML/JavaScript patterns.
     * 
     * This method detects potential XSS attempts without modifying the content.
     * Used for logging and monitoring security events.
     *
     * @param content the content to check
     * @return true if dangerous patterns are detected, false otherwise
     */
    public boolean containsDangerousPatterns(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        return DANGEROUS_PATTERNS.matcher(content).find();
    }

    /**
     * Checks if content is safe (does not contain dangerous patterns).
     * 
     * This is the inverse of containsDangerousPatterns().
     *
     * @param content the content to check
     * @return true if content is safe, false if dangerous patterns are detected
     */
    public boolean isSafeContent(String content) {
        return !containsDangerousPatterns(content);
    }
}
