# Design Document: Frontend Splash and Landing Page Redesign

## Overview

This design document specifies the technical implementation for redesigning the frontend entry experience of the chat application. The redesign introduces a splash screen with progress indicator and a modern landing page featuring Kiro IDE's color theme (purple primary, black/dark background, orange accents).

### Goals

- Create a professional first impression with a splash screen during application initialization
- Provide a compelling landing page that communicates the application's value proposition
- Implement responsive design that works seamlessly across mobile, tablet, and desktop devices
- Establish a cohesive visual identity using the Kiro color theme
- Maintain accessibility standards (WCAG 2.1 AA)
- Preserve existing authentication and chat functionality

### Non-Goals

- Redesigning the existing chat interface
- Modifying backend authentication logic
- Implementing user analytics or tracking
- Creating a content management system for landing page content

## Architecture

### Component Hierarchy

```
app/
├── layout.tsx (root layout - updated with theme)
├── page.tsx (landing page - NEW)
├── splash/ (NEW)
│   └── page.tsx (splash screen route)
└── auth/
    ├── login/page.tsx (existing)
    └── register/page.tsx (existing)

components/
├── landing/ (NEW)
│   ├── SplashScreen.tsx
│   ├── NavigationHeader.tsx
│   ├── HeroSection.tsx
│   ├── UserAvatarDisplay.tsx
│   └── index.ts
└── ui/ (existing)
    ├── Button.tsx (enhanced)
    └── ...
```

### Routing Strategy

The application will use Next.js App Router with the following routing logic:

1. **Initial Load** (`/`):
   - Check authentication state
   - If authenticated → redirect to `/chat`
   - If not authenticated → show splash screen → transition to landing page

2. **Splash Screen Flow**:
   - Display splash screen component with progress indicator
   - Simulate loading (0-100% over 2-3 seconds)
   - Transition to landing page

3. **Landing Page** (`/` after splash):
   - Display navigation header, hero section, and user avatar display
   - Provide navigation to `/auth/register` and `/auth/login`

### State Management

The splash and landing page components will use React hooks for local state management:

- `useState` for progress tracking, animation states, and UI interactions
- `useEffect` for progress simulation and transitions
- `useRouter` (Next.js) for navigation
- Zustand `authStore` (existing) for authentication state checks

No global state management is required for these components beyond the existing auth store.

## Components and Interfaces

### 1. SplashScreen Component

**Purpose**: Display a loading screen with progress indicator during initial application load.

**Location**: `frontend/components/landing/SplashScreen.tsx`

**Props Interface**:
```typescript
interface SplashScreenProps {
  onComplete: () => void;
  duration?: number; // milliseconds, default 2500
}
```

**State**:
```typescript
{
  progress: number; // 0-100
  isComplete: boolean;
}
```

**Behavior**:
- Mounts and begins progress animation from 0% to 100%
- Uses `requestAnimationFrame` or interval-based updates for smooth progress
- Calls `onComplete` callback when progress reaches 100%
- Respects `prefers-reduced-motion` media query
- Displays progress percentage and animated progress bar

**Styling**:
- Full viewport height and width
- Dark background (`bg-zinc-950`)
- Purple gradient progress bar
- Centered content with progress indicator
- Fade-out animation on completion

### 2. NavigationHeader Component

**Purpose**: Provide top navigation with menu items and sign-up button.

**Location**: `frontend/components/landing/NavigationHeader.tsx`

**Props Interface**:
```typescript
interface NavigationHeaderProps {
  className?: string;
}
```

**State**:
```typescript
{
  isMobileMenuOpen: boolean;
  activeSection?: string;
}
```

**Menu Items**:
```typescript
const menuItems = [
  { label: 'Home', href: '#home' },
  { label: 'About', href: '#about' },
  { label: 'How It Works', href: '#how-it-works' },
  { label: 'Pricing', href: '#pricing' },
  { label: 'Contact', href: '#contact' }
];
```

**Behavior**:
- Sticky positioning on desktop (`sticky top-0`)
- Hamburger menu on mobile (< 640px)
- Smooth scroll to sections on menu item click
- Highlight active section based on scroll position
- Sign-up button navigates to `/auth/register`

**Styling**:
- Dark background with slight transparency (`bg-zinc-950/95`)
- Purple hover effects on menu items
- Orange accent for sign-up button
- Responsive layout (horizontal on desktop, vertical menu on mobile)

### 3. HeroSection Component

**Purpose**: Display the main headline, subheading, and call-to-action.

**Location**: `frontend/components/landing/HeroSection.tsx`

**Props Interface**:
```typescript
interface HeroSectionProps {
  className?: string;
}
```

**Content**:
```typescript
{
  headline: "Your new way for communication",
  highlightWord: "communication", // styled in orange
  subheading: "Stay connected with friends, family, and colleagues through real-time messaging",
  ctaText: "Register Now",
  ctaHref: "/auth/register"
}
```

**Behavior**:
- Fade-in animation on mount
- CTA button navigates to registration page
- Responsive text sizing

**Styling**:
- Large, bold headline (text-5xl on desktop, text-3xl on mobile)
- Orange color for "communication" word
- Centered layout on mobile, left-aligned on desktop
- Orange CTA button with hover effects

### 4. UserAvatarDisplay Component

**Purpose**: Display artistic arrangement of user avatars with connecting lines.

**Location**: `frontend/components/landing/UserAvatarDisplay.tsx`

**Props Interface**:
```typescript
interface UserAvatarDisplayProps {
  className?: string;
  avatarCount?: number; // default 6-8
}

interface Avatar {
  id: string;
  imageUrl: string;
  position: { x: number; y: number }; // percentage-based
  size: 'sm' | 'md' | 'lg';
}

interface Connection {
  from: string; // avatar id
  to: string; // avatar id
  color: string; // purple shade
}
```

**Behavior**:
- Generate or use predefined avatar positions
- Draw SVG lines connecting avatars
- Responsive repositioning based on viewport size
- Subtle animation on hover (avatar scale, line glow)

**Styling**:
- Absolute positioning for avatars
- SVG overlay for connecting lines
- Purple gradient lines with varying opacity
- Circular avatar images with border
- Responsive layout adjustments

### 5. Enhanced Landing Page

**Purpose**: Main entry page composing all landing components.

**Location**: `frontend/app/page.tsx` (replace existing)

**State**:
```typescript
{
  showSplash: boolean;
  isAuthenticated: boolean;
}
```

**Behavior**:
- Check authentication state on mount
- If authenticated, redirect to `/chat`
- If not authenticated, show splash screen
- After splash completes, show landing page content
- Handle navigation to auth pages

**Layout**:
```
┌─────────────────────────────────────┐
│ NavigationHeader (sticky)           │
├─────────────────────────────────────┤
│                                     │
│  HeroSection                        │
│  - Headline                         │
│  - Subheading                       │
│  - CTA Button                       │
│                                     │
│  UserAvatarDisplay (overlay)        │
│                                     │
└─────────────────────────────────────┘
```

## Data Models

### Theme Configuration

The Kiro color theme will be defined in Tailwind CSS configuration:

```typescript
// frontend/tailwind.config.ts
const config: Config = {
  theme: {
    extend: {
      colors: {
        kiro: {
          purple: {
            50: '#faf5ff',
            100: '#f3e8ff',
            200: '#e9d5ff',
            300: '#d8b4fe',
            400: '#c084fc',
            500: '#a855f7', // primary
            600: '#9333ea',
            700: '#7e22ce',
            800: '#6b21a8',
            900: '#581c87',
            950: '#3b0764',
          },
          orange: {
            400: '#fb923c',
            500: '#f97316', // accent
            600: '#ea580c',
          },
          dark: {
            bg: '#09090b', // zinc-950
            surface: '#18181b', // zinc-900
            border: '#27272a', // zinc-800
          }
        }
      },
      animation: {
        'fade-in': 'fadeIn 0.5s ease-in',
        'fade-out': 'fadeOut 0.5s ease-out',
        'slide-down': 'slideDown 0.3s ease-out',
        'progress': 'progress 2.5s ease-in-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        fadeOut: {
          '0%': { opacity: '1' },
          '100%': { opacity: '0' },
        },
        slideDown: {
          '0%': { transform: 'translateY(-10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
        progress: {
          '0%': { width: '0%' },
          '100%': { width: '100%' },
        },
      },
    },
  },
};
```

### Avatar Data Structure

For the UserAvatarDisplay component, we'll use placeholder avatar data:

```typescript
// frontend/lib/data/avatars.ts
export interface AvatarData {
  id: string;
  imageUrl: string;
  alt: string;
}

export const placeholderAvatars: AvatarData[] = [
  {
    id: 'avatar-1',
    imageUrl: 'https://api.dicebear.com/7.x/avataaars/svg?seed=1',
    alt: 'User avatar 1'
  },
  // ... more avatars
];

export interface AvatarPosition {
  desktop: { x: number; y: number; size: 'sm' | 'md' | 'lg' };
  tablet: { x: number; y: number; size: 'sm' | 'md' | 'lg' };
  mobile: { x: number; y: number; size: 'sm' | 'md' | 'lg' };
}

export const avatarLayout: Record<string, AvatarPosition> = {
  'avatar-1': {
    desktop: { x: 15, y: 20, size: 'md' },
    tablet: { x: 10, y: 15, size: 'sm' },
    mobile: { x: 5, y: 10, size: 'sm' }
  },
  // ... more positions
};
```

## Error Handling

### Client-Side Error Scenarios

1. **Authentication Check Failure**:
   - Scenario: Unable to verify authentication state
   - Handling: Default to unauthenticated state, show landing page
   - User feedback: None (graceful degradation)

2. **Navigation Failure**:
   - Scenario: Router navigation fails
   - Handling: Log error, retry navigation, fallback to window.location
   - User feedback: None (automatic retry)

3. **Animation Performance Issues**:
   - Scenario: Device cannot handle animations smoothly
   - Handling: Respect `prefers-reduced-motion`, disable non-essential animations
   - User feedback: None (automatic adaptation)

4. **Image Loading Failures**:
   - Scenario: Avatar images fail to load
   - Handling: Display fallback colored circles with initials or icons
   - User feedback: None (graceful degradation)

### Error Boundaries

Implement React Error Boundary for landing page components:

```typescript
// frontend/components/landing/LandingErrorBoundary.tsx
class LandingErrorBoundary extends React.Component {
  // Catch rendering errors and display fallback UI
  // Log errors for debugging
  // Provide "Reload" button to recover
}
```

## Testing Strategy

### Overview

This feature involves UI rendering, responsive design, and animations, which are NOT suitable for property-based testing. The testing strategy will focus on:

1. **Snapshot Tests**: Verify component rendering and structure
2. **Visual Regression Tests**: Ensure visual consistency across changes
3. **Example-Based Unit Tests**: Test specific interactions and behaviors
4. **Integration Tests**: Verify routing and authentication flow
5. **Accessibility Tests**: Ensure WCAG 2.1 AA compliance

### Unit Tests

**Component Tests** (using React Testing Library):

1. **SplashScreen Component**:
   - Renders with initial progress at 0%
   - Progress increases over time
   - Calls onComplete callback when reaching 100%
   - Respects prefers-reduced-motion setting
   - Displays correct progress percentage

2. **NavigationHeader Component**:
   - Renders all menu items correctly
   - Sign-up button navigates to /auth/register
   - Mobile menu toggles on hamburger click
   - Menu items trigger scroll to sections
   - Sticky positioning applies on desktop

3. **HeroSection Component**:
   - Renders headline with orange-highlighted word
   - Renders subheading text
   - CTA button navigates to /auth/register
   - Responsive text sizing applies

4. **UserAvatarDisplay Component**:
   - Renders correct number of avatars
   - Displays connecting lines between avatars
   - Handles image loading failures gracefully
   - Repositions avatars on viewport resize

5. **Landing Page Integration**:
   - Shows splash screen on initial load (unauthenticated)
   - Transitions to landing page after splash completes
   - Redirects to /chat if authenticated
   - All navigation links work correctly

### Snapshot Tests

Create Jest snapshots for:
- SplashScreen component at 0%, 50%, 100% progress
- NavigationHeader (desktop and mobile views)
- HeroSection component
- UserAvatarDisplay component
- Full landing page composition

### Visual Regression Tests

Use tools like Playwright or Chromatic to capture:
- Landing page on mobile (375px width)
- Landing page on tablet (768px width)
- Landing page on desktop (1280px width)
- Splash screen appearance
- Navigation header (expanded and collapsed)
- Hover states for buttons and links

### Accessibility Tests

Use jest-axe or similar tools to verify:
- Proper heading hierarchy (h1, h2, h3)
- ARIA labels on interactive elements
- Keyboard navigation support
- Color contrast ratios (WCAG 2.1 AA)
- Focus indicators visibility
- Screen reader compatibility

### Integration Tests

Test complete user flows:
1. **Unauthenticated User Flow**:
   - Visit root URL → see splash screen → see landing page → click "Register Now" → arrive at registration page

2. **Authenticated User Flow**:
   - Visit root URL while authenticated → redirect to /chat

3. **Navigation Flow**:
   - Click menu items → scroll to sections
   - Click sign-up button → navigate to registration

### Performance Tests

Verify performance requirements:
- Landing page loads within 3 seconds on standard broadband
- Splash screen animation runs at 60fps
- No layout shift during page load (CLS < 0.1)
- First Contentful Paint < 1.5s

### Test File Structure

```
frontend/
├── __tests__/
│   ├── components/
│   │   └── landing/
│   │       ├── SplashScreen.test.tsx
│   │       ├── NavigationHeader.test.tsx
│   │       ├── HeroSection.test.tsx
│   │       └── UserAvatarDisplay.test.tsx
│   ├── integration/
│   │   └── landing-flow.test.tsx
│   └── accessibility/
│       └── landing-a11y.test.tsx
└── e2e/
    └── landing-page.spec.ts (Playwright)
```

### Testing Tools

- **React Testing Library**: Component unit tests
- **Jest**: Test runner and assertions
- **jest-axe**: Accessibility testing
- **Playwright**: End-to-end and visual regression tests
- **MSW (Mock Service Worker)**: Mock API calls for authentication checks

### Test Coverage Goals

- Component unit tests: 90%+ coverage
- Integration tests: All critical user flows
- Accessibility tests: All interactive components
- Visual regression: All responsive breakpoints

## Implementation Plan

### Phase 1: Theme Configuration (1-2 hours)

1. Update `tailwind.config.ts` with Kiro color theme
2. Add custom animations and keyframes
3. Test theme in existing components

### Phase 2: Splash Screen (2-3 hours)

1. Create `SplashScreen` component
2. Implement progress animation logic
3. Add prefers-reduced-motion support
4. Write unit tests
5. Create snapshot tests

### Phase 3: Landing Page Components (4-6 hours)

1. Create `NavigationHeader` component
   - Desktop layout
   - Mobile hamburger menu
   - Sticky positioning
2. Create `HeroSection` component
   - Headline with orange highlight
   - CTA button
3. Create `UserAvatarDisplay` component
   - Avatar positioning logic
   - SVG connecting lines
   - Responsive layout
4. Write unit tests for each component

### Phase 4: Landing Page Integration (2-3 hours)

1. Update `app/page.tsx` with new landing page
2. Implement authentication check and routing logic
3. Integrate all landing components
4. Add transitions between splash and landing
5. Write integration tests

### Phase 5: Responsive Design (2-3 hours)

1. Test and refine mobile layout (< 640px)
2. Test and refine tablet layout (640px - 1024px)
3. Test and refine desktop layout (> 1024px)
4. Adjust avatar positions for each breakpoint
5. Verify navigation header responsiveness

### Phase 6: Accessibility (2-3 hours)

1. Add ARIA labels to all interactive elements
2. Ensure keyboard navigation works
3. Verify color contrast ratios
4. Test with screen reader
5. Run automated accessibility tests
6. Fix any issues found

### Phase 7: Testing and Polish (3-4 hours)

1. Write comprehensive unit tests
2. Create visual regression tests
3. Run performance audits
4. Fix any bugs or issues
5. Code review and refinement

**Total Estimated Time**: 16-24 hours

## Accessibility Considerations

### WCAG 2.1 AA Compliance

1. **Color Contrast**:
   - Text on dark background: minimum 4.5:1 ratio
   - Large text (18pt+): minimum 3:1 ratio
   - Orange accent on dark: verified contrast ratio
   - Purple on dark: verified contrast ratio

2. **Keyboard Navigation**:
   - All interactive elements focusable via Tab
   - Visible focus indicators (2px purple outline)
   - Logical tab order (top to bottom, left to right)
   - Escape key closes mobile menu

3. **Screen Reader Support**:
   - Semantic HTML (nav, main, section, button)
   - ARIA labels for icon-only buttons
   - ARIA live region for progress updates
   - Alt text for avatar images

4. **Motion and Animation**:
   - Respect `prefers-reduced-motion` media query
   - Disable non-essential animations when requested
   - Provide alternative feedback for reduced motion

5. **Focus Management**:
   - Focus trapped in mobile menu when open
   - Focus returns to hamburger button when menu closes
   - Skip-to-content link for keyboard users

### ARIA Attributes

```typescript
// SplashScreen
<div role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100}>

// NavigationHeader
<nav aria-label="Main navigation">
<button aria-label="Open menu" aria-expanded={isOpen}>

// HeroSection
<main aria-labelledby="hero-heading">
<h1 id="hero-heading">

// UserAvatarDisplay
<img alt="User avatar" role="img">
```

## Performance Considerations

### Optimization Strategies

1. **Code Splitting**:
   - Lazy load landing components (not critical for initial render)
   - Use Next.js dynamic imports for heavy components

2. **Image Optimization**:
   - Use Next.js Image component for avatars
   - Serve WebP format with fallbacks
   - Lazy load below-the-fold images
   - Use placeholder blur for loading states

3. **Animation Performance**:
   - Use CSS transforms (GPU-accelerated)
   - Avoid animating layout properties (width, height)
   - Use `will-change` sparingly
   - Debounce scroll event listeners

4. **Bundle Size**:
   - Tree-shake unused Tailwind classes
   - Minimize JavaScript bundle
   - Use CSS for animations instead of JS when possible

5. **Rendering Performance**:
   - Use React.memo for expensive components
   - Avoid unnecessary re-renders
   - Use CSS for hover effects instead of state

### Performance Metrics

Target metrics (Lighthouse):
- Performance: 90+
- Accessibility: 100
- Best Practices: 90+
- SEO: 90+

Specific targets:
- First Contentful Paint: < 1.5s
- Largest Contentful Paint: < 2.5s
- Time to Interactive: < 3.0s
- Cumulative Layout Shift: < 0.1

## Security Considerations

### Client-Side Security

1. **Authentication State**:
   - Never expose sensitive tokens in landing page
   - Use secure HTTP-only cookies for auth tokens
   - Validate authentication state server-side

2. **XSS Prevention**:
   - React automatically escapes content
   - Avoid dangerouslySetInnerHTML
   - Sanitize any user-generated content

3. **Navigation Security**:
   - Validate redirect URLs
   - Prevent open redirect vulnerabilities
   - Use relative URLs for internal navigation

4. **Third-Party Resources**:
   - Use trusted CDNs for avatar placeholders
   - Implement Content Security Policy (CSP)
   - Verify integrity of external resources

## Deployment Considerations

### Environment Configuration

No environment-specific configuration required for landing page. The feature uses:
- Existing authentication endpoints
- Client-side routing
- Static assets

### Build Process

1. Tailwind CSS will purge unused styles in production
2. Next.js will optimize images and bundle JavaScript
3. Static assets will be served from CDN

### Rollout Strategy

1. **Development**: Test on local environment
2. **Staging**: Deploy to staging for QA testing
3. **Production**: Deploy with feature flag (optional)
4. **Monitoring**: Track page load times and user engagement

### Rollback Plan

If issues arise:
1. Revert to previous landing page (simple home page)
2. Keep authentication flow intact
3. Monitor error logs for issues

## Future Enhancements

Potential improvements for future iterations:

1. **Content Management**:
   - Admin panel to edit landing page content
   - A/B testing for different headlines

2. **Analytics**:
   - Track user engagement on landing page
   - Measure conversion rate to registration

3. **Internationalization**:
   - Multi-language support
   - Localized content

4. **Advanced Animations**:
   - Parallax scrolling effects
   - Interactive avatar animations

5. **Social Proof**:
   - Display user count or testimonials
   - Show real-time activity indicators

6. **SEO Optimization**:
   - Meta tags for social sharing
   - Structured data markup
   - Sitemap generation

## Appendix

### Design Mockup References

(Placeholder for design mockups - to be added)

### Color Palette

```
Kiro Purple:
- Primary: #a855f7 (purple-500)
- Dark: #7e22ce (purple-700)
- Darker: #581c87 (purple-900)

Kiro Orange:
- Accent: #f97316 (orange-500)
- Hover: #ea580c (orange-600)

Kiro Dark:
- Background: #09090b (zinc-950)
- Surface: #18181b (zinc-900)
- Border: #27272a (zinc-800)
- Text: #fafafa (zinc-50)
- Muted: #a1a1aa (zinc-400)
```

### Typography Scale

```
Headline: text-5xl (48px) / text-3xl (30px) mobile
Subheading: text-xl (20px) / text-lg (18px) mobile
Body: text-base (16px)
Small: text-sm (14px)
```

### Spacing Scale

```
Section padding: py-16 (64px) / py-8 (32px) mobile
Component gap: gap-8 (32px) / gap-4 (16px) mobile
Element margin: mb-6 (24px) / mb-4 (16px) mobile
```

### Responsive Breakpoints

```
Mobile: < 640px
Tablet: 640px - 1024px
Desktop: > 1024px
```

---

**Document Version**: 1.0  
**Last Updated**: 2026-05-05  
**Author**: AI Design Agent  
**Status**: Ready for Review
