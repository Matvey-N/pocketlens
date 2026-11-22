import { Box, Card, CardContent, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import { Logo } from '@/layout/Logo';

/** Centered card layout for the public pages (login, registration). */
export function AuthCard({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <Box
      sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 2, bgcolor: 'background.default' }}
    >
      <Card sx={{ width: '100%', maxWidth: 440 }}>
        <CardContent sx={{ p: { xs: 3, sm: 4 } }}>
          <Stack spacing={1} sx={{ mb: 3, alignItems: 'center', textAlign: 'center' }}>
            <Logo size={40} />
            <Typography variant="h5" component="h1">
              {title}
            </Typography>
            <Typography color="text.secondary">{subtitle}</Typography>
          </Stack>
          {children}
        </CardContent>
      </Card>
    </Box>
  );
}
