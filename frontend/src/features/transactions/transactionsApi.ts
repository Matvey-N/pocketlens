import { api, cleanParams } from '@/shared/api/baseApi';
import type {
  Category,
  CategoryRequest,
  Page,
  Rule,
  Transaction,
  TransactionQuery,
  UpdateTransactionRequest,
  UpdateTransactionResponse,
} from '@/shared/types/api';

export const transactionsApi = api.injectEndpoints({
  endpoints: (build) => ({
    transactions: build.query<Page<Transaction>, TransactionQuery>({
      query: (query) => ({ url: '/transactions', params: cleanParams(query) }),
      providesTags: ['Transactions'],
    }),
    updateTransactionCategory: build.mutation<
      UpdateTransactionResponse,
      { id: string } & UpdateTransactionRequest
    >({
      query: ({ id, ...body }) => ({ url: `/transactions/${id}`, method: 'PATCH', data: body }),
      invalidatesTags: ['Transactions', 'Dashboard', 'Rules', 'Subscriptions'],
    }),
    categories: build.query<Category[], void>({
      query: () => ({ url: '/categories' }),
      providesTags: ['Categories'],
    }),
    createCategory: build.mutation<Category, CategoryRequest>({
      query: (body) => ({ url: '/categories', method: 'POST', data: body }),
      invalidatesTags: ['Categories'],
    }),
    deleteCategory: build.mutation<void, string>({
      query: (id) => ({ url: `/categories/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Categories', 'Transactions', 'Dashboard', 'Budgets', 'Rules'],
    }),
    rules: build.query<Rule[], void>({
      query: () => ({ url: '/rules' }),
      providesTags: ['Rules'],
    }),
    deleteRule: build.mutation<void, string>({
      query: (id) => ({ url: `/rules/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Rules'],
    }),
  }),
});

export const {
  useTransactionsQuery,
  useUpdateTransactionCategoryMutation,
  useCategoriesQuery,
  useCreateCategoryMutation,
  useDeleteCategoryMutation,
  useRulesQuery,
  useDeleteRuleMutation,
} = transactionsApi;
