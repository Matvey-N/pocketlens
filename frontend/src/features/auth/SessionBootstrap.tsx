import { Box, CircularProgress } from '@mui/material';
import { useEffect, type ReactNode } from 'react';
import { useAppSelector } from '@/app/store';
import { selectSessionStatus } from '@/features/auth/authSlice';
import { refreshSession } from '@/shared/api/httpClient';

/**
 * On start-up, restores the session from the HttpOnly refresh cookie (the access token is not persisted).
 * Children render once it is known whether the user is logged in.
 */
export function SessionBootstrap({ children }: { children: ReactNode }) {
  const status = useAppSelector(selectSessionStatus);
  useEffect(() => {
    if (status === 'unknown') {
      void refreshSession();
    }
  }, [status]);

  if (status === 'unknown') {
    return (
      <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }} aria-busy="true">
        <CircularProgress aria-label="Loading PocketLens" />
      </Box>
    );
  }
  return <>{children}</>;
}
