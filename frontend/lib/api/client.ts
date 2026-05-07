export class ApiError extends Error {
  status: number;
  details: unknown;

  constructor(message: string, status: number, details?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

export class NetworkError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'NetworkError';
  }
}

type ApiCallOptions = RequestInit & {
  token?: string | null;
};

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

const parseResponseBody = async (response: Response): Promise<unknown> => {
  const contentType = response.headers.get('content-type') ?? '';

  if (contentType.includes('application/json')) {
    return response.json();
  }

  return response.text();
};

const getErrorMessage = (details: unknown): string => {
  if (typeof details === 'string' && details.trim()) {
    return details;
  }

  if (details && typeof details === 'object') {
    const detailObject = details as {
      message?: unknown;
      errors?: Record<string, unknown>;
    };

    if (typeof detailObject.message === 'string' && detailObject.message.trim()) {
      return detailObject.message;
    }

    if (detailObject.errors && typeof detailObject.errors === 'object') {
      const firstError = Object.values(detailObject.errors).find(
        (value) => typeof value === 'string' && value.trim()
      );

      if (typeof firstError === 'string') {
        return firstError;
      }
    }
  }

  return 'Request failed with a non-success status.';
};

export const apiCall = async <T>(
  path: string,
  options: ApiCallOptions = {}
): Promise<T> => {
  const { token, headers, ...rest } = options;
  const requestHeaders = new Headers(headers);

  if (!requestHeaders.has('Accept')) {
    requestHeaders.set('Accept', 'application/json');
  }

  if (rest.body && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json');
  }

  if (token) {
    requestHeaders.set('Authorization', `Bearer ${token}`);
  }

  try {
    const url = `${API_BASE_URL}${path}`;
    console.debug(`[API] ${options.method ?? 'GET'} ${url}`);
    const response = await fetch(url, {
      ...rest,
      headers: requestHeaders
    });

    if (!response.ok) {
      const details = await parseResponseBody(response);
      const message = getErrorMessage(details);

      throw new ApiError(message, response.status, details);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return (await parseResponseBody(response)) as T;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }

    // Log the raw error so it's visible in the browser console / remote debugger
    console.error(`[API] Network error for ${API_BASE_URL}${path}:`, error);
    throw new NetworkError('Network error while contacting the API.');
  }
};
