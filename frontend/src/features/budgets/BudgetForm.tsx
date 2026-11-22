import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Grid, MenuItem, TextField } from '@mui/material';
import { Controller, useForm } from 'react-hook-form';
import type { z } from 'zod';
import { budgetSchema } from '@/features/budgets/budgetSchema';
import type { BudgetRequest, Category } from '@/shared/types/api';

type BudgetFormValues = z.input<typeof budgetSchema>;

interface BudgetFormProps {
  categories: Category[];
  saving: boolean;
  onSubmit: (values: BudgetRequest) => Promise<boolean>;
}

/** Form to add a monthly budget for an expense category (US-3.1). */
export function BudgetForm({ categories, saving, onSubmit }: BudgetFormProps) {
  const { control, handleSubmit, reset, formState } = useForm<BudgetFormValues, unknown, BudgetRequest>({
    resolver: zodResolver(budgetSchema),
    defaultValues: { categoryId: '', monthlyLimit: '', currency: 'EUR' },
  });
  const errors = formState.errors;
  const expenseCategories = categories.filter((c) => c.kind === 'EXPENSE');

  const submit = handleSubmit(async (values) => {
    const ok = await onSubmit({ ...values, currency: values.currency.toUpperCase() });
    if (ok) reset({ categoryId: '', monthlyLimit: '', currency: values.currency.toUpperCase() });
  });

  return (
    <form onSubmit={submit} noValidate aria-label="New budget">
      <Grid container spacing={2} sx={{ alignItems: 'flex-start' }}>
        <Grid size={{ xs: 12, sm: 5 }}>
          <Controller
            name="categoryId"
            control={control}
            render={({ field }) => (
              <TextField
                select
                label="Category"
                {...field}
                error={Boolean(errors.categoryId)}
                helperText={errors.categoryId?.message}
              >
                {expenseCategories.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.name}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
        </Grid>
        <Grid size={{ xs: 7, sm: 3 }}>
          <Controller
            name="monthlyLimit"
            control={control}
            render={({ field }) => (
              <TextField
                label="Monthly limit"
                inputMode="decimal"
                {...field}
                error={Boolean(errors.monthlyLimit)}
                helperText={errors.monthlyLimit?.message}
              />
            )}
          />
        </Grid>
        <Grid size={{ xs: 5, sm: 2 }}>
          <Controller
            name="currency"
            control={control}
            render={({ field }) => (
              <TextField
                label="Currency"
                {...field}
                error={Boolean(errors.currency)}
                helperText={errors.currency?.message}
              />
            )}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 2 }}>
          <Button type="submit" variant="contained" fullWidth loading={saving} sx={{ height: 40 }}>
            Add
          </Button>
        </Grid>
      </Grid>
    </form>
  );
}
