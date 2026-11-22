import { api } from '@/shared/api/baseApi';
import type { Dashboard, Subscription } from '@/shared/types/api';

export const dashboardApi = api.injectEndpoints({
  endpoints: (build) => ({
    dashboard: build.query<Dashboard, string>({
      query: (month) => ({ url: '/dashboard', params: { month } }),
      providesTags: ['Dashboard'],
    }),
    subscriptions: build.query<Subscription[], void>({
      query: () => ({ url: '/subscriptions' }),
      providesTags: ['Subscriptions'],
    }),
  }),
});

export const { useDashboardQuery, useSubscriptionsQuery } = dashboardApi;
