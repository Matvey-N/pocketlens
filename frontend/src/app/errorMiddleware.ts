import { isRejectedWithValue, type Middleware } from '@reduxjs/toolkit';
import { notify } from '@/features/notifications/notificationsSlice';
import { isProblem } from '@/shared/api/httpClient';
import type { ErrorCode } from '@/shared/types/api';

/**
 * Errors shown next to the field or on the page that caused them; everything else (403, 429, 500, 503, network)
 * becomes a global toast. 401 is handled by the session refresh in the HTTP client.
 */
const HANDLED_INLINE: ReadonlySet<ErrorCode> = new Set<ErrorCode>([
  'VALIDATION_FAILED',
  'INVALID_CREDENTIALS',
  'CONFLICT',
  'NOT_FOUND',
  'IMPORT_COLUMNS_MISSING',
  'IMPORT_ROWS_INVALID',
  'IMPORT_FILE_INVALID',
  'PROMPT_REJECTED',
  'UNAUTHORIZED',
  'INVALID_REFRESH_TOKEN',
]);

const FRIENDLY: Partial<Record<ErrorCode, string>> = {
  FORBIDDEN: 'You do not have permission for this action.',
  RATE_LIMITED: 'Too many requests — please wait a moment.',
  NETWORK_ERROR: 'No connection to the server.',
};

export const errorMiddleware: Middleware = (store) => (next) => (action) => {
  if (isRejectedWithValue(action) && isProblem(action.payload)) {
    const problem = action.payload;
    if (!HANDLED_INLINE.has(problem.code)) {
      store.dispatch(
        notify({
          severity: 'error',
          message: FRIENDLY[problem.code] ?? problem.detail ?? 'Something went wrong.',
          correlationId: problem.status >= 500 ? problem.correlationId : undefined,
        }),
      );
    }
  }
  return next(action);
};
