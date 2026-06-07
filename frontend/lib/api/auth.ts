import {
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ResetPasswordRequest,
  UpdateProfileRequest
} from '../../types/api';
import { User } from '../../types/domain';
import { apiCall } from './client';

export const login = async (request: LoginRequest): Promise<LoginResponse> =>
  apiCall<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(request)
  });

export const register = async (request: RegisterRequest): Promise<User> =>
  apiCall<User>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(request)
  });

export const getCurrentUser = async (token: string): Promise<User> =>
  apiCall<User>('/api/users/me', {
    method: 'GET',
    token
  });

export const updateProfile = async (
  token: string,
  request: UpdateProfileRequest
): Promise<User> =>
  apiCall<User>('/api/users/me', {
    method: 'PUT',
    token,
    body: JSON.stringify(request)
  });

export const forgotPassword = async (request: ForgotPasswordRequest): Promise<void> =>
  apiCall<void>('/api/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify(request)
  });

export const resetPassword = async (request: ResetPasswordRequest): Promise<void> =>
  apiCall<void>('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(request)
  });
