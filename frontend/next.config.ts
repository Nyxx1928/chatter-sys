import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Allow ngrok tunnels for local mobile testing.
  // Next.js 16 uses allowedDevOrigins (not allowedDevHosts).
  // The wildcard entry covers any ngrok subdomain so you don't need to
  // update this every time ngrok generates a new URL.
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

export default nextConfig;
