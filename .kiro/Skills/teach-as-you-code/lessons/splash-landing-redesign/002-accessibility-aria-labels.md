# Lesson: Accessibility ARIA Labels

## Task Context

- Goal: Add ARIA labels and semantic HTML to landing page components for WCAG 2.1 AA compliance
- Scope: Task 9.1 - Implement accessibility features for SplashScreen, NavigationHeader, HeroSection, and UserAvatarDisplay
- Constraints: Follow ARIA best practices, maintain existing functionality, ensure screen reader compatibility

## Files Modified

- frontend/components/landing/SplashScreen.tsx (modified)
- frontend/components/landing/NavigationHeader.tsx (modified)
- frontend/components/landing/HeroSection.tsx (modified)

## Step-by-Step Changes

1. **SplashScreen.tsx - Progress Bar Accessibility**
   - Added `role="progressbar"` to the progress indicator container
   - Added `aria-valuenow={progress}` to announce current progress value
   - Added `aria-valuemin={0}` and `aria-valuemax={100}` to define the range
   - Added `aria-label="Loading progress"` for screen reader context
   - Added `aria-hidden="true"` to the percentage text display (prevents redundant announcement)

   ```tsx
   <div
     className="mt-8 rounded-full bg-kiro-ink-900/80 p-1"
     role="progressbar"
     aria-valuenow={progress}
     aria-valuemin={0}
     aria-valuemax={100}
     aria-label="Loading progress"
   >
     <div
       className="h-3 rounded-full bg-gradient-to-r from-kiro-purple-500 via-kiro-purple-400 to-kiro-orange-500 transition-[width] duration-150"
       style={{ width: `${progress}%` }}
     />
   </div>
   <p className="mt-3 text-sm text-kiro-slate-200" aria-hidden="true">{progress}%</p>
   ```

2. **NavigationHeader.tsx - Navigation Landmark**
   - Added `aria-label="Main navigation"` to the `<nav>` element
   - The mobile menu button already had `aria-label` and `aria-expanded` attributes (no changes needed)

   ```tsx
   <nav className="hidden items-center gap-6 text-sm text-kiro-slate-200 lg:flex" aria-label="Main navigation">
   ```

3. **HeroSection.tsx - Section Labeling**
   - Added `aria-labelledby="hero-heading"` to the `<section>` element
   - Added `id="hero-heading"` to the `<h1>` element to serve as the label reference

   ```tsx
   <section
     id="home"
     className={`animate-fade-in px-4 py-16 sm:px-6 lg:py-24 ${className}`.trim()}
     aria-labelledby="hero-heading"
   >
     ...
     <h1 id="hero-heading" className="...">
   ```

4. **UserAvatarDisplay.tsx - Avatar Alt Text**
   - Verified that avatar images already have proper `alt` text from the `AvatarData` interface
   - The `alt` property is defined in `frontend/lib/data/avatars.ts` for each avatar
   - No changes needed - component already passes `alt={avatar.alt}` to the Image component

## Why This Approach

- **ARIA Progress Bar Pattern**: Using `role="progressbar"` with `aria-valuenow/min/max` follows the WAI-ARIA design pattern for progress indicators, allowing screen readers to announce progress changes

- **aria-hidden on Redundant Text**: The percentage text (`{progress}%`) is hidden from screen readers because the progressbar role already announces the value. This prevents double-announcement

- **Navigation Landmark Label**: Adding `aria-label` to `<nav>` helps screen reader users understand the purpose of the navigation region, especially when multiple nav elements exist on a page

- **aria-labelledby Pattern**: Using `aria-labelledby` with an ID reference creates a relationship between the section and its heading, providing context for screen reader users about what content the section contains

- **Semantic HTML**: Using native elements (`<nav>`, `<section>`, `<h1>`, `<button>`) provides built-in accessibility without needing excessive ARIA attributes

## Alternatives Considered

1. **aria-live for Progress Updates**: Could have added `aria-live="polite"` to announce progress changes, but:
   - Would cause frequent announcements during animation (annoying for users)
   - The progressbar role already provides sufficient information

2. **Live Region for Completion**: Could announce "Loading complete" with aria-live, but:
   - The visual transition to the landing page is self-evident
   - Could be considered for future enhancement if users request it

3. **Descriptive Alt Text for Avatars**: Could use more descriptive alt text like "Avatar of user Felix", but:
   - Placeholder avatars don't represent real users
   - Generic "User avatar 1" is appropriate for decorative/placeholder content

## Key Concepts

- **ARIA Roles**: `role="progressbar"` tells assistive technologies what the element is and how to interact with it

- **ARIA States and Properties**: `aria-valuenow`, `aria-valuemin`, `aria-valuemax` provide current state information

- **aria-labelledby**: Creates a relationship between an element and the text that labels it (uses ID reference)

- **aria-hidden**: Removes content from the accessibility tree when it would be redundant

- **Landmark Regions**: `<nav>` is a landmark element; adding `aria-label` distinguishes it from other nav elements

- **ID References**: Using IDs to link elements together for accessibility purposes

## Potential Pitfalls

1. **Updating aria-valuenow**: The `aria-valuenow` value must be updated dynamically as progress changes. Our implementation correctly binds it to the `progress` state variable

2. **ID Uniqueness**: The `id="hero-heading"` must be unique on the page. If multiple HeroSection components are rendered, this could cause issues. Consider using a generated unique ID if reuse is expected

3. **aria-labelledby Requires Existing ID**: The referenced ID must exist in the DOM at the same time. Our pattern (section referencing h1 inside it) works because both render together

4. **Screen Reader Testing**: ARIA attributes must be tested with actual screen readers (NVDA, JAWS, VoiceOver) to ensure they work as expected. Automated tests can miss some issues

## What You Learned

- How to implement the ARIA progressbar pattern with proper attributes
- When to use `aria-hidden` to prevent redundant announcements
- How to use `aria-labelledby` to create relationships between elements
- The importance of semantic HTML as a foundation for accessibility
- How navigation landmarks help screen reader users orient themselves
- That proper alt text on images is a fundamental accessibility requirement
