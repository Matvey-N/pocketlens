import { zodResolver } from '@hookform/resolvers/zod';
import DeleteOutline from '@mui/icons-material/DeleteOutline';
import {
  Alert,
  Box,
  Button,
  Chip,
  Grid,
  IconButton,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  TextField,
  Tooltip,
} from '@mui/material';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import {
  useCategoriesQuery,
  useCreateCategoryMutation,
  useDeleteCategoryMutation,
} from '@/features/transactions/transactionsApi';
import type { Problem } from '@/shared/types/api';

const schema = z.object({
  name: z.string().trim().min(1, 'Enter a name').max(40, 'At most 40 characters'),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Choose a colour'),
  kind: z.enum(['EXPENSE', 'INCOME', 'TRANSFER']),
});
type CategoryFormValues = z.infer<typeof schema>;

/** Custom categories (US-2.4). Default categories are shown but cannot be changed by users. */
export function CategoryManager() {
  const { data: categories = [] } = useCategoriesQuery();
  const [create, createState] = useCreateCategoryMutation();
  const [remove] = useDeleteCategoryMutation();
  const { control, handleSubmit, reset, formState } = useForm<CategoryFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', color: '#8D6E63', kind: 'EXPENSE' },
  });

  const submit = handleSubmit(async (values) => {
    const result = await create(values);
    if ('data' in result) reset({ name: '', color: values.color, kind: values.kind });
  });
  const problem = createState.error as Problem | undefined;
  const custom = categories.filter((c) => !c.isDefault);

  return (
    <Box>
      <form onSubmit={submit} noValidate aria-label="New category">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 5 }}>
            <Controller
              name="name"
              control={control}
              render={({ field }) => (
                <TextField
                  label="New category"
                  {...field}
                  error={Boolean(formState.errors.name)}
                  helperText={formState.errors.name?.message}
                />
              )}
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 3 }}>
            <Controller
              name="kind"
              control={control}
              render={({ field }) => (
                <TextField select label="Kind" {...field}>
                  <MenuItem value="EXPENSE">Expense</MenuItem>
                  <MenuItem value="INCOME">Income</MenuItem>
                  <MenuItem value="TRANSFER">Transfer</MenuItem>
                </TextField>
              )}
            />
          </Grid>
          <Grid size={{ xs: 3, sm: 2 }}>
            <Controller
              name="color"
              control={control}
              render={({ field }) => <TextField type="color" label="Colour" {...field} />}
            />
          </Grid>
          <Grid size={{ xs: 3, sm: 2 }}>
            <Button
              type="submit"
              variant="contained"
              fullWidth
              loading={createState.isLoading}
              sx={{ height: 40 }}
            >
              Add
            </Button>
          </Grid>
        </Grid>
      </form>
      {problem && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {problem.detail}
        </Alert>
      )}
      <List dense sx={{ mt: 1 }}>
        {custom.map((c) => (
          <ListItem
            key={c.id}
            disableGutters
            secondaryAction={
              <Tooltip title="Delete category">
                <IconButton
                  edge="end"
                  aria-label={`Delete category ${c.name}`}
                  onClick={() => void remove(c.id)}
                >
                  <DeleteOutline />
                </IconButton>
              </Tooltip>
            }
          >
            <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: c.color, mr: 1.5 }} />
            <ListItemText primary={c.name} secondary={c.kind.toLowerCase()} />
          </ListItem>
        ))}
      </List>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
        {categories
          .filter((c) => c.isDefault)
          .map((c) => (
            <Chip key={c.id} size="small" label={c.name} variant="outlined" />
          ))}
      </Box>
    </Box>
  );
}
