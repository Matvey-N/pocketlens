import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import LoginPage from '@/features/auth/LoginPage';
import { renderWithProviders, testUser } from '@/test/render';
import { server } from '@/test/server';

describe('LoginPage', () => {
  it('validates the form before calling the API', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' });

    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByText('Enter your e-mail')).toBeInTheDocument();
    expect(screen.getByText('Enter your password')).toBeInTheDocument();
  });

  it('logs in, stores the session and opens the dashboard', async () => {
    server.use(
      http.post('*/api/v1/auth/login', async ({ request }) => {
        const body = (await request.json()) as { email: string };
        expect(body.email).toBe('anna@example.com');
        return HttpResponse.json({ accessToken: 'jwt', tokenType: 'Bearer', expiresIn: 900, user: testUser });
      }),
    );
    const user = userEvent.setup();
    const { store } = renderWithProviders(<LoginPage />, {
      route: '/login',
      path: '/login',
      extraRoutes: [{ path: '/', element: <p>Dashboard page</p> }],
    });

    await user.type(screen.getByLabelText(/e-mail/i), 'anna@example.com');
    await user.type(screen.getByLabelText(/password/i), 'Secret12345');
    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByText('Dashboard page')).toBeInTheDocument();
    expect(store.getState().auth.status).toBe('authenticated');
    expect(store.getState().auth.accessToken).toBe('jwt');
  });

  it('shows a clear message for wrong credentials', async () => {
    server.use(
      http.post('*/api/v1/auth/login', () =>
        HttpResponse.json(
          { status: 401, code: 'INVALID_CREDENTIALS', detail: 'E-mail or password is incorrect' },
          { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const user = userEvent.setup();
    const { store } = renderWithProviders(<LoginPage />, { route: '/login', path: '/login' });

    await user.type(screen.getByLabelText(/e-mail/i), 'anna@example.com');
    await user.type(screen.getByLabelText(/password/i), 'wrong-password');
    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail or password is incorrect.');
    await waitFor(() => expect(store.getState().notifications.queue).toHaveLength(0));
  });
});
