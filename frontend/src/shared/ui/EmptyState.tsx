import { Box, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
  icon?: ReactNode;
}

export function EmptyState({ title, description, action, icon }: EmptyStateProps) {
  return (
    <Box sx={{ textAlign: 'center', py: 6, px: 2 }} role="status">
      {icon && <Box sx={{ color: 'text.secondary', mb: 1, '& svg': { fontSize: 48 } }}>{icon}</Box>}
      <Typography variant="h6">{title}</Typography>
      {description && (
        <Typography color="text.secondary" sx={{ mt: 1, maxWidth: 480, mx: 'auto' }}>
          {description}
        </Typography>
      )}
      {action && <Box sx={{ mt: 2 }}>{action}</Box>}
    </Box>
  );
}
