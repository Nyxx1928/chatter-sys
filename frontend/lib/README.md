# Lib Directory

This directory contains utility libraries and business logic.

## Structure

- **stomp/** - STOMP client setup, hooks, and types
  - `client.ts` - STOMP client factory
  - `hooks.ts` - React hooks for STOMP subscriptions
  - `types.ts` - STOMP-related TypeScript types

- **api/** - HTTP API client and endpoints
  - `client.ts` - Base HTTP client with error handling
  - `auth.ts` - Authentication API calls
  - `rooms.ts` - Chat room API calls
  - `messages.ts` - Message API calls

- **store/** - Zustand state management stores
  - `authStore.ts` - Authentication state
  - `chatStore.ts` - Chat state (rooms, messages)
  - `connectionStore.ts` - WebSocket connection state

## Guidelines

- All code should be fully typed with TypeScript
- Use proper error handling and type guards
- Keep business logic separate from UI components
