import { CreateRoomRequest } from '../../types/api';
import { ChatRoom, User } from '../../types/domain';
import { apiCall } from './client';

export const createRoom = async (
  token: string,
  request: CreateRoomRequest
): Promise<ChatRoom> =>
  apiCall<ChatRoom>('/api/rooms', {
    method: 'POST',
    token,
    body: JSON.stringify(request)
  });

export const listRooms = async (token: string): Promise<ChatRoom[]> =>
  apiCall<ChatRoom[]>('/api/rooms', {
    method: 'GET',
    token
  });

export const getRoomDetails = async (
  token: string,
  roomId: number
): Promise<ChatRoom> =>
  apiCall<ChatRoom>(`/api/rooms/${roomId}`, {
    method: 'GET',
    token
  });

export const getRoomMembers = async (
  token: string,
  roomId: number
): Promise<User[]> =>
  apiCall<User[]>(`/api/rooms/${roomId}/members`, {
    method: 'GET',
    token
  });

export const deleteRoom = async (token: string, roomId: number): Promise<void> =>
  apiCall<void>(`/api/rooms/${roomId}`, {
    method: 'DELETE',
    token
  });
