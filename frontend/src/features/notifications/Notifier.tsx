import { Alert, Snackbar } from '@mui/material';
import { useAppDispatch, useAppSelector } from '@/app/store';
import { dismiss, selectCurrentNotification } from '@/features/notifications/notificationsSlice';

/** Shows global notifications (success messages and API errors) one at a time. */
export function Notifier() {
  const dispatch = useAppDispatch();
  const current = useAppSelector(selectCurrentNotification);
  if (!current) return null;
  const close = () => dispatch(dismiss(current.id));
  return (
    <Snackbar
      key={current.id}
      open
      autoHideDuration={current.severity === 'error' ? 8000 : 4000}
      onClose={(_, reason) => reason !== 'clickaway' && close()}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
    >
      <Alert severity={current.severity} onClose={close} variant="filled" sx={{ width: '100%' }}>
        {current.message}
        {current.correlationId && ` (reference ${current.correlationId.slice(0, 8)})`}
      </Alert>
    </Snackbar>
  );
}
