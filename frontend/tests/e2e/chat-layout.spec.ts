import { test, expect } from '@playwright/test';
import { injectFakeAuth } from './helpers/auth';

/**
 * Visual consistency tests for the chat layout and room list.
 *
 * Because the chat page requires authentication and makes real API calls,
 * we:
 *  1. Inject a fake auth session via localStorage before the page loads.
 *  2. Mock the API endpoints so the page renders without a live backend.
 *
 * This keeps the tests fast, deterministic, and runnable in CI without
 * a running backend.
 */

const MOCK_ROOMS = [
  {
    id: 1,
    name: 'general',
    description: 'General discussion',
    createdBy: { id: 1, username: 'testuser', displayName: 'Test User', email: 'test@example.com', online: true },
    memberCount: 3,
    createdAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 2,
    name: 'random',
    description: 'Random stuff',
    createdBy: { id: 2, username: 'other', displayName: 'Other User', email: 'other@example.com', online: false },
    memberCount: 2,
    createdAt: '2026-01-02T00:00:00Z',
  },
];

const MOCK_MESSAGES = {
  content: [
    {
      id: 1,
      content: 'Hello world!',
      senderId: 2,
      senderUsername: 'other',
      senderDisplayName: 'Other User',
      chatRoomId: 1,
      timestamp: '2026-01-01T10:00:00Z',
    },
    {
      id: 2,
      content: 'Hey there!',
      senderId: 1,
      senderUsername: 'testuser',
      senderDisplayName: 'Test User',
      chatRoomId: 1,
      timestamp: '2026-01-01T10:01:00Z',
    },
  ],
  totalElements: 2,
  totalPages: 1,
  number: 0,
  size: 50,
};

const MOCK_MEMBERS = [
  { id: 1, username: 'testuser', displayName: 'Test User', email: 'test@example.com', online: true },
  { id: 2, username: 'other', displayName: 'Other User', email: 'other@example.com', online: false },
];

test.describe('Chat layout — unauthenticated', () => {
  test('redirects to login when not authenticated', async ({ page }) => {
    await page.goto('/chat');
    await expect(page).toHaveURL(/\/auth\/login/, { timeout: 10_000 });
  });
});

test.describe('Chat layout — authenticated', () => {
  test.beforeEach(async ({ page }) => {
    // Inject fake auth before the page loads
    await injectFakeAuth(page);

    // Mock the session validation endpoint
    await page.route('**/api/users/me', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_MEMBERS[0]),
      })
    );

    // Mock the rooms list endpoint
    await page.route('**/api/rooms', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_ROOMS),
      })
    );

    // Mock individual room details
    await page.route('**/api/rooms/1', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_ROOMS[0]),
      })
    );

    // Mock message history
    await page.route('**/api/rooms/1/messages**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_MESSAGES),
      })
    );

    // Mock room members
    await page.route('**/api/rooms/1/members', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_MEMBERS),
      })
    );

    // Mock WebSocket / STOMP — just block it so the page doesn't hang
    await page.route('**/ws**', (route) => route.abort());
    await page.route('**/stomp**', (route) => route.abort());

    await page.goto('/chat');
  });

  test('shows the room list sidebar', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Chats' })).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText('general').first()).toBeVisible();
    await expect(page.getByText('random').first()).toBeVisible();
  });

  test('desktop: shows icon-only left nav', async ({ page }) => {
    // The desktop nav is hidden on mobile viewports
    const desktopNav = page.locator('nav[aria-label="Main navigation"]');
    const viewport = page.viewportSize();
    if (viewport && viewport.width >= 768) {
      await expect(desktopNav).toBeVisible({ timeout: 10_000 });
    }
  });

  test('mobile: shows bottom tab bar', async ({ page }) => {
    const mobileNav = page.locator('nav[aria-label="Mobile navigation"]');
    const viewport = page.viewportSize();
    if (viewport && viewport.width < 768) {
      await expect(mobileNav).toBeVisible({ timeout: 10_000 });
    }
  });

  test('visual snapshot — room list (no room selected)', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Chats' })).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(300);

    await expect(page).toHaveScreenshot('chat-room-list.png', {
      animations: 'disabled',
    });
  });

  test('visual snapshot — chat view (room selected)', async ({ page }) => {
    await expect(page.getByText('general').first()).toBeVisible({ timeout: 10_000 });

    // Click the first room to open it
    await page.getByText('general').first().click();

    // Wait for messages to load
    await expect(page.getByText('Hello world!')).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(300);

    await expect(page).toHaveScreenshot('chat-room-open.png', {
      animations: 'disabled',
    });
  });
});
