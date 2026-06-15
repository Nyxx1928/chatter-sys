import { User, Message } from './domain';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
  csrfToken: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName: string;
}

export interface UpdateProfileRequest {
  email?: string;
  displayName?: string;
}

export interface CreateRoomRequest {
  name: string;
  description?: string;
}

export interface FriendRequestCreateRequest {
  recipientId: number;
}

export interface MessageHistoryResponse {
  content: Message[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: { empty: boolean; sorted: boolean; unsorted: boolean };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: { empty: boolean; sorted: boolean; unsorted: boolean };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}
