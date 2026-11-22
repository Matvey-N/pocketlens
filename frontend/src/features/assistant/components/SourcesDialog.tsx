import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  List,
  ListItem,
  ListItemText,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material';
import { useAnswerSourcesQuery } from '@/features/assistant/assistantApi';
import { formatDate, formatMoney } from '@/shared/lib/format';
import { Money } from '@/shared/ui/Money';
import { QueryError } from '@/shared/ui/QueryError';

interface SourcesDialogProps {
  conversationId: string;
  messageId: string | null;
  onClose: () => void;
}

/** "Show list": the transactions an answer is based on and their totals (US-4.3). */
export function SourcesDialog({ conversationId, messageId, onClose }: SourcesDialogProps) {
  const { data, isLoading, error, refetch } = useAnswerSourcesQuery(
    { conversationId, messageId: messageId ?? '' },
    { skip: !messageId },
  );
  return (
    <Dialog
      open={Boolean(messageId)}
      onClose={onClose}
      fullWidth
      maxWidth="sm"
      aria-labelledby="sources-title"
    >
      <DialogTitle id="sources-title">Transactions behind this answer</DialogTitle>
      <DialogContent dividers>
        {isLoading && <Skeleton variant="rounded" height={160} />}
        {error && <QueryError error={error} onRetry={refetch} />}
        {data && (
          <>
            <Stack direction="row" spacing={2} sx={{ mb: 1, flexWrap: 'wrap' }}>
              {data.totals.map((t) => (
                <Typography key={t.currency} sx={{ fontWeight: 600 }}>
                  Total: {formatMoney(t.amount, t.currency)} ({t.count})
                </Typography>
              ))}
            </Stack>
            <List dense>
              {data.transactions.map((t) => (
                <ListItem key={t.id} disableGutters divider>
                  <ListItemText
                    primary={t.merchant}
                    secondary={`${formatDate(t.date)} · ${t.category?.name ?? 'Uncategorised'}`}
                  />
                  <Money amount={t.amount} currency={t.currency} signed />
                </ListItem>
              ))}
            </List>
          </>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}
