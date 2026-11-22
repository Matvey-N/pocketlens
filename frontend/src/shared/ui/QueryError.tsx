import { Alert, Button } from '@mui/material';
import type { Problem } from '@/shared/types/api';

interface QueryErrorProps {
  error: unknown;
  onRetry?: () => void;
}

/** Inline error for a failed query with a retry button and the correlation id for support. */
export function QueryError({ error, onRetry }: QueryErrorProps) {
  const problem = error as Partial<Problem> | undefined;
  return (
    <Alert
      severity="error"
      action={
        onRetry && (
          <Button color="inherit" size="small" onClick={onRetry}>
            Retry
          </Button>
        )
      }
    >
      {problem?.detail ?? 'The data could not be loaded.'}
      {problem?.correlationId && ` (reference ${problem.correlationId.slice(0, 8)})`}
    </Alert>
  );
}
