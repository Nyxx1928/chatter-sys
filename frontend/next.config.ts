import { withSentryConfig } from '@sentry/nextjs';
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Allow ngrok tunnels for local mobile testing.
  // Next.js 16 uses allowedDevOrigins (not allowedDevHosts).
  // The wildcard entry covers any ngrok subdomain so you don't need to
  // update this every time ngrok generates a new URL.
  productionBrowserSourceMaps: true,
  allowedDevOrigins: ["*.ngrok-free.app", "*.ngrok-free.dev", "*.ngrok.io"],
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'api.dicebear.com',
        pathname: '/7.x/avataaars/png',
      },
    ],
    formats: ['image/avif', 'image/webp'],
  },
};

export default withSentryConfig(nextConfig, {
  // Source map upload
  org: process.env.SENTRY_ORG,
  project: process.env.SENTRY_PROJECT,
  authToken: process.env.SENTRY_AUTH_TOKEN,
  silent: process.env.CI !== 'true',   // verbose in CI for debugging
  widenClientFileUpload: true,         // upload a bit more for better stack traces
  webpack: {
    treeshake: {
      removeDebugLogging: true,
    },
  },
});

