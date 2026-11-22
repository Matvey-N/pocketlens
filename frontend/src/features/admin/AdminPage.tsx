import DeleteOutline from '@mui/icons-material/DeleteOutline';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Grid,
  IconButton,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import {
  useAdminCategoriesQuery,
  useAdminRulesQuery,
  useAdminStatsQuery,
  useCreateGlobalRuleMutation,
  useDeleteGlobalRuleMutation,
} from '@/features/admin/adminApi';
import type { Problem } from '@/shared/types/api';
import { CardsSkeleton } from '@/shared/ui/LoadingSkeleton';
import { PageHeader } from '@/shared/ui/PageHeader';

/** Administration: usage statistics (US-7.2) and global merchant rules (US-7.1). */
export default function AdminPage() {
  const { data: stats, isLoading } = useAdminStatsQuery();
  const { data: rules = [] } = useAdminRulesQuery();
  const { data: categories = [] } = useAdminCategoriesQuery();
  const [createRule, createState] = useCreateGlobalRuleMutation();
  const [deleteRule] = useDeleteGlobalRuleMutation();
  const [pattern, setPattern] = useState('');
  const [categoryId, setCategoryId] = useState('');

  const add = async () => {
    const result = await createRule({ pattern, categoryId });
    if ('data' in result) setPattern('');
  };

  const figures = stats
    ? [
        { label: 'Users', value: stats.users },
        { label: 'Transactions', value: stats.transactions },
        { label: 'Assistant answers (30 days)', value: stats.assistantAnswersLast30Days },
        {
          label: 'AI tokens in / out (30 days)',
          value: `${stats.inputTokensLast30Days} / ${stats.outputTokensLast30Days}`,
        },
      ]
    : [];
  const problem = createState.error as Problem | undefined;

  return (
    <>
      <PageHeader title="Administration" subtitle="Usage and global categorisation rules" />
      {isLoading && <CardsSkeleton count={4} />}
      {stats && !stats.aiEnabled && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          AI features are disabled: no AI_API_KEY is configured on the server.
        </Alert>
      )}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        {figures.map((f) => (
          <Grid key={f.label} size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent>
                <Typography variant="body2" color="text.secondary">
                  {f.label}
                </Typography>
                <Typography variant="h5" component="p">
                  {f.value}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
      <Card>
        <CardContent>
          <Typography variant="h6" component="h2" gutterBottom>
            Global merchant rules
          </Typography>
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid size={{ xs: 12, sm: 5 }}>
              <TextField
                label="Merchant contains"
                value={pattern}
                onChange={(e) => setPattern(e.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 5 }}>
              <TextField
                select
                label="Category"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
              >
                {categories.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 2 }}>
              <Button
                variant="contained"
                fullWidth
                sx={{ height: 40 }}
                disabled={pattern.trim().length < 2 || !categoryId}
                loading={createState.isLoading}
                onClick={add}
              >
                Add rule
              </Button>
            </Grid>
          </Grid>
          {problem && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {problem.detail}
            </Alert>
          )}
          <TableContainer>
            <Table size="small" aria-label="Global rules">
              <TableHead>
                <TableRow>
                  <TableCell>Pattern</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rules.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell>{r.pattern}</TableCell>
                    <TableCell>{r.categoryName}</TableCell>
                    <TableCell align="right">
                      <IconButton
                        aria-label={`Delete rule ${r.pattern}`}
                        onClick={() => void deleteRule(r.id)}
                      >
                        <DeleteOutline />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </>
  );
}
