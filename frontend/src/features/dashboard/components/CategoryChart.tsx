import { Box, Card, CardContent, List, ListItem, ListItemText, Typography } from '@mui/material';
import { memo } from 'react';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { formatMoney } from '@/shared/lib/format';
import type { CurrencySummary } from '@/shared/types/api';
import { EmptyState } from '@/shared/ui/EmptyState';

/** Donut chart of spending by category with an accessible legend list. Memoised: re-renders only on new data. */
export const CategoryChart = memo(function CategoryChart({ summary }: { summary: CurrencySummary }) {
  const data = summary.byCategory;
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="h6" component="h2" gutterBottom>
          Spending by category
        </Typography>
        {data.length === 0 ? (
          <EmptyState title="No spending in this month" />
        ) : (
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: '220px 1fr' },
              gap: 2,
              alignItems: 'center',
            }}
          >
            <Box sx={{ height: 220 }} role="img" aria-label="Donut chart of spending by category">
              <ResponsiveContainer>
                <PieChart>
                  <Pie
                    data={data}
                    dataKey="amount"
                    nameKey="name"
                    innerRadius="55%"
                    outerRadius="90%"
                    paddingAngle={1}
                  >
                    {data.map((slice) => (
                      <Cell key={slice.name} fill={slice.color} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => formatMoney(Number(value), summary.currency)} />
                </PieChart>
              </ResponsiveContainer>
            </Box>
            <List dense disablePadding aria-label="Categories">
              {data.slice(0, 8).map((slice) => (
                <ListItem key={slice.name} disableGutters>
                  <Box
                    sx={{
                      width: 10,
                      height: 10,
                      borderRadius: '50%',
                      bgcolor: slice.color,
                      mr: 1.5,
                      flexShrink: 0,
                    }}
                  />
                  <ListItemText primary={slice.name} secondary={`${slice.count} transactions`} />
                  <Typography sx={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}>
                    {formatMoney(slice.amount, summary.currency)}
                  </Typography>
                </ListItem>
              ))}
            </List>
          </Box>
        )}
      </CardContent>
    </Card>
  );
});
