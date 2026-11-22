import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests run against the production build (`vite preview`). The API is mocked per test with
 * `page.route`, so the tests exercise the real UI, routing, state management and error handling without a backend.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'desktop',
      use: {
        ...devices['Desktop Chrome'],
        launchOptions: { executablePath: process.env.PW_CHROMIUM_EXECUTABLE || undefined },
      },
    },
    {
      name: 'mobile',
      use: {
        ...devices['Pixel 7'],
        launchOptions: { executablePath: process.env.PW_CHROMIUM_EXECUTABLE || undefined },
      },
    },
  ],
  webServer: {
    command: 'npm run preview',
    url: 'http://localhost:4173',
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
});
