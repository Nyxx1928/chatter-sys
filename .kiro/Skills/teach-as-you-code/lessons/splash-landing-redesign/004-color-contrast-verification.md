# Color Contrast Verification

## Overview

This lesson covers WCAG 2.1 color contrast requirements and the process of verifying and fixing contrast issues in the Kiro color theme for the splash and landing page redesign.

**Task:** 9.3 Verify color contrast ratios
**Requirements:** 7.4, 9.1, 9.2, 9.3

## WCAG 2.1 Contrast Requirements

The Web Content Accessibility Guidelines (WCAG) 2.1 define minimum contrast ratios:

| Text Type | Minimum Ratio | Use Case |
|-----------|---------------|----------|
| Normal text (< 18pt) | 4.5:1 | Body text, navigation items |
| Large text (≥ 18pt or 14pt bold) | 3:1 | Headlines, subheadings |
| UI components | 3:1 | Buttons, form inputs |
| Graphical objects | 3:1 | Icons, borders |

### Contrast Ratio Calculation

The contrast ratio is calculated using relative luminance:

```
Contrast Ratio = (L1 + 0.05) / (L2 + 0.05)
```

Where L1 is the lighter color's luminance and L2 is the darker color's luminance.

## Initial Contrast Analysis

The original Kiro color palette had several contrast issues:

| Color Combination | Original Ratio | Status |
|-------------------|----------------|--------|
| kiro-slate-500 on dark | 3.90:1 | ❌ FAIL (needs 4.5:1) |
| white on kiro-orange-500 | 2.80:1 | ❌ FAIL (needs 4.5:1) |
| white on kiro-purple-500 | 4.35:1 | ❌ FAIL (needs 4.5:1) |

## Color Fixes Applied

### 1. Muted Text Color (kiro-slate-500)

**Problem:** `#6b6b82` had only 3.90:1 contrast on dark backgrounds.

**Fix:** Lightened to `#8b8b9e`

```typescript
// frontend/tailwind.config.ts
slate: {
  400: "#a1a1aa", // New shade for better contrast (7.89:1)
  500: "#8b8b9e", // Fixed: 6.05:1 contrast on dark
}
```

**Result:** 6.05:1 contrast ratio (AA compliant)

### 2. Button Background Colors

**Problem:** Orange and purple button backgrounds didn't have sufficient contrast with white text.

**Fix:** Adjusted colors to meet 4.5:1 minimum:

```typescript
// frontend/tailwind.config.ts
purple: {
  500: "#6f42c1", // Fixed: 6.51:1 with white text
}
orange: {
  400: "#fb923c", // Lighter shade for text accents (8.94:1 on dark)
  500: "#c2410c", // Fixed: 5.18:1 with white text
  600: "#9a3412", // Hover state
}
```

### 3. Orange Accent Text

**Problem:** The darker orange used for buttons needed a lighter alternative for text accents.

**Fix:** Added orange-400 for text accents on dark backgrounds:

```typescript
orange: {
  400: "#fb923c", // 8.94:1 contrast on dark backgrounds
}
```

Updated HeroSection to use the lighter orange:

```typescript
// frontend/components/landing/HeroSection.tsx
<span className="text-kiro-orange-400">
  {HERO_CONTENT.highlightWord}
</span>
```

## Final Contrast Results

All critical contrast requirements are now met:

| Color Combination | Ratio | WCAG Level |
|-------------------|-------|------------|
| White on orange buttons | 5.18:1 | AA ✅ |
| White on purple buttons | 6.51:1 | AA ✅ |
| Orange accent text on dark | 8.94:1 | AAA ✅ |
| Purple accent text on dark | 6.47:1 | AA ✅ |
| Primary heading text on dark | 17.99:1 | AAA ✅ |
| Muted text on dark | 6.05:1 | AA ✅ |
| Navigation menu items | 14.47:1 | AAA ✅ |

## Key Takeaways

1. **Test both directions:** Button colors need testing in two directions:
   - Text ON the button background (white text on colored background)
   - Button background against page background (less critical)

2. **Use separate shades for different purposes:**
   - Lighter shades for text accents on dark backgrounds
   - Darker shades for button backgrounds with white text

3. **WCAG levels:**
   - AA (4.5:1) is the minimum requirement
   - AAA (7:1) is enhanced, ideal for critical content
   - Target AAA for headings and important text when possible

4. **Tools for verification:**
   - WebAIM Contrast Checker: https://webaim.org/resources/contrastchecker/
   - Chrome DevTools Accessibility panel
   - Automated tools: axe-core, jest-axe

## Implementation Notes

When implementing color themes:

1. Define colors in Tailwind config for reusability
2. Create multiple shades of each color for flexibility
3. Test all text/background combinations
4. Document contrast ratios in comments
5. Consider hover and focus states

## Related Files

- `frontend/tailwind.config.ts` - Color definitions
- `frontend/components/ui/Button.tsx` - Button variants
- `frontend/components/landing/HeroSection.tsx` - Orange accent text usage
- `frontend/components/landing/NavigationHeader.tsx` - Text contrast usage
