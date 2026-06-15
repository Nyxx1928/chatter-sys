import { apiCall } from './client';

export const registerPushToken = async (token: string, pushToken: string, platform: string): Promise<void> =>
  apiCall<void>('/api/push/register', {
    method: 'POST',
    token,
    body: JSON.stringify({ pushToken, platform })
  });

export const unregisterPushToken = async (token: string, pushToken: string): Promise<void> =>
  apiCall<void>('/api/push/unregister', {
    method: 'POST',
    token,
    body: JSON.stringify({ pushToken })
  });
