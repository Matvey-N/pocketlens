import { Box, LinearProgress, Stack, Typography } from '@mui/material';
import { formatDate, formatMoney } from '@/shared/lib/format';
import type { BudgetStatus } from '@/shared/types/api';

const HEALTH_COLOR = { ON_TRACK: 'success', AT_RISK: 'warning', EXCEEDED: 'error' } as const;

/** One budget: progress bar, spent/limit and the early warning with the projected overrun date. */
export function BudgetProgress({ budget }: { budget: BudgetStatus }) {
  const label =
    budget.health === 'EXCEEDED'
      ? `Exceeded by ${formatMoney(-budget.remaining, budget.currency)}`
      : budget.health === 'AT_RISK' && budget.projectedOverrunDate
        ? `At this pace the limit is reached on ${formatDate(budget.projectedOverrunDate)}`
        : `${formatMoney(budget.remaining, budget.currency)} left`;
  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.5, gap: 1 }}>
        <Typography sx={{ fontWeight: 600 }}>{budget.categoryName}</Typography>
        <Typography sx={{ fontVariantNumeric: 'tabular-nums' }}>
          {formatMoney(budget.spent, budget.currency)} / {formatMoney(budget.limit, budget.currency)}
        </Typography>
      </Stack>
      <LinearProgress
        variant="determinate"
        value={Math.min(budget.percentUsed, 100)}
        color={HEALTH_COLOR[budget.health]}
        sx={{ height: 8, borderRadius: 4 }}
        aria-label={`${budget.categoryName}: ${budget.percentUsed}% used`}
      />
      <Typography
        variant="body2"
        sx={{ mt: 0.5 }}
        color={budget.health === 'ON_TRACK' ? 'text.secondary' : `${HEALTH_COLOR[budget.health]}.main`}
      >
        {label}
      </Typography>
    </Box>
  );
}
