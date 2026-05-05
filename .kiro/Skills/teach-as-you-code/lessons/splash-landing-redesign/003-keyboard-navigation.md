# Keyboard Navigation Implementation

## Overview

This lesson covers implementing comprehensive keyboard navigation for the splash and landing page redesign. Keyboard accessibility is essential for users who cannot use a mouse, including those with motor disabilities, power users who prefer keyboard shortcuts, and screen reader users.

## Requirements

- All interactive elements must be focusable via Tab
- Visible focus indicators (2px purple outline)
- Logical tab order (top to bottom, left to right)
- Escape key handler to close mobile menu
- Focus trapping in mobile menu when open
- Return focus to hamburger button when menu closes

## Key Concepts

### 1. Focus Indicators

Focus indicators provide visual feedback to keyboard users about which element currently has focus. Without them, keyboard users cannot navigate effectively.

```tsx
// Focus styles using Tailwind CSS
className="focus:outline-none focus-visible:ring-2 focus-visible:ring-kiro-purple-400 focus-visible:ring-offset-2 focus-visible:ring-offset-kiro-ink-950"
```

**Why use `focus-visible` instead of `focus`?**
- `focus-visible` only shows the ring when the user is navigating with keyboard
- Mouse clicks won't show the ring, providing a cleaner experience for mouse users
- This is the modern best practice for focus indicators

### 2. Focus Trapping

Focus trapping confines keyboard focus within a specific container (like a modal or mobile menu). This prevents users from accidentally navigating to elements outside the intended context.

```tsx
useEffect(() => {
  if (!isMenuOpen) return;

  const handleTabKey = (event: KeyboardEvent) => {
    if (event.key !== 'Tab') return;

    const focusableElements = containerRef.current?.querySelectorAll(
      'button, a[href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );

    if (!focusableElements || focusableElements.length === 0) return;

    const firstElement = focusableElements[0] as HTMLElement;
    const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;

    if (event.shiftKey) {
      // Shift + Tab: if on first element, move to last
      if (document.activeElement === firstElement) {
        event.preventDefault();
        lastElement.focus();
      }
    } else {
      // Tab: if on last element, move to first
      if (document.activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus();
      }
    }
  };

  document.addEventListener('keydown', handleTabKey);
  return () => document.removeEventListener('keydown', handleTabKey);
}, [isMenuOpen]);
```

**Key Points:**
- Query all focusable elements using CSS selectors
- Intercept Tab and Shift+Tab at boundaries
- Wrap focus from last to first element (and vice versa)

### 3. Escape Key Handler

The Escape key provides a quick way for keyboard users to close modals and menus.

```tsx
useEffect(() => {
  const handleKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Escape' && isMenuOpen) {
      handleMenuClose();
    }
  };

  if (isMenuOpen) {
    document.addEventListener('keydown', handleKeyDown);
  }

  return () => {
    document.removeEventListener('keydown', handleKeyDown);
  };
}, [isMenuOpen, handleMenuClose]);
```

### 4. Focus Return

When a modal or menu closes, focus should return to the element that opened it. This maintains context for keyboard and screen reader users.

```tsx
const handleMenuClose = useCallback(() => {
  setIsMenuOpen(false);
  // Return focus to hamburger button when menu closes
  menuButtonRef.current?.focus();
}, []);
```

### 5. ARIA Attributes for Modals

When implementing mobile menus as modals, use appropriate ARIA attributes:

```tsx
<div 
  ref={mobileMenuRef}
  role="dialog"
  aria-modal="true"
  aria-label="Mobile navigation menu"
>
  {/* Menu content */}
</div>
```

- `role="dialog"`: Identifies this as a dialog/modal
- `aria-modal="true"`: Tells screen readers to limit navigation to this container
- `aria-label`: Provides an accessible name for the dialog

## Implementation Patterns

### Pattern 1: Navigation Menu with Mobile Dropdown

```tsx
export function NavigationHeader() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const mobileMenuRef = useRef<HTMLDivElement>(null);

  // 1. Focus trap when menu is open
  // 2. Escape key to close
  // 3. Focus return on close
  // 4. Focus indicators on all interactive elements
}
```

### Pattern 2: Progress Indicator with Live Region

```tsx
<div
  role="progressbar"
  aria-valuenow={progress}
  aria-valuemin={0}
  aria-valuemax={100}
  aria-label="Loading progress"
  aria-live="polite"
  aria-busy={!isComplete}
>
```

- `aria-live="polite"`: Screen readers announce changes without interrupting
- `aria-busy`: Indicates the element is being updated

## CSS Focus Styles

### Global Focus Ring Variables

The focus ring uses a consistent 2px purple outline:

```css
/* Focus ring constants */
--focus-ring-width: 2px;
--focus-ring-color: kiro-purple-400;
--focus-ring-offset: 2px;
```

### Focus Ring with Offset

Using `ring-offset-2` creates a small gap between the element and the ring, improving visibility on dark backgrounds:

```tsx
focus-visible:ring-2 
focus-visible:ring-kiro-purple-400 
focus-visible:ring-offset-2 
focus-visible:ring-offset-kiro-ink-950
```

### Focus Ring Inset (for Container Children)

For elements inside a container (like menu items), use an inset ring to avoid overflow issues:

```tsx
focus-visible:ring-2 
focus-visible:ring-kiro-purple-400 
focus-visible:ring-inset
```

## Tab Order

Tab order follows the DOM order by default. Ensure logical flow:

1. **Header**: Logo → Desktop nav items → Sign Up button → Hamburger menu
2. **Hero Section**: Heading → Subheading → CTA button
3. **Mobile Menu** (when open): First menu item → ... → Last menu item → Sign Up button

## Testing Keyboard Navigation

### Manual Testing Checklist

- [ ] Tab through all interactive elements
- [ ] All interactive elements show visible focus indicator
- [ ] Tab order follows logical flow (top to bottom, left to right)
- [ ] Escape key closes mobile menu
- [ ] Focus is trapped within mobile menu when open
- [ ] Focus returns to hamburger button when menu closes
- [ ] Shift+Tab works in reverse order
- [ ] Focus indicator is visible on all backgrounds

### Testing with Screen Readers

1. **NVDA (Windows)**: Use NVDA + Tab to hear focus changes
2. **VoiceOver (Mac)**: Use VO + Right Arrow to navigate
3. **JAWS (Windows)**: Use Tab and virtual cursor

## Common Pitfalls

1. **Missing `focus-visible` support**: Older browsers may need polyfill
2. **Focus ring cut off**: Use `ring-inset` or ensure container has overflow visible
3. **Focus trap on wrong container**: Trap on the modal, not the entire page
4. **Not returning focus**: Always return focus after closing modals
5. **Inconsistent focus styles**: Use the same ring style across all interactive elements

## Browser Compatibility

- `focus-visible` is supported in all modern browsers
- For older browsers, consider using `focus` styles as fallback
- Focus trapping works in all browsers that support `querySelectorAll`

## Resources

- [MDN: :focus-visible](https://developer.mozilla.org/en-US/docs/Web/CSS/:focus-visible)
- [WAI-ARIA Authoring Practices](https://www.w3.org/WAI/ARIA/apg/)
- [WCAG 2.4.7: Focus Visible](https://www.w3.org/WAI/WCAG21/Understanding/focus-visible.html)

## Summary

Keyboard navigation implementation requires:

1. **Visible focus indicators** using `focus-visible` with a 2px purple outline
2. **Focus trapping** to confine navigation within modals/menus
3. **Escape key handling** to close overlays
4. **Focus return** to maintain context after closing modals
5. **Proper ARIA attributes** for screen reader compatibility

These patterns ensure WCAG 2.1 AA compliance and provide an accessible experience for all users.
