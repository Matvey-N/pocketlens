import {
  filtersChanged,
  filtersReset,
  initialFilters,
  pageChanged,
  selectActiveFilterCount,
  selectTransactionQuery,
  transactionFiltersReducer,
} from '@/features/transactions/transactionFiltersSlice';

describe('transactionFiltersSlice', () => {
  it('resets to the first page when a filter changes', () => {
    let state = transactionFiltersReducer(initialFilters, pageChanged({ page: 3, size: 50 }));
    expect(state.page).toBe(3);
    state = transactionFiltersReducer(state, filtersChanged({ q: 'lidl' }));
    expect(state.page).toBe(0);
    expect(state.size).toBe(50);
    expect(transactionFiltersReducer(state, filtersReset())).toEqual(initialFilters);
  });

  it('maps UI filters to API query parameters', () => {
    const state = transactionFiltersReducer(
      initialFilters,
      filtersChanged({ categoryId: 'none', q: '  cafe ', needsReviewOnly: true, from: '2026-11-01' }),
    );
    const query = selectTransactionQuery({ transactionFilters: state });
    expect(query).toMatchObject({ uncategorized: true, categoryId: undefined, q: 'cafe', needsReview: true });
    expect(selectActiveFilterCount({ transactionFilters: state })).toBe(4);
    // memoised: same input → same object
    expect(selectTransactionQuery({ transactionFilters: state })).toBe(query);
  });
});
