'use client';

import * as Sentry from '@sentry/nextjs';
import { useEffect } from 'react';

export default function GlobalError({
    error,
    reset,
}: {
    error: Error & { digest?: string };
    reset: () => void;
}) {
    useEffect(() => {
        Sentry.captureException(error);
    }, [error]);

    return (
        <html>
            <body>
                <div style={{ padding: 40, textAlign: 'center' }}>
                    <h1>Something went wrong</h1>
                    <p>We've been notified. Please try again.</p>
                    <button onClick={reset}>Try again</button>
                </div>
            </body>
        </html>
    )
}