import { zodResolver } from '@hookform/resolvers/zod';
import {
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { formatMoney } from '@/shared/lib/format';
import type { Category, Transaction } from '@/shared/types/api';

const schema = z.object({
  categoryId: z.string().min(1, 'Choose a category'),
  applyToMerchant: z.boolean(),
});
export type CategoryForm = z.infer<typeof schema>;

interface CategoryDialogProps {
  transaction: Transaction | null;
  categories: Category[];
  saving: boolean;
  onClose: () => void;
  onSave: (values: CategoryForm) => void;
}

/** Correct the category of a transaction; optionally remember it for the merchant (US-2.3). */
export function CategoryDialog({ transaction, categories, saving, onClose, onSave }: CategoryDialogProps) {
  const { control, handleSubmit, reset, formState } = useForm<CategoryForm>({
    resolver: zodResolver(schema),
    defaultValues: { categoryId: '', applyToMerchant: true },
  });

  useEffect(() => {
    if (transaction) reset({ categoryId: transaction.category?.id ?? '', applyToMerchant: true });
  }, [transaction, reset]);

  return (
    <Dialog
      open={Boolean(transaction)}
      onClose={onClose}
      fullWidth
      maxWidth="xs"
      aria-labelledby="category-title"
    >
      <form onSubmit={handleSubmit(onSave)} noValidate>
        <DialogTitle id="category-title">Change category</DialogTitle>
        <DialogContent>
          {transaction && (
            <Stack spacing={2} sx={{ pt: 1 }}>
              <Typography>
                <strong>{transaction.merchant}</strong> ·{' '}
                {formatMoney(transaction.amount, transaction.currency)}
              </Typography>
              <Controller
                name="categoryId"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="Category"
                    {...field}
                    error={Boolean(formState.errors.categoryId)}
                    helperText={formState.errors.categoryId?.message}
                  >
                    {categories.map((c) => (
                      <MenuItem key={c.id} value={c.id}>
                        {c.name}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
              <Controller
                name="applyToMerchant"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    control={
                      <Checkbox checked={field.value} onChange={(e) => field.onChange(e.target.checked)} />
                    }
                    label={`Always use this category for ${transaction.merchant}`}
                  />
                )}
              />
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" loading={saving}>
            Save
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
