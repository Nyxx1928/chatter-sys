import * as SecureStore from 'expo-secure-store';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { User } from '../types/domain';

type StoredAuth = {
  token: string | null;
  user: User | null;
};

const SECURE_TOKEN_KEY = 'chat_token';
const CACHED_USER_KEY = 'chat_user';

// In-memory cache for synchronous access
let cachedToken: string | null = null;
let cachedUser: User | null = null;

export const setSecureToken = async (token: string): Promise<void> => {
  try {
    await SecureStore.setItemAsync(SECURE_TOKEN_KEY, token);
    cachedToken = token;
  } catch {
    // Ignore storage errors
  }
};

export const getSecureToken = async (): Promise<string | null> => {
  try {
    return await SecureStore.getItemAsync(SECURE_TOKEN_KEY);
  } catch {
    return null;
  }
};

export const clearSecureToken = async (): Promise<void> => {
  try {
    await SecureStore.deleteItemAsync(SECURE_TOKEN_KEY);
    cachedToken = null;
  } catch {
    // Ignore storage errors
  }
};

export const setCachedUser = async (user: User): Promise<void> => {
  try {
    await AsyncStorage.setItem(CACHED_USER_KEY, JSON.stringify(user));
    cachedUser = user;
  } catch {
    // Ignore storage errors
  }
};

export const getCachedUser = async (): Promise<User | null> => {
  try {
    const raw = await AsyncStorage.getItem(CACHED_USER_KEY);
    if (!raw) return null;
    const user = JSON.parse(raw) as User;
    cachedUser = user;
    return user;
  } catch {
    return null;
  }
};

export const clearCachedUser = async (): Promise<void> => {
  try {
    await AsyncStorage.removeItem(CACHED_USER_KEY);
    cachedUser = null;
  } catch {
    // Ignore storage errors
  }
};

export const clearAll = async (): Promise<void> => {
  await clearSecureToken();
  await clearCachedUser();
};

export const getStoredToken = (): string | null => {
  return cachedToken;
};

export const setStoredAuth = async (token: string, user: User): Promise<void> => {
  await setSecureToken(token);
  await setCachedUser(user);
};

export const setStoredUser = async (user: User): Promise<void> => {
  await setCachedUser(user);
};

export const clearStoredAuth = async (): Promise<void> => {
  await clearAll();
};

export const getStoredAuth = (): StoredAuth => ({
  token: cachedToken,
  user: cachedUser
});
