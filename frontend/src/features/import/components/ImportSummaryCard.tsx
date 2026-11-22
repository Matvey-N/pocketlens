import CheckCircleOutline from '@mui/icons-material/CheckCircleOutline';
import { Alert, Button, Card, CardContent, Grid, Stack, Typography } from '@mui/material';
import type { ImportSummary } from '@/shared/types/api';

interface ImportSummaryCardProps {
  summary: ImportSummary;
  onReview: () => void;
}

/** Result of an import (UC-1 step 7): read / new / duplicates / need review. */
export function ImportSummaryCard({ summary, onReview }: ImportSummaryCardProps) {
  const figures = [
    { label: 'Rows read', value: summary.rowsRead },
    { label: 'New transactions', value: summary.imported },
    { label: 'Already imported', value: summary.duplicates },
    { label: 'Need review', value: summary.needsReview },
  ];
  return (
    <Card aria-live="polite">
      <CardContent>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 2 }}>
          <CheckCircleOutline color="success" />
          <Typography variant="h6" component="h2">
            {summary.fileName} imported
          </Typography>
        </Stack>
        <Grid container spacing={2}>
          {figures.map((f) => (
            <Grid key={f.label} size={{ xs: 6, sm: 3 }}>
              <Typography variant="h4" component="p">
                {f.value}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {f.label}
              </Typography>
            </Grid>
          ))}
        </Grid>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
          Categorised automatically: {summary.categorizedByRules} by your rules, {summary.categorizedByAi} by
          AI.
          {summary.skipped > 0 && ` ${summary.skipped} rows were skipped (not booked or zero amount).`}
        </Typography>
        {!summary.aiAvailable && (
          <Alert severity="info" sx={{ mt: 2 }}>
            AI categorisation is temporarily unavailable. Unknown merchants are marked for review and will be
            categorised automatically later.
          </Alert>
        )}
        {summary.needsReview > 0 && (
          <Button variant="contained" sx={{ mt: 2 }} onClick={onReview}>
            Review {summary.needsReview} transaction{summary.needsReview === 1 ? '' : 's'}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
