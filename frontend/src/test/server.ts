import { setupServer } from 'msw/node';

/** Mock API server for unit and component tests; tests register handlers per case. */
export const server = setupServer();
