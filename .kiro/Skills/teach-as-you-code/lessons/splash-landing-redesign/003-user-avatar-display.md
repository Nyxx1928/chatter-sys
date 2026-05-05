# Lesson: UserAvatarDisplay Component Implementation

## Task Context

- **Goal**: Create a visually appealing avatar display component with connecting lines that showcases the social nature of the application
- **Scope**: Tasks 5.2-5.5 of the splash and landing redesign - component creation, SVG lines, responsive positioning, and error handling
- **Constraints**: Must use Kiro color theme (purple primary, orange accent, dark backgrounds), support responsive breakpoints, handle image loading failures gracefully

## Files Modified

- `frontend/components/landing/UserAvatarDisplay.tsx` (created)
- `.kiro/Skills/teach-as-you-code/lessons/splash-landing-redesign/003-user-avatar-display.md` (created)

## Step-by-Step Changes

### 1. Component Structure and Props Interface

Created the main component with TypeScript props interface matching the design specification:

```typescript
export interface UserAvatarDisplayProps {
  className?: string;
  avatarCount?: number; // default 8
}
```

### 2. Viewport Detection for Responsive Positioning

Implemented viewport size detection to determine which avatar positions to use:

```typescript
type ViewportSize = 'mobile' | 'tablet' | 'desktop';

useEffect(() => {
  const updateViewportSize = () => {
    const width = window.innerWidth;
    if (width < 640) {
      setViewportSize('mobile');
    } else if (width < 1024) {
      setViewportSize('tablet');
    } else {
      setViewportSize('desktop');
    }
  };
  // ...
}, []);
```

This maps directly to the breakpoints defined in the design:
- Mobile: < 640px
- Tablet: 640px - 1024px
- Desktop: > 1024px

### 3. SVG Connecting Lines

Created a dedicated `ConnectionLines` component that:
- Calculates line endpoints based on avatar center positions
- Uses SVG gradient for purple styling
- Recalculates when viewport or container size changes

```typescript
<svg className="pointer-events-none absolute inset-0 h-full w-full">
  <defs>
    <linearGradient id="lineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stopColor="#7c5cff" stopOpacity="0.4" />
      <stop offset="50%" stopColor="#9b7cff" stopOpacity="0.6" />
      <stop offset="100%" stopColor="#7c5cff" stopOpacity="0.4" />
    </linearGradient>
  </defs>
  {lines.map((line) => (
    <line key={line.id} x1={line.x1} y1={line.y1} x2={line.x2} y2={line.y2}
      stroke="url(#lineGradient)" strokeWidth="2" />
  ))}
</svg>
```

### 4. Avatar Rendering with Absolute Positioning

Each avatar is positioned using percentage-based coordinates from the avatar data:

```typescript
<div
  className="absolute transition-transform duration-200 hover:scale-110 hover:z-10"
  style={{
    left: `${position.x}%`,
    top: `${position.y}%`,
    width: size,
    height: size,
  }}
>
```

This allows the layout to scale proportionally with the container.

### 5. Image Loading Error Handling

Implemented graceful degradation when avatar images fail to load:

```typescript
const [imageErrors, setImageErrors] = useState<Set<string>>(new Set());

const handleImageError = useCallback((id: string) => {
  setImageErrors((prev) => new Set(prev).add(id));
}, []);
```

The `FallbackAvatar` component displays a colored circle with a user icon:

```typescript
function FallbackAvatar({ avatar, size }: FallbackAvatarProps) {
  // Color is determined consistently based on avatar ID
  const colors = ['bg-kiro-purple-500', 'bg-kiro-purple-600', 'bg-kiro-purple-700'];
  const colorIndex = Math.abs(avatar.id.charCodeAt(avatar.id.length - 1) - 48) % colors.length;
  
  return (
    <div className={`${bgColor} flex items-center justify-center rounded-full`}>
      <svg className="h-1/2 w-1/2 text-kiro-slate-100">
        <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
      </svg>
    </div>
  );
}
```

### 6. Container Size Tracking with ResizeObserver

Used ResizeObserver to track container dimensions for accurate line positioning:

```typescript
useEffect(() => {
  if (!containerRef) return;

  const resizeObserver = new ResizeObserver((entries) => {
    const rect = entries[0].contentRect;
    setContainerSize({ width: rect.width, height: rect.height });
  });

  resizeObserver.observe(containerRef);
  return () => resizeObserver.disconnect();
}, [containerRef]);
```

### 7. Next.js Image Optimization

Used Next.js `Image` component with `unoptimized` flag for external DiceBear URLs:

```typescript
<Image
  src={avatar.imageUrl}
  alt={avatar.alt}
  fill
  className="object-cover"
  onError={handleError}
  unoptimized // Required for external URLs without explicit domains
/>
```

## Why This Approach

1. **Component Composition**: Split into `AvatarItem`, `FallbackAvatar`, and `ConnectionLines` for better separation of concerns and reusability

2. **Percentage-Based Positioning**: Using percentage coordinates allows the avatar layout to scale naturally with any container size, avoiding hardcoded pixel calculations

3. **ResizeObserver over window.resize**: Tracking the actual container element's size is more accurate than relying solely on window dimensions, especially when the component might be used in different layout contexts

4. **Set for Error Tracking**: Using a `Set<string>` for image errors provides O(1) lookup and automatic deduplication if the same image fails multiple times

5. **CSS Transitions for Hover Effects**: Using CSS `transition-transform` instead of JavaScript animations for hover effects is more performant and keeps the hover logic in the presentation layer

6. **SVG Gradient Definition**: Defining the gradient once in `<defs>` and reusing it via `url(#lineGradient)` is more efficient than duplicating gradient definitions for each line

## Alternatives Considered

1. **Canvas for Lines**: Could have used HTML5 Canvas for drawing lines, but SVG provides better declarative structure and is easier to style with CSS

2. **CSS Lines**: Could have used pseudo-elements for simple lines, but SVG provides precise coordinate control needed for connecting arbitrary points

3. **CSS Grid/Flexbox for Avatar Positioning**: Could have used grid or flexbox, but absolute positioning with percentages provides more artistic freedom for the network pattern design

4. **Intersection Observer for Visibility**: Could have added visibility detection to defer calculations, but the component is always visible in the hero section, so it would add unnecessary complexity

5. **WebP with Fallback**: Could have added explicit WebP handling for avatar images, but DiceBear SVGs are already optimized vector graphics

## Key Concepts

1. **Responsive Design Patterns**: Using viewport detection to switch between pre-defined position configurations rather than calculating on-the-fly

2. **SVG Overlay Pattern**: Positioning an SVG layer absolutely to draw over positioned elements without affecting their layout

3. **Error Boundary Pattern at Component Level**: Handling image errors at the individual avatar level rather than failing the entire component

4. **ResizeObserver API**: Modern approach to detecting element size changes, more accurate than window resize events

5. **Next.js Image Component**: Leveraging Next.js optimizations while handling external URLs that don't match configured domains

6. **Memorization with useMemo**: Preventing unnecessary recalculations of line positions when unrelated state changes

## Potential Pitfalls

1. **Container Height**: The parent container must have a defined height. Using `min-h-[400px]` ensures a baseline, but the parent should control the actual height

2. **SSR Considerations**: The component uses `'use client'` and browser APIs (`window`, `ResizeObserver`), so it's client-side only

3. **External URL Domains**: Using `unoptimized` on Next.js Image bypasses optimization. For production, add DiceBear domains to `next.config.js` for proper image optimization

4. **Line Positioning Timing**: Lines are recalculated after container resize, causing a brief delay. Could be mitigated with initial estimates or skeleton rendering

5. **Z-index on Hover**: Avatars scale on hover and need `hover:z-10` to prevent being clipped by adjacent avatars

6. **Memory Leaks**: Always clean up event listeners and ResizeObserver in useEffect return functions

## What You Learned

1. How to create a visually dynamic component with absolute positioning and SVG overlays

2. Implementing graceful degradation for image loading failures with styled fallback components

3. Using ResizeObserver for accurate container dimension tracking in responsive layouts

4. Building reusable sub-components (`AvatarItem`, `FallbackAvatar`, `ConnectionLines`) within a feature component

5. Balancing Next.js Image optimization with external URL flexibility using the `unoptimized` flag

6. Creating a network visualization pattern that emphasizes social connectivity in the UI

## Requirements Implemented

- **5.2**: Avatar rendering with absolute positioning and hover scale effect
- **5.3**: SVG connecting lines with purple gradient styling
- **5.4**: Responsive avatar repositioning based on viewport size
- **5.5**: Image loading error handling with colored circle fallbacks

## Next Steps

1. Write unit tests for the component (Task 5.6)
2. Integrate with the landing page (Task 6.2)
3. Test responsive behavior at all breakpoints (Tasks 8.1-8.3)
4. Add accessibility features (Tasks 9.1-9.2)
