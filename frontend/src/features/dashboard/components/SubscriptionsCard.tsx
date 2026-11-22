import TrendingUp from '@mui/icons-material/TrendingUp';
import { Card, CardContent, Chip, List, ListItem, ListItemText, Skeleton, Typography } from '@mui/material';
import { useSubscriptionsQuery } from '@/features/dashboard/dashboardApi';
import { formatDate, formatMoney } from '@/shared/lib/format';
import { EmptyState } from '@/shared/ui/EmptyState';
import { QueryError } from '@/shared/ui/QueryError';

const CADENCE = { WEEKLY: 'weekly', MONTHLY: 'monthly', YEARLY: 'yearly' } as const;

export function SubscriptionsCard() {
  const { data, isLoading, error, refetch } = useSubscriptionsQuery();
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="h6" component="h2" gutterBottom>
          Subscriptions
        </Typography>
        {isLoading && <Skeleton variant="rounded" height={120} />}
        {error && <QueryError error={error} onRetry={refetch} />}
        {data && data.length === 0 && <EmptyState title="No recurring payments detected yet" />}
        {data && data.length > 0 && (
          <List dense disablePadding>
            {data.map((s) => (
              <ListItem key={`${s.merchant}-${s.currency}`} disableGutters>
                <ListItemText
                  primary={s.merchant}
                  secondary={`${CADENCE[s.cadence]} · next ${formatDate(s.nextExpectedDate)}`}
                />
                {s.priceIncreased && (
                  <Chip size="small" color="warning" icon={<TrendingUp />} label="Price up" sx={{ mr: 1 }} />
                )}
                <Typography sx={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}>
                  {formatMoney(s.lastAmount, s.currency)}
                </Typography>
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
}
