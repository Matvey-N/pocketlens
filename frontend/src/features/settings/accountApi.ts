import { api } from '@/shared/api/baseApi';
import type { User } from '@/shared/types/api';

export const accountApi = api.injectEndpoints({
  endpoints: (build) => ({
    me: build.query<User, void>({
      query: () => ({ url: '/users/me' }),
      providesTags: ['Me'],
    }),
    exportData: build.mutation<unknown, void>({
      query: () => ({ url: '/users/me/export' }),
    }),
    deleteAccount: build.mutation<void, { password: string }>({
      query: (body) => ({ url: '/users/me', method: 'DELETE', data: body }),
    }),
  }),
});

export const { useMeQuery, useExportDataMutation, useDeleteAccountMutation } = accountApi;
