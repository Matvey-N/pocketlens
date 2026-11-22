import ReceiptLongOutlined from '@mui/icons-material/ReceiptLongOutlined';
import { Box, Button, Card } from '@mui/material';
import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/store';
import { notify } from '@/features/notifications/notificationsSlice';
import { CategoryDialog, type CategoryForm } from '@/features/transactions/components/CategoryDialog';
import { TransactionFilters } from '@/features/transactions/components/TransactionFilters';
import { TransactionTable } from '@/features/transactions/components/TransactionTable';
import {
  filtersChanged,
  pageChanged,
  selectTransactionQuery,
} from '@/features/transactions/transactionFiltersSlice';
import {
  useCategoriesQuery,
  useTransactionsQuery,
  useUpdateTransactionCategoryMutation,
} from '@/features/transactions/transactionsApi';
import type { Transaction } from '@/shared/types/api';
import { EmptyState } from '@/shared/ui/EmptyState';
import { TableSkeleton } from '@/shared/ui/LoadingSkeleton';
import { PageHeader } from '@/shared/ui/PageHeader';
import { QueryError } from '@/shared/ui/QueryError';

export default function TransactionsPage() {
  const dispatch = useAppDispatch();
  const query = useAppSelector(selectTransactionQuery);
  const { data, isLoading, isFetching, error, refetch } = useTransactionsQuery(query);
  const { data: categories = [] } = useCategoriesQuery();
  const [updateCategory, { isLoading: saving }] = useUpdateTransactionCategoryMutation();
  const [editing, setEditing] = useState<Transaction | null>(null);

  const save = async (values: CategoryForm) => {
    if (!editing) return;
    const result = await updateCategory({ id: editing.id, ...values });
    if ('data' in result && result.data) {
      const { ruleCreated, recategorizedCount } = result.data;
      dispatch(
        notify({
          severity: 'success',
          message: ruleCreated
            ? `Saved. ${recategorizedCount} other transaction${recategorizedCount === 1 ? '' : 's'} of this merchant updated; future imports will use this category.`
            : 'Category saved.',
        }),
      );
      setEditing(null);
    }
  };

  return (
    <>
      <PageHeader
        title="Transactions"
        subtitle="Search, filter and correct categories"
        actions={
          <Button variant="contained" component={RouterLink} to="/import">
            Import statement
          </Button>
        }
      />
      <TransactionFilters categories={categories} />
      {error && <QueryError error={error} onRetry={refetch} />}
      {isLoading && <TableSkeleton />}
      {data && data.totalItems === 0 && (
        <Card>
          <EmptyState
            icon={<ReceiptLongOutlined />}
            title="No transactions found"
            description="Change the filters or import a statement."
          />
        </Card>
      )}
      {data && data.totalItems > 0 && (
        <Box sx={{ opacity: isFetching ? 0.6 : 1, transition: 'opacity 0.2s' }}>
          <TransactionTable
            page={data}
            sort={query.sort}
            onSortChange={(sort) => dispatch(filtersChanged({ sort }))}
            onPageChange={(page, size) => dispatch(pageChanged({ page, size }))}
            onEditCategory={setEditing}
          />
        </Box>
      )}
      <CategoryDialog
        transaction={editing}
        categories={categories}
        saving={saving}
        onClose={() => setEditing(null)}
        onSave={save}
      />
    </>
  );
}
