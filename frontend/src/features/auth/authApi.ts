import { api } from '@/shared/api/baseApi';
import type { AuthResponse, LoginRequest, PrivacyNotice, RegisterRequest } from '@/shared/types/api';

export const authApi = api.injectEndpoints({
  endpoints: (build) => ({
    privacyNotice: build.query<PrivacyNotice, void>({
      query: () => ({ url: '/privacy-notice' }),
    }),
    login: build.mutation<AuthResponse, LoginRequest>({
      query: (body) => ({ url: '/auth/login', method: 'POST', data: body }),
    }),
    register: build.mutation<AuthResponse, RegisterRequest>({
      query: (body) => ({ url: '/auth/register', method: 'POST', data: body }),
    }),
    logout: build.mutation<void, void>({
      query: () => ({ url: '/auth/logout', method: 'POST' }),
    }),
  }),
});

export const { usePrivacyNoticeQuery, useLoginMutation, useRegisterMutation, useLogoutMutation } = authApi;
