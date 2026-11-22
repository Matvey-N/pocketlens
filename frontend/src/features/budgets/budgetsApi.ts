import { api } from '@/shared/api/baseApi';
import type { Budget, BudgetRequest, BudgetStatus } from '@/shared/types/api';

export const budgetsApi = api.injectEndpoints({
  endpoints: (build) => ({
    budgets: build.query<BudgetStatus[], string>({
      query: (month) => ({ url: '/budgets', params: { month } }),
      providesTags: ['Budgets'],
    }),
    createBudget: build.mutation<Budget, BudgetRequest>({
      query: (body) => ({ url: '/budgets', method: 'POST', data: body }),
      invalidatesTags: ['Budgets', 'Dashboard'],
    }),
    updateBudget: build.mutation<Budget, { id: string; monthlyLimit: number }>({
      query: ({ id, monthlyLimit }) => ({ url: `/budgets/${id}`, method: 'PUT', data: { monthlyLimit } }),
      invalidatesTags: ['Budgets', 'Dashboard'],
    }),
    deleteBudget: build.mutation<void, string>({
      query: (id) => ({ url: `/budgets/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Budgets', 'Dashboard'],
    }),
  }),
});

export const { useBudgetsQuery, useCreateBudgetMutation, useUpdateBudgetMutation, useDeleteBudgetMutation } =
  budgetsApi;
