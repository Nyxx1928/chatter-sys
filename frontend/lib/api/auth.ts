import {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
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
