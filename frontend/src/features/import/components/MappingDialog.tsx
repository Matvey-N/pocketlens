import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  MenuItem,
  TextField,
  Typography,
} from '@mui/material';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { z } from 'zod';
import type { CsvPreview, ImportProfileRequest, Problem } from '@/shared/types/api';

const schema = z
  .object({
    name: z.string().trim().min(1, 'Name the mapping').max(60),
    dateColumn: z.string().min(1, 'Choose the date column'),
    dateFormat: z.string().trim().min(1, 'Enter the date format'),
    descriptionColumn: z.string().min(1, 'Choose the description column'),
    amountMode: z.enum(['single', 'split']),
    amountColumn: z.string(),
    debitColumn: z.string(),
    creditColumn: z.string(),
    currencyColumn: z.string(),
    defaultCurrency: z.string(),
    decimalSeparator: z.enum(['.', ',']),
  })
  .superRefine((v, ctx) => {
    if (v.amountMode === 'single' && !v.amountColumn) {
      ctx.addIssue({ code: 'custom', path: ['amountColumn'], message: 'Choose the amount column' });
    }
    if (v.amountMode === 'split' && (!v.debitColumn || !v.creditColumn)) {
      ctx.addIssue({ code: 'custom', path: ['debitColumn'], message: 'Choose debit and credit columns' });
    }
    if (!v.currencyColumn && !/^[A-Z]{3}$/.test(v.defaultCurrency)) {
      ctx.addIssue({
        code: 'custom',
        path: ['defaultCurrency'],
        message: 'Choose a currency column or a 3-letter code',
      });
    }
  });
type MappingForm = z.infer<typeof schema>;

interface MappingDialogProps {
  preview: CsvPreview | null;
  saving: boolean;
  error?: Problem;
  onClose: () => void;
  onSave: (profile: ImportProfileRequest) => void;
}

/** Map the columns of an unknown bank format once (US-1.4). */
export function MappingDialog({ preview, saving, error, onClose, onSave }: MappingDialogProps) {
  const headers = preview?.headers ?? [];
  const { control, handleSubmit, formState } = useForm<MappingForm>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      dateColumn: '',
      dateFormat: 'yyyy-MM-dd',
      descriptionColumn: '',
      amountMode: 'single',
      amountColumn: '',
      debitColumn: '',
      creditColumn: '',
      currencyColumn: '',
      defaultCurrency: 'EUR',
      decimalSeparator: '.',
    },
  });
  const amountMode = useWatch({ control, name: 'amountMode' });
  const errors = formState.errors;

  const submit = handleSubmit((v) =>
    onSave({
      name: v.name,
      delimiter: preview?.delimiter ?? ',',
      dateColumn: v.dateColumn,
      dateFormat: v.dateFormat,
      descriptionColumn: v.descriptionColumn,
      amountColumn: v.amountMode === 'single' ? v.amountColumn : null,
      debitColumn: v.amountMode === 'split' ? v.debitColumn : null,
      creditColumn: v.amountMode === 'split' ? v.creditColumn : null,
      currencyColumn: v.currencyColumn || null,
      defaultCurrency: v.currencyColumn ? null : v.defaultCurrency,
      decimalSeparator: v.decimalSeparator,
    }),
  );

  const columnSelect = (name: keyof MappingForm, label: string, optional = false) => (
    <Controller
      name={name}
      control={control}
      render={({ field }) => (
        <TextField
          select
          label={label}
          {...field}
          error={Boolean(errors[name])}
          helperText={errors[name]?.message}
        >
          {optional && <MenuItem value="">— none —</MenuItem>}
          {headers.map((h) => (
            <MenuItem key={h} value={h}>
              {h}
            </MenuItem>
          ))}
        </TextField>
      )}
    />
  );

  return (
    <Dialog open={Boolean(preview)} onClose={onClose} fullWidth maxWidth="sm" aria-labelledby="mapping-title">
      <form onSubmit={submit} noValidate>
        <DialogTitle id="mapping-title">Map the columns of your bank</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error.detail}
            </Alert>
          )}
          {preview && preview.rows[0] && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              First row: {preview.rows[0].join(' | ')}
            </Typography>
          )}
          <Grid container spacing={2} sx={{ pt: 1 }}>
            <Grid size={12}>
              <Controller
                name="name"
                control={control}
                render={({ field }) => (
                  <TextField
                    label="Mapping name (e.g. your bank)"
                    {...field}
                    error={Boolean(errors.name)}
                    helperText={errors.name?.message}
                  />
                )}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>{columnSelect('dateColumn', 'Date column')}</Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <Controller
                name="dateFormat"
                control={control}
                render={({ field }) => (
                  <TextField
                    label="Date format"
                    {...field}
                    error={Boolean(errors.dateFormat)}
                    helperText={errors.dateFormat?.message ?? 'e.g. yyyy-MM-dd or dd.MM.yyyy'}
                  />
                )}
              />
            </Grid>
            <Grid size={12}>{columnSelect('descriptionColumn', 'Description column')}</Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <Controller
                name="amountMode"
                control={control}
                render={({ field }) => (
                  <TextField select label="Amounts" {...field}>
                    <MenuItem value="single">One column (negative = expense)</MenuItem>
                    <MenuItem value="split">Separate debit and credit columns</MenuItem>
                  </TextField>
                )}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <Controller
                name="decimalSeparator"
                control={control}
                render={({ field }) => (
                  <TextField select label="Decimal separator" {...field}>
                    <MenuItem value=".">Point (12.50)</MenuItem>
                    <MenuItem value=",">Comma (12,50)</MenuItem>
                  </TextField>
                )}
              />
            </Grid>
            {amountMode === 'single' ? (
              <Grid size={12}>{columnSelect('amountColumn', 'Amount column')}</Grid>
            ) : (
              <>
                <Grid size={{ xs: 12, sm: 6 }}>{columnSelect('debitColumn', 'Debit (money out)')}</Grid>
                <Grid size={{ xs: 12, sm: 6 }}>{columnSelect('creditColumn', 'Credit (money in)')}</Grid>
              </>
            )}
            <Grid size={{ xs: 12, sm: 6 }}>{columnSelect('currencyColumn', 'Currency column', true)}</Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <Controller
                name="defaultCurrency"
                control={control}
                render={({ field }) => (
                  <TextField
                    label="Currency if no column"
                    {...field}
                    onChange={(e) => field.onChange(e.target.value.toUpperCase())}
                    error={Boolean(errors.defaultCurrency)}
                    helperText={errors.defaultCurrency?.message}
                  />
                )}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" loading={saving}>
            Save mapping
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
