import { Card, CardContent, Grid, Typography } from '@mui/material';
import type { CurrencySummary } from '@/shared/types/api';
import { Money } from '@/shared/ui/Money';

/** Expenses, income and net of one currency. */
export function SummaryCards({ summary }: { summary: CurrencySummary }) {
  const cards = [
    { label: 'Spent', value: summary.expenses },
    { label: 'Income', value: summary.income },
    { label: 'Net', value: summary.net, signed: true },
  ];
  return (
    <Grid container spacing={2}>
      {cards.map((card) => (
        <Grid key={card.label} size={{ xs: 12, sm: 4 }}>
          <Card>
            <CardContent>
              <Typography variant="body2" color="text.secondary">
                {card.label} · {summary.currency}
              </Typography>
              <Typography variant="h5" component="p" sx={{ mt: 0.5 }}>
                <Money amount={card.value} currency={summary.currency} signed={card.signed} />
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
}
