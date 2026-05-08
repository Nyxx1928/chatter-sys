import { test, expect } from '@playwright/test';
import { injectFakeAuth } from './helpers/auth';

/**
 * Mobile-first navigation e2e tests.
 *
 * Covers:
 *  - Bottom tab bar visibility / routing on mobile (375 × 812)
 *  - Desktop sidebar visibility / routing on desktop (1280 × 800)
 *  - Mobile room-to-chat panel switching with back button
 *  - New pages (Channels, Contacts, Profile) render correctly
 *  - Profile page logout flow
 *  - No horizontal overflow at 320 px
 *  - MessageInput icon visibility on mobile
 *
 * Requirements: 1.1, 1.5, 1.6, 2.1-2.6, 3.2-3.6, 4.1, 4.2, 5.1, 5.2,
 *               6.1, 6.2, 6.3, 9.1, 10.1-10.3, 12.2
 */

// ── Shared mock data ────────────────────────────────────────────────────────

const MOCK_USER = {
  id: 1,
  username: 'testuser',
  displayName: 'Test User',
  email: 'test@example.com',
  online: true,
};

const MOCK_ROOMS = [
  {
    id: 1,
    name: 'general',
    description: 'General discussion',
    createdBy: MOCK_USER,
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
  MOCK_USER,
  { id: 2, username: 'other', displayName: 'Other User', email: 'other@example.com', online: false },
];

// ── Viewport helpers ─────────────────────────────────────────────────────────

const MOBILE_VIEWPORT  = { width: 375, height: 812 };
const DESKTOP_VIEWPORT = { width: 1280, height: 800 };

// ── Shared setup helper ──────────────────────────────────────────────────────

/**
 * Registers all API mocks that the chat pages need.
 * Call this before `page.goto(...)`.
 */
async function setupMocks(page: import('@playwright/test').Page) {
  await injectFakeAuth(page);

  await page.route('**/api/users/me', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_USER) })
  );
  await page.route('**/api/rooms', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_ROOMS) })
  );
  await page.route('**/api/rooms/1', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_ROOMS[0]) })
  );
  await page.route('**/api/rooms/1/messages**', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_MESSAGES) })
  );
  await page.route('**/api/rooms/1/members', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_MEMBERS) })
  );
  await page.route('**/api/friends', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) })
  );
  await page.route('**/api/friends/requests', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ incoming: [], outgoing: [] }),
    })
  );
  // Block WebSocket / STOMP so the page doesn't hang
  await page.route('**/ws**', (route) => route.abort());
  await page.route('**/stomp**', (route) => route.abort());
}

// ── Task 10.2 — Bottom tab bar visibility and navigation ─────────────────────

test.describe('Task 10.2 — Bottom tab bar visibility and navigation', () => {
  test('mobile (375px): bottom tab bar is visible, desktop sidebar is hidden', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    // Desktop nav has aria-label="Main navigation" (hidden md:flex)
    // Mobile nav has aria-label="Mobile navigation" (md:hidden)
    const desktopNav = page.locator('nav[aria-label="Main navigation"]');
    const mobileNav  = page.locator('nav[aria-label="Mobile navigation"]');

    await expect(desktopNav).toBeHidden({ timeout: 10_000 });
    await expect(mobileNav).toBeVisible({ timeout: 10_000 });
  });

  test('desktop (1280px): desktop sidebar is visible, bottom tab bar is hidden', async ({ page }) => {
    await page.setViewportSize(DESKTOP_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    const desktopNav = page.locator('nav[aria-label="Main navigation"]');
    const mobileNav  = page.locator('nav[aria-label="Mobile navigation"]');

    await expect(desktopNav).toBeVisible({ timeout: 10_000 });
    await expect(mobileNav).toBeHidden({ timeout: 10_000 });
  });

  test('mobile: clicking Channels tab navigates to /chat/channels', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    // Wait for the page to settle
    await page.waitForLoadState('networkidle');

    const channelsTab = page.locator('nav[aria-label="Mobile navigation"]').filter({ hasText: 'Channels' }).locator('a[href="/chat/channels"]').first();
    await channelsTab.click();
    await expect(page).toHaveURL(/\/chat\/channels/, { timeout: 10_000 });
  });

  test('mobile: clicking Contacts tab navigates to /chat/contacts', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    await page.waitForLoadState('networkidle');

    const contactsTab = page.locator('nav[aria-label="Mobile navigation"]').filter({ hasText: 'Contacts' }).locator('a[href="/chat/contacts"]').first();
    await contactsTab.click();
    await expect(page).toHaveURL(/\/chat\/contacts/, { timeout: 10_000 });
  });

  test('mobile: clicking Profile tab navigates to /chat/profile', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    await page.waitForLoadState('networkidle');

    const profileTab = page.locator('nav[aria-label="Mobile navigation"]').filter({ hasText: 'Profile' }).locator('a[href="/chat/profile"]').first();
    await profileTab.click();
    await expect(page).toHaveURL(/\/chat\/profile/, { timeout: 10_000 });
  });

  test('mobile: active tab has aria-current="page"', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat/channels');

    await page.waitForLoadState('networkidle');

    // The Channels link in the bottom tab bar should have aria-current="page"
    const activeTab = page.locator('nav[aria-label="Mobile navigation"] a[aria-current="page"]').first();
    await expect(activeTab).toBeVisible({ timeout: 10_000 });
    const href = await activeTab.getAttribute('href');
    expect(href).toBe('/chat/channels');
  });
});

// ── Task 10.3 — Desktop sidebar navigation ───────────────────────────────────

test.describe('Task 10.3 — Desktop sidebar navigation', () => {
  test('desktop: clicking Channels icon navigates to /chat/channels', async ({ page }) => {
    await page.setViewportSize(DESKTOP_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    await page.waitForLoadState('networkidle');

    // Desktop nav: NavIcon links with aria-label
    const channelsLink = page.locator('nav.hidden.md\\:flex a[aria-label="Channels"]');
    await channelsLink.click();
    await expect(page).toHaveURL(/\/chat\/channels/, { timeout: 10_000 });
  });

  test('desktop: clicking Contacts icon navigates to /chat/contacts', async ({ page }) => {
    await page.setViewportSize(DESKTOP_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    await page.waitForLoadState('networkidle');

    const contactsLink = page.locator('nav.hidden.md\\:flex a[aria-label="Contacts"]');
    await contactsLink.click();
    await expect(page).toHaveURL(/\/chat\/contacts/, { timeout: 10_000 });
  });

  test('desktop: clicking Profile icon navigates to /chat/profile', async ({ page }) => {
    await page.setViewportSize(DESKTOP_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    await page.waitForLoadState('networkidle');

    const profileLink = page.locator('nav.hidden.md\\:flex a[aria-label="Profile"]');
    await profileLink.click();
    await expect(page).toHaveURL(/\/chat\/profile/, { timeout: 10_000 });
  });

  test('desktop: active icon has active styling class', async ({ page }) => {
    await page.setViewportSize(DESKTOP_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat/channels');

    await page.waitForLoadState('networkidle');

    // The active NavIcon gets bg-kiro-purple-600/30 and text-kiro-purple-400 classes
    const channelsLink = page.locator('nav.hidden.md\\:flex a[aria-label="Channels"]');
    await expect(channelsLink).toBeVisible({ timeout: 10_000 });
    const classes = await channelsLink.getAttribute('class') ?? '';
    // Active state includes the purple background class
    expect(classes).toContain('bg-kiro-purple-600/30');
  });
});

// ── Task 10.4 — Mobile room-to-chat flow ─────────────────────────────────────

test.describe('Task 10.4 — Mobile room-to-chat flow', () => {
  test('mobile: room list is visible and chat area is hidden initially', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    // Room list panel should be visible (shows "Chats" heading)
    await expect(page.getByRole('heading', { name: 'Chats' })).toBeVisible({ timeout: 10_000 });

    // Chat column (message input) should not be visible before selecting a room
    const messageInput = page.locator('[aria-label="Message input"]');
    await expect(messageInput).not.toBeVisible({ timeout: 10_000 });
  });

  test('mobile: selecting a room shows chat area and hides room list', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    // Wait for rooms to load
    await expect(page.getByText('general').first()).toBeVisible({ timeout: 10_000 });

    // Click the first room
    await page.getByText('general').first().click();

    // Chat area (message input) should now be visible
    await expect(page.locator('[aria-label="Message input"]')).toBeVisible({ timeout: 10_000 });

    // Room list heading should be hidden (panel is off-screen)
    await expect(page.getByRole('heading', { name: 'Chats' })).not.toBeVisible({ timeout: 10_000 });
  });

  test('mobile: clicking back button returns to room list', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    // Select a room
    await expect(page.getByText('general').first()).toBeVisible({ timeout: 10_000 });
    await page.getByText('general').first().click();

    // Wait for chat to appear
    await expect(page.locator('[aria-label="Message input"]')).toBeVisible({ timeout: 10_000 });

    // Click the back button
    const backButton = page.locator('[aria-label="Back to rooms"]');
    await expect(backButton).toBeVisible({ timeout: 10_000 });
    await backButton.click();

    // Room list should be visible again
    await expect(page.getByRole('heading', { name: 'Chats' })).toBeVisible({ timeout: 10_000 });

    // Chat area should be hidden again
    await expect(page.locator('[aria-label="Message input"]')).not.toBeVisible({ timeout: 10_000 });
  });
});

// ── Task 10.5 — New pages render correctly ───────────────────────────────────

test.describe('Task 10.5 — New pages render correctly', () => {
  test('/chat/channels: room list is rendered', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat/channels');

    // The Channels page renders a RoomSelector with the rooms list
    await expect(page.getByText('general').first()).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText('random').first()).toBeVisible({ timeout: 10_000 });
  });

  test('/chat/contacts: FriendsPanel content is rendered', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat/contacts');

    // The Contacts page renders a "Contacts" heading and FriendsPanel
    await expect(page.getByRole('heading', { name: 'Contacts' })).toBeVisible({ timeout: 10_000 });
  });

  test('/chat/profile: display name and Log Out button are visible', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat/profile');

    // Display name from MOCK_USER
    await expect(page.getByText('Test User')).toBeVisible({ timeout: 10_000 });

    // Log Out button
    await expect(page.getByRole('button', { name: 'Log Out' })).toBeVisible({ timeout: 10_000 });
  });
});

// ── Task 10.6 — Profile page logout ─────────────────────────────────────────

test.describe('Task 10.6 — Profile page logout', () => {
  test('clicking Log Out redirects to /', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat/profile');

    // Wait for the Log Out button
    const logOutBtn = page.getByRole('button', { name: 'Log Out' });
    await expect(logOutBtn).toBeVisible({ timeout: 10_000 });

    await logOutBtn.click();

    // Should redirect to the login page (auth guard redirects unauthenticated users)
    await expect(page).toHaveURL('/auth/login', { timeout: 10_000 });
  });
});

// ── Task 10.7 — No horizontal overflow at 320 px ─────────────────────────────

test.describe('Task 10.7 — No horizontal overflow at 320 px', () => {
  test('no horizontal scroll at 320 px viewport width', async ({ page }) => {
    await page.setViewportSize({ width: 320, height: 568 });
    await setupMocks(page);
    await page.goto('/chat');

    // Wait for the page to settle
    await page.waitForLoadState('networkidle');

    const hasOverflow = await page.evaluate(() => {
      return document.documentElement.scrollWidth > document.documentElement.clientWidth;
    });

    expect(hasOverflow).toBe(false);
  });
});

// ── Task 10.8 — MessageInput icon visibility on mobile ───────────────────────

test.describe('Task 10.8 — MessageInput icon visibility on mobile', () => {
  test('at 375px: Attach, GIF, Format buttons are not visible; Send button is visible', async ({ page }) => {
    await page.setViewportSize(MOBILE_VIEWPORT);
    await setupMocks(page);
    await page.goto('/chat');

    // Select a room so the MessageInput is rendered
    await expect(page.getByText('general').first()).toBeVisible({ timeout: 10_000 });
    await page.getByText('general').first().click();

    // Wait for the message input to appear
    await expect(page.locator('[aria-label="Message input"]')).toBeVisible({ timeout: 10_000 });

    // Attach, GIF, Format should be hidden at 375 px (< 480 px breakpoint)
    await expect(page.locator('[aria-label="Attach file"]')).not.toBeVisible({ timeout: 10_000 });
    await expect(page.locator('[aria-label="Send GIF"]')).not.toBeVisible({ timeout: 10_000 });
    await expect(page.locator('[aria-label="Format text"]')).not.toBeVisible({ timeout: 10_000 });

    // Send button should always be visible
    await expect(page.locator('[aria-label="Send message"]')).toBeVisible({ timeout: 10_000 });
  });
});


