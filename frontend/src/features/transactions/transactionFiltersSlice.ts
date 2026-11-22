import { createSelector, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { TransactionQuery, TransactionSort } from '@/shared/types/api';

export interface TransactionFilters {
  from: string;
  to: string;
  categoryId: string;
  q: string;
  currency: string;
  needsReviewOnly: boolean;
  sort: TransactionSort;
  page: number;
  size: number;
}

export const initialFilters: TransactionFilters = {
  from: '',
  to: '',
  categoryId: '',
  q: '',
  currency: '',
  needsReviewOnly: false,
  sort: 'date,desc',
  page: 0,
  size: 20,
};

/**
 * Filters of the transaction list are global state, so they survive navigation (e.g. dashboard → "review" link
 * → back) and can be set from other pages.
 */
const transactionFiltersSlice = createSlice({
  name: 'transactionFilters',
  initialState: initialFilters,
  reducers: {
    filtersChanged(state, action: PayloadAction<Partial<Omit<TransactionFilters, 'page'>>>) {
      return { ...state, ...action.payload, page: 0 };
    },
    pageChanged(state, action: PayloadAction<{ page: number; size?: number }>) {
      state.page = action.payload.page;
      if (action.payload.size) state.size = action.payload.size;
    },
    filtersReset() {
      return initialFilters;
    },
  },
});

export const { filtersChanged, pageChanged, filtersReset } = transactionFiltersSlice.actions;
export const transactionFiltersReducer = transactionFiltersSlice.reducer;

const selectFilters = (state: { transactionFilters: TransactionFilters }) => state.transactionFilters;

/** Memoised conversion of UI filters into API query parameters. */
export const selectTransactionQuery = createSelector([selectFilters], (f): TransactionQuery => ({
  from: f.from || undefined,
  to: f.to || undefined,
  categoryId: f.categoryId && f.categoryId !== 'none' ? f.categoryId : undefined,
  uncategorized: f.categoryId === 'none' ? true : undefined,
  needsReview: f.needsReviewOnly ? true : undefined,
  q: f.q.trim() || undefined,
  currency: f.currency || undefined,
  page: f.page,
  size: f.size,
  sort: f.sort,
}));

export const selectActiveFilterCount = createSelector(
  [selectFilters],
  (f) => [f.from, f.to, f.categoryId, f.q, f.currency, f.needsReviewOnly ? 'x' : ''].filter(Boolean).length,
);

export { selectFilters as selectTransactionFilters };
