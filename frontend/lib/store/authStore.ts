'use client';

import { create } from 'zustand';
import { LoginRequest, RegisterRequest, RegistrationResponse } from '../../types/api';
import { User } from '../../types/domain';
import {
  getCurrentUser,
  login as loginApi,
  register as registerApi
} from '../api/auth';
import {
  clearStoredAuth,
  getStoredAuth,
  setStoredAuth,
  setStoredUser
} from '../../utils/storage';

type AuthState = {
  user: User | null;
  token: string | null;
  csrfToken: string | null;
  isAuthenticated: boolean;
  isInitialized: boolean;
  isChecking: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<RegistrationResponse>;
  validateSession: () => Promise<void>;
  logout: () => void;
};

const storedAuth = getStoredAuth();

export const useAuthStore = create<AuthState>((set, get) => ({
  user: storedAuth.user,
  token: storedAuth.token,
  csrfToken: null,
  isAuthenticated: Boolean(storedAuth.token),
  isInitialized: false,
  isChecking: false,
  login: async (request) => {
    const response = await loginApi(request);

    setStoredAuth(response.token, response.user);

    set({
      user: response.user,
      token: response.token,
      csrfToken: response.csrfToken || null,
      isAuthenticated: true,
      isInitialized: true
    });
  },
  register: async (request) => {
    const response = await registerApi(request);

    set({
      token: null,
      isAuthenticated: false,
      isInitialized: true
    });

    return response;
  },
  validateSession: async () => {
    const { token } = get();

    if (!token) {
      set({
        user: null,
        token: null,
        csrfToken: null,
        isAuthenticated: false,
        isInitialized: true,
        isChecking: false
      });
      return;
    }

    set({ isChecking: true });

    try {
      // Race the session check against a 8-second timeout.
      // On Render's free tier the backend can take 30-60s to cold-start;
      // without this the splash screen stalls indefinitely on first load.
      const user = await Promise.race([
        getCurrentUser(token),
        new Promise<never>((_, reject) =>
          setTimeout(() => reject(new Error('Session validation timed out')), 8000)
        ),
      ]);
      setStoredUser(user);

      set({
        user,
        isAuthenticated: true,
        isInitialized: true,
        isChecking: false
      });
    } catch {
      clearStoredAuth();

      set({
        user: null,
        token: null,
        csrfToken: null,
        isAuthenticated: false,
        isInitialized: true,
        isChecking: false
      });
    }
  },
  logout: () => {
    clearStoredAuth();

    set({
      user: null,
      token: null,
      csrfToken: null,
      isAuthenticated: false,
      isInitialized: true,
      isChecking: false
    });
  }
}));
