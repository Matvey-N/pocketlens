import { Button } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { EmptyState } from '@/shared/ui/EmptyState';

export default function NotFoundPage() {
  return (
    <EmptyState
      title="Page not found"
      description="The page you are looking for does not exist."
      action={
        <Button variant="contained" component={RouterLink} to="/">
          Go to dashboard
        </Button>
      }
    />
  );
}
