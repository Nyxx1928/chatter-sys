import { create } from 'zustand';
import { ForgotPasswordRequest, LoginRequest, RegisterRequest, ResetPasswordRequest, UpdateProfileRequest } from '../types/api';
import { User } from '../types/domain';
import { forgotPassword as forgotPasswordApi, getCurrentUser, login as loginApi, register as registerApi, resetPassword as resetPasswordApi, updateProfile as updateProfileApi } from '../api/auth';
import { deleteAccount as deleteAccountApi } from '../api/users';
import { clearStoredAuth, getSecureToken, getStoredAuth, setStoredAuth, setStoredUser } from '../utils/storage';

type AuthState = {
  user: User | null;
  token: string | null;
  csrfToken: string | null;
  isAuthenticated: boolean;
  isInitialized: boolean;
  isChecking: boolean;
  pendingRequestCount: number;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<User>;
  validateSession: () => Promise<void>;
  updateProfile: (request: UpdateProfileRequest) => Promise<void>;
  deleteAccount: () => Promise<void>;
  logout: () => void;
  forgotPassword: (request: ForgotPasswordRequest) => Promise<void>;
  resetPassword: (request: ResetPasswordRequest) => Promise<void>;
  setPendingRequestCount: (count: number) => void;
};

const storedAuth = getStoredAuth();

export const useAuthStore = create<AuthState>((set, get) => ({
  user: storedAuth.user,
  token: storedAuth.token,
  csrfToken: null,
  isAuthenticated: Boolean(storedAuth.token),
  isInitialized: false,
  isChecking: false,
  pendingRequestCount: 0,
  login: async (request) => {
    const response = await loginApi(request);
    await setStoredAuth(response.token, response.user);
    set({
      user: response.user,
      token: response.token,
      csrfToken: response.csrfToken || null,
      isAuthenticated: true,
      isInitialized: true
    });
  },
  register: async (request) => {
    const user = await registerApi(request);
    await setStoredUser(user);
    set({ user, token: null, isAuthenticated: false, isInitialized: true });
    return user;
  },
  validateSession: async () => {
    let { token } = get();
    if (!token) {
      token = await getSecureToken();
    }
    if (!token) {
      set({ user: null, token: null, csrfToken: null, isAuthenticated: false, isInitialized: true, isChecking: false });
      return;
    }
    set({ isChecking: true });
    try {
      const user = await Promise.race([
        getCurrentUser(token),
        new Promise<never>((_, reject) =>
          setTimeout(() => reject(new Error('Session validation timed out')), 8000)
        ),
      ]);
      await setStoredUser(user);
      set({ user, token, isAuthenticated: true, isInitialized: true, isChecking: false });
    } catch {
      await clearStoredAuth();
      set({ user: null, token: null, csrfToken: null, isAuthenticated: false, isInitialized: true, isChecking: false });
    }
  },
  logout: async () => {
    await clearStoredAuth();
    set({ user: null, token: null, csrfToken: null, isAuthenticated: false, isInitialized: true, isChecking: false });
  },
  updateProfile: async (request) => {
    const token = get().token;
    if (!token) throw new Error('Not authenticated');
    const user = await updateProfileApi(token, request);
    await setStoredUser(user);
    set({ user });
  },
  deleteAccount: async () => {
    const token = get().token;
    if (!token) throw new Error('Not authenticated');
    await deleteAccountApi(token);
    await clearStoredAuth();
    set({ user: null, token: null, csrfToken: null, isAuthenticated: false, isInitialized: true });
  },
  forgotPassword: async (request) => {
    await forgotPasswordApi(request);
  },
  resetPassword: async (request) => {
    await resetPasswordApi(request);
  },
  setPendingRequestCount: (count) => set({ pendingRequestCount: count }),
}));
