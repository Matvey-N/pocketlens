import { http as mswHttp, HttpResponse } from 'msw';
import { configureHttpClient, http, toProblem } from '@/shared/api/httpClient';
import { server } from '@/test/server';

describe('httpClient', () => {
  it('refreshes the session once on 401 and retries the request', async () => {
    let token = 'expired';
    const onRefreshed = vi.fn((r: { accessToken: string }) => {
      token = r.accessToken;
    });
    configureHttpClient({ getAccessToken: () => token, onRefreshed, onExpired: vi.fn() });
    server.use(
      mswHttp.get('*/api/v1/users/me', ({ request }) =>
        request.headers.get('Authorization') === 'Bearer fresh'
          ? HttpResponse.json({ email: 'anna@example.com' })
          : HttpResponse.json({ status: 401, code: 'UNAUTHORIZED' }, { status: 401 }),
      ),
      mswHttp.post('*/api/v1/auth/refresh', () =>
        HttpResponse.json({ accessToken: 'fresh', tokenType: 'Bearer', expiresIn: 900, user: {} }),
      ),
    );

    const response = await http.get('/users/me');

    expect(response.data).toEqual({ email: 'anna@example.com' });
    expect(onRefreshed).toHaveBeenCalledTimes(1);
  });

  it('ends the session when the refresh fails', async () => {
    const onExpired = vi.fn();
    configureHttpClient({ getAccessToken: () => 'expired', onRefreshed: vi.fn(), onExpired });
    server.use(
      mswHttp.get('*/api/v1/users/me', () =>
        HttpResponse.json({ status: 401, code: 'UNAUTHORIZED' }, { status: 401 }),
      ),
      mswHttp.post('*/api/v1/auth/refresh', () =>
        HttpResponse.json({ status: 401, code: 'INVALID_REFRESH_TOKEN' }, { status: 401 }),
      ),
    );

    await expect(http.get('/users/me')).rejects.toMatchObject({ status: 401, code: 'UNAUTHORIZED' });
    expect(onExpired).toHaveBeenCalled();
  });

  it('normalises problem responses, plain HTTP errors and network failures', async () => {
    configureHttpClient({ getAccessToken: () => null, onRefreshed: vi.fn(), onExpired: vi.fn() });
    server.use(
      mswHttp.get('*/api/v1/conflict', () =>
        HttpResponse.json(
          { status: 409, code: 'CONFLICT', detail: 'exists', correlationId: 'abc' },
          { status: 409 },
        ),
      ),
      mswHttp.get('*/api/v1/plain', () => new HttpResponse('oops', { status: 500 })),
      mswHttp.get('*/api/v1/offline', () => HttpResponse.error()),
    );

    await expect(http.get('/conflict')).rejects.toMatchObject({
      status: 409,
      code: 'CONFLICT',
      correlationId: 'abc',
    });
    await expect(http.get('/plain')).rejects.toMatchObject({ status: 500, code: 'INTERNAL_ERROR' });
    await expect(http.get('/offline')).rejects.toMatchObject({ status: 0, code: 'NETWORK_ERROR' });
    expect(toProblem(new Error('boom'))).toMatchObject({ code: 'INTERNAL_ERROR', detail: 'boom' });
  });
});
