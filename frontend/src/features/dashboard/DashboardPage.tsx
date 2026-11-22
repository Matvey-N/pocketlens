import UploadFileOutlined from '@mui/icons-material/UploadFileOutlined';
import { Alert, Button, Card, CardContent, Grid, Stack, Tab, Tabs, Typography } from '@mui/material';
import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { useAppDispatch } from '@/app/store';
import { BudgetProgress } from '@/features/budgets/BudgetProgress';
import { CategoryChart } from '@/features/dashboard/components/CategoryChart';
import { SubscriptionsCard } from '@/features/dashboard/components/SubscriptionsCard';
import { SummaryCards } from '@/features/dashboard/components/SummaryCards';
import { TrendChart } from '@/features/dashboard/components/TrendChart';
import { useDashboardQuery } from '@/features/dashboard/dashboardApi';
import { filtersChanged, filtersReset } from '@/features/transactions/transactionFiltersSlice';
import { currentMonth } from '@/shared/lib/format';
import { EmptyState } from '@/shared/ui/EmptyState';
import { CardsSkeleton, ChartSkeleton } from '@/shared/ui/LoadingSkeleton';
import { MonthPicker } from '@/shared/ui/MonthPicker';
import { PageHeader } from '@/shared/ui/PageHeader';
import { QueryError } from '@/shared/ui/QueryError';

export default function DashboardPage() {
  const [month, setMonth] = useState(currentMonth());
  const [currencyIndex, setCurrencyIndex] = useState(0);
  const { data, isLoading, isFetching, error, refetch } = useDashboardQuery(month);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const reviewTransactions = () => {
    dispatch(filtersReset());
    dispatch(filtersChanged({ needsReviewOnly: true }));
    navigate('/transactions');
  };

  const summary = data?.currencies[Math.min(currencyIndex, (data?.currencies.length ?? 1) - 1)];
  const warnings = data?.budgets.filter((b) => b.health !== 'ON_TRACK') ?? [];

  return (
    <>
      <PageHeader
        title="Dashboard"
        subtitle="Where your money went this month"
        actions={<MonthPicker value={month} onChange={setMonth} />}
      />
      {error && <QueryError error={error} onRetry={refetch} />}
      {isLoading && (
        <Stack spacing={2}>
          <CardsSkeleton />
          <ChartSkeleton />
        </Stack>
      )}
      {data && (
        <Stack spacing={2} sx={{ opacity: isFetching ? 0.7 : 1, transition: 'opacity 0.2s' }}>
          {data.needsReviewCount > 0 && (
            <Alert
              severity="info"
              action={
                <Button color="inherit" size="small" onClick={reviewTransactions}>
                  Review
                </Button>
              }
            >
              {data.needsReviewCount} transaction{data.needsReviewCount === 1 ? '' : 's'} need your review.
            </Alert>
          )}
          {warnings.map((b) => (
            <Alert key={b.budgetId} severity={b.health === 'EXCEEDED' ? 'error' : 'warning'}>
              <BudgetProgress budget={b} />
            </Alert>
          ))}
          {data.currencies.length === 0 ? (
            <Card>
              <EmptyState
                icon={<UploadFileOutlined />}
                title="No transactions in this month"
                description="Import a CSV statement from your bank to see your spending by category, trends and budgets."
                action={
                  <Button variant="contained" component={RouterLink} to="/import">
                    Import a statement
                  </Button>
                }
              />
            </Card>
          ) : (
            <>
              {data.currencies.length > 1 && (
                <Tabs
                  value={currencyIndex}
                  onChange={(_, value: number) => setCurrencyIndex(value)}
                  aria-label="Currency"
                >
                  {data.currencies.map((c) => (
                    <Tab key={c.currency} label={c.currency} />
                  ))}
                </Tabs>
              )}
              {summary && (
                <>
                  <SummaryCards summary={summary} />
                  <Grid container spacing={2}>
                    <Grid size={{ xs: 12, lg: 6 }}>
                      <CategoryChart summary={summary} />
                    </Grid>
                    <Grid size={{ xs: 12, lg: 6 }}>
                      <TrendChart summary={summary} />
                    </Grid>
                  </Grid>
                </>
              )}
            </>
          )}
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, lg: 6 }}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <Typography variant="h6" component="h2" gutterBottom>
                    Budgets
                  </Typography>
                  {data.budgets.length === 0 ? (
                    <EmptyState
                      title="No budgets yet"
                      action={
                        <Button component={RouterLink} to="/budgets">
                          Set a budget
                        </Button>
                      }
                    />
                  ) : (
                    <Stack spacing={2}>
                      {data.budgets.map((b) => (
                        <BudgetProgress key={b.budgetId} budget={b} />
                      ))}
                    </Stack>
                  )}
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12, lg: 6 }}>
              <SubscriptionsCard />
            </Grid>
          </Grid>
        </Stack>
      )}
    </>
  );
}
