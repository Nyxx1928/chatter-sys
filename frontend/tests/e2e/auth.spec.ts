import { test, expect } from '@playwright/test';

/**
 * Visual consistency tests for the authentication pages.
 *
 * These pages are static (no auth required) so no session injection needed.
 */
test.describe('Login page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/auth/login');
  });

  test('shows the login form', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /welcome back/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /create one now/i })).toBeVisible();
  });

  test('visual snapshot', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /welcome back/i })).toBeVisible();
    await expect(page).toHaveScreenshot('login.png', { animations: 'disabled' });
  });
});

test.describe('Register page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/auth/register');
  });

  test('shows the registration form', async ({ page }) => {
    // The register page should have a heading and a link back to login
    await expect(page.getByRole('link', { name: /sign in/i })).toBeVisible({ timeout: 8_000 });
  });

  test('visual snapshot', async ({ page }) => {
    await expect(page.getByRole('link', { name: /sign in/i })).toBeVisible({ timeout: 8_000 });
    await expect(page).toHaveScreenshot('register.png', { animations: 'disabled' });
  });
});
