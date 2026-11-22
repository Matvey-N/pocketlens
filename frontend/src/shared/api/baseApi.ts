import { createApi, type BaseQueryFn } from '@reduxjs/toolkit/query/react';
import type { AxiosRequestConfig } from 'axios';
import { http, toProblem } from '@/shared/api/httpClient';
import type { Problem } from '@/shared/types/api';

export interface RequestArgs {
  url: string;
  method?: AxiosRequestConfig['method'];
  data?: unknown;
  params?: Record<string, unknown>;
  headers?: Record<string, string>;
}

/** RTK Query base query backed by the shared axios instance (token, refresh, error normalisation). */
const axiosBaseQuery: BaseQueryFn<RequestArgs, unknown, Problem> = async ({
  url,
  method = 'GET',
  data,
  params,
  headers,
}) => {
  try {
    const response = await http.request({ url, method, data, params, headers });
    return { data: response.data };
  } catch (error) {
    return { error: toProblem(error) };
  }
};

/**
 * Root API slice. Feature modules add their endpoints with `api.injectEndpoints`, so each feature owns its
 * requests while caching and invalidation stay central.
 */
export const api = createApi({
  reducerPath: 'api',
  baseQuery: axiosBaseQuery,
  tagTypes: [
    'Me',
    'Transactions',
    'Categories',
    'Rules',
    'Budgets',
    'Dashboard',
    'Subscriptions',
    'Imports',
    'Profiles',
    'Conversations',
    'Admin',
  ],
  endpoints: () => ({}),
});

/** Removes undefined/empty values so they are not sent as query parameters. */
export function cleanParams<T extends object>(params: T): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}
