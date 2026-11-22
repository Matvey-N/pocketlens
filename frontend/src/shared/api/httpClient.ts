import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { logger } from '@/shared/lib/logger';
import type { AuthResponse, ErrorCode, Problem } from '@/shared/types/api';

/**
 * Central HTTP client. Every API call goes through this instance:
 * - adds the access token,
 * - refreshes the session once on 401 and retries the request (concurrent 401s share one refresh),
 * - converts every failure into a {@link Problem} (RFC 9457) so the UI handles errors uniformly.
 */
export const API_BASE_URL = '/api/v1';

export const http = axios.create({ baseURL: API_BASE_URL, withCredentials: true, timeout: 60_000 });

interface SessionHooks {
  getAccessToken: () => string | null;
  onRefreshed: (response: AuthResponse) => void;
  onExpired: () => void;
}

let hooks: SessionHooks = {
  getAccessToken: () => null,
  onRefreshed: () => undefined,
  onExpired: () => undefined,
};

export function configureHttpClient(next: SessionHooks): void {
  hooks = next;
}

let refreshInFlight: Promise<string | null> | null = null;

/** Exchanges the HttpOnly refresh cookie for a new access token. Returns null if the session has expired. */
export function refreshSession(): Promise<string | null> {
  refreshInFlight ??= axios
    .post<AuthResponse>(`${API_BASE_URL}/auth/refresh`, null, { withCredentials: true })
    .then((response) => {
      hooks.onRefreshed(response.data);
      return response.data.accessToken;
    })
    .catch(() => {
      hooks.onExpired();
      return null;
    })
    .finally(() => {
      refreshInFlight = null;
    });
  return refreshInFlight;
}

http.interceptors.request.use((config) => {
  const token = hooks.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined;
    const isAuthCall = original?.url?.startsWith('/auth/') ?? false;
    if (error.response?.status === 401 && original && !original._retried && !isAuthCall) {
      original._retried = true;
      const token = await refreshSession();
      if (token) {
        original.headers.Authorization = `Bearer ${token}`;
        return http(original);
      }
    }
    const problem = toProblem(error);
    if (problem.status >= 500 || problem.code === 'NETWORK_ERROR') {
      logger.error('API request failed', {
        url: original?.url,
        status: problem.status,
        code: problem.code,
        correlationId: problem.correlationId,
      });
    }
    return Promise.reject(problem);
  },
);

const STATUS_CODES: Record<number, ErrorCode> = {
  400: 'VALIDATION_FAILED',
  401: 'UNAUTHORIZED',
  403: 'FORBIDDEN',
  404: 'NOT_FOUND',
  409: 'CONFLICT',
  413: 'FILE_TOO_LARGE',
  429: 'RATE_LIMITED',
  503: 'AI_UNAVAILABLE',
};

/** Normalises any error (problem response, other HTTP error, network failure) into a Problem. */
export function toProblem(error: unknown): Problem {
  // AxiosError also has `code` and `status`, so it must be checked before the Problem shape.
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const data: unknown = error.response?.data;
    if (status && isProblemBody(data)) {
      return { ...data, status };
    }
    if (status) {
      return {
        status,
        code: STATUS_CODES[status] ?? 'INTERNAL_ERROR',
        detail: status >= 500 ? 'The server could not process the request.' : error.message,
      };
    }
    return {
      status: 0,
      code: 'NETWORK_ERROR',
      detail: 'No connection to the server. Check your internet connection.',
    };
  }
  if (isProblem(error)) {
    return error;
  }
  return {
    status: 0,
    code: 'INTERNAL_ERROR',
    detail: error instanceof Error ? error.message : 'Unexpected error',
  };
}

function isProblemBody(value: unknown): value is Omit<Problem, 'status'> {
  return typeof value === 'object' && value !== null && 'code' in value;
}

export function isProblem(value: unknown): value is Problem {
  return (
    typeof value === 'object' &&
    value !== null &&
    'code' in value &&
    'status' in value &&
    typeof (value as Problem).status === 'number'
  );
}
