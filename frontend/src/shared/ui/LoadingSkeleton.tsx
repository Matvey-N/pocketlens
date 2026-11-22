import { Card, CardContent, Grid, Skeleton, Stack } from '@mui/material';

/** Placeholder shown while data loads, shaped like the content (cards, table rows, chart). */
export function CardsSkeleton({ count = 3 }: { count?: number }) {
  return (
    <Grid container spacing={2} aria-busy="true" aria-label="Loading">
      {Array.from({ length: count }, (_, i) => (
        <Grid key={i} size={{ xs: 12, md: 12 / count }}>
          <Card>
            <CardContent>
              <Skeleton width="40%" />
              <Skeleton variant="text" sx={{ fontSize: '2rem' }} width="70%" />
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
}

export function TableSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <Stack spacing={1} aria-busy="true" aria-label="Loading">
      {Array.from({ length: rows }, (_, i) => (
        <Skeleton key={i} variant="rounded" height={40} />
      ))}
    </Stack>
  );
}

export function ChartSkeleton() {
  return <Skeleton variant="rounded" height={260} aria-busy="true" aria-label="Loading chart" />;
}
