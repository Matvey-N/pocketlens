import { Alert, AlertTitle, Box, Button } from '@mui/material';
import { Component, type ErrorInfo, type ReactNode } from 'react';
import { logger } from '@/shared/lib/logger';

interface ErrorBoundaryProps {
  children: ReactNode;
  /** Changing this value (e.g. the route) resets the boundary. */
  resetKey?: string;
}

interface ErrorBoundaryState {
  error: Error | null;
}

/**
 * Catches rendering errors so one broken widget does not blank the whole application, logs them and lets the
 * user retry.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    logger.error('Rendering error', { message: error.message, componentStack: info.componentStack });
  }

  componentDidUpdate(previous: ErrorBoundaryProps): void {
    if (previous.resetKey !== this.props.resetKey && this.state.error) {
      this.setState({ error: null });
    }
  }

  render() {
    if (this.state.error) {
      return (
        <Box sx={{ p: 3 }}>
          <Alert
            severity="error"
            action={
              <Button color="inherit" size="small" onClick={() => this.setState({ error: null })}>
                Try again
              </Button>
            }
          >
            <AlertTitle>This part of the page could not be shown</AlertTitle>
            Please try again. If the problem persists, reload the page.
          </Alert>
        </Box>
      );
    }
    return this.props.children;
  }
}
