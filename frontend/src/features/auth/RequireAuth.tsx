import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAppSelector } from '@/app/store';
import { selectCurrentUser, selectSessionStatus } from '@/features/auth/authSlice';
import type { Role } from '@/shared/types/api';

/** Route guard: anonymous users go to the login page; `role` restricts a route (e.g. admin). */
export function RequireAuth({ children, role }: { children: ReactNode; role?: Role }) {
  const status = useAppSelector(selectSessionStatus);
  const user = useAppSelector(selectCurrentUser);
  const location = useLocation();
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (role && user?.role !== role) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

/** Public-only routes (login, register) redirect logged-in users to the dashboard. */
export function PublicOnly({ children }: { children: ReactNode }) {
  const status = useAppSelector(selectSessionStatus);
  return status === 'authenticated' ? <Navigate to="/" replace /> : <>{children}</>;
}
