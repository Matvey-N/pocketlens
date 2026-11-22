import { expect, test } from '@playwright/test';
import { mockApi } from './support/mockApi';

test('anonymous user logs in and sees the dashboard with a budget warning', async ({ page }) => {
  await mockApi(page, { loggedIn: false });
  await page.goto('/');

  await expect(page).toHaveURL(/\/login$/);
  await page.getByLabel('E-mail').fill('anna@example.com');
  await page.getByLabel('Password').fill('wrong-password');
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page.getByRole('alert')).toContainText('E-mail or password is incorrect');

  await page.getByLabel('Password').fill('Secret12345');
  await page.getByRole('button', { name: 'Log in' }).click();

  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
  await expect(page.getByText('€1,284.35')).toBeVisible();
  await expect(page.getByText('2 transactions need your review.')).toBeVisible();
  await expect(page.getByText(/At this pace the limit is reached on/).first()).toBeVisible();
  await expect(page.getByText('Price up')).toBeVisible();
});
