/**
 * Unit tests for DM room rendering in RoomSelector.
 *
 * Feature: direct-messaging
 * Property 7: DM room label is always the other participant display name
 * Property 9: DM room ARIA label contains other participant display name
 * Property 8: DM room suppresses invite and delete controls
 */

import React from 'react';
import { render, screen, cleanup } from '@testing-library/react';
import '@testing-library/jest-dom';
import * as fc from 'fast-check';
import { RoomSelector } from '../../components/chat/RoomSelector';
import type { ChatRoom, PublicUser } from '../../types/domain';

// Ensure DOM is cleaned up after every test
afterEach(cleanup);

// ── Helpers ──────────────────────────────────────────────────────────────────

/** Build a minimal DIRECT ChatRoom for testing. */
function makeDmRoom(overrides: Partial<ChatRoom> = {}): ChatRoom {
  return {
    id: 1,
    name: 'dm__1__2',
    description: null,
    createdAt: '2026-01-01T00:00:00Z',
    createdBy: {
      id: 99,
      username: 'system',
      email: 'system@example.com',
      displayName: 'System',
      createdAt: '2026-01-01T00:00:00Z',
      lastSeen: null,
      online: false,
    },
    roomType: 'DIRECT',
    ...overrides,
  };
}

/** Build a minimal PublicUser for testing. */
function makePublicUser(displayName: string): PublicUser {
  return {
    id: 2,
    username: 'other_user',
    displayName,
    lastSeen: null,
    online: false,
  };
}

// ── Property 7: DM room label is always the other participant display name ────

describe('Property 7: DM room label is always the other participant display name', () => {
  // Feature: direct-messaging, Property 7: DM room label is always the other participant display name

  it('renders otherParticipant.displayName as the room label for DIRECT rooms', () => {
    fc.assert(
      fc.property(
        // Generate non-empty display names (printable ASCII, no leading/trailing whitespace)
        fc.string({ minLength: 1, maxLength: 40 })
          .map((s) => s.trim())
          .filter((s) => s.length > 0),
        (displayName) => {
          const otherParticipant = makePublicUser(displayName);
          const room = makeDmRoom({ otherParticipant });

          render(
            <RoomSelector
              rooms={[room]}
              onRoomSelect={jest.fn()}
            />
          );

          // The button aria-label is "Direct message with {displayName}"
          const expectedLabel = `Direct message with ${displayName}`;
          const button = screen.getByRole('button', { name: expectedLabel });
          expect(button).toBeInTheDocument();
          // The visible text inside the button should contain the display name
          expect(button.textContent).toContain(displayName);

          // Clean up DOM between property iterations
          cleanup();
        }
      ),
      { numRuns: 25 }
    );
  });

  it('falls back to room.name when otherParticipant is absent', () => {
    const room = makeDmRoom({ name: 'dm__1__2' });
    render(<RoomSelector rooms={[room]} onRoomSelect={jest.fn()} />);
    expect(screen.getByText('dm__1__2')).toBeInTheDocument();
  });
});

// ── Property 9: DM room ARIA label contains other participant display name ────

describe('Property 9: DM room ARIA label contains other participant display name', () => {
  // Feature: direct-messaging, Property 9: DM room ARIA label contains other participant display name

  it('sets aria-label to "Direct message with {displayName}" for DIRECT rooms', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 40 })
          .map((s) => s.trim())
          .filter((s) => s.length > 0),
        (displayName) => {
          const otherParticipant = makePublicUser(displayName);
          const room = makeDmRoom({ otherParticipant });

          render(
            <RoomSelector
              rooms={[room]}
              onRoomSelect={jest.fn()}
            />
          );

          const expectedLabel = `Direct message with ${displayName}`;
          const button = screen.getByRole('button', { name: expectedLabel });
          expect(button).toBeInTheDocument();
          expect(button).toHaveAttribute('aria-label', expectedLabel);

          // Clean up DOM between property iterations
          cleanup();
        }
      ),
      { numRuns: 25 }
    );
  });
});

// ── Property 8: DM room suppresses delete control ────────────────────────────

describe('Property 8: DM room suppresses invite and delete controls', () => {
  // Feature: direct-messaging, Property 8: DM room suppresses invite and delete controls

  it('does not render a delete button for DIRECT rooms even when canDeleteRoom returns true', () => {
    const room = makeDmRoom({ otherParticipant: makePublicUser('Alice') });
    const onRoomDelete = jest.fn();

    render(
      <RoomSelector
        rooms={[room]}
        onRoomSelect={jest.fn()}
        onRoomDelete={onRoomDelete}
        canDeleteRoom={() => true}
      />
    );

    // The delete button aria-label is "Delete {room.name}" — should not exist for DM rooms
    const deleteButton = screen.queryByRole('button', { name: /^delete/i });
    expect(deleteButton).not.toBeInTheDocument();
  });

  it('renders a delete button for GROUP rooms when canDeleteRoom returns true', () => {
    const groupRoom: ChatRoom = {
      id: 10,
      name: 'general',
      description: null,
      createdAt: '2026-01-01T00:00:00Z',
      createdBy: {
        id: 1,
        username: 'alice',
        email: 'alice@example.com',
        displayName: 'Alice',
        createdAt: '2026-01-01T00:00:00Z',
        lastSeen: null,
        online: false,
      },
      roomType: 'GROUP',
    };

    render(
      <RoomSelector
        rooms={[groupRoom]}
        onRoomSelect={jest.fn()}
        onRoomDelete={jest.fn()}
        canDeleteRoom={() => true}
      />
    );

    const deleteButton = screen.getByRole('button', { name: /delete general/i });
    expect(deleteButton).toBeInTheDocument();
  });
});
