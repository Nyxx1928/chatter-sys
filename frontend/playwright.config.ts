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

  /* Visual snapshot update threshold — 0.2% pixel diff allowed */
  expect: {
    toHaveScreenshot: {
      maxDiffPixelRatio: 0.002,
    },
  },

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

  /* Start the Next.js dev server automatically when running tests locally */
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
