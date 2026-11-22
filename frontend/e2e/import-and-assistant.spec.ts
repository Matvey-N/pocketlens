import { expect, test } from '@playwright/test';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { mockApi } from './support/mockApi';

const here = path.dirname(fileURLToPath(import.meta.url));

test('imports a statement and shows the summary', async ({ page }) => {
  await mockApi(page, { loggedIn: true });
  await page.goto('/import');

  await page.getByTestId('file-input').setInputFiles(path.join(here, 'fixtures', 'statement.csv'));
  await expect(page.getByText('statement.csv').first()).toBeVisible();
  await page.getByRole('button', { name: 'Import', exact: true }).click();

  await expect(page.getByText('statement.csv imported')).toBeVisible();
  await expect(page.getByText('New transactions')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Review 1 transaction' })).toBeVisible();
});

test('asks the assistant and opens the transactions behind the answer', async ({ page }) => {
  await mockApi(page, { loggedIn: true });
  const answer = {
    id: 'm2',
    role: 'ASSISTANT',
    content: 'In October you spent 284.10 EUR on groceries in 17 purchases.',
    interpretation: ['Spending 2026-10-01 – 2026-10-31, category: Groceries'],
    toolCalls: [],
    sourceTransactionCount: 2,
    createdAt: '2026-11-22T09:31:06Z',
  };
  const question = {
    ...answer,
    id: 'm1',
    role: 'USER',
    content: 'How much did I spend on groceries last month?',
    interpretation: [],
    sourceTransactionCount: 0,
  };
  let asked = false;
  await page.route('**/api/v1/conversations', (route) =>
    route.request().method() === 'POST'
      ? route.fulfill({
          status: 201,
          json: { id: 'conv-1', title: null, createdAt: answer.createdAt, updatedAt: answer.createdAt },
        })
      : route.fulfill({ json: [] }),
  );
  await page.route('**/api/v1/conversations/conv-1', (route) =>
    route.fulfill({
      json: {
        id: 'conv-1',
        title: question.content,
        createdAt: answer.createdAt,
        updatedAt: answer.createdAt,
        messages: asked ? [question, answer] : [],
      },
    }),
  );
  await page.route('**/api/v1/conversations/conv-1/messages', async (route) => {
    asked = true;
    await route.fulfill({ status: 201, json: { question, answer } });
  });
  await page.route('**/api/v1/conversations/conv-1/messages/m2/transactions', (route) =>
    route.fulfill({
      json: {
        transactions: [
          {
            id: 't1',
            date: '2026-10-03',
            amount: -200.0,
            currency: 'EUR',
            description: 'LIDL',
            merchant: 'LIDL VILNIUS',
            category: { id: 'c1', name: 'Groceries', color: '#4CAF50' },
            categorizationSource: 'RULE',
            confidence: 1,
            needsReview: false,
          },
          {
            id: 't2',
            date: '2026-10-20',
            amount: -84.1,
            currency: 'EUR',
            description: 'MAXIMA',
            merchant: 'MAXIMA',
            category: { id: 'c1', name: 'Groceries', color: '#4CAF50' },
            categorizationSource: 'RULE',
            confidence: 1,
            needsReview: false,
          },
        ],
        totals: [{ currency: 'EUR', amount: 284.1, count: 2 }],
      },
    }),
  );

  await page.goto('/assistant');
  await page.getByRole('button', { name: 'How much did I spend on groceries last month?' }).click();

  await expect(page.getByText('In October you spent 284.10 EUR')).toBeVisible();
  await page.getByText('How I understood your question').click();
  await expect(page.getByText(/category: Groceries/)).toBeVisible();
  await page.getByRole('button', { name: 'Show 2 transactions' }).click();
  await expect(page.getByRole('dialog')).toContainText('Total: €284.10 (2)');
  await expect(page.getByRole('dialog')).toContainText('MAXIMA');
});
