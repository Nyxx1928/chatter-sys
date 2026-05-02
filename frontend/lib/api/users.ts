import { UserSearchResult } from '../../types/domain';
import { apiCall } from './client';

export const searchUsers = async (
  token: string,
  query: string
): Promise<UserSearchResult[]> => {
  const searchParams = new URLSearchParams();
  searchParams.set('q', query);

  return apiCall<UserSearchResult[]>(`/api/users/search?${searchParams.toString()}`, {
    method: 'GET',
    token
  });
};
