import { MessageHistoryResponse, PaginationParams } from '../types/api';
import { apiCall } from './client';

const buildQueryString = (params: PaginationParams): string => {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());
  if (params.sort) searchParams.set('sort', params.sort);
  const query = searchParams.toString();
  return query ? `?${query}` : '';
};

export const getMessageHistory = async (
  token: string, roomId: number, params: PaginationParams = {}
): Promise<MessageHistoryResponse> =>
  apiCall<MessageHistoryResponse>(
    `/api/rooms/${roomId}/messages${buildQueryString(params)}`,
    { method: 'GET', token }
  );
