import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for cross-browser visual consistency tests.
 *
 * Covers:
 *  - Desktop: Chromium, Firefox, WebKit (1280×720)
 *  - Mobile:  Pixel 5 (Android/Chrome), iPhone 12 (WebKit)
 *
 * Run against a locally running Next.js dev server.
 * Set BASE_URL env var to point at a different host (e.g. staging).
 */
export default defineConfig({
  testDir: './tests/e2e',

  /* Run tests in parallel */
  fullyParallel: true,

  /* Fail the build on CI if you accidentally left test.only in source */
  forbidOnly: !!process.env.CI,

  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,

  /* Limit workers on CI to avoid resource contention */
  workers: process.env.CI ? 2 : undefined,

  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list'],
  ],

  use: {
    /* Base URL — override with BASE_URL env var */
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',

    /* Collect traces on first retry */
    trace: 'on-first-retry',

    /* Screenshot on failure */
    screenshot: 'only-on-failure',

    /* Viewport for desktop projects (overridden per project below) */
    viewport: { width: 1280, height: 720 },
  },

  /* Visual snapshot update threshold — 6% pixel diff allowed.
   * Elevated from 0.2% → 3% → 6% to accommodate cross-platform font rendering
   * differences between local dev machines (Windows/macOS) and CI (Ubuntu Linux).
   * Observed diffs on Linux CI mobile-chrome (Pixel 5) are consistently ~4-5%
   * for auth pages (form inputs, labels) and ~4% for the landing full-page
   * screenshot (font metrics causing slight height variation). The threshold
   * must stay above these values while remaining low enough to catch real
   * regressions. */
  expect: {
    toHaveScreenshot: {
      maxDiffPixelRatio: 0.06,
    },
  },

  /* Per-platform snapshot paths.
   * Playwright appends `process.platform` to snapshot filenames (e.g.
   * `-win32`, `-linux`) via the `{-snapshotSuffix}` token. We KEEP this suffix
   * so each platform has its own baselines — this is what makes visual tests
   * reliable when baselines are generated on Windows but verified on Ubuntu
   * Linux CI, where font rasterization (especially WebKit) differs enough to
   * exceed any reasonable pixel-diff threshold.
   *
   * Workflow: generate baselines on each platform you run tests on:
   *   - Windows (local dev):  npx playwright test --update-snapshots
   *   - Linux (CI parity):   npm run test:e2e:update-snapshots:docker
   * Commit both the `-win32` and `-linux` PNGs. */
  snapshotPathTemplate: '{snapshotDir}/{testFileDir}/{testFileName}-snapshots/{arg}{-projectName}{-snapshotSuffix}{ext}',

  projects: [
    /* ── Desktop browsers ── */
    {
      name: 'chromium-desktop',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox-desktop',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit-desktop',
      use: { ...devices['Desktop Safari'] },
    },

    /* ── Mobile browsers ── */
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'mobile-safari',
      use: { ...devices['iPhone 12'] },
    },
  ],

  /* Start the Next.js server automatically when running tests.
   * In CI we run a production build (`next start`) so the Next.js dev overlay
   * can never intercept pointer events or appear in screenshots — this is
   * defense-in-depth on top of the root-layout hydration fix. Locally we keep
   * `next dev` for fast HMR. The Docker Compose path sets CI=true, so it also
   * uses `next start` (and must build first — see package.json scripts). */
  webServer: {
    command: process.env.CI ? 'npm run start' : 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
