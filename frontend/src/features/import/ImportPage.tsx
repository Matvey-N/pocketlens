import TableChartOutlined from '@mui/icons-material/TableChartOutlined';
import {
  Alert,
  AlertTitle,
  Button,
  Card,
  CardContent,
  Grid,
  List,
  ListItem,
  ListItemText,
  ListSubheader,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch } from '@/app/store';
import { FileDropzone } from '@/features/import/components/FileDropzone';
import { ImportSummaryCard } from '@/features/import/components/ImportSummaryCard';
import { MappingDialog } from '@/features/import/components/MappingDialog';
import {
  useCreateImportProfileMutation,
  useFormatsQuery,
  useImportHistoryQuery,
  useImportProfilesQuery,
  useImportStatementMutation,
  usePreviewStatementMutation,
} from '@/features/import/importApi';
import { notify } from '@/features/notifications/notificationsSlice';
import { filtersChanged, filtersReset } from '@/features/transactions/transactionFiltersSlice';
import { formatDate } from '@/shared/lib/format';
import type { CsvPreview, ImportProfileRequest, Problem } from '@/shared/types/api';
import { PageHeader } from '@/shared/ui/PageHeader';

const PROFILE_PREFIX = 'profile:';

export default function ImportPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [file, setFile] = useState<File | null>(null);
  const [selection, setSelection] = useState('generic');
  const [fileError, setFileError] = useState<string | null>(null);
  const [preview, setPreview] = useState<CsvPreview | null>(null);
  const { data: formats = [] } = useFormatsQuery();
  const { data: profiles = [] } = useImportProfilesQuery();
  const { data: history = [] } = useImportHistoryQuery();
  const [importStatement, importState] = useImportStatementMutation();
  const [previewStatement, previewState] = usePreviewStatementMutation();
  const [createProfile, profileState] = useCreateImportProfileMutation();

  const chooseFile = async (chosen: File) => {
    setFile(chosen);
    setFileError(null);
    importState.reset();
    const result = await previewStatement(chosen);
    if ('data' in result && result.data?.matchingFormats[0]) {
      setSelection(result.data.matchingFormats[0]);
    }
  };

  const submit = async () => {
    if (!file) {
      setFileError('Choose a file first.');
      return;
    }
    const target = selection.startsWith(PROFILE_PREFIX)
      ? { profileId: selection.slice(PROFILE_PREFIX.length) }
      : { format: selection };
    await importStatement({ file, ...target });
  };

  const openMapping = async () => {
    if (!file) {
      setFileError('Choose a file first.');
      return;
    }
    if (previewState.data) {
      setPreview(previewState.data);
      return;
    }
    const result = await previewStatement(file);
    if ('data' in result && result.data) setPreview(result.data);
  };

  const saveMapping = async (profile: ImportProfileRequest) => {
    const result = await createProfile(profile);
    if ('data' in result && result.data) {
      setSelection(`${PROFILE_PREFIX}${result.data.id}`);
      setPreview(null);
      dispatch(notify({ severity: 'success', message: `Mapping "${result.data.name}" saved.` }));
    }
  };

  const review = () => {
    dispatch(filtersReset());
    dispatch(filtersChanged({ needsReviewOnly: true }));
    navigate('/transactions');
  };

  const problem = importState.error as Problem | undefined;

  return (
    <>
      <PageHeader title="Import" subtitle="Upload a CSV statement from any bank" />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 7 }}>
          <Card>
            <CardContent>
              <Stack spacing={2}>
                <FileDropzone file={file} onFile={chooseFile} onError={setFileError} />
                {fileError && <Alert severity="warning">{fileError}</Alert>}
                <TextField
                  select
                  label="Bank format"
                  value={selection}
                  onChange={(e) => setSelection(e.target.value)}
                  helperText={
                    previewState.data && previewState.data.matchingFormats.length === 0
                      ? 'No built-in format matches this file — map its columns once.'
                      : 'Detected automatically when possible'
                  }
                >
                  <ListSubheader>Built-in formats</ListSubheader>
                  {formats.map((f) => (
                    <MenuItem key={f.id} value={f.id}>
                      {f.name}
                    </MenuItem>
                  ))}
                  {profiles.length > 0 && <ListSubheader>Your mappings</ListSubheader>}
                  {profiles.map((p) => (
                    <MenuItem key={p.id} value={`${PROFILE_PREFIX}${p.id}`}>
                      {p.name}
                    </MenuItem>
                  ))}
                </TextField>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                  <Button variant="contained" size="large" onClick={submit} loading={importState.isLoading}>
                    Import
                  </Button>
                  <Button startIcon={<TableChartOutlined />} onClick={openMapping} disabled={!file}>
                    My bank is not listed
                  </Button>
                </Stack>
                {problem && (
                  <Alert severity="error" role="alert">
                    <AlertTitle>Nothing was imported</AlertTitle>
                    {problem.detail}
                    {problem.missingColumns && (
                      <Typography variant="body2" sx={{ mt: 1 }}>
                        Columns in your file: {problem.foundColumns?.join(', ')}
                      </Typography>
                    )}
                    {problem.rowErrors && (
                      <List dense>
                        {problem.rowErrors.map((r) => (
                          <ListItem key={r.row} disableGutters>
                            <ListItemText primary={`Row ${r.row}: ${r.message}`} />
                          </ListItem>
                        ))}
                      </List>
                    )}
                  </Alert>
                )}
              </Stack>
            </CardContent>
          </Card>
          {importState.data && (
            <Stack sx={{ mt: 2 }}>
              <ImportSummaryCard summary={importState.data} onReview={review} />
            </Stack>
          )}
        </Grid>
        <Grid size={{ xs: 12, lg: 5 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" component="h2" gutterBottom>
                Recent imports
              </Typography>
              {history.length === 0 ? (
                <Typography color="text.secondary">No imports yet.</Typography>
              ) : (
                <List dense disablePadding>
                  {history.map((h) => (
                    <ListItem key={h.id} disableGutters divider>
                      <ListItemText
                        primary={h.fileName}
                        secondary={`${formatDate(h.createdAt)} · ${h.imported} new · ${h.duplicates} duplicates`}
                      />
                    </ListItem>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      <MappingDialog
        preview={preview}
        saving={profileState.isLoading}
        error={profileState.error as Problem | undefined}
        onClose={() => setPreview(null)}
        onSave={saveMapping}
      />
    </>
  );
}
