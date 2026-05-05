'use client';

import { create } from 'zustand';
import { LoginRequest, RegisterRequest } from '../../types/api';
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
  isAuthenticated: boolean;
  isInitialized: boolean;
  isChecking: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  validateSession: () => Promise<void>;
  logout: () => void;
};

const storedAuth = getStoredAuth();

export const useAuthStore = create<AuthState>((set, get) => ({
  user: storedAuth.user,
  token: storedAuth.token,
  isAuthenticated: Boolean(storedAuth.token),
  isInitialized: false,
  isChecking: false,
  login: async (request) => {
    const response = await loginApi(request);

    setStoredAuth(response.token, response.user);

    set({
      user: response.user,
      token: response.token,
      isAuthenticated: true,
      isInitialized: true
    });
  },
  register: async (request) => {
    const user = await registerApi(request);
    setStoredUser(user);

    set({
      user,
      token: null,
      isAuthenticated: false,
      isInitialized: true
    });
  },
  validateSession: async () => {
    const { token } = get();

    if (!token) {
      set({
        user: null,
        token: null,
        isAuthenticated: false,
        isInitialized: true,
        isChecking: false
      });
      return;
    }

    set({ isChecking: true });

    try {
      const user = await getCurrentUser(token);
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
      isAuthenticated: false,
      isInitialized: true,
      isChecking: false
    });
  }
}));
