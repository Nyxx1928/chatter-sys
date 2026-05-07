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
    // Wait for splash to finish and landing to fade in
    await expect(page.getByRole('navigation')).toBeVisible({ timeout: 10_000 });
  });

  test('shows hero section after splash', async ({ page }) => {
    // The hero section contains the main heading
    const hero = page.locator('main').first();
    await expect(hero).toBeVisible({ timeout: 10_000 });
  });

  test('visual snapshot — full page', async ({ page }) => {
    // Wait for the landing content to be fully visible
    await expect(page.getByRole('navigation')).toBeVisible({ timeout: 10_000 });

    // Small pause to let CSS transitions settle
    await page.waitForTimeout(300);

    await expect(page).toHaveScreenshot('landing-full.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('visual snapshot — above the fold', async ({ page }) => {
    await expect(page.getByRole('navigation')).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(300);

    await expect(page).toHaveScreenshot('landing-viewport.png', {
      animations: 'disabled',
    });
  });
});
