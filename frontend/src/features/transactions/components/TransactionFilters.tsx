import ClearAll from '@mui/icons-material/ClearAll';
import Search from '@mui/icons-material/Search';
import {
  Badge,
  Button,
  Card,
  CardContent,
  FormControlLabel,
  Grid,
  InputAdornment,
  MenuItem,
  Switch,
  TextField,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useAppDispatch, useAppSelector } from '@/app/store';
import {
  filtersChanged,
  filtersReset,
  selectActiveFilterCount,
  selectTransactionFilters,
} from '@/features/transactions/transactionFiltersSlice';
import type { Category } from '@/shared/types/api';

const SEARCH_DEBOUNCE_MS = 350;

/** Search, period, category, currency and "needs review" filters of the transaction list. */
export function TransactionFilters({ categories }: { categories: Category[] }) {
  const dispatch = useAppDispatch();
  const filters = useAppSelector(selectTransactionFilters);
  const activeCount = useAppSelector(selectActiveFilterCount);
  const [search, setSearch] = useState(filters.q);
  const [syncedQuery, setSyncedQuery] = useState(filters.q);

  // When the stored query changes elsewhere (e.g. "Clear filters"), reset the input during render.
  if (filters.q !== syncedQuery) {
    setSyncedQuery(filters.q);
    setSearch(filters.q);
  }

  // Local state keeps typing responsive; the query parameter updates after a short pause.
  useEffect(() => {
    if (search === filters.q) return undefined;
    const timer = window.setTimeout(() => dispatch(filtersChanged({ q: search })), SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(timer);
  }, [search, filters.q, dispatch]);

  return (
    <Card sx={{ mb: 2 }}>
      <CardContent>
        <Grid container spacing={2} sx={{ alignItems: 'center' }}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              label="Search"
              placeholder="Merchant or description"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <Search fontSize="small" />
                    </InputAdornment>
                  ),
                },
              }}
            />
          </Grid>
          <Grid size={{ xs: 6, md: 2 }}>
            <TextField
              label="From"
              type="date"
              value={filters.from}
              onChange={(e) => dispatch(filtersChanged({ from: e.target.value }))}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Grid>
          <Grid size={{ xs: 6, md: 2 }}>
            <TextField
              label="To"
              type="date"
              value={filters.to}
              onChange={(e) => dispatch(filtersChanged({ to: e.target.value }))}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              select
              label="Category"
              value={filters.categoryId}
              onChange={(e) => dispatch(filtersChanged({ categoryId: e.target.value }))}
            >
              <MenuItem value="">All categories</MenuItem>
              <MenuItem value="none">Uncategorised</MenuItem>
              {categories.map((c) => (
                <MenuItem key={c.id} value={c.id}>
                  {c.name}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 6, md: 2 }}>
            <TextField
              select
              label="Currency"
              value={filters.currency}
              onChange={(e) => dispatch(filtersChanged({ currency: e.target.value }))}
            >
              <MenuItem value="">All</MenuItem>
              {['EUR', 'USD', 'GBP', 'PLN', 'SEK'].map((c) => (
                <MenuItem key={c} value={c}>
                  {c}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 6, md: 4 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={filters.needsReviewOnly}
                  onChange={(e) => dispatch(filtersChanged({ needsReviewOnly: e.target.checked }))}
                />
              }
              label="Needs review only"
            />
          </Grid>
          <Grid size={{ xs: 12, md: 'grow' }} sx={{ textAlign: { md: 'right' } }}>
            <Badge badgeContent={activeCount} color="primary">
              <Button
                startIcon={<ClearAll />}
                onClick={() => dispatch(filtersReset())}
                disabled={activeCount === 0}
              >
                Clear filters
              </Button>
            </Badge>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
}
