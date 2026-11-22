import { api } from '@/shared/api/baseApi';
import type { AdminStats, Category, Rule } from '@/shared/types/api';

export const adminApi = api.injectEndpoints({
  endpoints: (build) => ({
    adminStats: build.query<AdminStats, void>({
      query: () => ({ url: '/admin/stats' }),
      providesTags: ['Admin'],
    }),
    adminRules: build.query<Rule[], void>({
      query: () => ({ url: '/admin/rules' }),
      providesTags: ['Admin'],
    }),
    adminCategories: build.query<Category[], void>({
      query: () => ({ url: '/admin/categories' }),
      providesTags: ['Admin'],
    }),
    createGlobalRule: build.mutation<Rule, { pattern: string; categoryId: string }>({
      query: (body) => ({ url: '/admin/rules', method: 'POST', data: body }),
      invalidatesTags: ['Admin'],
    }),
    deleteGlobalRule: build.mutation<void, string>({
      query: (id) => ({ url: `/admin/rules/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Admin'],
    }),
  }),
});

export const {
  useAdminStatsQuery,
  useAdminRulesQuery,
  useAdminCategoriesQuery,
  useCreateGlobalRuleMutation,
  useDeleteGlobalRuleMutation,
} = adminApi;
