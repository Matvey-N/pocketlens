import { Box, LinearProgress } from '@mui/material';
import { lazy, Suspense, type ReactNode } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { PublicOnly, RequireAuth } from '@/features/auth/RequireAuth';
import { AppLayout } from '@/layout/AppLayout';

// Every page is a separate chunk loaded on first visit (code splitting).
const LoginPage = lazy(() => import('@/features/auth/LoginPage'));
const RegisterPage = lazy(() => import('@/features/auth/RegisterPage'));
const DashboardPage = lazy(() => import('@/features/dashboard/DashboardPage'));
const TransactionsPage = lazy(() => import('@/features/transactions/TransactionsPage'));
const ImportPage = lazy(() => import('@/features/import/ImportPage'));
const BudgetsPage = lazy(() => import('@/features/budgets/BudgetsPage'));
const AssistantPage = lazy(() => import('@/features/assistant/AssistantPage'));
const SettingsPage = lazy(() => import('@/features/settings/SettingsPage'));
const AdminPage = lazy(() => import('@/features/admin/AdminPage'));
const NotFoundPage = lazy(() => import('@/app/NotFoundPage'));

function page(element: ReactNode) {
  return (
    <Suspense
      fallback={
        <Box sx={{ p: 2 }}>
          <LinearProgress aria-label="Loading page" />
        </Box>
      }
    >
      {element}
    </Suspense>
  );
}

export const routes = [
  { path: '/login', element: <PublicOnly>{page(<LoginPage />)}</PublicOnly> },
  { path: '/register', element: <PublicOnly>{page(<RegisterPage />)}</PublicOnly> },
  {
    path: '/',
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: page(<DashboardPage />) },
      { path: 'transactions', element: page(<TransactionsPage />) },
      { path: 'import', element: page(<ImportPage />) },
      { path: 'budgets', element: page(<BudgetsPage />) },
      { path: 'assistant', element: page(<AssistantPage />) },
      { path: 'assistant/:conversationId', element: page(<AssistantPage />) },
      { path: 'settings', element: page(<SettingsPage />) },
      {
        path: 'admin',
        element: <RequireAuth role="ADMIN">{page(<AdminPage />)}</RequireAuth>,
      },
      { path: 'dashboard', element: <Navigate to="/" replace /> },
      { path: '*', element: page(<NotFoundPage />) },
    ],
  },
];

export function createRouter() {
  return createBrowserRouter(routes);
}
