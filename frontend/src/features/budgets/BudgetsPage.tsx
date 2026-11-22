import DeleteOutline from '@mui/icons-material/DeleteOutline';
import { Alert, Card, CardContent, IconButton, Stack, Tooltip, Typography } from '@mui/material';
import { useState } from 'react';
import { useAppDispatch } from '@/app/store';
import { BudgetForm } from '@/features/budgets/BudgetForm';
import { BudgetProgress } from '@/features/budgets/BudgetProgress';
import {
  useBudgetsQuery,
  useCreateBudgetMutation,
  useDeleteBudgetMutation,
} from '@/features/budgets/budgetsApi';
import { notify } from '@/features/notifications/notificationsSlice';
import { useCategoriesQuery } from '@/features/transactions/transactionsApi';
import { currentMonth } from '@/shared/lib/format';
import type { BudgetRequest, BudgetStatus, Problem } from '@/shared/types/api';
import { ConfirmDialog } from '@/shared/ui/ConfirmDialog';
import { EmptyState } from '@/shared/ui/EmptyState';
import { TableSkeleton } from '@/shared/ui/LoadingSkeleton';
import { MonthPicker } from '@/shared/ui/MonthPicker';
import { PageHeader } from '@/shared/ui/PageHeader';
import { QueryError } from '@/shared/ui/QueryError';

export default function BudgetsPage() {
  const dispatch = useAppDispatch();
  const [month, setMonth] = useState(currentMonth());
  const { data, isLoading, error, refetch } = useBudgetsQuery(month);
  const { data: categories = [] } = useCategoriesQuery();
  const [createBudget, createState] = useCreateBudgetMutation();
  const [deleteBudget, deleteState] = useDeleteBudgetMutation();
  const [toDelete, setToDelete] = useState<BudgetStatus | null>(null);

  const create = async (values: BudgetRequest) => {
    const result = await createBudget(values);
    if ('data' in result) {
      dispatch(notify({ severity: 'success', message: 'Budget added.' }));
      return true;
    }
    return false;
  };

  const confirmDelete = async () => {
    if (!toDelete) return;
    await deleteBudget(toDelete.budgetId);
    setToDelete(null);
  };

  const createProblem = createState.error as Problem | undefined;

  return (
    <>
      <PageHeader
        title="Budgets"
        subtitle="Monthly limits with an early warning before you overspend"
        actions={<MonthPicker value={month} onChange={setMonth} />}
      />
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" component="h2" gutterBottom>
            New budget
          </Typography>
          <BudgetForm categories={categories} saving={createState.isLoading} onSubmit={create} />
          {createProblem && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {createProblem.code === 'CONFLICT'
                ? 'A budget for this category and currency already exists.'
                : createProblem.detail}
            </Alert>
          )}
        </CardContent>
      </Card>
      {error && <QueryError error={error} onRetry={refetch} />}
      {isLoading && <TableSkeleton rows={3} />}
      {data && (
        <Card>
          <CardContent>
            {data.length === 0 ? (
              <EmptyState
                title="No budgets yet"
                description="Add a limit for a category you want to keep an eye on."
              />
            ) : (
              <Stack spacing={3}>
                {data.map((b) => (
                  <Stack key={b.budgetId} direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
                    <Stack sx={{ flexGrow: 1 }}>
                      <BudgetProgress budget={b} />
                    </Stack>
                    <Tooltip title="Delete budget">
                      <IconButton
                        aria-label={`Delete budget ${b.categoryName}`}
                        onClick={() => setToDelete(b)}
                      >
                        <DeleteOutline />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                ))}
              </Stack>
            )}
          </CardContent>
        </Card>
      )}
      <ConfirmDialog
        open={Boolean(toDelete)}
        title="Delete budget?"
        message={`The budget for ${toDelete?.categoryName ?? ''} will be removed. Transactions are not affected.`}
        confirmLabel="Delete"
        destructive
        loading={deleteState.isLoading}
        onConfirm={confirmDelete}
        onClose={() => setToDelete(null)}
      />
    </>
  );
}
