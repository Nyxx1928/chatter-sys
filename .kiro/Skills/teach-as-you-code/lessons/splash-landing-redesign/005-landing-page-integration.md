# Landing Page Integration

## Overview

This lesson covers how to integrate multiple React components into a cohesive landing page experience with splash screen transitions. You'll learn about component orchestration, state management for transitions, and creating smooth user experiences.

**Validates: Requirements 1.1, 1.5, 2.1, 2.2, 2.3, 6.1, 6.2, 6.3, 6.4, 6.5, 10.2**

## Learning Objectives

By the end of this lesson, you will:
- Understand how to create barrel exports for clean component imports
- Manage multiple state transitions in a single page
- Implement smooth fade transitions between splash and landing content
- Handle authentication-based routing in Next.js
- Compose multiple components into a cohesive layout

## Component Barrel Exports

### What is a Barrel Export?

A barrel export (or index file) is a pattern for re-exporting multiple modules from a single entry point. This simplifies imports and provides a clean API for your components.

**Before (without barrel export):**
```typescript
import { SplashScreen } from '@/components/landing/SplashScreen';
import { NavigationHeader } from '@/components/landing/NavigationHeader';
import { HeroSection } from '@/components/landing/HeroSection';
import { UserAvatarDisplay } from '@/components/landing/UserAvatarDisplay';
```

**After (with barrel export):**
```typescript
import {
  HeroSection,
  NavigationHeader,
  SplashScreen,
  UserAvatarDisplay,
} from '@/components/landing';
```

### Creating the Index File

The barrel export file (`frontend/components/landing/index.ts`) serves as the single entry point for all landing components:

```typescript
/**
 * Landing page components for the Kiro Chat application.
 * These components create the entry experience before authentication.
 */

export { SplashScreen } from './SplashScreen';
export type { SplashScreenProps } from './SplashScreen';

export { NavigationHeader } from './NavigationHeader';
export type { NavigationHeaderProps } from './NavigationHeader';

export { HeroSection } from './HeroSection';
export type { HeroSectionProps } from './HeroSection';

export { UserAvatarDisplay } from './UserAvatarDisplay';
export type { UserAvatarDisplayProps } from './UserAvatarDisplay';
```

**Key Points:**
- Export both the component and its TypeScript props type
- Use named exports for better tree-shaking
- Add documentation comments for clarity
- Order exports alphabetically or logically

## Managing Page State and Transitions

### State Management Strategy

The landing page needs to manage multiple states:
1. **Authentication state**: Check if user is logged in
2. **Splash visibility**: Whether to show the splash screen
3. **Landing visibility**: Whether to show the landing content

```typescript
const [showSplash, setShowSplash] = useState(true);
const [showLanding, setShowLanding] = useState(false);
const { isAuthenticated } = useAuthStore();
```

### Authentication-Based Routing

When a user is authenticated, they should bypass the landing page entirely:

```typescript
useEffect(() => {
  if (isAuthenticated) {
    router.push('/chat');
  }
}, [isAuthenticated, router]);

// Don't render anything while checking auth or redirecting
if (isAuthenticated) {
  return null;
}
```

**Why return null?**
- Prevents flash of landing page content before redirect
- Cleaner user experience
- Avoids unnecessary component renders

### Transition Flow

The transition between splash and landing involves:

1. **Splash Screen Active**: `showSplash = true`, `showLanding = false`
2. **Splash Completes**: Callback triggers `setShowSplash(false)`
3. **Brief Pause**: 50ms delay for fade-out to begin
4. **Landing Appears**: `setShowLanding(true)` triggers fade-in

```typescript
const handleSplashComplete = () => {
  setShowSplash(false);
  // Small delay to allow fade-out before showing landing
  setTimeout(() => setShowLanding(true), 50);
};
```

## Implementing Smooth Transitions

### CSS Transitions vs Animations

**CSS Transitions**: Best for state-based changes (on/off)
```css
.transition-opacity {
  transition-property: opacity;
  transition-duration: 500ms;
}
```

**CSS Animations**: Best for complex sequences
```css
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.animate-fade-in {
  animation: fadeIn 0.5s ease-in;
}
```

### Combining Transitions and Animations

The landing page uses both for optimal effect:

```typescript
<div
  className={`transition-opacity duration-500 ${
    showLanding ? 'animate-fade-in opacity-100' : 'opacity-0'
  }`}
>
```

**Breakdown:**
- `transition-opacity duration-500`: Smooth transition when opacity changes
- `animate-fade-in`: Animation class when landing becomes visible
- `opacity-100` or `opacity-0`: State-based opacity control

### Splash Screen Fade-Out

The SplashScreen component handles its own fade-out animation:

```typescript
<div
  className={`fixed inset-0 z-50 ${
    isComplete ? 'animate-fade-out' : 'animate-fade-in'
  }`}
>
```

When `isComplete` becomes true, the component fades out over 500ms before calling `onComplete`.

## Component Composition

### Layout Structure

The landing page composition follows a clear hierarchy:

```
┌─────────────────────────────────────┐
│ SplashScreen (conditional)          │ z-index: 50, fixed
├─────────────────────────────────────┤
│ NavigationHeader (sticky)           │ z-index: 40
├─────────────────────────────────────┤
│                                     │
│  HeroSection                        │
│  - Headline                         │
│  - Subheading                       │
│  - CTA Button                       │
│                                     │
│  UserAvatarDisplay (overlay)        │ pointer-events-none on mobile
│                                     │
├─────────────────────────────────────┤
│ Additional Sections                 │
│ - About                             │
│ - How It Works                      │
│ - Pricing                           │
│ - Contact                           │
└─────────────────────────────────────┘
```

### Z-Index Layering

Proper z-index management ensures elements layer correctly:
- **z-50**: SplashScreen (highest priority)
- **z-40**: NavigationHeader (sticky, above content)
- **z-10**: Additional sections (above decorative elements)
- **Default**: HeroSection, UserAvatarDisplay

### Responsive Positioning

The UserAvatarDisplay uses absolute positioning with pointer-events control:

```typescript
<div className="absolute inset-0 top-32 pointer-events-none lg:pointer-events-auto">
  <UserAvatarDisplay className="h-full w-full opacity-40 lg:opacity-60" />
</div>
```

**Why `pointer-events-none` on mobile?**
- Prevents avatar interactions from blocking hero content clicks
- Better mobile UX where space is limited
- `lg:pointer-events-auto` restores interactions on desktop

## Best Practices

### 1. Clean Import Structure
Use barrel exports to keep imports organized and maintainable.

### 2. State Management Simplicity
Keep state as local as possible. Only lift state when multiple components need it.

### 3. Transition Timing
- Splash screen: 2200ms progress + 350ms fade-out
- Landing fade-in: 500ms
- Total transition: Under 3 seconds for smooth UX

### 4. Conditional Rendering Patterns
```typescript
// Good: Clear conditional structure
{showSplash && <SplashScreen onComplete={handleSplashComplete} />}
{!showSplash && <LandingContent />}

// Avoid: Deeply nested conditionals
{showSplash ? (
  <SplashScreen />
) : showLanding ? (
  <LandingContent />
) : null}
```

### 5. Performance Considerations
- Use CSS animations instead of JavaScript when possible
- Leverage `transform` and `opacity` for GPU acceleration
- Avoid animating layout properties (width, height, margin)

## Common Patterns

### Authentication Check Pattern
```typescript
const { isAuthenticated } = useAuthStore();
const router = useRouter();

useEffect(() => {
  if (isAuthenticated) {
    router.push('/chat');
  }
}, [isAuthenticated, router]);

if (isAuthenticated) {
  return null; // Prevent flash of content
}
```

### Transition State Pattern
```typescript
const [isVisible, setIsVisible] = useState(false);

const handleTransition = () => {
  setIsHidden(true);
  setTimeout(() => setIsVisible(true), 50);
};

return (
  <div className={`transition-opacity duration-500 ${
    isVisible ? 'opacity-100' : 'opacity-0'
  }`}>
```

### Barrel Export Pattern
```typescript
// index.ts
export { ComponentA } from './ComponentA';
export { ComponentB } from './ComponentB';
export type { ComponentAProps } from './ComponentA';
export type { ComponentBProps } from './ComponentB';
```

## Key Takeaways

1. **Barrel exports simplify imports** and provide a clean component API
2. **State transitions should be gradual** with brief delays for smooth UX
3. **CSS transitions are more performant** than JavaScript animations
4. **Authentication checks should prevent content flash** with early returns
5. **Z-index management is critical** for layered UIs
6. **Pointer-events control enables decorative overlays** without blocking interactions

## Related Concepts

- React state management with useState and useEffect
- Next.js App Router and useRouter hook
- CSS transitions and Tailwind utility classes
- Component composition patterns
- Authentication flow handling

## Next Steps

After completing this integration, consider:
- Adding loading states for async operations
- Implementing error boundaries for resilience
- Adding analytics tracking for user engagement
- Optimizing images with Next.js Image component
- Implementing code splitting for faster initial loads
