import { test, expect } from '@playwright/test';

/**
 * Visual consistency tests for the landing page (/).
 *
 * The splash screen auto-dismisses after its animation; we wait for the
 * landing content to appear before taking the snapshot so the screenshot
 * is stable across runs.
 */
test.describe('Landing page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('renders the navigation header', async ({ page }) => {
    // Wait for splash to finish and landing to fade in.
    // Use the <header> element which is always visible on all viewports.
    // (The desktop <nav> is hidden on mobile via lg:flex.)
    await expect(page.getByRole('banner')).toBeVisible({ timeout: 10_000 });
  });

  test('shows hero section after splash', async ({ page }) => {
    // The hero section contains the main heading
    const hero = page.locator('main').first();
    await expect(hero).toBeVisible({ timeout: 10_000 });
  });

  test('visual snapshot — full page', async ({ page }) => {
    // Wait for the landing content to be fully visible
    await expect(page.getByRole('banner')).toBeVisible({ timeout: 10_000 });

    // Small pause to let CSS transitions settle
    await page.waitForTimeout(300);

    // Freeze WebGL animation so Playwright can capture a stable screenshot
    await page.evaluate(() => { (window as any).__LIGHTFALL_PAUSED__ = true; });
    await page.waitForTimeout(100);

    const viewport = page.viewportSize();

    // On narrow viewports (mobile) the full-page height varies between platforms
    // due to font-metric differences, causing dimension-mismatch failures on CI.
    // We use a fixed-height clip that captures substantially more than the viewport
    // but avoids the variable-height tail. Desktop viewports are unaffected.
    if (viewport && viewport.width < 768) {
      await expect(page).toHaveScreenshot('landing-full.png', {
        clip: { x: 0, y: 0, width: viewport.width, height: 3000 },
        animations: 'disabled',
      });
    } else {
      await expect(page).toHaveScreenshot('landing-full.png', {
        fullPage: true,
        animations: 'disabled',
      });
    }
  });

  test('visual snapshot — above the fold', async ({ page }) => {
    await expect(page.getByRole('banner')).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(300);

    // Freeze WebGL animation so Playwright can capture a stable screenshot
    await page.evaluate(() => { (window as any).__LIGHTFALL_PAUSED__ = true; });
    await page.waitForTimeout(100);

    await expect(page).toHaveScreenshot('landing-viewport.png', {
      animations: 'disabled',
    });
  });
});
