import { User } from '../types/domain';

type StoredAuth = {
  token: string | null;
  user: User | null;
};

const STORAGE_KEYS = {
  token: 'chat_token',
  user: 'chat_user'
} as const;

const canUseStorage = () =>
  typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';

export const getStoredToken = (): string | null => {
  if (!canUseStorage()) {
    return null;
  }

  try {
    return window.localStorage.getItem(STORAGE_KEYS.token);
  } catch {
    return null;
  }
};

export const getStoredUser = (): User | null => {
  if (!canUseStorage()) {
    return null;
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEYS.user);
    if (!raw) {
      return null;
    }

    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
};

export const setStoredAuth = (token: string, user: User): void => {
  if (!canUseStorage()) {
    return;
  }

  try {
    window.localStorage.setItem(STORAGE_KEYS.token, token);
    window.localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(user));
  } catch {
    // Ignore storage errors
  }
};

export const setStoredUser = (user: User): void => {
  if (!canUseStorage()) {
    return;
  }

  try {
    window.localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(user));
  } catch {
    // Ignore storage errors
  }
};

export const clearStoredAuth = (): void => {
  if (!canUseStorage()) {
    return;
  }

  try {
    window.localStorage.removeItem(STORAGE_KEYS.token);
    window.localStorage.removeItem(STORAGE_KEYS.user);
  } catch {
    // Ignore storage errors
  }
};

export const getStoredAuth = (): StoredAuth => ({
  token: getStoredToken(),
  user: getStoredUser()
});
