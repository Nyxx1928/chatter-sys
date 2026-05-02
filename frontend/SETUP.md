# Frontend Setup Documentation

## Project Overview

This is a Next.js 14+ frontend application for a real-time chat system, built with TypeScript and mobile-first responsive design.

## Technology Stack

- **Framework**: Next.js 16.2.4 (App Router)
- **Language**: TypeScript 5.x (strict mode)
- **Styling**: Tailwind CSS 4.x
- **State Management**: Zustand 5.0.12
- **WebSocket**: @stomp/stompjs 7.3.0 + sockjs-client 1.6.1
- **UI Library**: React 19.2.4

## Project Structure

```
frontend/
├── app/                    # Next.js App Router pages
│   ├── layout.tsx         # Root layout
│   ├── page.tsx           # Home page
│   └── globals.css        # Global styles with Tailwind
├── components/            # React components
│   ├── chat/             # Chat UI components
│   ├── auth/             # Authentication forms
│   └── ui/               # Reusable UI components
├── lib/                  # Business logic
│   ├── stomp/           # STOMP WebSocket client
│   ├── api/             # HTTP API client
│   └── store/           # Zustand state stores
├── types/               # TypeScript type definitions
├── utils/               # Utility functions
└── public/              # Static assets
```

## Getting Started

### Prerequisites

- Node.js 18+ or 20+
- npm or yarn

### Installation

Dependencies are already installed. If you need to reinstall:

```bash
npm install
```

### Development

Start the development server:

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Build

Create a production build:

```bash
npm run build
```

### Start Production Server

```bash
npm start
```

## Configuration

### TypeScript

The project uses strict TypeScript configuration in `tsconfig.json`:

- `strict: true` - All strict type-checking options enabled
- `strictNullChecks: true` - Prevents null/undefined errors
- `noImplicitAny: true` - Requires explicit type annotations
- `noUnusedLocals: true` - Flags unused variables
- `noUnusedParameters: true` - Flags unused parameters

### Tailwind CSS

Mobile-first breakpoints configured in `app/globals.css`:

- `sm`: 640px (landscape phones)
- `md`: 768px (tablets)
- `lg`: 1024px (desktops)
- `xl`: 1280px (large desktops)
- `2xl`: 1536px (extra large)

### Path Aliases

Import paths use the `@/*` alias:

```typescript
import { Button } from '@/components/ui/Button';
import { User } from '@/types/models';
```

## Key Dependencies

### Production Dependencies

- **@stomp/stompjs**: STOMP protocol client for WebSocket messaging
- **sockjs-client**: WebSocket fallback for older browsers
- **zustand**: Lightweight state management
- **next**: React framework with SSR and routing
- **react**: UI library
- **tailwindcss**: Utility-first CSS framework

### Development Dependencies

- **@types/sockjs-client**: TypeScript types for SockJS
- **@types/node**: Node.js type definitions
- **@types/react**: React type definitions
- **typescript**: TypeScript compiler
- **eslint**: Code linting

## Next Steps

1. **Task 19**: Create TypeScript type definitions (models, API, STOMP)
2. **Task 21**: Implement Zustand stores (auth, chat, connection)
3. **Task 23**: Create HTTP API client
4. **Task 25**: Implement STOMP WebSocket client
5. **Task 27**: Build UI components
6. **Task 29**: Create pages and routing

## Resources

- [Next.js Documentation](https://nextjs.org/docs)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [Zustand Documentation](https://docs.pmnd.rs/zustand)
- [STOMP.js Documentation](https://stomp-js.github.io/stomp-websocket/)

## Troubleshooting

### Build Errors

If you encounter build errors, try:

```bash
rm -rf .next node_modules
npm install
npm run build
```

### TypeScript Errors

Check `tsconfig.json` for strict mode settings. You may need to add type annotations or null checks.

### Tailwind Not Working

Ensure `app/globals.css` is imported in `app/layout.tsx` and contains the `@import "tailwindcss"` directive.

## License

This project is part of a learning exercise for building real-time chat systems.
