import { Page } from '@playwright/test';

/**
 * Injects a fake auth session into localStorage so tests can bypass
 * the real login flow when testing authenticated pages.
 *
 * Keys mirror what utils/storage.ts writes:
 *   chat_token  — JWT string
 *   chat_user   — JSON-serialised User object
 */
export async function injectFakeAuth(page: Page) {
  await page.addInitScript(() => {
    const fakeUser = JSON.stringify({
      id: 1,
      username: 'testuser',
      displayName: 'Test User',
      email: 'test@example.com',
      online: true,
    });
    localStorage.setItem('chat_token', 'fake-test-token');
    localStorage.setItem('chat_user', fakeUser);
  });
}

/**
 * Clears the stored auth session from localStorage.
 */
export async function clearFakeAuth(page: Page) {
  await page.addInitScript(() => {
    localStorage.removeItem('chat_token');
    localStorage.removeItem('chat_user');
  });
}
