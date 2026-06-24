# Requirements Document: Slack-Inspired Visual Redesign

## Introduction

Redesign the Chatter chat application's visual identity across both web (Next.js) and mobile (Expo React Native) platforms using Slack's design language as the primary inspiration. The goal is to replace the current generic styling with a distinctive, professional design system that avoids "AI-slop" appearance — defined by Slack's deep-aubergine primary palette, warm cream-lavender surfaces, humanist display typography, and pill-shaped interactive elements.

## Glossary

- **Design Token**: A named value for a visual property (color, spacing, typography, shadow) used consistently across platforms
- **Primary Palette**: The dominant brand color family — aubergine (`#4A154B`) with supporting shades
- **Accent Palette**: Secondary colors for CTAs, links, and highlights — green (`#2EB67D`), blue, yellow, red
- **Surface**: Background colors for cards, panels, and containers
- **Pill CTA**: A call-to-action button with fully rounded corners (`border-radius: 9999px`)
- **Humanist Sans**: A typeface with varied stroke widths and organic shapes (e.g., Noto Sans, Museo Sans)
- **DESIGN.md**: A design system definition file installed via `npx getdesign@latest add slack`, containing Slack's exact design tokens

## Requirements

### Requirement 1: Design Token Foundation

**User Story:** As a developer, I want a shared set of design tokens imported from Slack's DESIGN.md so that all UI components use a consistent color, typography, and spacing system.

#### Acceptance Criteria

1. WHEN the DESIGN.md is installed via `npx getdesign@latest add slack`, THE design tokens SHALL be available in `.opencode/design.md`
2. WHEN a component references a primary color, THE system SHALL use Slack's aubergine palette (`#4A154B` base)
3. WHEN a component references a surface color, THE system SHALL use Slack's cream-lavender tones in light mode and deep-aubergine tones in dark mode
4. WHEN a component renders a primary call-to-action, THE system SHALL use pill-shaped styling (`border-radius: 9999px`)
5. WHEN a heading is rendered, THE system SHALL use a humanist display sans-serif typeface

### Requirement 2: Authentication Screens Redesign

**User Story:** As a user, I want the login and registration screens to reflect Slack's warm, approachable aesthetic so that the app feels polished and trustworthy from first use.

#### Acceptance Criteria

1. WHEN the login screen renders, THE background SHALL use Slack's lavender gradient and the primary CTA SHALL be a pill-shaped button in aubergine
2. WHEN the registration screen renders, THE form SHALL use Slack surface colors and tight vertical spacing
3. WHEN the forgot-password screen renders, THE layout SHALL match Slack's single-column centered form pattern
4. WHEN any auth screen shows an error, THE error message SHALL use Slack's red accent color

### Requirement 3: Main Chat Interface Redesign

**User Story:** As a user, I want the chat interface to use Slack's visual language so that conversations are easy to read and navigate.

#### Acceptance Criteria

1. WHEN the chat room list renders, each room item SHALL use Slack's surface colors and typography scale
2. WHEN a message bubble renders, THE bubble SHALL use Slack's message styling (rounded corners, sender color coding)
3. WHEN the message input area renders, THE input SHALL use Slack's input styling (pill-shaped, with send button)
4. WHEN the channel/chats tab bar renders, THE tabs SHALL use Slack's active tab indicator style
5. WHEN the contacts list renders, THE list items SHALL match Slack's contact density and typography

### Requirement 4: Navigation and Shell Redesign

**User Story:** As a user, I want the app's navigation to follow Slack's sidebar-and-content layout patterns so that the information hierarchy is clear.

#### Acceptance Criteria

1. WHEN the main tab layout renders on mobile, THE tab bar SHALL use Slack's bottom navigation density
2. WHEN the channels list renders on mobile, THE list SHALL use Slack's sidebar-inspired section headers
3. WHEN the profile screen renders, THE layout SHALL use Slack's settings-like card grouping
4. WHEN a modal or bottom sheet renders, THE overlay SHALL use Slack's surface opacity and border radius

### Requirement 5: Cross-Platform Consistency

**User Story:** As a developer, I want the web and mobile versions to share the same design tokens so that the brand feels unified across platforms.

#### Acceptance Criteria

1. WHEN a color token is defined, THE same hex value SHALL be used on both web and mobile
2. WHEN a typography token is defined, THE same font family and weight SHALL be used on both platforms
3. WHEN a spacing token is defined, THE same rem/pt value SHALL be used on both platforms
4. WHEN a component exists on both platforms, THE visual appearance SHALL be identical (within platform rendering constraints)

### Scope

**In-Scope:**
- Installation of Slack DESIGN.md via `npx getdesign@latest add slack`
- Design token definitions (colors, typography, spacing, border radii, shadows)
- Mobile app redesign (all auth screens, chat screens, navigation, contacts, profile)
- Web frontend redesign (if web frontend exists with visual components)
- Light and dark mode for both platforms
- **New screens or features** required by the redesign — if Slack's design language necessitates new UI patterns (e.g., workspace switcher, sidebar nav component, settings panels) or new screens to maintain visual consistency, they SHALL be added

**Out-of-Scope:**
- Backend changes (no API, database, or server-side modifications)
- Animations and micro-interactions (can be added in a follow-up)
- Accessibility audit (should be done separately after visual freeze)
- Logo and favicon redesign
