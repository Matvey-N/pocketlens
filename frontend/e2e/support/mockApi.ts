import type { Page, Route } from '@playwright/test';

export const user = {
  id: 'b3f4e5d6-a7b8-4c9d-8e0f-1a2b3c4d5e6f',
  email: 'anna@example.com',
  displayName: 'Anna',
  role: 'USER',
  privacyNoticeVersion: '2026-11',
  createdAt: '2026-11-20T08:00:00Z',
};

const auth = { accessToken: 'e2e-token', tokenType: 'Bearer', expiresIn: 900, user };

function json(route: Route, body: unknown, status = 200) {
  return route.fulfill({
    status,
    contentType: status >= 400 ? 'application/problem+json' : 'application/json',
    body: JSON.stringify(body),
  });
}

function currentMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

export const dashboard = () => ({
  month: currentMonth(),
  currencies: [
    {
      currency: 'EUR',
      expenses: 1284.35,
      income: 2500,
      net: 1215.65,
      byCategory: [
        { categoryId: 'c4', name: 'Housing', color: '#795548', amount: 650, count: 1 },
        { categoryId: 'c1', name: 'Groceries', color: '#4CAF50', amount: 284.1, count: 17 },
      ],
      trend: [{ month: currentMonth(), expenses: 1284.35, income: 2500 }],
    },
  ],
  budgets: [
    {
      budgetId: 'b1',
      categoryId: 'c1',
      categoryName: 'Groceries',
      categoryColor: '#4CAF50',
      currency: 'EUR',
      limit: 300,
      spent: 284.1,
      remaining: 15.9,
      projectedSpending: 387.41,
      percentUsed: 95,
      health: 'AT_RISK',
      projectedOverrunDate: `${currentMonth()}-23`,
    },
  ],
  needsReviewCount: 2,
});

/**
 * Installs API mocks. `loggedIn` decides whether the refresh cookie restores a session on page load.
 */
export async function mockApi(page: Page, { loggedIn }: { loggedIn: boolean }) {
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/api/v1', '');
    const method = request.method();

    if (path === '/auth/refresh') {
      return loggedIn ? json(route, auth) : json(route, { status: 401, code: 'INVALID_REFRESH_TOKEN' }, 401);
    }
    if (path === '/auth/login' && method === 'POST') {
      const body = request.postDataJSON() as { password: string };
      return body.password === 'Secret12345'
        ? json(route, auth)
        : json(
            route,
            { status: 401, code: 'INVALID_CREDENTIALS', detail: 'E-mail or password is incorrect' },
            401,
          );
    }
    if (path === '/dashboard') return json(route, dashboard());
    if (path === '/subscriptions') {
      return json(route, [
        {
          merchant: 'SPOTIFY',
          currency: 'EUR',
          cadence: 'MONTHLY',
          lastAmount: 11.99,
          previousAmount: 10.99,
          priceIncreased: true,
          monthlyCost: 11.99,
          lastPaymentDate: '2026-11-03',
          nextExpectedDate: '2026-12-03',
          occurrences: 4,
          transactionIds: [],
        },
      ]);
    }
    if (path === '/imports/formats') {
      return json(route, [
        {
          id: 'generic',
          name: 'PocketLens template',
          delimiter: ',',
          requiredColumns: ['Date', 'Description', 'Amount', 'Currency'],
        },
      ]);
    }
    if (path === '/imports/preview') {
      return json(route, {
        delimiter: ',',
        headers: ['Date', 'Description', 'Amount', 'Currency'],
        rows: [['2026-11-01', 'LIDL', '-12.30', 'EUR']],
        matchingFormats: ['generic'],
      });
    }
    if (path === '/imports' && method === 'POST') {
      return json(
        route,
        {
          batchId: 'batch-1',
          fileName: 'statement.csv',
          format: 'PocketLens template',
          rowsRead: 8,
          imported: 8,
          duplicates: 0,
          skipped: 0,
          categorizedByRules: 6,
          categorizedByAi: 1,
          needsReview: 1,
          uncategorized: 0,
          aiAvailable: true,
        },
        201,
      );
    }
    if (path === '/imports' || path === '/import-profiles' || path === '/conversations')
      return json(route, []);
    if (path === '/conversations' && method === 'POST') {
      return json(
        route,
        { id: 'conv-1', title: null, createdAt: '2026-11-22T09:30:00Z', updatedAt: '2026-11-22T09:30:00Z' },
        201,
      );
    }
    return json(route, { status: 404, code: 'NOT_FOUND', detail: `No mock for ${method} ${path}` }, 404);
  });
}
