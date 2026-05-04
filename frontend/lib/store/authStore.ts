'use client';

import { create } from 'zustand';
import { LoginRequest, RegisterRequest } from '../../types/api';
import { User } from '../../types/domain';
import { login as loginApi, register as registerApi } from '../api/auth';
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
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  logout: () => void;
};

const storedAuth = getStoredAuth();

export const useAuthStore = create<AuthState>((set) => ({
  user: storedAuth.user,
  token: storedAuth.token,
  isAuthenticated: Boolean(storedAuth.token),
  login: async (request) => {
    const response = await loginApi(request);

    setStoredAuth(response.token, response.user);

    set({
      user: response.user,
      token: response.token,
      isAuthenticated: true
    });
  },
  register: async (request) => {
    const user = await registerApi(request);
    setStoredUser(user);

    set({
      user,
      token: null,
      isAuthenticated: false
    });
  },
  logout: () => {
    clearStoredAuth();

    set({
      user: null,
      token: null,
      isAuthenticated: false
    });
  }
}));
