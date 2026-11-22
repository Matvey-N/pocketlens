import { CssBaseline, ThemeProvider } from '@mui/material';
import { render, type RenderOptions } from '@testing-library/react';
import type { ReactElement, ReactNode } from 'react';
import { Provider } from 'react-redux';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { createStore, type RootState } from '@/app/store';
import { buildTheme } from '@/app/theme';
import type { User } from '@/shared/types/api';

export const testUser: User = {
  id: 'b3f4e5d6-a7b8-4c9d-8e0f-1a2b3c4d5e6f',
  email: 'anna@example.com',
  displayName: 'Anna',
  role: 'USER',
  privacyNoticeVersion: '2026-11',
  createdAt: '2026-11-20T08:00:00Z',
};

interface Options extends Omit<RenderOptions, 'wrapper'> {
  preloadedState?: Partial<RootState>;
  route?: string;
  /** Extra routes rendered next to the component under test (e.g. a redirect target). */
  extraRoutes?: { path: string; element: ReactNode }[];
  path?: string;
}

/** Renders a component with Redux store, theme and router like the real application. */
export function renderWithProviders(ui: ReactElement, options: Options = {}) {
  const { preloadedState, route = '/', extraRoutes = [], path = '*', ...rest } = options;
  const store = createStore(preloadedState);
  const result = render(
    <Provider store={store}>
      <ThemeProvider theme={buildTheme('light')}>
        <CssBaseline />
        <MemoryRouter initialEntries={[route]}>
          <Routes>
            <Route path={path} element={ui} />
            {extraRoutes.map((r) => (
              <Route key={r.path} path={r.path} element={r.element} />
            ))}
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    </Provider>,
    rest,
  );
  return { store, ...result };
}

export const authenticatedState: Partial<RootState> = {
  auth: { status: 'authenticated', accessToken: 'test-token', user: testUser },
};
