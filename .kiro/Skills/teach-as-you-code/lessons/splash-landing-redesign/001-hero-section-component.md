# Lesson: Hero Section Component

## Task Context

- Goal: Create a hero section component for the landing page with a headline, subheading, and call-to-action button
- Scope: Task 4.1 - Implement HeroSection component with responsive styling and Kiro theme
- Constraints: Follow existing component patterns, use Kiro color theme, implement fade-in animation

## Files Modified

- frontend/components/landing/HeroSection.tsx (created)

## Step-by-Step Changes

1. Created the HeroSection component file at `frontend/components/landing/HeroSection.tsx`

2. Defined the props interface:
   ```typescript
   export interface HeroSectionProps {
     className?: string;
   }
   ```

3. Created a content object with all the required text:
   ```typescript
   const HERO_CONTENT: HeroContent = {
     headline: 'Your new way for communication',
     highlightWord: 'communication',
     subheading: 'Stay connected with friends, family, and colleagues through real-time messaging',
     ctaText: 'Register Now',
     ctaHref: '/auth/register',
   };
   ```

4. Implemented the headline rendering with the orange-highlighted word:
   - Split the headline by the highlight word
   - Render the parts with the highlight word wrapped in a span with `text-kiro-orange-500`

5. Applied responsive text sizing:
   - Headline: `text-3xl` on mobile, `text-5xl` on desktop (sm breakpoint)
   - Subheading: `text-lg` on mobile, `text-xl` on desktop (sm breakpoint)

6. Implemented responsive alignment:
   - `text-center` on mobile, `text-left` on desktop (sm breakpoint)
   - `justify-center` on mobile, `justify-start` on desktop for the CTA button

7. Used the existing Button component with `variant="secondary"` for orange styling

8. Added `animate-fade-in` class for fade-in animation on mount

## Why This Approach

- **Content Object Pattern**: Storing content in a constant makes it easy to update and maintain, following the pattern used in other landing components

- **String Split for Highlighting**: Splitting the headline by the highlight word is a clean approach that allows us to style just that word without complex JSX or regex logic

- **Mobile-First Responsive**: Using Tailwind's mobile-first approach means base styles are for mobile, with `sm:` prefixes for larger screens

- **Existing Button Component**: Reusing the Button component with `variant="secondary"` gives us the orange accent color with proper hover states and accessibility built-in

- **Section ID**: Added `id="home"` for navigation header scroll targeting

## Alternatives Considered

1. **Using dangerouslySetInnerHTML**: Could have used this to insert HTML for the highlighted word, but rejected because:
   - Security risk with user content
   - React best practice is to avoid it

2. **Creating a custom highlight component**: Could have made a `<HighlightedText>` component, but:
   - Overkill for a single use case
   - Simple span with utility class is sufficient

3. **Inline styles for colors**: Could have used inline styles, but:
   - Tailwind classes maintain consistency with the design system
   - Easier to maintain and update

## Key Concepts

- **Responsive Typography**: Using Tailwind's responsive prefixes (`sm:text-5xl`) to change text size at different breakpoints

- **Conditional Alignment**: Different text alignment and flex justification at mobile vs desktop breakpoints

- **Content Separation**: Keeping content data separate from rendering logic for maintainability

- **Component Composition**: Using the existing Button component rather than building a custom button element

- **Kiro Color Theme**: Using custom Tailwind colors (`kiro-orange-500`, `kiro-slate-100`, etc.) defined in tailwind.config.ts

## Potential Pitfalls

1. **Highlight word not found**: If the highlight word isn't in the headline, `split()` returns a single-element array. The component handles this gracefully but the word won't be styled

2. **Multiple occurrences**: If the highlight word appears multiple times, only the first occurrence gets styled. Could use `split()` with a limit of 2 parts, or use `replaceAll` with a more complex approach

3. **Mobile-first approach**: Remember that base classes apply to mobile, and `sm:` prefixes apply from 640px and up (tablet/desktop)

4. **Unused React import**: With the Next.js compiler, you don't need to import React for JSX. The linter will catch this

## What You Learned

- How to render text with a highlighted word using string splitting
- Mobile-first responsive design patterns with Tailwind CSS
- Component composition with existing UI components
- Content object pattern for maintainable text content
- Using custom Tailwind color theme classes (`kiro-orange-500`, `kiro-slate-100`, etc.)
