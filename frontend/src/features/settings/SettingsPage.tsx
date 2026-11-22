import DeleteOutline from '@mui/icons-material/DeleteOutline';
import Download from '@mui/icons-material/Download';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Grid,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/store';
import { selectCurrentUser, sessionEnded } from '@/features/auth/authSlice';
import { notify } from '@/features/notifications/notificationsSlice';
import { CategoryManager } from '@/features/settings/CategoryManager';
import { useDeleteAccountMutation, useExportDataMutation } from '@/features/settings/accountApi';
import { useDeleteRuleMutation, useRulesQuery } from '@/features/transactions/transactionsApi';
import { api } from '@/shared/api/baseApi';
import type { Problem } from '@/shared/types/api';
import { PageHeader } from '@/shared/ui/PageHeader';

function downloadJson(data: unknown, fileName: string) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

export default function SettingsPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const user = useAppSelector(selectCurrentUser);
  const { data: rules = [] } = useRulesQuery();
  const [deleteRule] = useDeleteRuleMutation();
  const [exportData, exportState] = useExportDataMutation();
  const [deleteAccount, deleteState] = useDeleteAccountMutation();
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [password, setPassword] = useState('');

  const doExport = async () => {
    const result = await exportData();
    if ('data' in result) {
      downloadJson(result.data, `pocketlens-export-${new Date().toISOString().slice(0, 10)}.json`);
      dispatch(notify({ severity: 'success', message: 'Your data was exported.' }));
    }
  };

  const doDelete = async () => {
    const result = await deleteAccount({ password });
    if (!('error' in result)) {
      dispatch(sessionEnded());
      dispatch(api.util.resetApiState());
      navigate('/login', { replace: true });
    }
  };

  const deleteProblem = deleteState.error as Problem | undefined;

  return (
    <>
      <PageHeader title="Settings" subtitle="Categories, rules and your data" />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 6 }}>
          <Card sx={{ mb: 2 }}>
            <CardContent>
              <Typography variant="h6" component="h2">
                Profile
              </Typography>
              <Typography color="text.secondary">
                {user?.displayName} · {user?.email}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Privacy notice accepted: version {user?.privacyNoticeVersion}
              </Typography>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" gutterBottom>
                Categories
              </Typography>
              <CategoryManager />
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <Card sx={{ mb: 2 }}>
            <CardContent>
              <Typography variant="h6" component="h2">
                Your merchant rules
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Created when you choose “Always use this category”. New imports use them without AI.
              </Typography>
              {rules.length === 0 ? (
                <Typography color="text.secondary">No rules yet.</Typography>
              ) : (
                <List dense>
                  {rules.map((r) => (
                    <ListItem
                      key={r.id}
                      disableGutters
                      secondaryAction={
                        <IconButton
                          edge="end"
                          aria-label={`Delete rule ${r.pattern}`}
                          onClick={() => void deleteRule(r.id)}
                        >
                          <DeleteOutline />
                        </IconButton>
                      }
                    >
                      <ListItemText primary={r.pattern} secondary={`→ ${r.categoryName}`} />
                    </ListItem>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" gutterBottom>
                Your data
              </Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                <Button
                  startIcon={<Download />}
                  variant="outlined"
                  onClick={doExport}
                  loading={exportState.isLoading}
                >
                  Export all data (JSON)
                </Button>
                <Button
                  startIcon={<DeleteOutline />}
                  color="error"
                  variant="outlined"
                  onClick={() => setDeleteOpen(true)}
                >
                  Delete account
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      <Dialog open={deleteOpen} onClose={() => setDeleteOpen(false)} aria-labelledby="delete-account-title">
        <DialogTitle id="delete-account-title">Delete your account?</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            All transactions, budgets, rules and conversations will be deleted permanently. Enter your
            password to confirm.
          </DialogContentText>
          {deleteProblem && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {deleteProblem.code === 'INVALID_CREDENTIALS'
                ? 'The password is incorrect.'
                : deleteProblem.detail}
            </Alert>
          )}
          <TextField
            type="password"
            label="Password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteOpen(false)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            disabled={!password}
            loading={deleteState.isLoading}
            onClick={doDelete}
          >
            Delete permanently
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
