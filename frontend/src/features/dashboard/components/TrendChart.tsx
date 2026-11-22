import { Box, Card, CardContent, Typography, useTheme } from '@mui/material';
import { memo, useMemo } from 'react';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { formatMoney, formatMonth } from '@/shared/lib/format';
import type { CurrencySummary } from '@/shared/types/api';

/** Six-month bar chart of expenses vs income. */
export const TrendChart = memo(function TrendChart({ summary }: { summary: CurrencySummary }) {
  const theme = useTheme();
  const data = useMemo(
    () => summary.trend.map((point) => ({ ...point, label: formatMonth(point.month) })),
    [summary.trend],
  );
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="h6" component="h2" gutterBottom>
          Last six months
        </Typography>
        <Box sx={{ height: 240 }} role="img" aria-label="Bar chart of monthly expenses and income">
          <ResponsiveContainer>
            <BarChart data={data} margin={{ left: 8, right: 8 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={theme.palette.divider} />
              <XAxis dataKey="label" tick={{ fill: theme.palette.text.secondary, fontSize: 12 }} />
              <YAxis tick={{ fill: theme.palette.text.secondary, fontSize: 12 }} width={56} />
              <Tooltip formatter={(value) => formatMoney(Number(value), summary.currency)} />
              <Legend />
              <Bar
                dataKey="expenses"
                name="Expenses"
                fill={theme.palette.primary.main}
                radius={[4, 4, 0, 0]}
              />
              <Bar dataKey="income" name="Income" fill={theme.palette.secondary.main} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
});
