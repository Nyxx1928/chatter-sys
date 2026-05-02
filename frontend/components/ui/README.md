# UI Components

Base reusable UI components built with mobile-first design principles using Tailwind CSS and TypeScript.

## Overview

These components implement the requirements for the real-time chat system frontend:
- **Requirement 13.2**: Mobile-first responsive design
- **Requirement 13.3**: Single-column mobile layout (<768px)
- **Requirement 13.4**: Optimized tablet/desktop layout (≥768px)
- **Requirement 13.5**: Tailwind CSS styling
- **Requirement 15.5**: Touch-friendly interface with minimum 44x44px touch targets

## Components

### Button

Mobile-friendly button component with minimum 44x44px touch targets.

**Props:**
- `variant`: 'primary' | 'secondary' | 'danger' | 'ghost' (default: 'primary')
- `size`: 'sm' | 'md' | 'lg' (default: 'md')
- `fullWidth`: boolean (default: false)
- All standard HTML button attributes

**Example:**
```tsx
import { Button } from '@/components/ui';

<Button variant="primary" size="md" onClick={handleClick}>
  Click Me
</Button>

<Button variant="danger" fullWidth>
  Delete
</Button>
```

**Features:**
- Minimum 44x44px touch target on all sizes
- WCAG AA compliant color contrast
- Focus indicators for keyboard navigation
- Disabled state styling
- Hover and active states

### Input

Mobile-optimized input field with proper touch targets and accessibility.

**Props:**
- `label`: string (optional)
- `error`: string (optional)
- `helperText`: string (optional)
- `fullWidth`: boolean (default: false)
- All standard HTML input attributes

**Example:**
```tsx
import { Input } from '@/components/ui';

<Input
  label="Username"
  placeholder="Enter username"
  error={errors.username}
  fullWidth
  required
/>
```

**Features:**
- Minimum 44px height for easy tapping
- Automatic ID generation for accessibility
- Error and helper text support
- ARIA attributes for screen readers
- Required field indicator

### TextArea

Mobile-optimized textarea with proper touch targets.

**Props:**
- `label`: string (optional)
- `error`: string (optional)
- `helperText`: string (optional)
- `fullWidth`: boolean (default: false)
- `rows`: number (default: 3)
- All standard HTML textarea attributes

**Example:**
```tsx
import { TextArea } from '@/components/ui';

<TextArea
  label="Message"
  placeholder="Enter your message"
  rows={4}
  fullWidth
/>
```

### Card

Responsive card component with mobile-first padding and shadows.

**Props:**
- `variant`: 'default' | 'outlined' | 'elevated' (default: 'default')
- `padding`: 'none' | 'sm' | 'md' | 'lg' (default: 'md')
- All standard HTML div attributes

**Sub-components:**
- `CardHeader`: Header section with consistent spacing
- `CardTitle`: Title with responsive typography
- `CardContent`: Main content area
- `CardFooter`: Footer section with border separator

**Example:**
```tsx
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '@/components/ui';

<Card variant="elevated">
  <CardHeader>
    <CardTitle>Card Title</CardTitle>
  </CardHeader>
  <CardContent>
    Card content goes here
  </CardContent>
  <CardFooter>
    <Button>Action</Button>
  </CardFooter>
</Card>
```

**Features:**
- Responsive padding (smaller on mobile, larger on desktop)
- Multiple visual variants
- Composable sub-components
- Smooth shadow transitions

## Design Principles

### Mobile-First Approach

All components are designed mobile-first, meaning:
1. Base styles target mobile devices
2. Larger screens get enhanced styles via breakpoints
3. Touch targets meet minimum 44x44px requirement
4. Padding and spacing scale appropriately

### Breakpoints

Following Tailwind's default breakpoints:
- `sm`: 640px (landscape phones)
- `md`: 768px (tablets)
- `lg`: 1024px (desktops)
- `xl`: 1280px (large desktops)

### Accessibility

All components follow accessibility best practices:
- Semantic HTML elements
- ARIA labels and attributes
- Keyboard navigation support
- Focus indicators
- WCAG AA color contrast ratios
- Screen reader support

### TypeScript

All components are fully typed with:
- Strict TypeScript mode enabled
- Proper prop interfaces exported
- Type-safe event handlers
- IntelliSense support

## Usage

Import components from the index file:

```tsx
import { Button, Input, Card, CardHeader, CardTitle } from '@/components/ui';
```

Or import individually:

```tsx
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
```

## Demo

See `demo.tsx` for a comprehensive showcase of all components and their variants.

## Testing

Components should be tested for:
- Rendering with different props
- Accessibility compliance
- Responsive behavior
- User interactions (clicks, input changes)
- Keyboard navigation

Example test:
```tsx
import { render, screen } from '@testing-library/react';
import { Button } from './Button';

test('button renders with correct text', () => {
  render(<Button>Click Me</Button>);
  expect(screen.getByRole('button', { name: 'Click Me' })).toBeInTheDocument();
});
```

## Future Enhancements

Potential additions:
- Loading states for Button
- Input validation patterns
- Card loading skeleton
- Dark mode support
- Animation variants
- Additional input types (checkbox, radio, select)
