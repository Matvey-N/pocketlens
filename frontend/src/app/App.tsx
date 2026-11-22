import { CssBaseline, ThemeProvider } from '@mui/material';
import { useMemo } from 'react';
import { RouterProvider } from 'react-router-dom';
import { createRouter } from '@/app/router';
import { useAppSelector } from '@/app/store';
import { selectThemeMode } from '@/app/uiSlice';
import { buildTheme } from '@/app/theme';
import { SessionBootstrap } from '@/features/auth/SessionBootstrap';
import { Notifier } from '@/features/notifications/Notifier';
import { ErrorBoundary } from '@/shared/ui/ErrorBoundary';

const router = createRouter();

export function App() {
  const mode = useAppSelector(selectThemeMode);
  const theme = useMemo(() => buildTheme(mode), [mode]);
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline enableColorScheme />
      <ErrorBoundary>
        <SessionBootstrap>
          <RouterProvider router={router} />
        </SessionBootstrap>
      </ErrorBoundary>
      <Notifier />
    </ThemeProvider>
  );
}
