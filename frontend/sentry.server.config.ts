import * as Sentry from '@sentry/nextjs';

Sentry.init({
    dsn: process.env.SENTRY_DSN,    // server-side: no NEXT_PUBLIC prefix
    tracesSampleRate: 0.05,         // lower on server - more volume
});