# Requirements Document: Cross-Platform Slack-Inspired Design Implementation

## Introduction

Apply Slack's design language from `.opencode/specs/slack-inspired-redesign/design.md` to the web frontend and verify parity with the mobile app. The web uses a dark purple "Kiro" theme that must be replaced with Slack's token system while preserving all existing functionality, routes, and components.

## Glossary

- **Slack Token**: A named design value (color, typography, spacing, radius) defined in `design.md` under `--slack-*` or `--type-*` prefixes
- **Kiro Theme**: The existing dark purple design system on the web (`kiro-purple-*`, `kiro-ink-*`, `kiro-slate-*`)
- **Cross-Platform Parity**: The same Slack token resolves to identical hex/pixel values on both web and mobile
- **CTA Uniqueness**: Each screen has exactly one primary call-to-action styled with `--slack-primary` (#4A154B) background and `--radius-pill` (9999px)

## Requirements

### Requirement 1: Web Token Foundation

**User Story:** As a developer, I want all Slack tokens available as Tailwind utility classes and CSS custom properties, so that components can use them consistently.

#### Acceptance Criteria

1.1 WHEN a component uses `bg-slack-primary` in dark mode, THE rendered color SHALL be `#611F69`
1.2 WHEN a component uses `text-slack-text-secondary`, THE rendered color SHALL be `#616061` (light) or `#9D9EA0` (dark)
1.3 WHEN a component uses `rounded-pill`, THE rendered border-radius SHALL be `9999px`
1.4 WHEN a component references `font-sans` or `font-display`, THE font-family SHALL resolve to `'Noto Sans Display', 'Noto Sans', -apple-system, BlinkMacSystemFont, sans-serif`
1.5 ALL Slack tokens from `design.md` SHALL have corresponding dark-mode values in `tailwind.config.ts`

### Requirement 2: Auth Screens — Web Parity

**User Story:** As a user, I want the web login, register, forgot-password, reset-password, and verify-email screens to use Slack's design language, matching the mobile app.

#### Acceptance Criteria

2.1 WHEN I view the login screen, THE background SHALL be a cream-lavender gradient (`#F4EDE4` → `#FFFFFF`) in light mode, and `--slack-surface-primary` (`#1A1D21`) in dark mode
2.2 WHEN I view any auth screen, THE primary CTA button SHALL have `--slack-primary` background and `--radius-pill`
2.3 WHEN I view a link on an auth screen, THE text color SHALL be `--slack-accent-blue` (`#36C5F0`)
2.4 WHEN I view an error message, THE text color SHALL be `--slack-accent-red` (`#E01E5A`)
2.5 WHEN I view a success message, THE text color SHALL be `--slack-accent-green` (`#2EB67D`)
2.6 WHEN I view page titles or labels, THE typography SHALL match the Slack type scale (`--type-display-xl` for titles, `--type-body-md` for labels)

### Requirement 3: Chat Interface — Web Parity

**User Story:** As a user, I want the web chat interfaces (DMs, channels, contacts, profile) to use Slack's design language, matching the mobile app.

#### Acceptance Criteria

3.1 WHEN I view the DM/channel list, THE background SHALL be `--slack-surface-secondary`, search input SHALL be pill-shaped with `--slack-surface-tertiary` background, and list items SHALL use `--slack-surface-primary` with `--slack-border` dividers
3.2 WHEN I view a chat room, MY messages SHALL have `--slack-primary` background with white text, OTHER users' messages SHALL have `--slack-surface-tertiary` background with `--slack-text-primary`
3.3 WHEN I view a message timestamp or secondary text, IT SHALL use `--type-caption` font size and `--slack-text-secondary` color
3.4 WHEN I see an online presence indicator, IT SHALL use `--slack-accent-green` (`#2EB67D`)
3.5 WHEN I see the tab bar or navigation, THE active tab SHALL use `--slack-primary`, inactive tabs SHALL use `--slack-text-secondary`, and the tab bar background SHALL be `--slack-surface-primary`

### Requirement 4: Landing Page — Slack Styling

**User Story:** As a visitor, I want the landing page to use Slack's design language for brand consistency.

#### Acceptance Criteria

4.1 WHEN I view the landing page header/footer, THE colors SHALL use Slack tokens (primary CTAs → `--slack-primary`, text → `--slack-text-primary`/`--slack-text-secondary`)
4.2 WHEN I view feature cards or sections, THE background SHALL use `--slack-surface-secondary`

### Requirement 5: Dark Mode — Full Coverage

**User Story:** As a user who prefers dark mode, I want every screen to render correctly with Slack's dark-mode tokens.

#### Acceptance Criteria

5.1 EVERY component using a Slack token SHALL have a corresponding dark-mode value (no undefined colors, no light-mode fallbacks)
5.2 Dark mode SHALL respond to `prefers-color-scheme: dark` media query on web
5.3 No hardcoded hex colors SHALL remain in any component styles

### Requirement 6: Preservation

**User Story:** As an existing user, I want all current functionality to continue working after the visual redesign.

#### Acceptance Criteria

6.1 EVERY button, input, link, and navigation target that existed before the redesign SHALL still exist and function after
6.2 EVERY API call, form submission, and routing flow SHALL produce the same behavior
6.3 EVERY component file SHALL export the same interfaces and props

### Scope

#### In-Scope

- All Tailwind utility classes in all `components/`, `app/` pages on web frontend
- `tailwind.config.ts` — dark mode token definitions
- `app/globals.css` — CSS custom properties and font loading
- Auth screens, chat screens, landing page, demo pages
- Light and dark mode for every screen
- New visual-only components or screens required to achieve Slack design consistency (e.g., workspace switcher, sidebar navigation, settings screen, message search screen)

#### Out-of-Scope

- Mobile app changes (already completed in previous phases)
- Functional changes to any API, store, or routing logic
- Accessibility audit (separate concern)
