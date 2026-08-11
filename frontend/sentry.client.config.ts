import * as Sentry from '@sentry/nextjs';

Sentry.init({
    dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,     
    tracesSampleRate: 0.1,           // 10% of transactions - tune as needed
    replaysSessionSampleRate: 0.1,    // Session replay on 10% of session
    replaysOnErrorSampleRate: 1.0,    // Always replay on error
    integrations: [
        Sentry.replayIntegration(),
    ],
    // Filter out noisy / expected events
    ignoreErrors: [
        'NetworkError',               // your custom NetworkError is a connectivity issue, not a bug  
        'AbortError',
        'ResizeObserver loop limit exceeded',
    ],
});

